/*
 *  Copyright (c) 2011-2015 The original author or authors
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *       The Eclipse Public License is available at
 *       http://www.eclipse.org/legal/epl-v10.html
 *
 *       The Apache License v2.0 is available at
 *       http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 */

package io.vertx.ext.mail.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.impl.NoStackTraceThrowable;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.impl.pool.Lease;
import io.vertx.ext.mail.MailConfig;

import java.io.IOException;
import java.util.List;

/**
 * SMTP connection to a server.
 * <p>
 * Encapsulate the NetSocket connection and the data writing/reading
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
class SMTPConnection {

  private static final Logger log = LoggerFactory.getLogger(SMTPConnection.class);

  private final NetSocket ns;
  private final MailConfig config;
  private Lease<SMTPConnection> lease;
  private MultilineParser nsHandler;
  private final Handler<Void> evictionHandler;

  private boolean evicted;
  private boolean socketClosed;
  private boolean shutdown;
  private boolean closing;
  private boolean inuse;
  private boolean quitSent;

  private Handler<String> commandReplyHandler;
  private Handler<Throwable> errorHandler;
  private Handler<AsyncResult<Void>> closeHandler;

  private Capabilities capa = new Capabilities();
  private final ContextInternal context;
  private long expirationTimestamp;

  SMTPConnection(MailConfig config, NetSocket ns, ContextInternal context, Handler<Void> evictionHandler) {
    this.config = config;
    this.ns = ns;
    this.context = context;
    this.evictionHandler = evictionHandler;
  }

  /**
   * Compute the expiration timeout of the connection, relative to the current time.
   *
   * @param config the MailConfig
   * @return the expiration timestamp
   */
  private static long expirationTimestampOf(MailConfig config) {
    long timeout = config.getKeepAliveTimeout();
    return timeout == 0 ? 0L : System.currentTimeMillis() + config.getKeepAliveTimeoutUnit().toMillis(timeout);
  }

  SMTPConnection setLease(Lease<SMTPConnection> lease) {
    this.lease = lease;
    return this;
  }

  boolean isInitialized() {
    return this.nsHandler != null;
  }

  void init(Handler<String> initialReplyHandler) {
    if (nsHandler != null) {
      throw new IllegalStateException("SMTPConnection has been initialized.");
    }
    this.nsHandler = new MultilineParser(buffer -> {
      if (commandReplyHandler == null && !quitSent) {
        handleError(new IllegalStateException("dropping reply arriving after we stopped processing the buffer."));
      } else {
        // make sure we only call the handler once
        Handler<String> currentHandler = commandReplyHandler;
        commandReplyHandler = null;
        if (currentHandler != null) {
          context.emit(buffer.toString(), currentHandler);
        }
      }
    });
    ns.exceptionHandler(this::handleNSException);
    ns.closeHandler(this::handleNSClosed);
    commandReplyHandler = initialReplyHandler;
    this.expirationTimestamp = expirationTimestampOf(config);
    ns.handler(this.nsHandler);
  }

  void handleNSException(Throwable t) {
    if (!socketClosed && !shutdown) {
      // shutdown() clear the handler, so gets a reference on the error handler first.
      Handler<Throwable> handler;
      synchronized (this) {
        handler = errorHandler;
      }
      shutdown();
      // some SMTP servers may not close the TCP connection gracefully
      // https://github.com/vert-x3/vertx-mail-client/issues/175
      if (quitSent) {
        log.debug("got an exception on the netsocket after quit sent", t);
      } else {
        handleError(t, handler);
      }
    } else {
      log.debug("not returning follow-up exception", t);
    }
  }

  boolean isAvailable() {
    return !socketClosed && !shutdown;
  }

  boolean isValid() {
    return (expirationTimestamp == 0 || System.currentTimeMillis() <= expirationTimestamp) && !quitSent;
  }

  void handleNSClosed(Void v) {
    log.trace("handleNSClosed() - socket has been closed");
    socketClosed = true;
    if (!shutdown && !quitSent) {
      handleError(new IOException("socket was closed unexpected."));
      shutdown();
    }
    handleClosed();
  }

  private void handleClosed() {
    if (closeHandler != null) {
      closeHandler.handle(Future.succeededFuture());
      closeHandler = null;
    }
    if (!evicted) {
      evicted = true;
      evictionHandler.handle(null);
      cleanHandlers();
    }
  }

  /**
   * @return the capabilities object
   */
  Capabilities getCapa() {
    return capa;
  }

  /**
   * parse capabilities from the ehlo reply string
   *
   * @param message the capabilities to set
   */
  void parseCapabilities(String message) {
    capa = new Capabilities();
    capa.parseCapabilities(message);
    if (log.isDebugEnabled()) {
      StringBuilder sb = new StringBuilder();
      sb.append("Supported Auth methods: ");
      capa.getCapaAuth().forEach(a -> sb.append(a).append(" "));
      sb.append("\n");
      if (capa.getSize() > 0) {
        sb.append("Max Size: ").append(capa.getSize()).append("\n");
      }
      sb.append("Support STARTTLS: ").append(capa.isStartTLS()).append(", Current connection TLS: ").append(this.isSsl()).append("\n");
      sb.append("Support PIPELINING: ").append(capa.isCapaPipelining()).append("\n");
      log.debug(sb);
    }
  }

  void shutdown() {
    shutdown = true;
    if (!socketClosed) {
      socketClosed = true;
      ns.close();
    }
    handleClosed();
  }

  private void cleanHandlers() {
    errorHandler = null;
    commandReplyHandler = null;
  }

  void writeCommands(List<String> commands, Handler<String> resultHandler) {
    String cmds = String.join("\r\n", commands);
    this.nsHandler.setExpected(commands.size());
    this.write(cmds, r -> {
      try {
        resultHandler.handle(r);
      } finally {
        this.nsHandler.setExpected(1);
      }
    });
  }

  /*
   * write command without masking anything
   */
  void write(String str, Handler<String> commandResultHandler) {
    write(str, -1, commandResultHandler);
  }

  /*
   * write command masking everything after position blank
   */
  void write(String str, int blank, Handler<String> commandResultHandler) {
    this.commandReplyHandler = commandResultHandler;
    checkClosed();
    context.emit(roc -> {
      if (log.isDebugEnabled()) {
        String logStr;
        if (blank >= 0) {
          StringBuilder sb = new StringBuilder();
          for (int i = blank; i < str.length(); i++) {
            sb.append('*');
          }
          logStr = str.substring(0, blank) + sb;
        } else {
          logStr = str;
        }
        // avoid logging large mail body
        if (logStr.length() < 1000) {
          log.debug("command: " + logStr);
        } else {
          log.debug("command: " + logStr.substring(0, 1000) + "...");
        }
      }
      ns.write(str + "\r\n", r -> {
        if (r.failed()) {
          handleNSException(r.cause());
        }
      });
    });
  }

  // write single line not expecting a reply, using drain handler
  void writeLineWithDrainPromise(String str, boolean mayLog, Promise<Void> promise) {
    if (checkClosed()) {
      promise.fail("Connection was closed");
      return;
    }
    if (mayLog) {
      log.debug(str);
    }
    context.emit(roc -> {
      if (ns.writeQueueFull()) {
        ns.drainHandler(v -> {
          // avoid getting confused by being called twice
          ns.drainHandler(null);
          ns.write(str + "\r\n").onComplete(promise);
        });
      } else {
        ns.write(str + "\r\n").onComplete(promise);
      }
    });
  }

  private void handleError(Throwable t) {
    context.emit(t, err -> {
      Handler<Throwable> handler;
      synchronized (SMTPConnection.this) {
        handler = errorHandler;
      }
      if (handler != null) {
        handler.handle(err);
      } else {
        if (log.isDebugEnabled()) {
          log.error(t.getMessage(), t);
        } else {
          log.error(t.getMessage());
        }
      }
    });
  }

  private void handleError(Throwable t, Handler<Throwable> handler) {
    context.emit(t, err -> {
      if (handler != null) {
        handler.handle(err);
      } else {
        if (log.isDebugEnabled()) {
          log.error(t.getMessage(), t);
        } else {
          log.error(t.getMessage());
        }
      }
    });
  }

  boolean isSsl() {
    return ns.isSsl();
  }

  void upgradeToSsl(Handler<AsyncResult<Void>> handler) {
    ns.upgradeToSsl(handler);
  }

  Future<SMTPConnection> returnToPool() {
    log.trace("return to pool");
    Promise<SMTPConnection> promise = context.promise();
    try {
      if (config.isKeepAlive() && !closing) {
        // recycle
        log.trace("recycle for next use");
        cleanHandlers();
        lease.recycle();
        inuse = false;
        expirationTimestamp = expirationTimestampOf(config);
        promise.complete(this);
      } else {
        Promise<Void> p = Promise.promise();
        p.future().onComplete(conn -> {
          handleClosed();
          promise.complete(this);
        });
        quitCloseConnection(p);
      }
    } catch (Exception e) {
      promise.fail(e);
    }
    return promise.future();
  }

  /**
   * send QUIT and close the connection, this operation waits for the success of the quit command but will close the
   * connection on exception as well
   */
  void quitCloseConnection(Promise<Void> promise) {
    quitSent = true;
    inuse = false;
    log.trace("send QUIT to close");
    writeLineWithDrainPromise("QUIT", false, promise);
  }

  private boolean checkClosed() {
    if (isClosed() || shutdown) {
      handleError(new NoStackTraceThrowable("Connection was closed."));
      return true;
    }
    return false;
  }

  void setErrorHandler(Handler<Throwable> newHandler) {
    errorHandler = newHandler;
  }

  void setInUse() {
    inuse = true;
    expirationTimestamp = expirationTimestampOf(config);
  }

  /**
   * close the connection doing a QUIT command first
   */
  void close(Promise<Void> promise) {
    closing = true;
    if (!inuse) {
      log.trace("close by sending quit in close()");
      quitCloseConnection(promise);
    } else {
      this.closeHandler = promise;
      if (quitSent) {
        shutdown();
      }
    }
  }

  /**
   * check if a connection is already closed (this is mostly for unit tests)
   */
  boolean isClosed() {
    return socketClosed;
  }

  /**
   * get the context associated with this connection
   *
   * @return the context
   */
  Context getContext() {
    return context;
  }

  /**
   * Gets the underline NetSocket to the email server.
   *
   * @return the underline NetSocket
   */
  NetSocket getSocket() {
    return ns;
  }

}

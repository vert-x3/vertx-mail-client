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

import io.vertx.core.*;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.core.net.NetSocket;
import io.vertx.core.internal.pool.Lease;
import io.vertx.ext.mail.MailConfig;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * SMTP connection to a server.
 * <p>
 * Encapsulate the NetSocket connection and the data writing/reading
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
public class SMTPConnection {

  private static final Logger log = LoggerFactory.getLogger(SMTPConnection.class);
  private static final Pattern linePattern = Pattern.compile("\r\n");

  private final NetSocket ns;
  private final MailConfig config;
  private Lease<SMTPConnection> lease;
  private MultilineParser nsHandler;
  private final Handler<Void> evictionHandler;

  private boolean evicted;
  private boolean socketClosed;
  private boolean shutdown;
  private boolean inuse;
  private boolean quitSent;

  private Completable<String> commandReplyHandler;
  private Handler<Throwable> exceptionHandler;
  private Completable<Void> closeHandler;
  private Capabilities capa = new Capabilities();
  private final ContextInternal context;
  private long expirationTimestamp;
  private final AtomicLong emailsSent;

  SMTPConnection(MailConfig config, NetSocket ns, ContextInternal context, Handler<Void> evictionHandler) {
    this.config = config;
    this.ns = ns;
    this.context = context;
    this.evictionHandler = evictionHandler;
    this.emailsSent = new AtomicLong(0);
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

  Future<String> init() {
    if (nsHandler != null) {
      return context.failedFuture(new IllegalStateException("SMTPConnection has been initialized."));
    }

    this.nsHandler = new MultilineParser(buffer -> {
      if (commandReplyHandler == null && !quitSent) {
        log.error("dropping reply arriving after we stopped processing the buffer.");
      } else {
        // make sure we only call the handler once
        Completable<String> currentHandler = commandReplyHandler;
        commandReplyHandler = null;
        if (currentHandler != null) {
          currentHandler.succeed(buffer.toString());
        }
      }
    });

    Promise<String> promise = context.promise();
    commandReplyHandler = promise;
    expirationTimestamp = expirationTimestampOf(config);
    ns.handler(this.nsHandler);
    ns.exceptionHandler(this::handleNSException);
    ns.closeHandler(this::handleNSClosed);

    return promise.future();
  }

  void handleNSException(Throwable t) {
    if (isAvailable()) {
      // shutdown() clear the handler, so gets a reference on the error handler first.
      final Handler<Throwable> handler;
      synchronized (this) {
        handler = exceptionHandler;
      }
      shutdown();
      // some SMTP servers may not close the TCP connection gracefully
      // https://github.com/vert-x3/vertx-mail-client/issues/175
      if (quitSent) {
        log.debug("got an exception on the netsocket after quit sent", t);
      } else {
        if (handler != null) {
          context.emit(t, handler);
        }
      }
    } else {
      log.debug("not returning follow-up exception", t);
    }
  }

  private boolean isAvailable() {
    return !socketClosed && !shutdown;
  }

  boolean isValid() {
    return (expirationTimestamp == 0 || System.currentTimeMillis() <= expirationTimestamp) && !quitSent;
  }

  void handleNSClosed(Void v) {
    log.trace("handleNSClosed() - socket has been closed");
    boolean unexpected = false;
    socketClosed = true;
    if (!shutdown && !quitSent) {
      handleError(new IOException("socket was closed unexpected."));
      unexpected = true;
    }
    if (unexpected) {
      shutdown();
    }
    handleClosed();
  }

  private void handleClosed() {
    setNoUse();
    if (closeHandler != null) {
      closeHandler.succeed();
      closeHandler = null;
    }
    if (!evicted) {
      evicted = true;
      evictionHandler.handle(null);
      cleanHandlers();
    }
    this.emailsSent.set(0);
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
    exceptionHandler = null;
    commandReplyHandler = null;
  }

  public Future<Void> returnToPool() {
    log.trace("return to pool");
    setNoUse();
    Promise<Void> promise = context.promise();
    try {
      final long count = emailsSent.incrementAndGet();
      boolean exceed = config.getMaxMailsPerConnection() > 0 && count >= config.getMaxMailsPerConnection();
      if (!config.isKeepAlive() || this.closeHandler != null || exceed) {
        quitCloseConnection().onComplete(ignored -> {
          handleClosed();
          promise.complete();
        });
      } else {
        // recycle
        log.trace("recycle for next use");
        cleanHandlers();
        lease.recycle();
        expirationTimestamp = expirationTimestampOf(config);
        promise.complete();
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
  Future<Void> quitCloseConnection() {
    quitSent = true;
    setNoUse();
    return writeLineWithDrain("QUIT", true);
  }

  void setExceptionHandler(Handler<Throwable> exceptionHandler) {
    this.exceptionHandler = exceptionHandler;
  }

  void setInUse() {
    inuse = true;
    expirationTimestamp = expirationTimestampOf(config);
  }

  void setNoUse() {
    inuse = false;
  }

  /**
   * close the connection doing a QUIT command first
   */
  Future<Void> close() {
    if (!isAvailable()) {
      return context.succeededFuture();
    }
    if (!inuse) {
      log.trace("close by sending quit in close()");
      return quitCloseConnection();
    } else {
      Promise<Void> promise = context.promise();
      this.closeHandler = promise;
      if (quitSent) {
        shutdown();
      }
      return promise.future();
    }
  }

  private void handleError(Throwable t) {
    context.emit(roc -> {
      Completable<String> currentHandler = commandReplyHandler;
      if (currentHandler != null) {
        commandReplyHandler = null;
        currentHandler.fail(t);
      } else if (log.isDebugEnabled()) {
        log.debug(t.getMessage(), t);
      }
    });
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
      sb.append("Support ENHANCEDSTATUSCODES: ").append(capa.isCapaEnhancedStatusCodes()).append("\n");
      log.debug(sb);
    }
  }

  Future<SMTPResponse[]> writeCommands(List<String> commands) {
    String cmds = String.join("\r\n", commands);
    nsHandler.setExpected(commands.size());
    return doWrite(cmds, -1).map(response -> {
      try {
        String[] lines = linePattern.split(response);
        SMTPResponse[] result = new SMTPResponse[lines.length];
        for (int i = 0; i < lines.length; i++) {
          String message = lines[i];
          result[i] = new SMTPResponse(message);
        }
        return result;
      } finally {
        nsHandler.setExpected(1);
      }
    });
  }

  /**
   * write command without log masking
   */
  public Future<SMTPResponse> write(String str) {
    return doWrite(str, -1).map(SMTPResponse::new);
  }

  /**
   * write command with log masking (everything after position {@code blank} is replaced with {@code *})
   * this method expects a response from SMTP server
   */
  Future<SMTPResponse> write(String str, int blank) {
    return doWrite(str, blank).map(SMTPResponse::new);
  }

  private Future<String> doWrite(String str, int blank) {
    Promise<String> promise = context.promise();
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
      this.commandReplyHandler = promise;
      ns.write(str + "\r\n").onFailure(t -> {
        handleError(t);
        shutdown();
      });
    });
    return promise.future();
  }

  /**
   * write single line not expecting a reply, using drain handler
   */
  public Future<Void> writeLineWithDrain(String str, boolean mayLog) {
    if (mayLog) {
      log.debug(str);
    }
    Promise<Void> promise = context.promise();
    context.emit(roc -> {
      if (isAvailable()) {
        if (ns.writeQueueFull()) {
          ns.drainHandler(v -> {
            // avoid getting confused by being called twice
            ns.drainHandler(null);
            ns.write(str + "\r\n").onComplete(promise);
          });
        } else {
          ns.write(str + "\r\n").onComplete(promise);
        }
      } else {
        promise.fail("Connection was closed.");
      }
    });
    return promise.future();
  }

  boolean isSsl() {
    return ns.isSsl();
  }

  Future<Void> upgradeToSsl() {
    return ns.upgradeToSsl();
  }

  /**
   * check if a connection is already closed (this is mostly for unit tests)
   */
  public boolean isClosed() {
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

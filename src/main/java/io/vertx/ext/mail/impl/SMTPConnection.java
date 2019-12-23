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
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.impl.pool.ConnectionListener;
import io.vertx.core.impl.NoStackTraceThrowable;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetSocket;
import io.vertx.ext.mail.MailConfig;

import java.util.UUID;

/**
 * SMTP connection to a server.
 * <p>
 * Encapsulate the NetSocket connection and the data writing/reading
 *
 * Context maybe changed if it is reused by another MailClient instance.
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
class SMTPConnection {

  private static final Logger log = LoggerFactory.getLogger(SMTPConnection.class);

  private Handler<String> commandReplyHandler;
  private Handler<Throwable> errorHandler;
  private Capabilities capa = new Capabilities();
  private Context context;

  // if the connection is connected yet
  private boolean connected;

  // if the connection is broken
  private boolean broken = true;
  private boolean socketClosed;
  private boolean connShutDown;
  private boolean idle;
  // if client wants to close the connection, this is only be true when close() method is called.
  private boolean tryClose;

  private final NetClient netClient;
  private final ConnectionListener<SMTPConnection> connListener;
  private final MailConfig config;
  private final String id;
  private Handler<AsyncResult<Void>> closeHandler;
  private NetSocket ns;
  private final long idleTimeout;

  // https://tools.ietf.org/html/rfc5321#section-4.5.3.2.7
  static final long DEFAULT_IDLE_TIMEOUT = 300000L; // 5 minutes in milliseconds

  SMTPConnection(MailConfig config, NetClient netClient, long idleTimeout, ConnectionListener<SMTPConnection> connListener) {
    this.id = UUID.randomUUID().toString();
    this.netClient = netClient;
    this.config = config;
    this.connListener = connListener;
    this.idleTimeout = idleTimeout;
  }

  void openConnection(Handler<String> initialReplyHandler, Handler<Throwable> errorHandler) {
    this.errorHandler = errorHandler;
    netClient.connect(config.getPort(), config.getHostname(), netSocket -> {
      if (netSocket.succeeded()) {
        ns = netSocket.result();
        this.connected = true;
        this.broken = false;
        ns.exceptionHandler(ctxHandler(e -> {
          // avoid returning two exceptions
          log.debug("exceptionHandler called");
          if (!isClosed() && !broken) {
            setBroken();
            log.debug("got an exception on the netsocket", e);
            handleError(e);
          } else {
            log.debug("not returning follow-up exception", e);
          }
        }));
        ns.closeHandler(ctxHandler(v -> {
          log.debug("socket has been socketClosed");
          socketClosed = true;
          // avoid exception if we regularly shut down the socket on our side
          if (!connShutDown && !broken && !idle) {
            setBroken();
            log.debug("throwing: connection has been closed by the server");
            handleError("connection has been closed by the server");
          } else {
            log.debug("close has been expected");
            if (!broken) {
              setBroken();
            }
            if (!connShutDown) {
              shutdown();
            }
          }
        }));
        commandReplyHandler = initialReplyHandler;
        final Handler<Buffer> mlp = new MultilineParser(buffer -> {
          if (commandReplyHandler == null) {
            log.warn("dropping reply arriving after we stopped   processing \"" + buffer.toString() + "\"");
          } else {
            // make sure we only call the handler once
            Handler<String> currentHandler = commandReplyHandler;
            commandReplyHandler = null;
            currentHandler.handle(buffer.toString());
          }
        });
        ns.handler(ctxHandler(mlp));
      } else {
        // failed to connect, the connection needs to get evicted from the pool
        evict();
        handleError(netSocket.cause());
      }
    });
  }

  private <T> Handler<T> ctxHandler(Handler<T> handler) {
    return t -> this.context.runOnContext(v -> handler.handle(t));
  }

  private void handleInContext(Handler<Void> handler) {
    this.context.runOnContext(v -> handler.handle(null));
  }

  private <T> void handleInContext(Handler<T> handler, T t) {
    this.context.runOnContext(v -> handler.handle(t));
  }

  boolean isConnected() {
    return connected;
  }

  boolean needsReset() {
    return this.connected && idle && !isClosed();
  }

  boolean isClosed() {
    return socketClosed || connShutDown;
  }

  void useConnection() {
    log.debug("useConnection()");
    this.idle = false;
  }

  String getId() {
    return id;
  }

  SMTPConnection setContext(Context context) {
    this.context = context;
    return this;
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
  }

  /**
   * Sets connection to broken and close the underline NetSocket.
   *
   * NetSocket's close will trigger the shutdown process to get evicted from the pool.
   */
  synchronized void setBroken() {
    if (!broken) {
      log.debug("setBroken() for: " + id);
      broken = true;
      shutdown();
    }
  }

  /**
   * This method is called to close the connection and get evicted from the pool.
   *
   * This method does not close the NetClient, which should be done in the pool.
   */
  void shutdown() {
    if (!connShutDown) {
      log.debug("shutdown() for: " + id);
      broken = true;
      commandReplyHandler = null;
      connShutDown = true;
      if (ns != null) {
        ns.close();
        ns = null;
      }
      evict();
      if (this.closeHandler != null) {
        this.closeHandler.handle(Future.succeededFuture());
      }
    }
  }

  private void evict() {
    log.debug("evict conn: " + id);
    this.connListener.onEvict();
  }

  synchronized void close(Handler<AsyncResult<Void>> closeHandler) {
    handleInContext(v -> {
      this.closeHandler = closeHandler;
      this.tryClose = true;
      if (isClosed()) {
        if (closeHandler != null) {
          closeHandler.handle(Future.succeededFuture());
        }
        return;
      }
      if (shouldClose()) {
        quitCloseConnection();
      }
    });
  }

  private boolean shouldClose () {
    return tryClose && connected && !socketClosed;
  }

  /**
   * This method is called after sending email by MailClient.
   * It either return the connection back to the pool or close it directly if keepAlive is false.
   */
  synchronized void returnToPool() {
    log.debug("returnToPool() for conn: " + id);
    try {
      if (!config.isKeepAlive() || shouldClose() || broken) {
        quitCloseConnection();
      } else {
        log.debug("setting error handler and commandReplyHandler to null");
        commandReplyHandler = null;
        errorHandler = null;
        recycle();
        this.idle = true;
      }
    } catch (Exception e) {
      handleError(e);
    }
  }

  private void recycle() {
    log.debug("recycle()");
    final long timeout = idleTimeout > 0 ? idleTimeout : DEFAULT_IDLE_TIMEOUT;
    connListener.onRecycle(System.currentTimeMillis() + timeout);
  }

  /**
   * send QUIT and close the connection, this operation waits for the success of the quit command but will close the
   * connection on exception as well
   */
  private void quitCloseConnection() {
    log.debug("quitCloseConnection");
    if (!isClosed()) {
      new SMTPQuit(this, vv -> shutdown()).start();
    }
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
    handleInContext(a -> {
      this.commandReplyHandler = commandResultHandler;
      this.idle = false;
      if (!isClosed()) {
        try {
          ns.write(str + "\r\n");
        } catch (Exception e) {
          handleError(e);
        }
      }
    });
  }

  // write single line not expecting a reply, using drain handler
  void writeLineWithDrainPromise(String str, boolean mayLog, Promise<Void> promise) {
    if (mayLog) {
      log.debug(str);
    }
    handleInContext(a -> {
      this.idle = false;
      if (!isClosed()) {
        try {
          if (ns.writeQueueFull()) {
            ns.drainHandler(v -> {
              // avoid getting confused by being called twice
              ns.drainHandler(null);
              ns.write(str + "\r\n").setHandler(promise);
            });
          } else {
            ns.write(str + "\r\n").setHandler(promise);
          }
        } catch (Exception e) {
          handleError(e);
        }
      }
    });
  }

  private void handleError(String message) {
    handleError(new NoStackTraceThrowable(message));
  }

  private void handleError(Throwable throwable) {
    handleInContext(errorHandler, throwable);
  }

  boolean isSsl() {
    return ns.isSsl();
  }

  void upgradeToSsl(Handler<AsyncResult<Void>> handler) {
    try {
      ns.upgradeToSsl(handler);
    } catch (Exception e) {
      handleError(e);
    }
  }

  /**
   * set error handler to a "local" handler to be reset later
   */
  private Handler<Throwable> prevErrorHandler = null;

  void setErrorHandler(Handler<Throwable> newHandler) {
    if (prevErrorHandler == null) {
      prevErrorHandler = errorHandler;
    }
    log.debug("Setting up errorHandler");
    errorHandler = newHandler;
  }

  /**
   * reset error handler to previous
   */
  void resetErrorHandler() {
    errorHandler = prevErrorHandler;
  }

  /**
   * get the context associated with this connection
   *
   * @return the current context
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

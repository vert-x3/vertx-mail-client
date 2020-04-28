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
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.impl.clientconnection.ConnectionListener;
import io.vertx.ext.mail.MailConfig;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * SMTP connection to a server.
 * <p>
 * Encapsulate the NetSocket connection and the data writing/reading
 *
 * Context maybe changed if it is reused by another MailClient instance.
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 * @author <a href="mailto: aoingl@gmail.com">Lin Gao</a>
 */
class SMTPConnection {

  private static final Logger log = LoggerFactory.getLogger(SMTPConnection.class);
  // https://tools.ietf.org/html/rfc5321#section-4.5.3.2.7
  static final long DEFAULT_IDLE_TIMEOUT = 300000L; // 5 minutes in milliseconds

  private Capabilities capa = new Capabilities();
  private ContextInternal context;

  private final ConnectionListener<SMTPConnection> connListener;
  private final MailConfig config;
  private final NetSocket ns;
  private final long recycleTimeout;
  private final MultilineParser nsHandler;

  private final AtomicLong latestUseTimestamp;
  private boolean closing;
  private boolean inUse;
  private boolean needsReset;

  SMTPConnection(ContextInternal context, NetSocket ns, MailConfig config, long recycleTimeout,
                 ConnectionListener<SMTPConnection> connListener) {
    this.context = context;
    this.ns = ns;
    this.config = config;
    this.connListener = connListener;
    this.recycleTimeout = recycleTimeout;
    this.latestUseTimestamp = new AtomicLong(System.currentTimeMillis());
    ns.exceptionHandler(this::fail);
    ns.closeHandler(this::closed);
    this.nsHandler = new MultilineParser(this::fail);
  }

  SMTPConnection setContext(ContextInternal context) {
    if (this.context != context) {
      this.context = context;
    }
    return this;
  }

  Future<String> openConnection() {
    Promise<String> promise = context.promise();
    openConnection(promise);
    return promise.future();
  }

  private void openConnection(Handler<AsyncResult<String>> greetingHandler) {
    log.trace("openConnection()");
    this.nsHandler.offer(1, greetingHandler);
    this.ns.handler(ctxHandler(this.nsHandler));
  }

  private <T> Handler<T> ctxHandler(Handler<T> handler) {
    return t -> this.context.runOnContext(v -> handler.handle(t));
  }

  Future<String> writeWithReply(String str) {
    Promise<String> promise = context.promise();
    writeWithReply(str, -1, promise);
    return promise.future();
  }

  Future<String> writeWithReply(String str, int blank) {
    Promise<String> promise = context.promise();
    writeWithReply(str, blank, promise);
    return promise.future();
  }

  private void writeWithReply(String str, int blank, Handler<AsyncResult<String>> writeHandler) {
    writeWithReply(str, blank, 1, writeHandler);
  }

  private void writeWithReply(String str, int blank, int expected, Handler<AsyncResult<String>> writeHandler) {
    log.trace("writeWithReply()");
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
    latestUseTimestamp.set(System.currentTimeMillis());
    context.runOnContext(v -> ns.write(str + "\r\n", h -> {
      if (h.failed()) {
        writeHandler.handle(Future.failedFuture(h.cause()));
      } else {
        if (!this.nsHandler.offer(expected, writeHandler)) {
          writeHandler.handle(Future.failedFuture("We need to wait replyHandler to be called before sending: " + str));
        }
      }
    }));
  }

  // write single line not expecting a reply, using drain replyHandler
  void writeLineWithDrainPromise(String str, boolean mayLog, Promise<Void> promise) {
    if (mayLog) {
      log.debug(str);
    }
    try {
      latestUseTimestamp.set(System.currentTimeMillis());
      if (ns.writeQueueFull()) {
        ns.drainHandler(v -> {
          // avoid getting confused by being called twice
          ns.drainHandler(null);
          ns.write(str + "\r\n").onComplete(ctxHandler(promise));
        });
      } else {
        ns.write(str + "\r\n").onComplete(ctxHandler(promise));
      }
    } catch (Exception e) {
      this.context.runOnContext(h -> promise.fail(e));
    }
  }

  Future<String> writeCommands(List<String> commands) {
    Promise<String> promise = Promise.promise();
    String cmds = String.join("\r\n", commands);
    writeWithReply(cmds, -1, commands.size(), promise);
    return promise.future();
  }

  void fail(Throwable t) {
    log.trace("fail()");
    cleanHandlers(t);
    forceClose();
  }

  private void closed(Void v) {
    log.trace("closed()");
    cleanHandlers(new RuntimeException("Connection was closed by server"));
    connListener.onEvict();
  }

  private void cleanHandlers(Throwable t) {
    context.runOnContext(v -> this.nsHandler.cleanHandlers(t));
  }

  void forceClose() {
    log.trace("forceClose()");
    connListener.onEvict();
    ns.close();
  }

  boolean isValid() {
    return this.recycleTimeout > System.currentTimeMillis() - this.latestUseTimestamp.get();
  }

  synchronized boolean needsReset() {
    return this.needsReset;
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

  synchronized void markClosing() {
    log.trace("markClosing()");
    this.closing = true;
    if (!inUse) {
      new SMTPQuit(this).start();
    }
  }

  /**
   * This method is called after sending email by MailClient.
   * It either returns the connection back to the pool or closes it directly if keepAlive is false.
   */
  void returnToPool() {
    log.trace("returnToPool()");
    if (!this.nsHandler.isClean()) {
      fail(new IllegalStateException("Handlers are not clean when connection returns to pool"));
      return;
    }
    boolean close;
    synchronized (this) {
      close = this.closing;
    }
    if (!config.isKeepAlive() || close) {
      new SMTPQuit(this).start();
    } else {
      recycle();
    }
  }

  synchronized SMTPConnection useConnection() {
    log.trace("useConnection()");
    this.inUse = true;
    return this;
  }

  private synchronized void recycle() {
    log.trace("recycle()");
    this.inUse = false;
    this.needsReset = true;
    connListener.onRecycle();
  }

  boolean isSsl() {
    return ns.isSsl();
  }

  Future<Void> upgradeToSsl() {
    return ns.upgradeToSsl();
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

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
import io.vertx.core.CompositeFuture;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.impl.pool.ConnectResult;
import io.vertx.core.http.impl.pool.ConnectionListener;
import io.vertx.core.http.impl.pool.ConnectionProvider;
import io.vertx.core.http.impl.pool.Pool;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.net.NetClient;
import io.vertx.ext.auth.PRNG;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.StartTLSOptions;
import io.vertx.ext.mail.impl.sasl.AuthOperationFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

class SMTPConnectionPool {

  private static final Logger log = LoggerFactory.getLogger(SMTPConnectionPool.class);
  private static final LongSupplier CLOCK = System::currentTimeMillis;

  private final Vertx vertx;
  private final Context context;
  private NetClient netClient;
  private final MailConfig config;
  private final PRNG prng;
  private final AuthOperationFactory authOperationFactory;
  private Pool<SMTPConnection> pool;
  private final Map<String, SMTPConnection> connections;
  private boolean closed;
  private long idleTimeout;
  private long timerId = -1L;

  SMTPConnectionPool(Vertx vertx, MailConfig config) {
    this(vertx, vertx.getOrCreateContext(), config);
  }

  SMTPConnectionPool(Vertx vertx, Context context, MailConfig config) {
    this.vertx = vertx;
    this.context = context;
    this.config = config;
    this.prng = new PRNG(vertx);
    this.authOperationFactory = new AuthOperationFactory(prng);

    // If the hostname verification isn't set yet, but we are configured to use SSL, update that now
    String verification = config.getHostnameVerificationAlgorithm();
    if ((verification == null || verification.isEmpty()) && !config.isTrustAll() &&
        (config.isSsl() || config.getStarttls() != StartTLSOptions.DISABLED)) {
      // we can use HTTPS verification, which matches the requirements for SMTPS
      this.config.setHostnameVerificationAlgorithm("HTTPS");
    }
    this.connections = new ConcurrentHashMap<>();
    initialize();
  }

  private void initialize() {
    this.netClient = vertx.createNetClient(this.config);
    this.pool = new Pool<>(context, new SMTPConnectionProvider(), CLOCK, -1, 1, config.getMaxPoolSize(),
      pc -> this.poolClosed(),
      c -> connections.putIfAbsent(c.getId(), c),
      c -> connections.remove(c.getId()),
      false);
    this.connections.clear();
    this.idleTimeout = SMTPConnection.DEFAULT_IDLE_TIMEOUT;
    if (config.getIdleTimeout() > 0 && config.getIdleTimeoutUnit() != null) {
      // milliseconds
      this.idleTimeout = config.getIdleTimeoutUnit().toMillis(config.getIdleTimeout());
    }

    if (this.timerId > 0) {
      vertx.cancelTimer(timerId);
    }
    this.timerId = vertx.setPeriodic(idleTimeout, v -> pool.closeIdle());
  }

  AuthOperationFactory getAuthOperationFactory() {
    return authOperationFactory;
  }

  private void poolClosed() {
    log.debug("Pool is closed, clear the connections and unset the timer.");
    this.connections.clear();
    if (this.timerId > 0) {
      vertx.cancelTimer(timerId);
    }
  }

  /**
   * Gets the connection and hand over to the resultHandler for next execution.
   *
   * @param resultHandler always gets executed in clientContext
   */
  void getConnection(String hostname, Handler<AsyncResult<SMTPConnection>> resultHandler) {
    log.debug("getConnection()");
    if (closed) {
      resultHandler.handle(Future.failedFuture("Connection Pool has been closed by client."));
      return;
    }
    Context ctx = vertx.getOrCreateContext();
    this.context.runOnContext(v -> getOrCreateConnection(hostname, ctx, resultHandler));
  }

  private synchronized void getOrCreateConnection(String hostname, Context ctx, Handler<AsyncResult<SMTPConnection>> resultHandler) {
    log.debug("getOrCreateConnection()");
    boolean poolConn = pool.getConnection(c -> ctx.runOnContext(v -> {
      if (c.succeeded()) {
        SMTPConnection conn = c.result().setContext(ctx);
        if (!conn.isConnected()) {
          log.debug("New connection created, connect it: " + conn.getId());
          // if connection is not opened yet, open it using SMTPStarter
          new SMTPStarter(conn, config, hostname, authOperationFactory, r -> {
            if (r.succeeded()) {
              resultHandler.handle(Future.succeededFuture(conn));
            } else {
              resultHandler.handle(Future.failedFuture(r.cause()));
            }
          }).start();
        } else if (conn.needsReset()) {
          log.debug("Reuse a connection, reset it: " + conn.getId());
          new SMTPReset(conn, vv -> {
            if (vv.succeeded()) {
              conn.useConnection();
              resultHandler.handle(Future.succeededFuture(conn));
            } else {
              log.debug("Failed to reset the connection, try to close it and create a new one.", vv.cause());
              try {
                context.runOnContext(vvv -> {
                  conn.shutdown();
                  getOrCreateConnection(hostname, ctx, resultHandler);
                });
              } catch (Exception e) {
                resultHandler.handle(Future.failedFuture(e));
              }
            }
          }).start();
        } else {
          log.warn("Unknown state of connection: " + conn.getId() + ", try to close it and create a new one.");
          try {
            context.runOnContext(vvv -> {
              conn.shutdown();
              getOrCreateConnection(hostname, ctx, resultHandler);
            });
          } catch (Exception e) {
            resultHandler.handle(Future.failedFuture(e));
          }
        }
      } else {
        resultHandler.handle(Future.failedFuture(c.cause()));
      }
    }));
    if (!poolConn) {
      log.debug("pool has been closed, create a new one on demand.");
      this.netClient.close();
      initialize();
      getOrCreateConnection(hostname, ctx, resultHandler);
    }
  }

  void close() {
    close(null);
  }

  synchronized void close(Handler<AsyncResult<Void>> finishedHandler) {
    if (!closed) {
      closed = true;
      if (this.timerId > 0) {
        vertx.cancelTimer(timerId);
      }
    }
    List<Future> connsClosed = new LinkedList<>();
    for (Map.Entry<String, SMTPConnection> conn : this.connections.entrySet()) {
      Promise<Void> closePromise = Promise.promise();
      conn.getValue().close(closePromise);
      connsClosed.add(closePromise.future());
    }
    CompositeFuture.all(connsClosed).setHandler(h -> context.runOnContext(v -> {
      this.prng.close();
      this.netClient.close();
      if (h.succeeded()) {
        if (finishedHandler != null) {
          finishedHandler.handle(Future.succeededFuture());
        }
      } else {
        if (finishedHandler != null) {
          finishedHandler.handle(Future.failedFuture(h.cause()));
        }
      }
    }));
  }

  int connCount() {
    return this.connections.size();
  }

  private class SMTPConnectionProvider implements ConnectionProvider<SMTPConnection> {

    @Override
    public void connect(ConnectionListener<SMTPConnection> listener, ContextInternal context,
                        Handler<AsyncResult<ConnectResult<SMTPConnection>>> asyncResultHandler) {
      // created SMTPConnection does not open yet!
      try {
        SMTPConnection conn = new SMTPConnection(config, netClient, idleTimeout, listener);
        ConnectResult<SMTPConnection> connResult = new ConnectResult<>(conn, 1, 1);
        asyncResultHandler.handle(Future.succeededFuture(connResult));
      } catch (Exception e) {
        asyncResultHandler.handle(Future.failedFuture(e));
      }
    }

    @Override
    public void init(SMTPConnection conn) {}

    @Override
    public void close(SMTPConnection conn) {
      conn.close(null);
    }
  }

}

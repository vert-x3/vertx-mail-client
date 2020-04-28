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
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.impl.ConcurrentHashSet;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.impl.clientconnection.ConnectResult;
import io.vertx.core.net.impl.clientconnection.ConnectionListener;
import io.vertx.core.net.impl.clientconnection.ConnectionProvider;
import io.vertx.core.net.impl.clientconnection.Pool;
import io.vertx.ext.auth.PRNG;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.impl.sasl.AuthOperationFactory;

import java.util.Set;

/**
 * The SMTP Connection Pool which uses {@link Pool} as the pooling infrastructure.
 *
 * @author <a href="mailto: aoingl@gmail.com">Lin Gao</a>
 */
class SMTPConnectionPool {

  private static final Logger log = LoggerFactory.getLogger(SMTPConnectionPool.class);

  private final MailConfig config;
  private String ownHostName;
  private final NetClient netClient;
  private final Pool<SMTPConnection> pool;
  private long timerID;
  private final AuthOperationFactory authOperationFactory;
  private final PRNG prng;
  private final Vertx vertx;
  private final Set<SMTPConnection> conns = new ConcurrentHashSet<>();
  private boolean closed;
  private final long period;

  SMTPConnectionPool(ContextInternal ctx, MailConfig config) {
    this.config = config;
    this.vertx = ctx.owner();
    this.prng = new PRNG(vertx);
    this.authOperationFactory = new AuthOperationFactory(prng);
    this.netClient = vertx.createNetClient(this.config);
    final ConnectionProvider<SMTPConnection> connectionProvider = new SMTPConnectionProvider();
    pool = new Pool<>(ctx, connectionProvider, -1, 1, config.getMaxPoolSize(),
      this::connectionAdded,
      this::connectionRemoved,
      false);
    if (config.getIdleTimeout() > 0 && config.getIdleTimeoutUnit() != null) {
      // milliseconds
      period = config.getIdleTimeoutUnit().toMillis(config.getIdleTimeout());
    } else {
      period = SMTPConnection.DEFAULT_IDLE_TIMEOUT;
    }
    this.timerID = period > 0L ? vertx.setTimer(period, id -> checkExpired(period)) : -1L;
  }

  private synchronized void checkExpired(long period) {
    log.trace("checkExpired()");
    pool.closeIdle();
    if (!closed) {
      timerID = vertx.setTimer(period, id -> checkExpired(period));
    }
  }

  int connCount() {
    return conns.size();
  }

  private void connectionAdded(SMTPConnection conn) {
    log.trace("connectionAdded()");
    conns.add(conn);
  }

  private void connectionRemoved(SMTPConnection conn) {
    log.trace("connectionRemoved()");
    conns.remove(conn);
    if (conns.isEmpty()) {
      synchronized (this) {
        if (closed) {
          log.debug("Last connection removed, close NetClient.");
          netClient.close();
        }
      }
    }
  }

  AuthOperationFactory getAuthOperationFactory() {
    return this.authOperationFactory;
  }

  Future<SMTPConnection> getConnection(ContextInternal ctx) {
    log.trace("getConnection()");
    Promise<SMTPConnection> promise = ctx.promise();
    synchronized (this) {
      if (closed) {
        promise.fail("Connection Pool is closed");
        return promise.future();
      }
    }
    // here the ctx is the resource context of the MailClient instance
    pool.getConnection(connResult -> {
      log.trace("got connResult in pool.getConnection()");
      if (connResult.failed()) {
        promise.handle(Future.failedFuture(connResult.cause()));
        return;
      }
      try {
        SMTPConnection conn = connResult.result().setContext(ctx).useConnection();
        if (conn.needsReset()) {
          new SMTPReset(conn).start().onComplete(resetResult -> {
            if (resetResult.failed()) {
              log.debug("reset failed, close it, try another conn");
              conn.forceClose();
              getConnection(ctx).onComplete(promise);
              return;
            }
            promise.handle(Future.succeededFuture(conn));
          });
        } else {
          promise.handle(Future.succeededFuture(conn));
        }
      } catch (Exception e) {
        promise.handle(Future.failedFuture(e));
      }
    });
    return promise.future();
  }

  protected void close() {
    log.trace("close()");
    synchronized (this) {
      if (timerID >= 0) {
        vertx.cancelTimer(timerID);
        timerID = -1;
      }
      if (closed) {
        return;
      }
      closed = true;
    }
    this.authOperationFactory.setAuthMethod(null);
    this.prng.close();
    conns.forEach(SMTPConnection::markClosing);
    // wait until all active connections closed
    if (conns.isEmpty()) {
      log.debug("No active connections, close NetClient");
      netClient.close();
    }
  }

  void setOwnerHostName(String ownHostName) {
    if (this.ownHostName != null) {
      this.ownHostName = ownHostName;
    }
  }

  private class SMTPConnectionProvider implements ConnectionProvider<SMTPConnection> {

    @Override
    public void connect(ConnectionListener<SMTPConnection> listener, ContextInternal context,
                        Handler<AsyncResult<ConnectResult<SMTPConnection>>> asyncResultHandler) {
      context.runOnContext(v -> {
        try {
          log.trace("SMTPConnectionProvider.connect()");
          netClient.connect(config.getPort(), config.getHostname())
            .flatMap(ns -> {
              log.debug("NetClient gets connected");
              final SMTPConnection conn = new SMTPConnection(context, ns, config, period, listener);
              return conn.openConnection()
                .flatMap(greeting -> new SMTPStarter(conn, config, ownHostName, authOperationFactory).start(greeting))
                .map(s -> new ConnectResult<>(conn, 1, 1));
            }).onComplete(asyncResultHandler);
        } catch (Exception e) {
          asyncResultHandler.handle(Future.failedFuture(e));
        }
      });
    }

    @Override
    public void init(SMTPConnection conn) {
    }

    // this is called on some expired connections by the pool
    @Override
    public void close(SMTPConnection conn) {
      conn.markClosing();
    }

    @Override
    public boolean isValid(SMTPConnection conn) {
      return conn.isValid();
    }
  }

}

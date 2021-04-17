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
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.impl.clientconnection.Lease;
import io.vertx.ext.auth.PRNG;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.StartTLSOptions;
import io.vertx.ext.mail.impl.sasl.AuthOperationFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

class SMTPConnectionPool {

  private static final Logger log = LoggerFactory.getLogger(SMTPConnectionPool.class);

  // max retry times if RSET failed when choosing an existed connection in pool, default to 5.
  private static final int RSET_MAX_RETRY = Integer.getInteger("vertx.mail.rset.max.retry", 5);

  private final PRNG prng;
  private final AuthOperationFactory authOperationFactory;
  private final Vertx vertx;
  private final NetClient netClient;
  private final MailConfig config;

  private boolean closed = false;
  private final AtomicReference<SMTPEndPoint> endPoint = new AtomicReference<>();
  private long timerID = -1;

  SMTPConnectionPool(Vertx vertx, MailConfig config) {
    this.vertx = vertx;
    this.config = config;
    // If the hostname verification isn't set yet, but we are configured to use SSL, update that now
    String verification = config.getHostnameVerificationAlgorithm();
    if ((verification == null || verification.isEmpty()) && !config.isTrustAll() &&
      (config.isSsl() || config.getStarttls() != StartTLSOptions.DISABLED)) {
      // we can use HTTPS verification, which matches the requirements for SMTPS
      config.setHostnameVerificationAlgorithm("HTTPS");
    }
    netClient = vertx.createNetClient(config);
    this.prng = new PRNG(vertx);
    this.authOperationFactory = new AuthOperationFactory(prng);
    if (config.getPoolCleanerPeriod() > 0 && config.isKeepAlive() && config.getKeepAliveTimeout() > 0) {
      timerID = vertx.setTimer(config.getPoolCleanerPeriod(), this::checkExpired);
    }
  }

  private void checkExpired(long timer) {
    getSMTPEndPoint().checkExpired(ar -> {
      if (ar.succeeded()) {
        ar.result().forEach(c -> c.quitCloseConnection(Promise.promise()));
      }
    });
    synchronized (this) {
      if (!closed) {
        timerID = vertx.setTimer(config.getPoolCleanerPeriod(), this::checkExpired);
      }
    }
  }

  AuthOperationFactory getAuthOperationFactory() {
    return authOperationFactory;
  }

  void getConnection(String hostname, Handler<AsyncResult<SMTPConnection>> resultHandler) {
    getConnection(hostname, vertx.getOrCreateContext(), resultHandler);
  }

  void getConnection(String hostname, Context ctx, Handler<AsyncResult<SMTPConnection>> resultHandler) {
    getConnection0(hostname, ctx, resultHandler, 0);
  }

  private void getConnection0(String hostname, Context ctx, Handler<AsyncResult<SMTPConnection>> resultHandler, final int i) {
    synchronized (this) {
      if (closed) {
        resultHandler.handle(Future.failedFuture("connection pool is closed"));
        return;
      }
    }
    ContextInternal contextInternal = (ContextInternal)ctx;
    Promise<Lease<SMTPConnection>> promise = contextInternal.promise();
    promise.future()
      .map(l -> l.get().setLease(l)).flatMap(conn -> {
      final Future<SMTPConnection> future;
      final boolean reset;
      conn.setInUse();
      if (conn.isInitialized()) {
        reset = true;
        Promise<SMTPConnection> connReset = contextInternal.promise();
        new SMTPReset(conn, connReset).start();
        future = connReset.future();
      } else {
        reset = false;
        Promise<SMTPConnection> connInitial = contextInternal.promise();
        SMTPStarter starter = new SMTPStarter(conn, this.config, hostname, authOperationFactory, connInitial);
        try {
          conn.init(starter::serverGreeting);
        } catch (Exception e) {
          connInitial.handle(Future.failedFuture(e));
        }
        future = connInitial.future();
      }
      return future.recover(t -> {
        // close the connection as it failed either in rset or handshake
        Promise<Void> quitPromise = contextInternal.promise();
        if (t instanceof IOException) {
          conn.shutdown();
          quitPromise.fail(t);
        } else {
          conn.quitCloseConnection(quitPromise);
        }
        return quitPromise.future().transform(v -> {
          if (reset && i < RSET_MAX_RETRY) {
            Promise<SMTPConnection> getConnAgain = contextInternal.promise();
            log.debug("Failed on RSET, try " + (i + 1) + " time");
            getConnection0(hostname, ctx, getConnAgain, i + 1);
            return getConnAgain.future();
          }
          return Future.failedFuture(t);
        });
      });
    }).onComplete(resultHandler);
    getSMTPEndPoint().getConnection(contextInternal, config.getConnectTimeout(), promise);
  }

  private SMTPEndPoint getSMTPEndPoint() {
    return endPoint.accumulateAndGet(endPoint.get(), (p, n) -> p == null ? new SMTPEndPoint(netClient, config, () -> endPoint.set(null)) : p);
  }

  public void close() {
    close(h -> {
      if (h.failed()) {
        log.warn("Failed to close the pool", h.cause());
      }
      log.debug("SMTP connection pool closed.");
    });
  }

  void close(Handler<AsyncResult<Void>> finishedHandler) {
    log.debug("trying to close the connection pool");
    synchronized (this) {
      if (closed) {
        throw new IllegalStateException("pool is already closed");
      }
      closed = true;
      if (timerID >= 0) {
        vertx.cancelTimer(timerID);
        timerID = -1;
      }
    }
    this.prng.close();
    Promise<List<Future<SMTPConnection>>> closePromise = Promise.promise();
    closePromise.future()
      .flatMap(list -> {
        List<Future> futures = list.stream()
          .filter(connFuture -> connFuture.succeeded() && connFuture.result().isAvailable())
          .map(connFuture -> {
            Promise<Void> promise = Promise.promise();
            connFuture.result().close(promise);
            return promise.future();
          })
          .collect(Collectors.toList());
        return CompositeFuture.all(futures);
      })
      .onComplete(r -> {
        log.debug("Close net client");
        if (r.succeeded()) {
          if (finishedHandler != null) {
            this.netClient.close(finishedHandler);
          } else {
            this.netClient.close();
          }
        } else {
          this.netClient.close();
          if (finishedHandler != null) {
            finishedHandler.handle(Future.failedFuture(r.cause()));
          }
        }
      });
    getSMTPEndPoint().close(closePromise);
  }

  int connCount() {
    return getSMTPEndPoint().size();
  }

  NetClient getNetClient() {
    return this.netClient;
  }

}

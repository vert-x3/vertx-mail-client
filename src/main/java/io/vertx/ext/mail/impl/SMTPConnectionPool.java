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

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.net.NetClient;
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
      timerID = vertx.setTimer(poolCleanTimeout(config), this::checkExpired);
    }
  }

  private static long poolCleanTimeout(MailConfig config) {
    return config.getPoolCleanerPeriodUnit().toMillis(config.getPoolCleanerPeriod());
  }

  private void checkExpired(long timer) {
    getSMTPEndPoint().checkExpired2()
      .onSuccess(conns -> conns.forEach(SMTPConnection::quitCloseConnection));
    synchronized (this) {
      if (!closed) {
        timerID = vertx.setTimer(poolCleanTimeout(config), this::checkExpired);
      }
    }
  }

  AuthOperationFactory getAuthOperationFactory() {
    return authOperationFactory;
  }

  Future<SMTPConnection> getConnection(String hostname) {
    return getConnection(hostname, vertx.getOrCreateContext());
  }

  Future<SMTPConnection> getConnection(String hostname, Context ctx) {
    return getConnection0(hostname, ctx, 0);
  }

  private Future<SMTPConnection> getConnection0(String hostname, Context ctx, final int retryAttempt) {
    ContextInternal contextInternal = (ContextInternal) ctx;
    synchronized (this) {
      if (closed) {
        return contextInternal.failedFuture("connection pool is closed");
      }
    }

    return getSMTPEndPoint().getConnection(contextInternal, config.getConnectTimeout())
      .map(l -> l.get().setLease(l))
      .flatMap(conn -> {
        final Future<SMTPConnection> future;
        final boolean reset;
        conn.setInUse();
        if (conn.isInitialized()) {
          reset = true;
          future = new SMTPReset(conn).start(contextInternal)
            .map(ignored -> conn);
        } else {
          reset = false;
          future = conn.init()
            .flatMap(new SMTPStarter(contextInternal, conn, config, hostname, authOperationFactory)::serverGreeting)
            .map(ignored -> conn);
        }
        return future.recover(t -> {
          // close the connection as it failed either in rset or handshake
          Promise<Void> quitPromise = contextInternal.promise();
          if (t instanceof IOException) {
            conn.shutdown();
            quitPromise.fail(t);
          } else {
            conn.quitCloseConnection().onComplete(quitPromise);
          }
          return quitPromise.future().transform(v -> {
            if (reset && retryAttempt < RSET_MAX_RETRY) {
              log.debug("Failed on RSET, try " + (retryAttempt + 1) + " time");
              return getConnection0(hostname, ctx, retryAttempt + 1);
            }
            conn.shutdown();
            return contextInternal.failedFuture(t);
          });
        });
      });
  }

  private SMTPEndPoint getSMTPEndPoint() {
    return endPoint.accumulateAndGet(endPoint.get(), (p, n) -> p == null ? new SMTPEndPoint(netClient, config, () -> endPoint.set(null)) : p);
  }

  public void close() {
    doClose().onComplete(h -> {
      if (h.failed()) {
        log.warn("Failed to close the pool", h.cause());
      }
      log.debug("SMTP connection pool closed.");
    });
  }

  Future<Void> doClose() {
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
    return getSMTPEndPoint().doClose()
      .flatMap(list -> {
        List<Future<Void>> futures = list.stream()
          .map(connFuture -> connFuture.result().close())
          .collect(Collectors.toList());
        return Future.all(futures);
      })
      .flatMap(f -> this.netClient.close())
      .eventually(() -> {
        log.debug("Close net client");
        return Future.succeededFuture();
      });
  }

  int connCount() {
    return getSMTPEndPoint().size();
  }

  NetClient getNetClient() {
    return this.netClient;
  }

}

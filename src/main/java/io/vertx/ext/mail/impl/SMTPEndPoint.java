/*
 *  Copyright (c) 2011-2021 The original author or authors
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

import io.vertx.core.Future;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.net.NetClient;
import io.vertx.core.internal.pool.ConnectResult;
import io.vertx.core.internal.pool.Lease;
import io.vertx.core.internal.pool.ConnectionPool;
import io.vertx.core.internal.pool.PoolConnector;
import io.vertx.ext.mail.MailConfig;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 * SMTPEndPoint represents a pool of connections to a SMTP server.
 *
 * @author <a href="mailto: aoingl@gmail.com">Lin Gao</a>
 */
class SMTPEndPoint implements PoolConnector<SMTPConnection> {

  private final NetClient netClient;
  private final MailConfig config;
  private final ConnectionPool<SMTPConnection> pool;
  private final AtomicReference<SMTPEndPoint> endPoint;

  SMTPEndPoint(NetClient netClient, MailConfig config, AtomicReference<SMTPEndPoint> endPoint) {
    int maxSockets = config.getMaxPoolSize();
    this.config = config;
    this.netClient = netClient;
    this.pool = ConnectionPool.pool(this, new int[] {maxSockets}, -1);
    this.endPoint = endPoint;
  }

  public Future<Lease<SMTPConnection>> requestConnection(ContextInternal ctx, long timeout) {
    ContextInternal eventLoopContext;
    if (ctx.isEventLoopContext()) {
      eventLoopContext = ctx;
    } else {
      eventLoopContext = ctx.owner().contextBuilder().withEventLoop(ctx.nettyEventLoop()).withWorkerPool(ctx.workerPool()).build();
    }
    return eventLoopContext.future(p -> pool.acquire(eventLoopContext, 0, p));
  }

  Future<List<SMTPConnection>> checkExpired2() {
    return Future.future(p -> pool.evict(conn -> !conn.isValid(), p));
  }

  private final AtomicInteger refCount = new AtomicInteger();

  @Override
  public Future<ConnectResult<SMTPConnection>> connect(ContextInternal context, Listener listener) {
    return netClient.connect(config.getPort(), config.getHostname())
      .map(conn -> {
        refCount.incrementAndGet();
        SMTPConnection connection = new SMTPConnection(config, conn, context, v -> {
          if (refCount.decrementAndGet() == 0) {
            cleanup();
          }
          listener.onRemove();
        });
        return new ConnectResult<>(connection, 1, 0);
      });
  }

  Future<List<Future<SMTPConnection>>> doClose() {
    return Future.future(p -> pool.close(p));
  }

  @Override
  public boolean isValid(SMTPConnection connection) {
    return connection.isValid();
  }

  int size() {
    return pool.size();
  }

  private void cleanup() {
    endPoint.set(null);
  }
}

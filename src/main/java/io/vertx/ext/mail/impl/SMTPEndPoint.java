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

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.impl.pool.ConnectResult;
import io.vertx.core.net.impl.pool.Endpoint;
import io.vertx.core.net.impl.pool.Lease;
import io.vertx.core.net.impl.pool.ConnectionPool;
import io.vertx.core.net.impl.pool.PoolConnector;
import io.vertx.ext.mail.MailConfig;

import java.util.List;

/**
 *
 * SMTPEndPoint represents a pool of connections to a SMTP server.
 *
 * @author <a href="mailto: aoingl@gmail.com">Lin Gao</a>
 */
class SMTPEndPoint extends Endpoint<Lease<SMTPConnection>> implements PoolConnector<SMTPConnection> {
  private final NetClient netClient;
  private final MailConfig config;
  private final ConnectionPool<SMTPConnection> pool;

  SMTPEndPoint(NetClient netClient, MailConfig config, Runnable dispose) {
    super(dispose);
    this.config = config;
    this.netClient = netClient;
    int maxSockets = config.getMaxPoolSize();
    this.pool = ConnectionPool.pool(this, new int[] {maxSockets}, -1);
  }

  @Override
  public void requestConnection(ContextInternal ctx, long timeout, Handler<AsyncResult<Lease<SMTPConnection>>> handler) {
    ContextInternal eventLoopContext;
    if (ctx.isEventLoopContext()) {
      eventLoopContext = ctx;
    } else {
      eventLoopContext = ctx.owner().createEventLoopContext(ctx.nettyEventLoop(), ctx.workerPool(), ctx.classLoader());
    }
    pool.acquire(eventLoopContext, 0, handler);
  }

  void checkExpired(Handler<AsyncResult<List<SMTPConnection>>> handler) {
    pool.evict(conn -> !conn.isValid(), handler);
  }

  @Override
  public void connect(ContextInternal context, PoolConnector.Listener listener, Handler<AsyncResult<ConnectResult<SMTPConnection>>> handler) {
    netClient.connect(config.getPort(), config.getHostname()).onComplete(ar -> {
      if (ar.succeeded()) {
        incRefCount();
        SMTPConnection connection = new SMTPConnection(config, ar.result(), context, v -> {
          decRefCount();
          listener.onRemove();
        });
        handler.handle(Future.succeededFuture(new ConnectResult<>(connection, 1, 0)));
      } else {
        handler.handle(Future.failedFuture(ar.cause()));
      }
    });
  }

  void close(Promise<List<Future<SMTPConnection>>> promise) {
    pool.close(promise);
  }

  @Override
  public boolean isValid(SMTPConnection connection) {
    return connection.isValid();
  }

  int size() {
    return pool.size();
  }

}

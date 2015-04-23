/*
 * Copyright (c) 2011-2014 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 *     The Eclipse Public License is available at
 *     http://www.eclipse.org/legal/epl-v10.html
 *
 *     The Apache License v2.0 is available at
 *     http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */

package io.vertx.ext.mail.impl;

import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.impl.ContextImpl;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

/**
 *
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public abstract class ConnectionManager {

  private static final Logger log = LoggerFactory.getLogger(ConnectionManager.class);

  private final int maxSockets;
  private ConnQueue queue;

  private SMTPConnectionPool pool;

  ConnectionManager(int maxSockets) {
    this.maxSockets = maxSockets;
  }

  public void getConnection(Handler<SMTPConnection> handler, Handler<Throwable> connectionExceptionHandler,
      Context context, SMTPConnectionPool pool) {
    this.pool = pool;
    if (queue == null) {
      queue = new ConnQueue();
    }
    queue.getConnection(handler, connectionExceptionHandler, context);
  }

  protected abstract void connect(Handler<SMTPConnection> connectHandler, Handler<Throwable> connectErrorHandler,
      Context context, ConnectionLifeCycleListener listener);

  public void close() {
    if (queue != null) {
      queue.closeAllConnections();
    }
  }

  private class ConnQueue implements ConnectionLifeCycleListener {

    private final Queue<Waiter> waiters = new ArrayDeque<>();
    private final Set<SMTPConnection> allConnections = new HashSet<>();
    private int connCount;

    // ConnQueue() {
    // }

    public synchronized void getConnection(Handler<SMTPConnection> handler,
        Handler<Throwable> connectionExceptionHandler, Context context) {
      SMTPConnection idleConn = null;
      for (SMTPConnection conn : allConnections) {
        if (!conn.isBroken() && conn.isIdle()) {
          idleConn = conn;
          break;
        }
      }
      if (idleConn == null && connCount >= maxSockets) {
        // Wait in queue
        log.debug("waiting for a free socket");
        waiters.add(new Waiter(handler, connectionExceptionHandler, context));
      } else {
        if (idleConn == null) {
          // Create a new connection
          log.debug("create a new connection");
          createNewConnection(handler, connectionExceptionHandler, context);
        } else {
          // if we have found a connection run a RSET command, this checks if the connection
          // is really usable. If this fails, we create a new connection, we may run over the connection limit
          // since the close operation is not finished before we open the new connection, however it will be closed
          // shortly after
          log.debug("found idle connection, checking");
          final SMTPConnection conn = idleConn;
          conn.useConnection();
          new SMTPReset(conn, v -> handler.handle(conn), v -> {
            conn.setBroken();
            log.debug("using idle connection failed, create a new connection");
            createNewConnection(handler, connectionExceptionHandler, context);
          }).start();
        }
      }
    }

    // Called when the request has ended
    public synchronized void requestEnded(SMTPConnection conn) {
      // this is currently not used
    }

    // Called when the response has ended
    public synchronized void responseEnded(SMTPConnection conn) {
      log.debug("checkReuseConnection");
      checkReuseConnection(conn);
    }

    void closeAllConnections() {
      log.debug("closeAllConnections()");
      Set<SMTPConnection> copy;
      synchronized (this) {
        copy = new HashSet<>(allConnections);
        allConnections.clear();
      }
      // Close outside sync block to avoid deadlock
      for (SMTPConnection conn : copy) {
        if (conn.isIdle() || conn.isBroken()) {
          log.debug("closing connection");
          try {
            conn.close();
          } catch (Throwable t) {
            log.error("Failed to close connection", t);
          }
        } else {
          log.debug("closing connection after current send operation finishes");
          conn.setDoShutdown();
        }
      }
    }

    private void checkReuseConnection(SMTPConnection conn) {
      if (conn.isBroken()) {
        log.debug("closing connection (if it hasn't been already)");
        conn.close();
      } else {
        Waiter waiter = waiters.poll();
        if (waiter != null) {
          // TODO: how to do this properly?
          log.debug("running one waiting operation");
          ((ContextImpl) conn.getContext()).executeSync(() -> waiter.handler.handle(conn));
        } else {
          log.debug("keeping connection idle");
        }
      }
    }

    private void createNewConnection(Handler<SMTPConnection> handler, Handler<Throwable> connectionExceptionHandler,
        Context context) {
      connCount++;
      connect(conn -> {
        allConnections.add(conn);
        handler.handle(conn);
      }, connectionExceptionHandler, context, this);
    }

    // Called if the connection is actually closed, OR the connection attempt
    // failed - in the latter case
    // conn will be null
    public synchronized void connectionClosed(SMTPConnection conn) {
      log.debug("connection closed, removing from pool");
      connCount--;
      if (conn != null) {
        allConnections.remove(conn);
        pool.afterRemoveFromPool(conn);
      }
      Waiter waiter = waiters.poll();
      if (waiter != null) {
        // There's a waiter - so it can have a new connection
        createNewConnection(waiter.handler, waiter.connectionExceptionHandler, waiter.context);
      }
    }
  }

  private static class Waiter {
    final Handler<SMTPConnection> handler;
    final Handler<Throwable> connectionExceptionHandler;
    final Context context;

    private Waiter(Handler<SMTPConnection> handler, Handler<Throwable> connectionExceptionHandler, Context context2) {
      this.handler = handler;
      this.connectionExceptionHandler = connectionExceptionHandler;
      this.context = context2;
    }
  }

  /**
   * @return
   */
  public int size() {
    return queue.connCount;
  }

}

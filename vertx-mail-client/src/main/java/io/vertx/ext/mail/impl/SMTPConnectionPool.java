package io.vertx.ext.mail.impl;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.impl.NoStackTraceThrowable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.ext.mail.MailConfig;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

class SMTPConnectionPool implements ConnectionLifeCycleListener {

  private static final Logger log = LoggerFactory.getLogger(SMTPConnectionPool.class);

  private final Vertx vertx;
  private final int maxSockets;
  private final Queue<Waiter> waiters = new ArrayDeque<>();
  private final Set<SMTPConnection> allConnections = new HashSet<>();
  private final NetClient netClient;
  private final MailConfig config;
  private boolean closed = false;
  private int connCount;

  SMTPConnectionPool(Vertx vertx, MailConfig config) {
    this.vertx = vertx;
    this.config = config;
    this.maxSockets = config.getMaxPoolSize();
    NetClientOptions netClientOptions;
    if (config.getNetClientOptions() == null) {
      netClientOptions = new NetClientOptions().setSsl(config.isSsl()).setTrustAll(config.isTrustAll());
    } else {
      netClientOptions = config.getNetClientOptions();
    }
    this.netClient = vertx.createNetClient(netClientOptions);
  }

  // FIXME Why not use Handler<AsyncResult<SMTPConnection>> - that's what it's for
  void getConnection(Handler<SMTPConnection> resultHandler, Handler<Throwable> errorHandler) {
    log.debug("getConnection()");
    if (closed) {
      errorHandler.handle(new NoStackTraceThrowable("connection pool is closed"));
    } else {
      getConnection0(resultHandler, errorHandler);
    }
  }

  void close() {
    close(v -> {
    });
  }

  synchronized void close(Handler<Void> finishedHandler) {
    closed = true;

    finishedHandler.handle(null);
  }

  synchronized int connCount() {
    return connCount;
  }

  // Lifecycle methods

  // Called when the response has ended
  public synchronized void responseEnded(SMTPConnection conn) {
    checkReuseConnection(conn);
  }

  // Called if the connection is actually closed, OR the connection attempt
  // failed - in the latter case
  // conn will be null
  public synchronized void connectionClosed(SMTPConnection conn) {
    log.debug("connection closed, removing from pool");
    connCount--;
    if (conn != null) {
      allConnections.remove(conn);
    }
    Waiter waiter = waiters.poll();
    if (waiter != null) {
      // There's a waiter - so it can have a new connection
      createNewConnection(waiter.handler, waiter.connectionExceptionHandler);
    }
  }

  // Private methods

  private synchronized void getConnection0(Handler<SMTPConnection> handler, Handler<Throwable> connectionExceptionHandler) {
    SMTPConnection idleConn = null;
    for (SMTPConnection conn : allConnections) {
      if (!conn.isBroken() && conn.isIdle()) {
        idleConn = conn;
        break;
      }
    }
    if (idleConn == null && maxSockets > 0 && connCount >= maxSockets) {
      // Wait in queue
      log.debug("waiting for a free socket");
      waiters.add(new Waiter(handler, connectionExceptionHandler));
    } else {
      if (idleConn == null) {
        // Create a new connection
        log.debug("create a new connection");
        createNewConnection(handler, connectionExceptionHandler);
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
          createNewConnection(handler, connectionExceptionHandler);
        }).start();
      }
    }
  }

  private void checkReuseConnection(SMTPConnection conn) {
    if (conn.isBroken()) {
      conn.close();
    } else {
      // if the pool is disabled, just close the connection
      if (maxSockets < 0) {
        conn.close();
      } else {
        Waiter waiter = waiters.poll();
        if (waiter != null) {
          log.debug("running one waiting operation");
          waiter.handler.handle(conn);
        } else {
          log.debug("keeping connection idle");
        }
      }
    }
  }

  private void createNewConnection(Handler<SMTPConnection> handler, Handler<Throwable> connectionExceptionHandler) {
    connCount++;
    createConnection(conn -> {
      allConnections.add(conn);
      handler.handle(conn);
    }, connectionExceptionHandler);
  }

  private void createConnection(Handler<SMTPConnection> resultHandler, Handler<Throwable> errorHandler) {
    SMTPConnection conn = new SMTPConnection(netClient, vertx, this);
    new SMTPStarter(vertx, conn, config, v -> resultHandler.handle(conn), errorHandler).start();
  }

  private static class Waiter {
    final Handler<SMTPConnection> handler;
    final Handler<Throwable> connectionExceptionHandler;

    private Waiter(Handler<SMTPConnection> handler, Handler<Throwable> connectionExceptionHandler) {
      this.handler = handler;
      this.connectionExceptionHandler = connectionExceptionHandler;
    }
  }
}

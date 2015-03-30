package io.vertx.ext.mail;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.impl.NoStackTraceThrowable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;

import java.util.Vector;

public class ConnectionPool {

  private static final Logger log = LoggerFactory.getLogger(ConnectionPool.class);

  private Vector<SMTPConnection> connections;
  private Vertx vertx;
  private boolean stopped = false;

  ConnectionPool(Vertx vertx) {
    this.vertx = vertx;
    connections = new Vector<SMTPConnection>();
  }

  void getConnection(MailConfig config, Handler<SMTPConnection> resultHandler, Handler<Throwable> errorHandler) {
    log.debug("getConnection()");
    if (stopped) {
      errorHandler.handle(new NoStackTraceThrowable("connection pool is stopped"));
    } else {
      if(connections.size()==0) {
        createNewConnection(config, resultHandler, errorHandler);
      } else {
        findUsableConnection(connections, config, 0, resultHandler, v -> {
          log.debug("no usable connection found, createNewConnection()");
          createNewConnection(config, resultHandler, errorHandler);
        });
      }
    }
  }

  /**
   * @param config
   * @param resultHandler
   * @param errorHandler
   * @param connections
   */
  private void createNewConnection(MailConfig config, Handler<SMTPConnection> resultHandler,
      Handler<Throwable> errorHandler) {
    log.debug("creating new connection");
    SMTPConnection conn = new SMTPConnection();
    connections.add(conn);
    new SMTPStarter(vertx, conn, config, v -> resultHandler.handle(conn), errorHandler).connect();
  }

  private void findUsableConnection(Vector<SMTPConnection> connections, MailConfig config, int i,
      Handler<SMTPConnection> foundHandler, Handler<Void> notFoundHandler) {
    if (i == connections.size()) {
      notFoundHandler.handle(null);
    } else {
      log.debug("findUsableConnection(" + i + ")");
      SMTPConnection conn = connections.get(i);
      if (!conn.isBroken() && conn.isIdle()) {
        conn.useConnection();
        new SMTPReset(conn, config, v -> foundHandler.handle(conn), v -> {
          // make sure we do not get confused by a close event later
          conn.setBroken();
          findUsableConnection(connections, config, i + 1, foundHandler, notFoundHandler);
        }).rsetCmd();
      } else {
        findUsableConnection(connections, config, i + 1, foundHandler, notFoundHandler);
      }
    }
  }

  void stop() {
    stop(v -> {
    });
  }

  void stop(Handler<Void> finishedHandler) {
    shutdownConnections(0, finishedHandler);
  }

  private void shutdownConnections(int i, Handler<Void> finishedHandler) {
    if (i == connections.size()) {
      finishedHandler.handle(null);
    } else {
      log.debug("STMPConnection.shutdown(" + i + ")");
      SMTPConnection conn = connections.get(i);
      if (!conn.isBroken() && conn.isIdle()) {
        conn.shutdown();
      }
      shutdownConnections(i + 1, finishedHandler);
    }
  }

}

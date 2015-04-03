package io.vertx.ext.mail.impl;

import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.impl.NoStackTraceThrowable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.ext.mail.MailConfig;

import java.util.ArrayList;
import java.util.List;

class ConnectionPool {

  private static final Logger log = LoggerFactory.getLogger(ConnectionPool.class);

  private final List<SMTPConnection> connections;
  private final Vertx vertx;
  private final NetClient netClient;
  private final boolean stopped = false;
  private Context context;

  ConnectionPool(Vertx vertx, MailConfig config, Context context) {
    this.vertx = vertx;
    connections = new ArrayList<SMTPConnection>();
    NetClientOptions netClientOptions = new NetClientOptions().setSsl(config.isSsl());
    netClient = vertx.createNetClient(netClientOptions);
    this.context = context;
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
    SMTPConnection conn = new SMTPConnection(netClient, context);
    connections.add(conn);
    new SMTPStarter(vertx, conn, config, v -> resultHandler.handle(conn), errorHandler).connect();
  }

  private void findUsableConnection(List<SMTPConnection> connections, MailConfig config, int i,
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
    netClient.close();
    if (i == connections.size()) {
      finishedHandler.handle(null);
    } else {
      log.debug("STMPConnection.shutdown(" + i + ")");
      SMTPConnection connection = connections.get(i);
      // TODO: have to wait for active connections still running
      // they will shut down when the operation is finished
      if (connection.isIdle()) {
        if (connection.isBroken()) {
          connection.shutdown();
          shutdownConnections(i + 1, finishedHandler);
        } else {
          connection.setBroken();
          log.debug("shutting down connection");
          new SMTPQuit(connection, v -> {
            connection.shutdown();
            log.debug("connection is shut down");
            shutdownConnections(i + 1, finishedHandler);
          }).quitCmd();
        }
      } else {
        shutdownConnections(i + 1, finishedHandler);
      }
    }
  }

}

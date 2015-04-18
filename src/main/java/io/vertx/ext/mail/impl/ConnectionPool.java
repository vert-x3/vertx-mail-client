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
  private NetClient netClient = null;
  private boolean stopped = false;
  private final MailConfig config;
  private final Context context;

  ConnectionPool(Vertx vertx, MailConfig config, Context context) {
    this.vertx = vertx;
    connections = new ArrayList<SMTPConnection>();
    this.context = context;
    this.config = config;
  }

  /**
   * @param vertx
   * @param config
   */
  private void createNetclient(Handler<Void> finishedHandler) {
    NetClientOptions netClientOptions;
    if(config.getNetClientOptions() == null)  {
      netClientOptions = new NetClientOptions().setSsl(config.isSsl()).setTrustAll(config.isTrustAll());
    } else {
      netClientOptions = config.getNetClientOptions();
    }
    context.runOnContext(v -> {
      netClient = vertx.createNetClient(netClientOptions);
      finishedHandler.handle(null);
    });
  }

  void getConnection(Handler<SMTPConnection> resultHandler, Handler<Throwable> errorHandler) {
    log.debug("getConnection()");
    if (stopped) {
      errorHandler.handle(new NoStackTraceThrowable("connection pool is stopped"));
    } else {
      if (connections.size() == 0) {
        createNewConnection(resultHandler, errorHandler);
      } else {
        findUsableConnection(0, resultHandler, v -> {
          log.debug("no usable connection found, createNewConnection()");
          createNewConnection(resultHandler, errorHandler);
        });
      }
    }
  }

  /**
   * set up NetClient and create a connection.
   * 
   * @param resultHandler
   * @param errorHandler
   */
  private void createNewConnection(Handler<SMTPConnection> resultHandler, Handler<Throwable> errorHandler) {
    log.debug("creating new connection");
    // if we have not yet created the netclient, do that first
    if (netClient == null) {
      createNetclient(v -> {
        createConnection(resultHandler, errorHandler);
      });
    } else {
      createConnection(resultHandler, errorHandler);
    }
  }

  /**
   * really open the connection.
   * 
   * @param config
   * @param resultHandler
   * @param errorHandler
   */
  private void createConnection(Handler<SMTPConnection> resultHandler, Handler<Throwable> errorHandler) {
    SMTPConnection conn = new SMTPConnection(netClient, context, this);
    connections.add(conn);
    new SMTPStarter(vertx, conn, config, v -> resultHandler.handle(conn), errorHandler).start();
  }

  private void findUsableConnection(int i, Handler<SMTPConnection> foundHandler,
      Handler<Void> notFoundHandler) {
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
            removeFromPool(conn);
            // at this point the current element is gone, so no i+1
            findUsableConnection(i, foundHandler, notFoundHandler);
          }).start();
      } else {
        findUsableConnection(i + 1, foundHandler, notFoundHandler);
      }
    }
  }

  /**
   * @param conn
   */
  void removeFromPool(SMTPConnection conn) {
    connections.remove(conn);
    log.debug("removed old connection, new size is "+connections.size());
  }

  void stop() {
    stop(v -> {});
  }

  void stop(Handler<Void> finishedHandler) {
//    if (netClient != null) {
//      netClient.close();
//    }
    stopped = true;
    shutdownConnections(0, finishedHandler);
  }

  private void shutdownConnections(int i, Handler<Void> finishedHandler) {
    if (i == connections.size()) {
      finishedHandler.handle(null);
    } else {
      log.debug("SMTPConnection.shutdown(" + i + ")");
      SMTPConnection connection = connections.get(i);
      if (connection.isBroken()) {
        connection.shutdown();
        removeFromPool(connection);
        shutdownConnections(i, finishedHandler);
      } else {
        if (connection.isIdle()) {
          connection.quitCloseConnection();
        } else {
          // shut down the connection at the end of current send operation
          connection.setDoShutdown();
        }
        shutdownConnections(i + 1, finishedHandler);
      }
    }
  }

}

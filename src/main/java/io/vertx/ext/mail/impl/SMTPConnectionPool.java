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

class SMTPConnectionPool {

  private static final Logger log = LoggerFactory.getLogger(SMTPConnectionPool.class);

  private final ConnectionManager connections;
  private final Vertx vertx;
  private NetClient netClient = null;
  private boolean stopped = false;
  private final MailConfig config;
  private final Context context;

  SMTPConnectionPool(Vertx vertx, MailConfig config, Context context) {
    this.vertx = vertx;
    this.config = config;
    this.context = context;

    connections = new ConnectionManager(config.getMaxPoolSize() == 0 ? 10 : config.getMaxPoolSize()) {
      @Override
      protected void connect(Handler<SMTPConnection> connectHandler, Handler<Throwable> connectErrorHandler,
          Context context, ConnectionLifeCycleListener listener) {
        createNewConnection(connectHandler, connectErrorHandler, listener);
      }
    };
  }

  private void createNetclient(Handler<Void> finishedHandler) {
    NetClientOptions netClientOptions;
    if (config.getNetClientOptions() == null) {
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
      connections.getConnection(resultHandler, errorHandler, context, this);
    }
  }

  /**
   * set up NetClient and create a connection.
   * 
   * @param resultHandler
   * @param errorHandler
   * @param listener
   */
  private void createNewConnection(Handler<SMTPConnection> resultHandler, Handler<Throwable> errorHandler,
      ConnectionLifeCycleListener listener) {
    log.debug("creating new connection");
    // if we have not yet created the netclient, do that first
    if (netClient == null) {
      createNetclient(v -> {
        createConnection(resultHandler, errorHandler, listener);
      });
    } else {
      createConnection(resultHandler, errorHandler, listener);
    }
  }

  /**
   * really open the connection.
   * 
   * @param config
   * @param resultHandler
   * @param errorHandler
   * @param listener
   */
  private void createConnection(Handler<SMTPConnection> resultHandler, Handler<Throwable> errorHandler,
      ConnectionLifeCycleListener listener) {
    SMTPConnection conn = new SMTPConnection(netClient, vertx, context, listener);
    new SMTPStarter(vertx, conn, config, v -> resultHandler.handle(conn), errorHandler).start();
  }

  /**
   * called after the connection has been removed
   * 
   * @param conn
   */
  void afterRemoveFromPool(SMTPConnection conn) {
    log.debug("afterRemoveFromPool()");
    log.debug("removed old connection, new size is " + connections.size());
    if (stopped && connections.size() == 0) {
      log.debug("final shutdown finished");
      if (netClient != null) {
        log.debug("closing netClient");
        netClient.close();
        netClient = null;
      }
    }
  }

  void stop() {
    stop(v -> {
    });
  }

  void stop(Handler<Void> finishedHandler) {
    log.debug("ConnectionPool.stop(...)");
    stopped = true;
    if (connections != null) {
      connections.close();
    }
    finishedHandler.handle(null);
  }

}

package io.vertx.ext.mail;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class ConnectionPool {

  private static final Logger log = LoggerFactory.getLogger(ConnectionPool.class);

  private Map<MailConfig, Vector<SMTPConnection>> connectionsByConfig;
  private Vertx vertx;

  ConnectionPool(Vertx vertx) {
    this.vertx = vertx;
    connectionsByConfig = new HashMap<MailConfig, Vector<SMTPConnection>>();
  }

  void getConnection(MailConfig config, Handler<SMTPConnection> resultHandler, Handler<Throwable> errorHandler) {
    log.info("getConnection()");
    Vector<SMTPConnection> connections = connectionsByConfig.get(config);
    if (connections == null) {
      log.info("create connections Vector");
      connections = new Vector<SMTPConnection>();
      connectionsByConfig.put(config, connections);
      log.info("createNewConnection()");
      createNewConnection(config, resultHandler, errorHandler);
    } else {
      log.info("findUsableConnection()");
      findUsableConnection(connections, config, 0, resultHandler, v -> {
        log.info("no usable connection found, createNewConnection()");
        createNewConnection(config, resultHandler, errorHandler);
      });
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
    log.info("creating new connection");
    SMTPConnection conn = new SMTPConnection();
    connectionsByConfig.get(config).add(conn);
    new SMTPStarter(vertx, conn, config, v -> {
      resultHandler.handle(conn);
    }, errorHandler).connect();
  }

  private void findUsableConnection(Vector<SMTPConnection> connections, MailConfig config, int i,
      Handler<SMTPConnection> foundHandler, Handler<Void> notFoundHandler) {
    if (i == connections.size()) {
      notFoundHandler.handle(null);
    } else {
      log.info("findUsableConnection("+i+")");
      SMTPConnection conn = connections.get(i);
      if (conn.isActive() && conn.isIdle()) {
        conn.useConnection();
        new SMTPReset(conn, config, v -> {
          foundHandler.handle(conn);
        }, v -> {
          // make sure we do not get confused by a close event later
          conn.setInactive();
          findUsableConnection(connections, config, i + 1, foundHandler, notFoundHandler);
        }).rsetCmd();
      } else {
        findUsableConnection(connections, config, i + 1, foundHandler, notFoundHandler);
      }
    }
  }
}

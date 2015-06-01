package io.vertx.ext.mail.impl;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.mail.MailConfig;

/**
 * TODO: this encapsulates initial dialogue and authentication, might as well
 * put authentication into the initial class
 * Issue #23
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
class SMTPStarter {

  private static final Logger log = LoggerFactory.getLogger(SMTPStarter.class);

  Vertx vertx;
  SMTPConnection connection;
  MailConfig config;
  Handler<Void> finishedHandler;
  Handler<Throwable> errorHandler;

  SMTPStarter(Vertx vertx, SMTPConnection connection, MailConfig config, Handler<Void> finishedHandler,
              Handler<Throwable> errorHandler) {
    this.vertx = vertx;
    this.connection = connection;
    this.config = config;
    this.finishedHandler = finishedHandler;
    this.errorHandler = errorHandler;
  }

  void start() {
    log.debug("connection.openConnection");
    connection.openConnection(config, this::serverGreeting, this::handleError);
  }

  private void serverGreeting(String message) {
    log.debug("SMTPInitialDialogue");
    new SMTPInitialDialogue(connection, config, v -> doAuthentication(), this::handleError).start(message);
  }

  private void doAuthentication() {
    log.debug("SMTPAuthentication");
    new SMTPAuthentication(connection, config, finishedHandler, this::handleError).start();
  }

  private void handleError(Throwable throwable) {
    log.debug("handleError:" + throwable);
    if (connection != null) {
      connection.setBroken();
    }
    errorHandler.handle(throwable);
  }

}

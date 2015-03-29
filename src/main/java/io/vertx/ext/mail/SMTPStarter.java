package io.vertx.ext.mail;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;

public class SMTPStarter {

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

  void connect() {
    log.debug("connection.openConnection");
    connection.openConnection(vertx, config, this::serverGreeting, this::handleError);
  }
  
  private void serverGreeting(String message) {
    log.debug("SMTPInitialDialogue");
    new SMTPInitialDialogue(connection, config, this::doAuthentication, this::handleError).serverGreeting(message);
  }

  private void doAuthentication(Void v) {
    log.debug("SMTPAuthentication");
    new SMTPAuthentication(connection, config, finishedHandler, this::handleError).startAuthentication();
  }

//  private void handleError(String message) {
//    errorHandler.handle(new NoStackTraceThrowable(message));
//  }

  private void handleError(Throwable throwable) {
    log.debug("handleError:"+throwable);
    if (connection != null) {
      log.debug("connection.setInactive");
      connection.setBroken();
    }
    errorHandler.handle(throwable);
  }

}

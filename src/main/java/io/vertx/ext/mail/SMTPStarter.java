package io.vertx.ext.mail;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;

public class SMTPStarter {
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
    connection.openConnection(vertx, config, this::serverGreeting, this::throwError);
  }
  
  private void serverGreeting(String message) {
    new SMTPInitialDialogue(connection, config, this::doAuthentication, this::throwError).serverGreeting(message);
  }

  private void doAuthentication(Void v) {
    new SMTPAuthentication(connection, config, finishedHandler, this::throwError).startAuthentication();
  }

//  private void throwError(String message) {
//    errorHandler.handle(new NoStackTraceThrowable(message));
//  }

  private void throwError(Throwable throwable) {
    errorHandler.handle(throwable);
  }

}

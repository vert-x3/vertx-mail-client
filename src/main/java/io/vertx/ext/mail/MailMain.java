package io.vertx.ext.mail;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;

/*
 * main operation of the smtp client
 *
 * this class takes care of the different SMTP steps, EHLO, AUTH etc
 * by calling the different SMTP* classes
 */

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
class MailMain {

  private static final Logger log = LoggerFactory.getLogger(MailMain.class);

  private Vertx vertx;
  private Handler<AsyncResult<JsonObject>> finishedHandler;
  private MailConfig config;

  private MailMessage email;
  private String mailMessage;

  private SMTPConnection connection = null;

  public MailMain(Vertx vertx, MailConfig config, Handler<AsyncResult<JsonObject>> finishedHandler) {
    this.vertx = vertx;
    this.config = config;
    this.finishedHandler = finishedHandler;
  }

  /**
   * start a mail send operation using the MailMessage object
   * 
   * @param email
   *          the mail to send
   */
  void sendMail(MailMessage email) {
    this.email = email;
    doSend();
  }

  /**
   * start a mail send operation using the parameters from MailMessage object
   * and a pregenerated message provided as String
   * 
   * @param email
   *          the mail parameters (from, to, etc)
   * @param message
   *          the message to send
   */
  void sendMail(MailMessage email, String message) {
    this.email = email;
    mailMessage = message;
    doSend();
  }

  private void doSend() {
    validateHeaders();
    connection = new SMTPConnection();
    connection.openConnection(vertx, config, this::serverGreeting, this::throwError);
  }

  // do some validation before we open the connection
  private void validateHeaders() {
    if (email.getBounceAddress() == null && email.getFrom() == null) {
      throwError("sender address is not present");
    } else if ((email.getTo() == null || email.getTo().size() == 0)
        && (email.getCc() == null || email.getCc().size() == 0)
        && (email.getBcc() == null || email.getBcc().size() == 0)) {
      log.warn("no recipient addresses are present");
      throwError("no recipient addresses are present");
    }
  }

  private void serverGreeting(String message) {
    new SMTPInitialDialogue(connection, config, this::doAuthentication, this::throwError).serverGreeting(message);
  }

  private void doAuthentication(Void v) {
    new SMTPAuthentication(connection, config, this::sendMessage, this::throwError).startAuthentication();
  }

  private void sendMessage(Void v) {
    new SMTPSendMail(connection, email, mailMessage, this::finishMail, this::throwError).startMail();
  }

  private void finishMail(Void v) {
    new SMTPQuit(connection).quitCmd();
    JsonObject result = new JsonObject();
    result.put("result", "success");
    returnResult(Future.succeededFuture(result));
  }

  private void throwError(Throwable throwable) {
    returnResult(Future.failedFuture(throwable));
  }

  private void throwError(String message) {
    returnResult(Future.failedFuture(message));
  }

  private void returnResult(Future<JsonObject> result) {
    if (connection != null) {
      connection.shutdown();
    }
    finishedHandler.handle(result);
  }

}

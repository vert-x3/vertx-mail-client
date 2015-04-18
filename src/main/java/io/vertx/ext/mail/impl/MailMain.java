package io.vertx.ext.mail.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.mail.MailMessage;

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

  private Handler<AsyncResult<JsonObject>> finishedHandler;

  private MailMessage email;
  private String mailMessage;

  private SMTPConnection connection = null;

  private ConnectionPool connectionPool;

  public MailMain(ConnectionPool connectionPool, Handler<AsyncResult<JsonObject>> finishedHandler) {
    this.connectionPool = connectionPool;
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
    if(validateHeaders()) {
      connectionPool.getConnection(this::sendMessage, this::handleError);
    }
  }

  // do some validation before we open the connection
  // return true on successful validation so we can stop processing above
  private boolean validateHeaders() {
    if (email.getBounceAddress() == null && email.getFrom() == null) {
      handleError("sender address is not present");
      return false;
    } else if ((email.getTo() == null || email.getTo().size() == 0)
        && (email.getCc() == null || email.getCc().size() == 0)
        && (email.getBcc() == null || email.getBcc().size() == 0)) {
      log.warn("no recipient addresses are present");
      handleError("no recipient addresses are present");
      return false;
    } else {
      return true;
    }
  }

  private void sendMessage(SMTPConnection connection) {
    log.debug("got a connection");
    this.connection=connection;
    new SMTPSendMail(connection, email, mailMessage, v -> finishMail(), this::handleError).startMail();
  }

  private void finishMail() {
    log.debug("finishMail");
    if (connection != null) {
      connection.returnToPool();
    }
    JsonObject result = new JsonObject();
    result.put("result", "success");
    returnResult(Future.succeededFuture(result));
  }

  private void handleError(Throwable throwable) {
    log.debug("handleError:"+throwable);
    if (connection != null) {
      log.debug("connection.setInactive");
      connection.setBroken();
    }
    returnResult(Future.failedFuture(throwable));
  }

  private void handleError(String message) {
    log.debug("handleError:"+message);
    if (connection != null) {
      log.debug("connection.setInactive");
      connection.setBroken();
    }
    returnResult(Future.failedFuture(message));
  }

  private void returnResult(Future<JsonObject> result) {
    if(finishedHandler!=null) {
      finishedHandler.handle(result);
    } else {
      if(result.succeeded()) {
        log.debug("dropping sendMail result");
      } else {
        log.info("dropping sendMail failure", result.cause());
      }
    }
  }

}

package io.vertx.ext.mail.impl;

import io.vertx.core.Handler;
import io.vertx.core.impl.NoStackTraceThrowable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.MailResult;
import io.vertx.ext.mail.mailencoder.EmailAddress;
import io.vertx.ext.mail.mailencoder.MailEncoder;

import java.util.ArrayList;
import java.util.List;

class SMTPSendMail {

  private static final Logger log = LoggerFactory.getLogger(SMTPSendMail.class);

  private final SMTPConnection connection;
  private MailMessage email;
  private final MailConfig config;
  private final Handler<MailResult> finishedHandler;
  private final Handler<Throwable> exceptionHandler;
  private final MailResult mailResult;

  private String mailMessage;

  SMTPSendMail(SMTPConnection connection, MailMessage email, MailConfig config, Handler<MailResult> finishedHandler,
               Handler<Throwable> exceptionHandler) {
    this.connection = connection;
    this.email = email;
    this.config = config;
    this.finishedHandler = finishedHandler;
    this.exceptionHandler = exceptionHandler;
    mailResult = new MailResult();
  }

  void startMail() {
    if (checkSize()) {
      mailFromCmd();
    }
  }

  /**
   * Check if message size is allowed if size is supported.
   * <p>
   * returns true if the message is allowed, have to make sure
   * that when returning from the handleError method it doesn't continue with the mail from
   * operation
   */
  private boolean checkSize() {
    if (connection.getCapa().getSize() > 0) {
      createMailMessage();
      if (mailMessage.length() > connection.getCapa().getSize()) {
        handleError("message exceeds allowed size limit");
        return false;
      } else {
        return true;
      }
    } else {
      return true;
    }
  }

  private void mailFromCmd() {
    try {
      String fromAddr;
      String bounceAddr = email.getBounceAddress();
      if (bounceAddr != null && !bounceAddr.isEmpty()) {
        fromAddr = bounceAddr;
      } else {
        fromAddr = email.getFrom();
      }
      EmailAddress from = new EmailAddress(fromAddr);
      connection.write("MAIL FROM:<" + from.getEmail() + ">", message -> {
        log.debug("MAIL FROM result: " + message);
        if (StatusCode.isStatusOk(message)) {
          rcptToCmd();
        } else {
          log.warn("sender address not accepted: " + message);
          handleError("sender address not accepted: " + message);
        }
      });
    } catch (IllegalArgumentException e) {
      log.error("address exception", e);
      handleError(e);
    }
  }

  private void rcptToCmd() {
    List<String> recipientAddrs = new ArrayList<String>();
    if (email.getTo() != null) {
      recipientAddrs.addAll(email.getTo());
    }
    if (email.getCc() != null) {
      recipientAddrs.addAll(email.getCc());
    }
    if (email.getBcc() != null) {
      recipientAddrs.addAll(email.getBcc());
    }
    rcptToCmd(recipientAddrs, 0);
  }

  private void rcptToCmd(List<String> recipientAddrs, int i) {
    try {
      EmailAddress toAddr = new EmailAddress(recipientAddrs.get(i));
      connection.write("RCPT TO:<" + toAddr.getEmail() + ">", message -> {
        log.debug("RCPT TO result: " + message);
        if (StatusCode.isStatusOk(message)) {
          mailResult.getRecipients().add(toAddr.getEmail());
          if (i + 1 < recipientAddrs.size()) {
            rcptToCmd(recipientAddrs, i + 1);
          } else {
            dataCmd();
          }
        } else {
          log.warn("recipient address not accepted: " + message);
          handleError("recipient address not accepted: " + message);
        }
      });
    } catch (IllegalArgumentException e) {
      log.error("address exception", e);
      handleError(e);
    }
  }

  private void handleError(Throwable throwable) {
    exceptionHandler.handle(throwable);
  }

  private void handleError(String message) {
    handleError(new NoStackTraceThrowable(message));
  }

  private void dataCmd() {
    connection.write("DATA", message -> {
      log.debug("DATA result: " + message);
      if (StatusCode.isStatusOk(message)) {
        sendMaildata();
      } else {
        log.warn("DATA command not accepted: " + message);
        handleError("DATA command not accepted: " + message);
      }
    });
  }

  private void sendMaildata() {
    // create the message here if it hasn't been created
    // for the size check above
    createMailMessage();
    // convert message to escape . at the start of line
    // TODO: this is probably bad for large messages
    // TODO: it is probably required to convert \n to \r\n to be completely
    // SMTP compliant
    connection.write(mailMessage.replaceAll("\n\\.", "\n..") + "\r\n.", message -> {
      log.debug("maildata result: " + message);
      if (StatusCode.isStatusOk(message)) {
        finishedHandler.handle(mailResult);
      } else {
        log.warn("sending data failed: " + message);
        handleError("sending data failed: " + message);
      }
    });
  }

  /**
   * create message if it hasn't been already
   */
  private void createMailMessage() {
    if (mailMessage == null) {
      MailEncoder encoder = new MailEncoder(email, config.getEhloHostname());
      mailMessage = encoder.encode();
      mailResult.setMessageID(encoder.getMessageID());
    }
  }

}

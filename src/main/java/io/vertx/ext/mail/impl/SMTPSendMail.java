/*
 *  Copyright (c) 2011-2015 The original author or authors
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *       The Eclipse Public License is available at
 *       http://www.eclipse.org/legal/epl-v10.html
 *
 *       The Apache License v2.0 is available at
 *       http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 */

package io.vertx.ext.mail.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.impl.NoStackTraceThrowable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
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
  private final MailMessage email;
  private final MailConfig config;
  private final Handler<AsyncResult<MailResult>> resultHandler;
  private final MailResult mailResult;
  private final String hostname;

  private String mailMessage;

  SMTPSendMail(SMTPConnection connection, MailMessage email, MailConfig config, String hostname, Handler<AsyncResult<MailResult>> resultHandler) {
    this.connection = connection;
    this.email = email;
    this.config = config;
    this.resultHandler = resultHandler;
    mailResult = new MailResult();
    this.hostname = hostname;
  }

  void start() {
    try {
      if (checkSize()) {
        mailFromCmd();
      }
    } catch (Exception e) {
      handleError(e);
    }
  }

  /**
   * Check if message size is allowed if size is supported.
   * <p>
   * returns true if the message is allowed, have to make sure that when returning from the handleError method it
   * doesn't continue with the mail from operation
   */
  private boolean checkSize() {
    final int size = connection.getCapa().getSize();
    if (size > 0) {
      createMailMessage();
      if (mailMessage.length() > size) {
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
      String sizeParameter;
      if (connection.getCapa().getSize() > 0) {
        sizeParameter = " SIZE=" + mailMessage.length();
      } else {
        sizeParameter = "";
      }
      connection.write("MAIL FROM:<" + from.getEmail() + ">" + sizeParameter, message -> {
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
    List<String> recipientAddrs = new ArrayList<>();
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
        if (StatusCode.isStatusOk(message)) {
          log.debug("RCPT TO result: " + message);
          mailResult.getRecipients().add(toAddr.getEmail());
          nextRcpt(recipientAddrs, i);
        } else {
          if (config.isAllowRcptErrors()) {
            log.warn("recipient address not accepted, continuing: " + message);
            nextRcpt(recipientAddrs, i);
          } else {
            log.warn("recipient address not accepted: " + message);
            handleError("recipient address not accepted: " + message);
          }
        }
      });
    } catch (IllegalArgumentException e) {
      log.error("address exception", e);
      handleError(e);
    }
  }

  private void nextRcpt(List<String> recipientAddrs, int i) {
    if (i + 1 < recipientAddrs.size()) {
      rcptToCmd(recipientAddrs, i + 1);
    } else {
      if (mailResult.getRecipients().size() > 0) {
        dataCmd();
      } else {
        log.warn("no recipient addresses were accepted, not sending mail");
        handleError("no recipient addresses were accepted, not sending mail");
      }
    }
  }

  private void handleError(Throwable throwable) {
    resultHandler.handle(Future.failedFuture(throwable));
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

    sendLineByLine(0, mailMessage.length());
  }

  private void sendLineByLine(int index, int length) {
    while (index < length) {
      int nextIndex = mailMessage.indexOf('\n', index);
      String line;
      if (nextIndex == -1) {
        line = mailMessage.substring(index);
        nextIndex = length;
      } else {
        line = mailMessage.substring(index, nextIndex);
        nextIndex++;
      }
      if (line.startsWith(".")) {
        line = "." + line;
      }
      final int nextIndexFinal = nextIndex;
      final boolean mayLog = nextIndex < 1000;
      if (connection.writeQueueFull()) {
        connection.writeLineWithDrainHandler(line, mayLog, v -> sendLineByLine(nextIndexFinal, length));
        // call to our handler will finish the whole message, we just return here
        return;
      } else {
        connection.writeLine(line, mayLog);
        index = nextIndex;
      }
    }
    connection.write(".", message -> {
      log.debug("maildata result: " + message);
      if (StatusCode.isStatusOk(message)) {
        resultHandler.handle(Future.succeededFuture(mailResult));
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
      MailEncoder encoder = new MailEncoder(email, hostname);
      mailMessage = encoder.encode();
      mailResult.setMessageID(encoder.getMessageID());
    }
  }

}

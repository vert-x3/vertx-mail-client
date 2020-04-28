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

import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.streams.ReadStream;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.MailResult;
import io.vertx.ext.mail.mailencoder.EmailAddress;
import io.vertx.ext.mail.mailencoder.EncodedPart;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * This is responsible to start an email transaction.
 *
 * @author <a href="mailto: aoingl@gmail.com">Lin Gao</a>
 */
class SMTPSendMail {

  private static final Logger log = LoggerFactory.getLogger(SMTPSendMail.class);
  private static final Pattern linePattern = Pattern.compile("\r\n");

  private final SMTPConnection connection;
  private final MailMessage email;
  private final MailConfig config;
  private final MailResult mailResult;
  private final EncodedPart encodedPart;
  private final AtomicLong written = new AtomicLong();

  SMTPSendMail(SMTPConnection connection, MailMessage email, MailConfig config,
               EncodedPart encodedPart, String messageId) {
    this.connection = connection;
    this.email = email;
    this.config = config;
    this.mailResult = new MailResult();
    this.encodedPart = encodedPart;
    this.mailResult.setMessageID(messageId);
  }

  /**
   * Starts a mail transaction.
   */
  void startMailTransaction(final Handler<AsyncResult<MailResult>> resultHandler) {
    sendMailEnvelope()
      .flatMap(this::sendMailData)
      .onComplete(resultHandler);
  }

  /**
   * Check if message size is allowed if size is supported.
   * <p>
   * returns true if the message is allowed.
   */
  private boolean checkSize() {
    final int size = connection.getCapa().getSize();
    return size == 0 || size >= encodedPart.size();
  }

  private String mailFromAddress() {
    final String fromAddr;
    String bounceAddr = email.getBounceAddress();
    if (bounceAddr != null && !bounceAddr.isEmpty()) {
      fromAddr = bounceAddr;
    } else {
      fromAddr = email.getFrom();
    }
    EmailAddress from = new EmailAddress(fromAddr);
    return from.getEmail();
  }

  private String sizeParameter() {
    final String sizeParameter;
    if (connection.getCapa().getSize() > 0) {
      sizeParameter = " SIZE=" + encodedPart.size();
    } else {
      sizeParameter = "";
    }
    return sizeParameter;
  }

  private List<String> allRecipients() {
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
    return recipientAddrs.stream().map(r -> {
      final String email;
      if (EmailAddress.POSTMASTER.equalsIgnoreCase(r)) {
        email = r;
      } else {
        email = new EmailAddress(r).getEmail();
      }
      return email;
    }).collect(Collectors.toList());
  }

  private Future<Boolean> sendMailEnvelope() {
    Promise<Boolean> envelopePromise = Promise.promise();
    try {
      if (checkSize()) {
        final String mailFromLine = "MAIL FROM:<" + mailFromAddress() + ">" + sizeParameter();
        final List<String> allRecipients = allRecipients();
        if (config.isPipelining() && connection.getCapa().isCapaPipelining()) {
          final List<String> groupCommands = new ArrayList<>();
          groupCommands.add(mailFromLine);
          groupCommands.addAll(allRecipients.stream().map(r -> "RCPT TO:<" + r + ">").collect(Collectors.toList()));
          groupCommands.add("DATA");
          connection.writeCommands(groupCommands).onComplete(envelope -> {
            if (envelope.failed()) {
              envelopePromise.fail(envelope.cause());
              return;
            }
            String[] envelopeResult = linePattern.split(envelope.result());
            if (groupCommands.size() != envelopeResult.length) {
              envelopePromise.fail("Sent " + groupCommands.size() + " commands, but got " + envelopeResult.length + " responses.");
            } else {
              // result follows the same order in the commands list
              for (int i = 0; i < envelopeResult.length; i ++) {
                String message = envelopeResult[i];
                if (i == 0) {
                  if (!StatusCode.isStatusOk(message)) {
                    envelopePromise.fail("sender address not accepted: " + message);
                    return;
                  }
                } else if (i < envelopeResult.length - 1) {
                  if (StatusCode.isStatusOk(message)) {
                    mailResult.getRecipients().add(allRecipients.get(i - 1));
                  } else {
                    if (!config.isAllowRcptErrors()) {
                      envelopePromise.fail("recipient address not accepted: " + message);
                      return;
                    }
                  }
                } else {
                  // DATA result
                  if (StatusCode.isStatusOk(message)) {
                    if (mailResult.getRecipients().size() == 0) {
                      // send dot only
                      envelopePromise.complete(false);
                      return;
                    }
                  } else {
                    envelopePromise.fail("DATA command not accepted: " + message);
                    return;
                  }
                }
              }
              envelopePromise.complete(true);
            }
          });
        } else {
          // sent line by line because PIPELINING is not supported
          Future<String> future = sendMailFrom(mailFromLine);
          for (String email: allRecipients) {
            future = future.flatMap(v -> sendRcptTo(email));
          }
          return future.flatMap(v -> sendDataCmd());
        }
      } else {
        envelopePromise.fail("message exceeds allowed size limit");
      }
    } catch (Exception e) {
      envelopePromise.fail(e);
    }
    return envelopePromise.future();
  }

  private Future<String> sendMailFrom(String mailFromLine) {
    return connection.writeWithReply(mailFromLine).flatMap(message -> {
      if (log.isDebugEnabled()) {
        written.getAndAdd(mailFromLine.length());
      }
      if (StatusCode.isStatusOk(message)) {
        return Future.succeededFuture(message);
      } else {
        return Future.failedFuture("sender address not accepted: " + message);
      }
    });
  }

  private Future<String> sendRcptTo(String email) {
    final String line =  "RCPT TO:<" + email + ">";
    return connection.writeWithReply(line).flatMap(message -> {
      if (log.isDebugEnabled()) {
        written.getAndAdd(line.length());
      }
      try {
        if (StatusCode.isStatusOk(message)) {
          mailResult.getRecipients().add(email);
          return Future.succeededFuture(message);
        } else {
          if (config.isAllowRcptErrors()) {
            return Future.succeededFuture(message);
          } else {
            return Future.failedFuture("recipient address not accepted: " + message);
          }
        }
      } catch (Exception e) {
        return Future.failedFuture(e);
      }
    });
  }

  private Future<Boolean> sendDataCmd() {
    if (mailResult.getRecipients().size() > 0) {
      return connection.writeWithReply("DATA").flatMap(message -> {
        if (log.isDebugEnabled()) {
          written.getAndAdd(4);
        }
        try {
          if (StatusCode.isStatusOk(message)) {
            return Future.succeededFuture(true);
          } else {
            return Future.failedFuture("DATA command not accepted: " + message);
          }
        } catch (Exception e) {
          return Future.failedFuture(e);
        }
      });
    }
    return Future.failedFuture("no recipient addresses were accepted, not sending mail");
  }

  private Future<MailResult> sendMailData(boolean includeData) {
    if (!includeData) {
      return sendEndDot();
    }
    return sendMailHeaders(this.encodedPart.headers())
      .flatMap(v -> sendMailBody())
      .flatMap(v -> sendEndDot());
  }

  private Future<Void> sendMailHeaders(MultiMap headers) {
    Promise<Void> promise = Promise.promise();
    try {
      StringBuilder sb = new StringBuilder();
      headers.forEach(header -> sb.append(header.getKey()).append(": ").append(header.getValue()).append("\r\n"));
      final String headerLines = sb.toString();
      connection.writeLineWithDrainPromise(headerLines, written.getAndAdd(headerLines.length()) < 1000, promise);
    } catch (Exception e) {
      promise.fail(e);
    }
    return promise.future();
  }

  private Future<MailResult> sendEndDot() {
    return connection.writeWithReply(".").flatMap(msg -> {
      try {
        if (StatusCode.isStatusOk(msg)) {
          return Future.succeededFuture(mailResult);
        } else {
          return Future.failedFuture("sending data failed: " + msg);
        }
      } catch (Exception e) {
        return Future.failedFuture(e);
      }
    });
  }

  private Future<Void> sendMailBody() {
    Promise<Void> promise = Promise.promise();
    final EncodedPart part = this.encodedPart;
    try {
      if (isMultiPart(part)) {
        sendMultiPart(part, 0, promise);
      } else {
        sendRegularPartBody(part, promise);
      }
    } catch (Exception e) {
      promise.fail(e);
    }
    return promise.future();
  }

  private void sendMultiPart(EncodedPart multiPart, final int i, Promise<Void> promise) {
    try {
      final String boundaryStart = "--" + multiPart.boundary();
      final EncodedPart thePart = multiPart.parts().get(i);

      Promise<Void> boundaryStartPromise = Promise.promise();
      boundaryStartPromise.future()
        .flatMap(v -> sendMailHeaders(thePart.headers())).onComplete(v -> {
        if (v.succeeded()) {
          Promise<Void> nextPromise = Promise.promise();
          nextPromise.future().onComplete(vv -> {
            if (vv.succeeded()) {
              if (i == multiPart.parts().size() - 1) {
                String boundaryEnd = boundaryStart + "--";
                connection.writeLineWithDrainPromise(boundaryEnd, written.getAndAdd(boundaryEnd.length()) < 1000, promise);
              } else {
                sendMultiPart(multiPart, i + 1, promise);
              }
            } else {
              promise.fail(vv.cause());
            }
          });
          if (isMultiPart(thePart)) {
            sendMultiPart(thePart, 0, nextPromise);
          } else {
            sendRegularPartBody(thePart, nextPromise);
          }
        } else {
          promise.fail(v.cause());
        }
      });
      connection.writeLineWithDrainPromise(boundaryStart, written.getAndAdd(boundaryStart.length()) < 1000, boundaryStartPromise);
    } catch (Exception e) {
      promise.fail(e);
    }
  }

  private boolean isMultiPart(EncodedPart part) {
    return part.parts() != null && part.parts().size() > 0;
  }

  private void sendBodyLineByLine(String[] lines, int i, Promise<Void> promise) {
    if (i < lines.length) {
      String line = lines[i];
      if (line.startsWith(".")) {
        line = "." + line;
      }
      Promise<Void> writeLinePromise = Promise.promise();
      connection.writeLineWithDrainPromise(line, written.getAndAdd(line.length()) < 1000, writeLinePromise);
      writeLinePromise.future().onComplete(v -> {
        if (v.succeeded()) {
          sendBodyLineByLine(lines, i + 1, promise);
        } else {
          promise.fail(v.cause());
        }
      });
    } else {
      promise.complete();
    }
  }

  private void sendRegularPartBody(EncodedPart part, Promise<Void> promise) {
    if (part.body() != null) {
      // send body string line by line
      sendBodyLineByLine(part.body().split("\n"), 0, promise);
    } else {
      ReadStream<Buffer> attachBodyStream = part.bodyStream(connection.getContext());
      if (attachBodyStream != null) {
        attachBodyStream.pipe().endOnComplete(false).to(connection.getSocket(), promise);
      } else {
        promise.fail(new IllegalStateException("No mail body and stream found"));
      }
    }
  }

}

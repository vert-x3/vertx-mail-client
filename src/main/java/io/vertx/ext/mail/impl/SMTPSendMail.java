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

import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.impl.NoStackTraceThrowable;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.streams.ReadStream;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.MailResult;
import io.vertx.ext.mail.mailencoder.EmailAddress;
import io.vertx.ext.mail.mailencoder.EncodedPart;
import io.vertx.ext.mail.mailencoder.MailEncoder;
import io.vertx.ext.mail.mailencoder.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

class SMTPSendMail {

  private static final Logger log = LoggerFactory.getLogger(SMTPSendMail.class);

  private final SMTPConnection connection;
  private final MailMessage email;
  private final MailConfig config;
  private final Handler<AsyncResult<MailResult>> resultHandler;
  private final MailResult mailResult;
  private final EncodedPart encodedPart;
  private final AtomicLong written = new AtomicLong();

  SMTPSendMail(SMTPConnection connection, MailMessage email, MailConfig config, String hostname, Handler<AsyncResult<MailResult>> resultHandler) {
    this.connection = connection;
    this.email = email;
    this.config = config;
    this.resultHandler = resultHandler;
    this.mailResult = new MailResult();
    final MailEncoder encoder = new MailEncoder(email, hostname);
    this.encodedPart = encoder.encodeMail();
    this.mailResult.setMessageID(encoder.getMessageID());
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
    if (size > 0 && encodedPart.size() > size) {
      handleError("message exceeds allowed size limit");
      return false;
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
        sizeParameter = " SIZE=" + encodedPart.size();
      } else {
        sizeParameter = "";
      }
      final String line = "MAIL FROM:<" + from.getEmail() + ">" + sizeParameter;
      connection.write(line, message -> {
        if (log.isDebugEnabled()) {
          written.getAndAdd(line.length());
          log.debug("MAIL FROM result: " + message);
        }
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
      final String line = "RCPT TO:<" + toAddr.getEmail() + ">";
      connection.write(line, message -> {
        if (log.isDebugEnabled()) {
          written.getAndAdd(line.length());
        }
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
      if (log.isDebugEnabled()) {
        written.getAndAdd(4);
        log.debug("DATA result: " + message);
      }
      if (StatusCode.isStatusOk(message)) {
        Promise<Void> promise = Promise.promise();
        promise.future().setHandler(endDotLineHandler());
        sendMaildata(promise);
      } else {
        log.warn("DATA command not accepted: " + message);
        handleError("DATA command not accepted: " + message);
      }
    });
  }

  private Handler<AsyncResult<Void>> endDotLineHandler() {
    return r -> {
      if (r.succeeded()) {
        connection.getContext().runOnContext(v -> connection.write(".", msg -> {
          if (StatusCode.isStatusOk(msg)) {
            resultHandler.handle(Future.succeededFuture(mailResult));
          } else {
            log.warn("sending data failed: " + msg);
            handleError("sending data failed: " + msg);
          }
        }));
      } else {
        handleError(r.cause());
      }
    };
  }

  private void sendMaildata(Promise<Void> promise) {
    final EncodedPart part = this.encodedPart;
    sendMailHeaders(part.headers().entries(), 0).setHandler(v -> {
      if (v.succeeded()) {
        if (isMultiPart(part)) {
          sendMultiPart(part, 0, promise);
        } else {
          sendRegularPartBody(part, promise);
        }
      } else {
        promise.fail(v.cause());
      }
    });
  }

  private void sendMultiPart(EncodedPart multiPart, final int i, Promise<Void> promise) {
    try {
      final String boundaryStart = "--" + multiPart.boundary();
      final EncodedPart thePart = multiPart.parts().get(i);

      Promise<Void> boundaryStartPromise = Promise.promise();
      boundaryStartPromise.future()
        .compose(v -> sendMailHeaders(thePart.headers().entries(), 0)).setHandler(v -> {
        if (v.succeeded()) {
          Promise<Void> nextPromise = Promise.promise();
          nextPromise.future().setHandler(vv -> {
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

  private Future<Void> sendMailHeaders(List<Map.Entry<String, String>> headers, int i) {
    Promise<Void> promise = Promise.promise();
    sendMailHeaders(headers, i, promise);
    return promise.future();
  }

  private void sendMailHeaders(List<Map.Entry<String, String>> headers, int i, Promise<Void> promise) {
    if (i < headers.size()) {
      String entryString = headers.get(i).toString();
      Promise<Void> next = Promise.promise();
      next.future().setHandler(v -> {
        if (v.succeeded()) {
          sendMailHeaders(headers, i + 1, promise);
        } else {
          promise.fail(v.cause());
        }
      });
      connection.writeLineWithDrainPromise(entryString, written.getAndAdd(entryString.length()) < 1000, next);
    } else {
      connection.writeLineWithDrainPromise("", written.get() < 1000, promise);
    }
  }

  private void sendBodyLineByLine(String[] lines, int i, Promise<Void> promise) {
    if (i < lines.length) {
      String line = lines[i];
      if (line.startsWith(".")) {
        line = "." + line;
      }
      Promise<Void> writeLinePromise = Promise.promise();
      connection.writeLineWithDrainPromise(line, written.getAndAdd(line.length()) < 1000, writeLinePromise);
      writeLinePromise.future().setHandler(v -> {
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
    } else if (part.bodyStream() != null) {
      // send attachment ReadStream as Base64 encoding
      BodyReadStream bodyReadStream = new BodyReadStream(part.bodyStream());
      bodyReadStream.pipe().endOnComplete(false).to(connection.getSocket(), promise);
    } else {
      promise.fail(new IllegalStateException("No mail body and stream found"));
    }
  }

  private Handler<Void> handlerInContext(Handler<Void> handler) {
    return vv -> connection.getContext().runOnContext(handler);
  }

  // what we need: strings line by line with CRLF as line terminator
  private class BodyReadStream implements ReadStream<Buffer> {

    private final ReadStream<Buffer> stream;

    // 57 / 3 * 4 = 76, plus CRLF is 78, which is the email line length limit.
    // see: https://tools.ietf.org/html/rfc5322#section-2.1.1
    private final int size = 57;
    private Buffer streamBuffer;
    private Handler<Buffer> handler;

    private BodyReadStream(ReadStream<Buffer> stream) {
      Objects.requireNonNull(stream, "ReadStream cannot be null");
      this.stream = stream;
      this.streamBuffer = Buffer.buffer();
    }

    @Override
    public BodyReadStream exceptionHandler(Handler<Throwable> handler) {
      if (handler != null) {
        stream.exceptionHandler(handler);
      }
      return this;
    }

    @Override
    public BodyReadStream handler(@Nullable Handler<Buffer> handler) {
      if (handler == null) {
        return this;
      }
      this.handler = handler;
      stream.handler(b -> connection.getContext().runOnContext(v -> {
        Buffer buffer = streamBuffer.appendBuffer(b);
        Buffer bufferToSent = Buffer.buffer();
        int start = 0;
        while(start + size < buffer.length()) {
          final String theLine = Utils.base64(buffer.getBytes(start, start + size));
          bufferToSent.appendBuffer(Buffer.buffer(theLine + "\r\n"));
          start += size;
        }
        streamBuffer = buffer.getBuffer(start, buffer.length());
        handler.handle(bufferToSent);
      }));
      return this;
    }

    @Override
    public BodyReadStream pause() {
      stream.pause();
      return this;
    }

    @Override
    public BodyReadStream resume() {
      stream.resume();
      return this;
    }

    @Override
    public BodyReadStream fetch(long amount) {
      stream.fetch(amount);
      return this;
    }

    @Override
    public BodyReadStream endHandler(@Nullable Handler<Void> endHandler) {
      stream.endHandler(handlerInContext(v -> {
        if (streamBuffer.length() > 0 && this.handler != null) {
          String theLine = Utils.base64(streamBuffer.getBytes());
          this.handler.handle(Buffer.buffer(theLine + "\r\n"));
        }
        endHandler.handle(null);
      }));
      return this;
    }
  }

}

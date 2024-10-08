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

package io.vertx.tests.mail.client;

import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * test what happens when the server closes the connection if we use a pooled connection
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
@RunWith(VertxUnitRunner.class)
public class MailPoolServerClosesTest extends SMTPTestDummy {

  private static final Logger log = LoggerFactory.getLogger(MailPoolServerClosesTest.class);

  /**
   * send two mails after each other when the server closes the connection immediately after the data send was
   * successfully
   *
   * @param context the TestContext
   */
  @Test
  public void mailConnectionCloseImmediatelyTest(TestContext context) {
    smtpServer.setCloseImmediately(true);
    MailClient mailClient = MailClient.create(vertx, configNoSSL());

    MailMessage email = exampleMessage();

    PassOnce pass1 = new PassOnce(context::fail);
    PassOnce pass2 = new PassOnce(context::fail);

    log.info("starting mail 1");
    mailClient.sendMail(email).onComplete(context.asyncAssertSuccess(result -> {
      log.info("mail finished 1");
      pass1.passOnce();
      log.info(result.toString());
      log.info("starting mail 2");
      mailClient.sendMail(email).onComplete(context.asyncAssertSuccess(result2 -> {
        pass2.passOnce();
        log.info("mail finished 2");
        log.info(result2.toString());
        mailClient.close().onComplete(context.asyncAssertSuccess());
      }));
    }));
  }

  /**
   * send two mails after each other when the server waits a time after the after the data send was successful and
   * closes the connection
   *
   * @param context the TestContext
   */
  @Test
  public void mailConnectionCloseWaitTest(TestContext context) {
    smtpServer.setCloseImmediately(false);
    smtpServer.setCloseWaitTime(1);

    MailClient mailClient = MailClient.create(vertx, configNoSSL());

    MailMessage email = exampleMessage();

    PassOnce pass1 = new PassOnce(context::fail);
    PassOnce pass2 = new PassOnce(context::fail);

    log.info("starting mail 1");
    mailClient.sendMail(email).onComplete(context.asyncAssertSuccess(result -> {
      log.info("mail finished 1");
      pass1.passOnce();
      log.info(result.toString());
      log.info("starting mail 2");
      mailClient.sendMail(email).onComplete(context.asyncAssertSuccess(result2 -> {
        pass2.passOnce();
        log.info("mail finished 2");
        log.info(result2.toString());
        mailClient.close().onComplete(context.asyncAssertSuccess());
      }));
    }));
  }

  /**
   * send two mails after each other when the server fails the RSET operation after sending the first mail
   */
  @Test
  public void mailConnectionRsetFailTest(TestContext context) {
    smtpServer.setCloseImmediately(false)
      .setDialogue("220 example.com ESMTP",
      "EHLO",
      "250-example.com\n" +
        "250 SIZE 1000000",
      "MAIL FROM",
      "250 2.1.0 Ok",
      "RCPT TO",
      "250 2.1.5 Ok",
      "DATA",
      "354 End data with <CR><LF>.<CR><LF>",
      "250 2.0.0 Ok: queued as ABCDDEF0123456789",
      "QUIT",
      "220 bye bye");

    MailClient mailClient = MailClient.create(vertx, configNoSSL());

    MailMessage email = exampleMessage();

    PassOnce pass1 = new PassOnce(context::fail);
    PassOnce pass2 = new PassOnce(context::fail);

    log.info("starting mail 1");
    mailClient.sendMail(email).onComplete(context.asyncAssertSuccess(result -> {
      pass1.passOnce();
      log.info("mail finished 1");
      log.info(result.toString());
      log.info("starting mail 2");
      mailClient.sendMail(email).onComplete(context.asyncAssertSuccess(result2 -> {
        pass2.passOnce();
        log.info("mail finished 2");
        log.info(result2.toString());
        mailClient.close().onComplete(context.asyncAssertSuccess());
      }));
    }));
  }

  /**
   * this test creates an error in the 2nd mail
   */
  @Test
  public void error2ndMail(TestContext context) {
    smtpServer.setCloseImmediately(true)
      .setDialogue("220 example.com ESMTP",
      "EHLO",
      "250-example.com\n" +
        "250 SIZE 1000000",
      "MAIL FROM",
      "250 2.1.0 Ok",
      "RCPT TO",
      "250 2.1.5 Ok",
      "DATA",
      "354 End data with <CR><LF>.<CR><LF>",
      "250 2.0.0 Ok: queued as ABCDDEF0123456789",
      "RSET",
      "220 reset ok");

    MailClient mailClient = MailClient.create(vertx, configNoSSL());

    MailMessage email = exampleMessage();

    PassOnce pass1 = new PassOnce(context::fail);
    PassOnce pass2 = new PassOnce(context::fail);

    log.info("starting mail 1");
    mailClient.sendMail(email).onComplete(context.asyncAssertSuccess(result -> {
      pass1.passOnce();
      log.info("mail finished 1");
      log.info(result.toString());
      log.info("starting mail 2");
      mailClient.sendMail(email).onComplete(context.asyncAssertFailure(result2 -> {
        pass2.passOnce();
        log.info("mail finished 2");
        log.info("(as expected) got exception 2", result2);
        mailClient.close().onComplete(context.asyncAssertSuccess());
      }));
    }));
  }

  @Override
  public void startSMTP() {
    super.startSMTP();
    smtpServer.setDialogue("220 example.com ESMTP",
      "EHLO",
      "250-example.com\n" +
        "250 SIZE 1000000",
      "MAIL FROM",
      "250 2.1.0 Ok",
      "RCPT TO",
      "250 2.1.5 Ok",
      "DATA",
      "354 End data with <CR><LF>.<CR><LF>",
      "250 2.0.0 Ok: queued as ABCDDEF0123456789");
  }

}

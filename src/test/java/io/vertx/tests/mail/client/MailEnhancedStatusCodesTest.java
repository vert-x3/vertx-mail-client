/*
 *  Copyright (c) 2011-2021 The original author or authors
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

import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.SMTPException;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests on enhanced status codes
 *
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
@RunWith(VertxUnitRunner.class)
public class MailEnhancedStatusCodesTest extends SMTPTestDummy {

  @Test
  public void testStatusCodeOutOfBusiness(TestContext context) {
    this.testContext = context;
    smtpServer.setDialogue("500 example.com ESMTP. It is not the time for business, bye.");
    MailClient mailClient = mailClientDefault();
    mailClient.sendMail(exampleMessage()).onComplete(testContext.asyncAssertFailure(e -> {
      Assert.assertEquals(SMTPException.class, e.getClass());
      SMTPException se = (SMTPException)e;
      Assert.assertTrue(se.isPermanent());
      Assert.assertEquals(SMTPException.EnhancedStatus.OTHER_UNKNOWN, se.getEnhancedStatus());
      Assert.assertEquals(0, se.getEnhancedStatus().getDetail());
      mailClient.close();
    }));
  }

  @Test
  public void testStatusCodeHeloNotSupported(TestContext context) {
    this.testContext = context;
    smtpServer.setDialogue("220 example.com ESMTP. welcome",
      "HELO",
      "500 HELO not supported",
      "QUIT",
      "221 2.0.0 Bye");
    MailConfig mailConfig = defaultConfig();
    mailConfig.setDisableEsmtp(true);
    MailClient mailClient = MailClient.create(vertx, mailConfig);
    mailClient.sendMail(exampleMessage()).onComplete(testContext.asyncAssertFailure(e -> {
      Assert.assertEquals(SMTPException.class, e.getClass());
      SMTPException se = (SMTPException)e;
      Assert.assertTrue(se.isPermanent());
      Assert.assertEquals(SMTPException.EnhancedStatus.OTHER_UNKNOWN, se.getEnhancedStatus());
      Assert.assertEquals(0, se.getEnhancedStatus().getDetail());
      mailClient.close();
    }));
  }

  @Test
  public void testStatusCodeAddressFailed(TestContext context) {
    this.testContext = context;
    smtpServer.setDialogue("220 example.com ESMTP",
      "EHLO",
      "250-example.com\n" +
        "250-ENHANCEDSTATUSCODES\n" +
        "250 AUTH LOGIN",
      "MAIL FROM",
      "415 4.1.5 Destination address valid",
      "QUIT",
      "221 2.0.0 Bye");
    MailConfig mailConfig = defaultConfig();
    MailClient mailClient = MailClient.create(vertx, mailConfig);
    mailClient.sendMail(exampleMessage()).onComplete(testContext.asyncAssertFailure(e -> {
      Assert.assertEquals(SMTPException.class, e.getClass());
      SMTPException se = (SMTPException)e;
      Assert.assertTrue(se.isTransient());
      Assert.assertEquals(SMTPException.EnhancedStatus.OTHER_ADDRESS, se.getEnhancedStatus());
      Assert.assertEquals(5, se.getEnhancedStatus().getDetail());
      mailClient.close();
    }));
  }

  @Test
  public void testStatusCodeMailboxFailed(TestContext context) {
    this.testContext = context;
    smtpServer.setDialogue("220 example.com ESMTP",
      "EHLO",
      "250-example.com\n" +
        "250-ENHANCEDSTATUSCODES\n" +
        "250 AUTH LOGIN",
      "MAIL FROM",
      "250 2.1.0 Ok",
      "RCPT TO",
      "425 4.2.3 Mailbox is full",
      "QUIT",
      "221 2.0.0 Bye");
    MailConfig mailConfig = defaultConfig();
    MailClient mailClient = MailClient.create(vertx, mailConfig);
    mailClient.sendMail(exampleMessage()).onComplete(testContext.asyncAssertFailure(e -> {
      Assert.assertEquals(SMTPException.class, e.getClass());
      SMTPException se = (SMTPException)e;
      Assert.assertTrue(se.isTransient());
      Assert.assertEquals(SMTPException.EnhancedStatus.OTHER_MAILBOX, se.getEnhancedStatus());
      Assert.assertEquals(3, se.getEnhancedStatus().getDetail());
      mailClient.close();
    }));
  }

  @Test
  public void testStatusCodeMailSystemFailed(TestContext context) {
    this.testContext = context;
    smtpServer.setDialogue("220 example.com ESMTP",
      "EHLO",
      "250-example.com\n" +
        "250-ENHANCEDSTATUSCODES\n" +
        "250 AUTH LOGIN",
      "MAIL FROM",
      "250 2.1.0 Ok",
      "RCPT TO",
      "432 4.3.2 System not accepting network messages",
      "QUIT",
      "221 2.0.0 Bye");
    MailConfig mailConfig = defaultConfig();
    MailClient mailClient = MailClient.create(vertx, mailConfig);
    mailClient.sendMail(exampleMessage()).onComplete(testContext.asyncAssertFailure(e -> {
      Assert.assertEquals(SMTPException.class, e.getClass());
      SMTPException se = (SMTPException)e;
      Assert.assertTrue(se.isTransient());
      Assert.assertEquals(SMTPException.EnhancedStatus.OTHER_MAIL_SYSTEM, se.getEnhancedStatus());
      Assert.assertEquals(2, se.getEnhancedStatus().getDetail());
      mailClient.close();
    }));
  }

  @Test
  public void testStatusCodeNetworkFailed(TestContext context) {
    this.testContext = context;
    smtpServer.setDialogue("220 example.com ESMTP",
      "EHLO",
      "250-example.com\n" +
        "250-ENHANCEDSTATUSCODES\n" +
        "250 PIPELINING",
      "MAIL FROM",
      "250 2.1.0 Ok",
      "RCPT TO",
      "250 2.1.5 Ok",
      "DATA",
      "550 5.4.2 Bad connection",
      "QUIT",
      "221 2.0.0 Bye");
    MailConfig mailConfig = defaultConfig();
    MailClient mailClient = MailClient.create(vertx, mailConfig);
    mailClient.sendMail(exampleMessage()).onComplete(testContext.asyncAssertFailure(e -> {
      Assert.assertEquals(SMTPException.class, e.getClass());
      SMTPException se = (SMTPException)e;
      Assert.assertTrue(se.isPermanent());
      Assert.assertEquals(SMTPException.EnhancedStatus.OTHER_NETWORK, se.getEnhancedStatus());
      Assert.assertEquals(2, se.getEnhancedStatus().getDetail());
      mailClient.close();
    }));
  }

  @Test
  public void testStatusCodeDeliveryFailed(TestContext context) {
    this.testContext = context;
    smtpServer.setDialogue("220 example.com ESMTP",
      "EHLO",
      "250-example.com\n" +
        "250-ENHANCEDSTATUSCODES\n" +
        "250 PIPELINING",
      "MAIL FROM",
      "250 2.1.0 Ok",
      "RCPT TO",
      "553 5.5.3 Too many recipients",
      "QUIT",
      "221 2.0.0 Bye");
    MailConfig mailConfig = defaultConfig();
    MailClient mailClient = MailClient.create(vertx, mailConfig);
    mailClient.sendMail(exampleMessage()).onComplete(testContext.asyncAssertFailure(e -> {
      Assert.assertEquals(SMTPException.class, e.getClass());
      SMTPException se = (SMTPException)e;
      Assert.assertTrue(se.isPermanent());
      Assert.assertEquals(SMTPException.EnhancedStatus.OTHER_MAIL_DELIVERY, se.getEnhancedStatus());
      Assert.assertEquals(3, se.getEnhancedStatus().getDetail());
      mailClient.close();
    }));
  }

  @Test
  public void testStatusCodeMessageContentFailed(TestContext context) {
    this.testContext = context;
    smtpServer.setDialogue("220 example.com ESMTP",
      "EHLO",
      "250-example.com\n" +
        "250-ENHANCEDSTATUSCODES\n" +
        "250 PIPELINING",
      "MAIL FROM",
      "250 2.1.0 Ok",
      "RCPT TO",
      "250 2.1.0 Ok",
      "DATA",
      "354 End data with <CR><LF>.<CR><LF>",
      "561 5.6.1 Media not supported",
      "QUIT",
      "221 2.0.0 Bye");
    MailConfig mailConfig = defaultConfig();
    MailClient mailClient = MailClient.create(vertx, mailConfig);
    mailClient.sendMail(exampleMessage()).onComplete(testContext.asyncAssertFailure(e -> {
      Assert.assertEquals(SMTPException.class, e.getClass());
      SMTPException se = (SMTPException)e;
      Assert.assertTrue(se.isPermanent());
      Assert.assertEquals(SMTPException.EnhancedStatus.OTHER_MAIL_MESSAGE, se.getEnhancedStatus());
      Assert.assertEquals(1, se.getEnhancedStatus().getDetail());
      mailClient.close();
    }));
  }

  @Test
  public void testStatusCodeAuthFailed(TestContext context) {
    this.testContext = context;
    smtpServer.setDialogue("220 example.com ESMTP",
      "EHLO",
      "250-example.com\n" +
        "250-ENHANCEDSTATUSCODES\n" +
        "250 AUTH LOGIN",
      "AUTH LOGIN",
      "334 VXNlcm5hbWU6",
      "eHh4",
      "334 UGFzc3dvcmQ6",
      "eXl5",
      "435 4.7.8 Error: authentication failed: authentication failure",
      "QUIT",
      "221 2.0.0 Bye");
    MailConfig mailConfig = configLogin();
    MailClient mailClient = MailClient.create(vertx, mailConfig);
    mailClient.sendMail(exampleMessage()).onComplete(testContext.asyncAssertFailure(e -> {
      Assert.assertEquals(SMTPException.class, e.getClass());
      SMTPException se = (SMTPException)e;
      Assert.assertTrue(se.isTransient());
      Assert.assertEquals(SMTPException.EnhancedStatus.OTHER_SECURITY, se.getEnhancedStatus());
      Assert.assertEquals(8, se.getEnhancedStatus().getDetail());
      mailClient.close();
    }));
  }

}

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

package io.vertx.ext.mail.impl;

import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.SMTPException;
import io.vertx.ext.mail.SMTPTestDummy;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests on {@link io.vertx.ext.mail.SMTPException}.
 *
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
@RunWith(VertxUnitRunner.class)
public class SMTPExceptionTest extends SMTPTestDummy {

  /**
   * Tests that SMTP server replies with failure on connection.
   */
  @Test
  public void testInitialFailure(TestContext testContext) {
    this.testContext = testContext;
    final String[][] dialogue = {
      {"500 Service closed - bye!"}
    };
    smtpServer.setDialogueArray(dialogue);
    MailMessage message = exampleMessage();
    MailClient mailClient = mailClientLogin();
    mailClient.sendMail(message).onComplete(testContext.asyncAssertFailure(t -> {
      testContext.assertEquals(t.getClass(), SMTPException.class);
      SMTPException smtpException = (SMTPException)t;
      testContext.assertEquals(500, smtpException.getReplyCode());
      testContext.assertEquals("500 Service closed - bye!", smtpException.getReplyMessage());
      testContext.assertTrue(smtpException.isPermanent());
      mailClient.close(testContext.asyncAssertSuccess());
    }));
  }

  /**
   * Tests on Authentication failure.
   */
  @Test
  public void testAuthenticationFailure(TestContext testContext) {
    this.testContext = testContext;
    final String[][] dialogue = {
      {"220 smtp.gmail.com ESMTP o8sm3958210pjs.6 - gsmtp"},
      {"EHLO"},
      {"250-smtp.gmail.com at your service, [209.132.188.80]\n" +
        "250 AUTH LOGIN"},
      {"AUTH LOGIN"},
      {"334 VXNlcm5hbWU6"},
      {"eHh4"},
      {"334 UGFzc3dvcmQ6"},
      {"eXl5"},
      {"435 4.7.8 Error: authentication failed: authentication failure"},
      {"QUIT"},
      {"220 bye"}
    };
    smtpServer.setDialogueArray(dialogue);
    MailMessage message = exampleMessage();
    MailClient mailClient = mailClientLogin();
    mailClient.sendMail(message).onComplete(testContext.asyncAssertFailure(t -> {
      testContext.assertEquals(t.getClass(), SMTPException.class);
      SMTPException smtpException = (SMTPException)t;
      testContext.assertEquals(435, smtpException.getReplyCode());
      testContext.assertEquals("435 4.7.8 Error: authentication failed: authentication failure", smtpException.getReplyMessage());
      testContext.assertTrue(smtpException.isTransient());
      mailClient.close(testContext.asyncAssertSuccess());
    }));
  }

  /**
   * Tests on not accepted sender
   */
  @Test
  public void testMailSenderFailure(TestContext testContext) {
    this.testContext = testContext;
    final String[][] dialogue = {
      {"220 smtp.gmail.com ESMTP o8sm3958210pjs.6 - gsmtp"},
      {"EHLO"},
      {"250-smtp.gmail.com at your service, [209.132.188.80]\n" +
        "250 AUTH LOGIN"},
      {"AUTH LOGIN"},
      {"334 VXNlcm5hbWU6"},
      {"eHh4"},
      {"334 UGFzc3dvcmQ6"},
      {"eXl5"},
      {"235 2.7.0 Accepted"},
      {"MAIL FROM"},
      {"451 4.1.7 Bad sender's mailbox address"},
      {"QUIT"},
      {"220 bye"}
    };
    smtpServer.setDialogueArray(dialogue);
    MailMessage message = exampleMessage();
    MailClient mailClient = mailClientLogin();
    mailClient.sendMail(message).onComplete(testContext.asyncAssertFailure(t -> {
      testContext.assertEquals(t.getClass(), SMTPException.class);
      SMTPException smtpException = (SMTPException)t;
      testContext.assertEquals(451, smtpException.getReplyCode());
      testContext.assertEquals("451 4.1.7 Bad sender's mailbox address", smtpException.getReplyMessage());
      testContext.assertTrue(smtpException.isTransient());
      mailClient.close(testContext.asyncAssertSuccess());
    }));
  }

  /**
   * Tests on not accepted recipient because of Domain not found.
   */
  @Test
  public void testBadDestination(TestContext testContext) {
    this.testContext = testContext;
    final String[][] dialogue = {
      {"220 smtp.gmail.com ESMTP o8sm3958210pjs.6 - gsmtp"},
      {"EHLO"},
      {"250-smtp.gmail.com at your service, [209.132.188.80]\n" +
        "250 AUTH LOGIN"},
      {"AUTH LOGIN"},
      {"334 VXNlcm5hbWU6"},
      {"eHh4"},
      {"334 UGFzc3dvcmQ6"},
      {"eXl5"},
      {"235 2.7.0 Accepted"},
      {"MAIL FROM"},
      {"250 2.1.0 Ok"},
      {"RCPT TO"},
      {"450 4.1.2 <foo@invalid.invalid>: Recipient address rejected: Domain not found"},
      {"QUIT"},
      {"221 2.0.0 Bye"}
    };
    smtpServer.setDialogueArray(dialogue);
    MailMessage message = exampleMessage();
    MailClient mailClient = MailClient.create(vertx, configLogin().setAllowRcptErrors(false));
    mailClient.sendMail(message).onComplete(testContext.asyncAssertFailure(t -> {
      testContext.assertEquals(t.getClass(), SMTPException.class);
      SMTPException smtpException = (SMTPException)t;
      testContext.assertEquals(450, smtpException.getReplyCode());
      testContext.assertEquals("450 4.1.2 <foo@invalid.invalid>: Recipient address rejected: Domain not found", smtpException.getReplyMessage());
      testContext.assertTrue(smtpException.isTransient());
      mailClient.close(testContext.asyncAssertSuccess());
    }));
  }

  /**
   * Tests on not accepted recipient because of Domain does not accept mail.
   */
  @Test
  public void testRecipientRejected(TestContext testContext) {
    this.testContext = testContext;
    final String[][] dialogue = {
      {"220 smtp.gmail.com ESMTP o8sm3958210pjs.6 - gsmtp"},
      {"EHLO"},
      {"250-smtp.gmail.com at your service, [209.132.188.80]\n" +
        "250 AUTH LOGIN"},
      {"AUTH LOGIN"},
      {"334 VXNlcm5hbWU6"},
      {"eHh4"},
      {"334 UGFzc3dvcmQ6"},
      {"eXl5"},
      {"235 2.7.0 Accepted"},
      {"MAIL FROM"},
      {"250 2.1.0 Ok"},
      {"RCPT TO"},
      {"556 5.1.10 <foo@example.com>: Recipient address rejected: Domain example.com does not accept mail (nullMX)"},
      {"QUIT"},
      {"221 2.0.0 Bye"}
    };
    smtpServer.setDialogueArray(dialogue);
    MailMessage message = exampleMessage();
    MailClient mailClient = MailClient.create(vertx, configLogin().setAllowRcptErrors(false));
    mailClient.sendMail(message).onComplete(testContext.asyncAssertFailure(t -> {
      testContext.assertEquals(t.getClass(), SMTPException.class);
      SMTPException smtpException = (SMTPException)t;
      testContext.assertEquals(556, smtpException.getReplyCode());
      testContext.assertEquals("556 5.1.10 <foo@example.com>: Recipient address rejected: Domain example.com does not accept mail (nullMX)", smtpException.getReplyMessage());
      testContext.assertTrue(smtpException.isPermanent());
      mailClient.close(testContext.asyncAssertSuccess());
    }));
  }

  /**
   * Tests that DATA is rejected.
   */
  @Test
  public void testDataRejected(TestContext testContext) {
    this.testContext = testContext;
    final String[][] dialogue = {
      {"220 smtp.gmail.com ESMTP o8sm3958210pjs.6 - gsmtp"},
      {"EHLO"},
      {"250-smtp.gmail.com at your service, [209.132.188.80]\n" +
        "250 AUTH LOGIN"},
      {"AUTH LOGIN"},
      {"334 VXNlcm5hbWU6"},
      {"eHh4"},
      {"334 UGFzc3dvcmQ6"},
      {"eXl5"},
      {"235 2.7.0 Accepted"},
      {"MAIL FROM"},
      {"250 2.1.0 Ok"},
      {"RCPT TO"},
      {"250 2.1.0 Ok"},
      {"DATA"},
      {"452 failed to send email data because of insufficient system storage"},
      {"QUIT"},
      {"221 2.0.0 Bye"}
    };
    smtpServer.setDialogueArray(dialogue);
    MailMessage message = exampleMessage();
    MailClient mailClient = mailClientLogin();
    mailClient.sendMail(message).onComplete(testContext.asyncAssertFailure(t -> {
      testContext.assertEquals(t.getClass(), SMTPException.class);
      SMTPException smtpException = (SMTPException)t;
      testContext.assertEquals(452, smtpException.getReplyCode());
      testContext.assertEquals("452 failed to send email data because of insufficient system storage", smtpException.getReplyMessage());
      testContext.assertTrue(smtpException.isTransient());
      mailClient.close(testContext.asyncAssertSuccess());
    }));
  }

}

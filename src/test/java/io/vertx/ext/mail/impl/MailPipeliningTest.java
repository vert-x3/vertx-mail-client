/*
 *  Copyright (c) 2011-2019 The original author or authors
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
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.SMTPException;
import io.vertx.ext.mail.SMTPTestDummy;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

/**
 * Tests SMTP Pipelining
 *
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
@RunWith(VertxUnitRunner.class)
public class MailPipeliningTest extends SMTPTestDummy {

  @Test
  public void pipeLiningSuccessTest(TestContext testContext) {
    this.testContext = testContext;
    final String[][] dialogue = {
      {"220 smtp.gmail.com ESMTP o8sm3958210pjs.6 - gsmtp"},
      {"EHLO"},
      {"250-smtp.gmail.com at your service, [209.132.188.80]\n" +
        "250-AUTH LOGIN PLAIN\n" +
        "250 PIPELINING"},
      {"AUTH LOGIN"},
      {"334 VXNlcm5hbWU6"},
      {"eHh4"},
      {"334 UGFzc3dvcmQ6"},
      {"eXl5"},
      {"250 2.1.0 Ok"},
      {"MAIL FROM", "RCPT TO", "DATA"},
      {"250 2.1.0 mail from Ok", "250 2.1.0 rcpto Ok", "354 End data with <CR><LF>.<CR><LF>"},
      {"250 2.0.0 Ok: queued as ABCD"},
      {"QUIT"},
      {"221 2.0.0 Bye"}
    };
    smtpServer.setDialogueArray(dialogue);
    testSuccess(mailClientLogin(), exampleMessage());
  }

  @Test
  public void pipeLiningMultipleRCPTsTest(TestContext testContext) {
    this.testContext = testContext;
    final String[][] dialogue = {
      {"220 smtp.gmail.com ESMTP o8sm3958210pjs.6 - gsmtp"},
      {"EHLO"},
      {"250-smtp.gmail.com at your service, [209.132.188.80]\n" +
        "250-AUTH LOGIN PLAIN\n" +
        "250 PIPELINING"},
      {"AUTH LOGIN"},
      {"334 VXNlcm5hbWU6"},
      {"eHh4"},
      {"334 UGFzc3dvcmQ6"},
      {"eXl5"},
      {"250 2.1.0 Ok"},
      {"MAIL FROM", "RCPT TO", "RCPT TO", "DATA"},
      {"250 2.1.0 Ok", "250 2.1.0 Ok", "250 2.1.0 Ok", "354 End data with <CR><LF>.<CR><LF>"},
      {"250 2.0.0 Ok: queued as ABCD"},
      {"QUIT"},
      {"221 2.0.0 Bye"}
    };
    smtpServer.setDialogueArray(dialogue);
    MailMessage message = exampleMessage().setTo(Arrays.asList("userA@example.com", "userB@example.com"));
    testSuccess(mailClientLogin(), message);
  }

  /**
   * Some rcpt-tos are rejected, it fails the mail send.
   */
  @Test
  public void pipeLiningMultipleRCPTsSomeRejectedTest(TestContext testContext) {
    this.testContext = testContext;
    final String[][] dialogue = {
      {"220 smtp.gmail.com ESMTP o8sm3958210pjs.6 - gsmtp"},
      {"EHLO"},
      {"250-smtp.gmail.com at your service, [209.132.188.80]\n" +
        "250-AUTH LOGIN PLAIN\n" +
        "250 PIPELINING"},
      {"AUTH LOGIN"},
      {"334 VXNlcm5hbWU6"},
      {"eHh4"},
      {"334 UGFzc3dvcmQ6"},
      {"eXl5"},
      {"250 2.1.0 Ok"},
      {"MAIL FROM", "RCPT TO", "RCPT TO", "DATA"},
      {"250 2.1.0 Ok", "250 2.1.0 Ok", "550 5.1.1 Unknown user: userB@example.com", "354 End data with <CR><LF>.<CR><LF>"},
      {"250 2.0.0 Ok: queued as ABCD"},
      {"QUIT"},
      {"221 2.0.0 Bye"}
    };
    smtpServer.setDialogueArray(dialogue);
    MailMessage message = exampleMessage().setTo(Arrays.asList("userA@example.com", "userB@example.com"));
    MailClient mailClient = mailClientLogin();
    mailClient.sendMail(message).onComplete(testContext.asyncAssertFailure(t -> {
        testContext.assertEquals(t.getClass(), SMTPException.class);
        SMTPException smtpException = (SMTPException)t;
        testContext.assertEquals(550, smtpException.getReplyCode());
        testContext.assertEquals("550 5.1.1 Unknown user: userB@example.com", smtpException.getReplyMessage());
        testContext.assertTrue(smtpException.isPermanent());
        testContext.assertTrue(t.getMessage().contains("550 5.1.1 Unknown user: userB@example.com"));
        mailClient.close().onComplete(testContext.asyncAssertSuccess());
      })
    );
  }

  /**
   * Some rcpt-to are rejected, but MailConfig allows such failures
   * In this case, recipients list in MailResult are less than specified in MailMessage.
   */
  @Test
  public void pipeLiningMultipleRCPTsSomeRejectedAllowedTest(TestContext testContext) {
    this.testContext = testContext;
    final String[][] dialogue = {
      {"220 smtp.gmail.com ESMTP o8sm3958210pjs.6 - gsmtp"},
      {"EHLO"},
      {"250-smtp.gmail.com at your service, [209.132.188.80]\n" +
        "250-AUTH LOGIN PLAIN\n" +
        "250 PIPELINING"},
      {"AUTH LOGIN"},
      {"334 VXNlcm5hbWU6"},
      {"eHh4"},
      {"334 UGFzc3dvcmQ6"},
      {"eXl5"},
      {"250 2.1.0 Ok"},
      {"MAIL FROM", "RCPT TO", "RCPT TO", "DATA"},
      {"250 2.1.0 Ok", "250 2.1.0 Ok", "550 5.1.1 Unknown user: userB@example.com", "354 End data with <CR><LF>.<CR><LF>"},
      {"250 2.0.0 Ok: queued as ABCD"},
      {"QUIT"},
      {"221 2.0.0 Bye"}
    };
    smtpServer.setDialogueArray(dialogue);
    MailMessage message = exampleMessage().setTo(Arrays.asList("userA@example.com", "userB@example.com"));
    MailConfig mailConfig = configLogin().setAllowRcptErrors(true);
    MailClient mailClient = MailClient.createShared(vertx, mailConfig);
    mailClient.sendMail(message).onComplete(testContext.asyncAssertSuccess(mr -> {
      testContext.assertTrue(mr.getRecipients().contains("userA@example.com"));
      testContext.assertFalse(mr.getRecipients().contains("userB@example.com"));
      mailClient.close().onComplete(testContext.asyncAssertSuccess());
    }));
  }

  /**
   * All rcpts are rejected, but DATA command response is OK, send dot only in this case
   */
  @Test
  public void pipeLiningMultipleRCPTsAllRejectedDataOKTest(TestContext testContext) {
    this.testContext = testContext;
    final String[][] dialogue = {
      {"220 smtp.gmail.com ESMTP o8sm3958210pjs.6 - gsmtp"},
      {"EHLO"},
      {"250-smtp.gmail.com at your service, [209.132.188.80]\n" +
        "250-AUTH LOGIN PLAIN\n" +
        "250 PIPELINING"},
      {"AUTH LOGIN"},
      {"334 VXNlcm5hbWU6"},
      {"eHh4"},
      {"334 UGFzc3dvcmQ6"},
      {"eXl5"},
      {"250 2.1.0 Ok"},
      {"MAIL FROM", "RCPT TO", "RCPT TO", "DATA"},
      {"250 2.1.0 Ok", "550 5.1.1 Unknown user: userA@example.com", "550 5.1.1 Unknown user: userB@example.com", "354 End data with <CR><LF>.<CR><LF>"},
      {"250 2.0.0 Ok: queued as ABCD"},
      {"QUIT"},
      {"554 no valid recipients"} // QUIT failures are ignored.
    };
    smtpServer.setDialogueArray(dialogue);
    MailMessage message = exampleMessage().setTo(Arrays.asList("userA@example.com", "userB@example.com"));
    MailConfig mailConfig = configLogin().setAllowRcptErrors(true);
    MailClient mailClient = MailClient.createShared(vertx, mailConfig);
    mailClient.sendMail(message).onComplete(testContext.asyncAssertSuccess(mr -> {
      testContext.assertFalse(mr.getRecipients().contains("userA@example.com"));
      testContext.assertFalse(mr.getRecipients().contains("userB@example.com"));
      // only dot got sent, but no way to test it in smtp server.
      mailClient.close().onComplete(testContext.asyncAssertSuccess());
    }));
  }

  /**
   * All rcpts are rejected, but DATA command response is fail, just close the mail transaction
   */
  @Test
  public void pipeLiningMultipleRCPTsAllRejectedDataFailTest(TestContext testContext) {
    this.testContext = testContext;
    final String[][] dialogue = {
      {"220 smtp.gmail.com ESMTP o8sm3958210pjs.6 - gsmtp"},
      {"EHLO"},
      {"250-smtp.gmail.com at your service, [209.132.188.80]\n" +
        "250-AUTH LOGIN PLAIN\n" +
        "250 PIPELINING"},
      {"AUTH LOGIN"},
      {"334 VXNlcm5hbWU6"},
      {"eHh4"},
      {"334 UGFzc3dvcmQ6"},
      {"eXl5"},
      {"250 2.1.0 Ok"},
      {"MAIL FROM", "RCPT TO", "RCPT TO", "DATA"},
      {"250 2.1.0 Ok", "550 5.1.1 Unknown user: userA@example.com", "550 5.1.1 Unknown user: userB@example.com", "554 no valid recipients given"},
      {"QUIT"},
      {"221 2.0.0 Bye"}
    };
    smtpServer.setDialogueArray(dialogue);
    MailMessage message = exampleMessage().setTo(Arrays.asList("userA@example.com", "userB@example.com"));
    MailConfig mailConfig = configLogin().setAllowRcptErrors(true);
    MailClient mailClient = MailClient.createShared(vertx, mailConfig);
    mailClient.sendMail(message).onComplete(testContext.asyncAssertFailure(t -> {
      testContext.assertEquals(t.getClass(), SMTPException.class);
      SMTPException smtpException = (SMTPException)t;
      testContext.assertEquals(554, smtpException.getReplyCode());
      testContext.assertEquals("554 no valid recipients given", smtpException.getReplyMessage());
      testContext.assertTrue(smtpException.isPermanent());
      testContext.assertTrue(t.getMessage().contains("554 no valid recipients given"));
      mailClient.close().onComplete(testContext.asyncAssertSuccess());
    }));
  }

  /**
   * Test the case of multi-lines responses in the group commands.
   */
  @Test
  public void pipeLiningMultilineResponseTest(TestContext testContext) {
    this.testContext = testContext;
    final String[][] dialogue = {
      {"220 smtp.gmail.com ESMTP o8sm3958210pjs.6 - gsmtp"},
      {"EHLO"},
      {"250-smtp.gmail.com at your service, [209.132.188.80]\n" +
        "250-AUTH LOGIN PLAIN\n" +
        "250 PIPELINING"},
      {"AUTH LOGIN"},
      {"334 VXNlcm5hbWU6"},
      {"eHh4"},
      {"334 UGFzc3dvcmQ6"},
      {"eXl5"},
      {"250 2.1.0 Ok"},
      {"MAIL FROM", "RCPT TO", "RCPT TO", "DATA"},
      {"250 2.1.0 Ok", "250-2.1.0 Ok\n" + "250 2.1.1 OK", "250 2.1.0 Ok", "354 End data with <CR><LF>.<CR><LF>"},
      {"250 2.0.0 Ok: queued as ABCD"},
      {"QUIT"},
      {"221 2.0.0 Bye"}
    };
    smtpServer.setDialogueArray(dialogue);
    MailMessage message = exampleMessage().setTo(Arrays.asList("userA@example.com", "userB@example.com"));
    MailConfig mailConfig = configLogin().setAllowRcptErrors(true);
    MailClient mailClient = MailClient.createShared(vertx, mailConfig);
    mailClient.sendMail(message).onComplete(testContext.asyncAssertSuccess(mr -> {
      testContext.assertTrue(mr.getRecipients().contains("userA@example.com"));
      testContext.assertTrue(mr.getRecipients().contains("userB@example.com"));
      mailClient.close().onComplete(testContext.asyncAssertSuccess());
    }));

  }

}

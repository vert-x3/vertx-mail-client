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
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.SMTPTestDummy;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests authentication chain.
 *
 * Keep it in the impl package to be able to test internal state like the default authentication mechanism after
 * first successful authentication.
 *
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
@RunWith(VertxUnitRunner.class)
public class MailAuthChainTest extends SMTPTestDummy {

  // XOAUTH2 failed, but next LOGIN succeeds
  @Test
  public void authChainTest(TestContext testContext) {
    this.testContext = testContext;
    smtpServer.setDialogue(
      "220 smtp.gmail.com ESMTP o8sm3958210pjs.6 - gsmtp",
      "EHLO",
      "250-smtp.gmail.com at your service, [209.132.188.80]\n" +
        "250-SIZE 35882577\n" +
        "250-8BITMIME\n" +
        "250-AUTH LOGIN PLAIN XOAUTH2 PLAIN-CLIENTTOKEN OAUTHBEARER XOAUTH\n" +
        "250-ENHANCEDSTATUSCODES\n" +
        "250-CHUNKING\n" +
        "250 SMTPUTF8",
      "AUTH XOAUTH2 dXNlcj14eHgBYXV0aD1CZWFyZXIgeXl5AQE=",
      "334 eyJzdGF0dXMiOiI0MDAiLCJzY2hlbWVzIjoiQmVhcmVyIiwic2NvcGUiOiJodHRwczovL21haWwuZ29vZ2xlLmNvbS8ifQ==",
      "",
      "535-5.7.8 Username and Password not accepted. Learn more at\n" +
        "535 5.7.8  https://support.google.com/mail/?p=BadCredentials o8sm3958210pjs.6 - gsmtp",
      "AUTH LOGIN",
      "334 VXNlcm5hbWU6",
      "eHh4",
      "334 UGFzc3dvcmQ6",
      "eXl5",
      "235 2.7.0 Accepted",
      "MAIL FROM",
      "250 2.1.0 Ok",
      "RCPT TO",
      "250 2.1.5 Ok",
      "DATA",
      "354 End data with <CR><LF>.<CR><LF>",
      "250 2.0.0 Ok: queued as ABCD",
      "QUIT",
      "221 2.0.0 Bye"
    );
    final MailClient mailClient = MailClient.create(vertx, configLogin().setKeepAlive(false));
    final MailMessage email = exampleMessage();
    MailClientImpl clientImpl = (MailClientImpl)mailClient;
    assertNull(clientImpl.getConnectionPool().getAuthOperationFactory().getAuthMethod());
    mailClient.sendMail(email, testContext.asyncAssertSuccess(r1 -> {
      assertEquals("LOGIN", clientImpl.getConnectionPool().getAuthOperationFactory().getAuthMethod());
      smtpServer.setDialogue(
        "220 smtp.gmail.com ESMTP o8sm3958210pjs.6 - gsmtp",
        "EHLO",
        "250-smtp.gmail.com at your service, [209.132.188.80]\n" +
          "250-SIZE 35882577\n" +
          "250-8BITMIME\n" +
          "250-AUTH LOGIN PLAIN XOAUTH2 PLAIN-CLIENTTOKEN OAUTHBEARER XOAUTH\n" +
          "250-ENHANCEDSTATUSCODES\n" +
          "250-CHUNKING\n" +
          "250 SMTPUTF8",
        "AUTH LOGIN",
        "334 VXNlcm5hbWU6",
        "eHh4",
        "334 UGFzc3dvcmQ6",
        "eXl5",
        "235 2.7.0 Accepted",
        "MAIL FROM",
        "250 2.1.0 Ok",
        "RCPT TO",
        "250 2.1.5 Ok",
        "DATA",
        "354 End data with <CR><LF>.<CR><LF>",
        "250 2.0.0 Ok: queued as ABCD",
        "QUIT",
        "221 2.0.0 Bye"
      );
      mailClient.sendMail(email, testContext.asyncAssertSuccess(r2 -> mailClient.close()));
    }));
  }

  // all auth methods failed
  @Test
  public void authChainFailedTest(TestContext testContext) {
    this.testContext = testContext;
    smtpServer.setDialogue(
      "220 smtp.gmail.com ESMTP o8sm3958210pjs.6 - gsmtp",
      "EHLO",
      "250-smtp.gmail.com at your service, [209.132.188.80]\n" +
        "250-SIZE 35882577\n" +
        "250-8BITMIME\n" +
        "250-AUTH LOGIN PLAIN XOAUTH2\n" +
        "250-ENHANCEDSTATUSCODES\n" +
        "250-CHUNKING\n" +
        "250 SMTPUTF8",
      "AUTH XOAUTH2 dXNlcj14eHgBYXV0aD1CZWFyZXIgeXl5AQE=",
      "334 eyJzdGF0dXMiOiI0MDAiLCJzY2hlbWVzIjoiQmVhcmVyIiwic2NvcGUiOiJodHRwczovL21haWwuZ29vZ2xlLmNvbS8ifQ==",
      "",
      "535-5.7.8 Username and Password not accepted. Learn more at\n" +
        "535 5.7.8  https://support.google.com/mail/?p=BadCredentials o8sm3958210pjs.6 - gsmtp",
      "AUTH LOGIN",
      "334 VXNlcm5hbWU6",
      "eHh4",
      "334 UGFzc3dvcmQ6",
      "eXl5",
      "435 4.7.8 Error: authentication failed: authentication failure",
      "AUTH PLAIN AHh4eAB5eXk=",
      "435 4.7.8 Error: authentication failed: bad protocol / cancel"
    );
    final MailClient mailClient = mailClientLogin();
    final MailMessage email = exampleMessage();
    MailClientImpl clientImpl = (MailClientImpl)mailClient;

    assertNull(clientImpl.getConnectionPool().getAuthOperationFactory().getAuthMethod());
    mailClient.sendMail(email, testContext.asyncAssertFailure(r1 -> {
      assertNull(clientImpl.getConnectionPool().getAuthOperationFactory().getAuthMethod());
      mailClient.close();
    }));
  }


  // Default auth: LOGIN failed, then try one-by-one and falls back to PLAIN
  @Test
  public void authChainDefaultFailedTest(TestContext testContext) {
    this.testContext = testContext;
    smtpServer.setDialogue(
      "220 smtp.gmail.com ESMTP o8sm3958210pjs.6 - gsmtp",
      "EHLO",
      "250-smtp.gmail.com at your service, [209.132.188.80]\n" +
        "250-SIZE 35882577\n" +
        "250-8BITMIME\n" +
        "250-AUTH LOGIN PLAIN XOAUTH2\n" +
        "250-ENHANCEDSTATUSCODES\n" +
        "250-CHUNKING\n" +
        "250 SMTPUTF8",
      "AUTH LOGIN",
      "334 VXNlcm5hbWU6",
      "eHh4",
      "334 UGFzc3dvcmQ6",
      "eXl5",
      "435 4.7.8 Error: authentication failed: authentication failure",
      "AUTH XOAUTH2 dXNlcj14eHgBYXV0aD1CZWFyZXIgeXl5AQE=",
      "334 eyJzdGF0dXMiOiI0MDAiLCJzY2hlbWVzIjoiQmVhcmVyIiwic2NvcGUiOiJodHRwczovL21haWwuZ29vZ2xlLmNvbS8ifQ==",
      "",
      "535-5.7.8 Username and Password not accepted. Learn more at\n" +
        "535 5.7.8  https://support.google.com/mail/?p=BadCredentials o8sm3958210pjs.6 - gsmtp",
      "AUTH LOGIN",
      "334 VXNlcm5hbWU6",
      "eHh4",
      "334 UGFzc3dvcmQ6",
      "eXl5",
      "435 4.7.8 Error: authentication failed: authentication failure",
      "AUTH PLAIN AHh4eAB5eXk=",
      "250 2.1.0 Ok",
      "MAIL FROM",
      "250 2.1.0 Ok",
      "RCPT TO",
      "250 2.1.5 Ok",
      "DATA",
      "354 End data with <CR><LF>.<CR><LF>",
      "250 2.0.0 Ok: queued as ABCD",
      "QUIT",
      "221 2.0.0 Bye"
    );
    final MailClient mailClient = MailClient.create(vertx, configLogin().setKeepAlive(false));
    final MailMessage email = exampleMessage();
    MailClientImpl clientImpl = (MailClientImpl)mailClient;
    // default is LOGIN, but will fail
    clientImpl.getConnectionPool().getAuthOperationFactory().setAuthMethod("LOGIN");
    assertEquals("LOGIN", clientImpl.getConnectionPool().getAuthOperationFactory().getAuthMethod());
    mailClient.sendMail(email, testContext.asyncAssertSuccess(r1 -> {
      assertEquals("PLAIN", clientImpl.getConnectionPool().getAuthOperationFactory().getAuthMethod());
      smtpServer.setDialogue(
        "220 smtp.gmail.com ESMTP o8sm3958210pjs.6 - gsmtp",
        "EHLO",
        "250-smtp.gmail.com at your service, [209.132.188.80]\n" +
          "250-SIZE 35882577\n" +
          "250-8BITMIME\n" +
          "250-AUTH LOGIN PLAIN XOAUTH2 PLAIN-CLIENTTOKEN OAUTHBEARER XOAUTH\n" +
          "250-ENHANCEDSTATUSCODES\n" +
          "250-CHUNKING\n" +
          "250 SMTPUTF8",
        "AUTH PLAIN AHh4eAB5eXk=",
        "250 2.1.0 Ok",
        "MAIL FROM",
        "250 2.1.0 Ok",
        "RCPT TO",
        "250 2.1.5 Ok",
        "DATA",
        "354 End data with <CR><LF>.<CR><LF>",
        "250 2.0.0 Ok: queued as ABCD",
        "QUIT",
        "221 2.0.0 Bye"
      );
      mailClient.sendMail(email, testContext.asyncAssertSuccess(r2 -> mailClient.close()));
    }));
  }

}

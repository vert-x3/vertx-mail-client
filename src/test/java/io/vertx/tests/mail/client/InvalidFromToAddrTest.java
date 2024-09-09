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

import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * test sending message with invalid from or to
 *
 * the exception wasn't reported when the message was created inside the checkSize method before the
 * mailFromCmd/rcptToCmd methods. This issue was fixed by pr#44 already.
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
@RunWith(VertxUnitRunner.class)
public class InvalidFromToAddrTest extends SMTPTestDummy {

  @Test
  public void mailInvalidToTest(TestContext testContext) {
    this.testContext = testContext;
    testException(new MailMessage().setFrom("user@example.com").setTo("user @example.com"));
  }

  @Test
  public void mailInvalidFromTest(TestContext testContext) {
    this.testContext = testContext;
    testException(new MailMessage().setFrom("user @example.com").setTo("user@example.com"));
  }

  // postmaster cannot be as mail from,
  // but can be as rcpt to as defined at: https://tools.ietf.org/html/rfc5321#section-2.3.5
  @Test
  public void mailFromAsPostmasterTest(TestContext testContext) {
    this.testContext = testContext;
    testException(new MailMessage().setFrom("postmaster").setTo("user@example.com"));
  }

  // postmaster maybe denied by server's policy
  @Test
  public void mailToPostmasterDeniedTest(TestContext testContext) {
    this.testContext = testContext;
    smtpServer.setDialogue("220 example.com ESMTP",
      "EHLO",
      "250-example.com\n" +
        "250 AUTH LOGIN",
      "AUTH LOGIN",
      "334 VXNlcm5hbWU6",
      "eHh4",
      "334 UGFzc3dvcmQ6",
      "eXl5",
      "250 2.1.0 Ok",
      "MAIL FROM:<from@example.com>",
      "250 2.1.0 Ok",
      "RCPT TO:<postmaster>",
      "550 DY-001 Mail rejected for policy reasons.");
    testException(mailClientLogin(), exampleMessage().setTo("postmaster"));
  }

  @Test
  public void mailToPostmasterTest(TestContext testContext) {
    this.testContext = testContext;
    smtpServer.setDialogue("220 example.com ESMTP",
      "EHLO",
      "250-example.com\n" +
        "250 AUTH LOGIN",
      "AUTH LOGIN",
      "334 VXNlcm5hbWU6",
      "eHh4",
      "334 UGFzc3dvcmQ6",
      "eXl5",
      "250 2.1.0 Ok",
      "MAIL FROM:<from@example.com>",
      "250 2.1.0 Ok",
      "RCPT TO:<postmaster>",
      "250 2.1.5 Recipient <postmaster@example.com> OK",
      "DATA",
      "354 End data with <CR><LF>.<CR><LF>",
      "250 2.0.0 Ok: queued as ABCD",
      "QUIT",
      "221 2.0.0 Bye");
    testSuccess(mailClientLogin(), exampleMessage().setTo("postmaster"));
  }

}

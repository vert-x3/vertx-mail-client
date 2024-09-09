/*
 *  Copyright (c) 2011-2023 The original author or authors
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
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.mail.internet.MimeMessage;

@RunWith(VertxUnitRunner.class)
public class MailSubjectTest extends SMTPTestWiser {

  @Test
  public void testSubjectUnicode(TestContext testContext) {
    this.testContext = testContext;
    MailClient mailClient = mailClientLogin();

    final String subject = "⭐⭐⭐⭐";

    MailMessage message = exampleMessage().setSubject(subject);
    testSuccess(mailClient, message, () -> {
      final MimeMessage mimeMessage = wiser.getMessages().get(0).getMimeMessage();
      assertEquals(subject, mimeMessage.getSubject());
    });
  }

  @Test
  public void testSubjectUnicodeFolding(TestContext testContext) {
    this.testContext = testContext;
    MailClient mailClient = mailClientLogin();

    final String subject = "⭐⭐⭐⭐⭐⭐⭐⭐⭐⭐⭐⭐⭐⭐⭐⭐⭐⭐";

    MailMessage message = exampleMessage().setSubject(subject);
    testSuccess(mailClient, message, () -> {
      final MimeMessage mimeMessage = wiser.getMessages().get(0).getMimeMessage();
      assertEquals(subject, mimeMessage.getSubject());
    });
  }
}

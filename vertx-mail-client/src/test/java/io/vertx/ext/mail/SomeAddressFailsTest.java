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

package io.vertx.ext.mail;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * check behaviour of failing recipient addresses, either fails the complete mail (default) or sends the mail to the
 * accepted recipients
 * 
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
@RunWith(VertxUnitRunner.class)
public class SomeAddressFailsTest extends SMTPTestDummy {

  private static final Logger log = LoggerFactory.getLogger(SomeAddressFailsTest.class);

  @Test
  public void addressFailureFailsMail(TestContext testContext) {
    this.testContext=testContext;
    MailMessage mail = exampleMessage()
        .setTo(Arrays.asList("user@example.com", "fail@example.org"));
    testException(mailClientNoSSL(), mail);
  }

  @Test
  public void addressFailureSendsMail(TestContext testContext) {
    this.testContext=testContext;
    MailMessage mail = exampleMessage()
        .setTo(Arrays.asList("user@example.com", "fail@example.org"));

    MailConfig config = configNoSSL()
        .setAllowRcptErrors(true);
    MailClient mailClient = MailClient.createNonShared(vertx, config);

    Async async = testContext.async();
    PassOnce pass = new PassOnce(s -> testContext.fail(s));

    mailClient.sendMail(mail, result -> {
      log.info("mail finished");
      pass.passOnce();
      mailClient.close();
      if (result.succeeded()) {
        log.info(result.result());
        testContext.assertEquals("[user@example.com]", result.result().getRecipients().toString());
        async.complete();
      } else {
        log.warn("got exception", result.cause());
        testContext.fail(result.cause());
      }
    });
  }

  @Test
  public void allAddressFailureFailsMail(TestContext testContext) {
    this.testContext=testContext;
    smtpServer.setDialogue("220 example.com ESMTP",
        "EHLO",
        "250 example.com",
        "MAIL FROM:",
        "250 2.1.0 Ok",
        "RCPT TO:",
        "501 5.1.3 Bad recipient address syntax", 
        "RCPT TO:",
        "501 5.1.3 Bad recipient address syntax");

    MailMessage mail = exampleMessage()
        .setTo(Arrays.asList("user@example.com", "fail@example.org"));

    MailConfig config = configNoSSL()
        .setAllowRcptErrors(true);
    MailClient mailClient = MailClient.createNonShared(vertx, config);

    testException(mailClient, mail);
  }

  @Override
  public void startSMTP() {
    super.startSMTP();
    smtpServer.setDialogue("220 example.com ESMTP",
        "EHLO",
        "250 example.com",
        "MAIL FROM:",
        "250 2.1.0 Ok",
        "RCPT TO:",
        "250 2.1.5 Ok",
        "RCPT TO:",
        "501 5.1.3 Bad recipient address syntax", 
        "DATA",
        "354 End data with <CR><LF>.<CR><LF>",
        "250 2.0.0 Ok: queued as ABCDDEF0123456789",
        "QUIT",
        "221 2.0.0 Bye");
  }

}

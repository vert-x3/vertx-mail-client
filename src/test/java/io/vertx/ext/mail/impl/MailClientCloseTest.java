/*
 *  Copyright (c) 2011-2020 The original author or authors
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

import io.vertx.ext.mail.LoginOption;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.SMTPTestDummy;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests MailClient close cases.
 *
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
@RunWith(VertxUnitRunner.class)
public class MailClientCloseTest extends SMTPTestDummy {

  /**
   * Test the case that when MailClient is closed after some time when all connections failed with wrong credentials,
   * the NetClient in the SMTPConnectionPool should be closed as well.
   */
  @Test
  public void testNetClientClosedWhenMailClientClosed(TestContext testContext) {
    this.testContext = testContext;
    smtpServer.setDialogue(
      "220 smtp.gmail.com ESMTP o8sm3958210pjs.6 - gsmtp",
      "EHLO",
      "250-smtp.gmail.com at your service, [209.132.188.80]\n" +
        "250-AUTH LOGIN\n" +
        "250 SMTPUTF8",
      "AUTH LOGIN",
      "334 VXNlcm5hbWU6",
      "eHh4",
      "334 UGFzc3dvcmQ6",
      "eXl5",
      "435 4.7.8 Error: authentication failed: authentication failure",
      "QUIT",
      "221 2.0.0 Bye"
    );
    MailConfig config = configLogin();
    MailClientImpl mailClient = (MailClientImpl)MailClient.create(vertx, config);
    Async async = testContext.async();
    mailClient.sendMail(exampleMessage(), testContext.asyncAssertFailure(t1 -> vertx.setTimer(100, r -> {
      mailClient.close();
      try {
        mailClient.getConnectionPool().getNetClient()
          .connect(config.getPort(), config.getHostname(), v -> {});
        fail("SHOULD NOT HERE !");
      } catch (IllegalStateException e) {
        assertTrue(e.getMessage().contains("Client is closed"));
        async.complete();
      }
    })));
  }

}

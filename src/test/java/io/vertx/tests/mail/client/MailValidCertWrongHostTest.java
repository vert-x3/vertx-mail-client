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

import io.vertx.core.net.JksOptions;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.StartTLSOptions;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

/**
 * check when the cert is valid but does not match the hostname, the connection is rejected unless trustAll is set
 *
 * this test uses a different server keystore than MailLocalTest
 *
 * due to a limitation in Wiser, we can only use one cert (defaultSecurityPolicy), so we use TestSmtpServer
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
@RunWith(VertxUnitRunner.class)
public class MailValidCertWrongHostTest extends SMTPTestDummy {

  @Test
  public void mailTestTLSValidCertWrongHost(TestContext testContext) {
    this.testContext = testContext;
    final MailConfig config = defaultConfig().setHostname("127.0.0.1").setPort(1587).setStarttls(StartTLSOptions.REQUIRED)
      .setTrustOptions(new JksOptions()
        .setPath("src/test/resources/certs/client.jks")
        .setPassword("password"));
    MailClient mailClient = MailClient.create(vertx, config);
    testException(mailClient);
  }

  @Test
  public void mailTestTLSWrongHostTrustAll(TestContext testContext) {
    this.testContext = testContext;
    final MailConfig config = defaultConfig().setHostname("127.0.0.1").setPort(1587).setStarttls(StartTLSOptions.REQUIRED)
      .setTrustOptions(new JksOptions()
        .setPath("src/test/resources/certs/client.jks")
        .setPassword("password"))
      .setTrustAll(true);
    MailClient mailClient = MailClient.create(vertx, config);
    testSuccess(mailClient);
  }

  @Override
  protected void startSMTP() {
    smtpServer = new TestSmtpServer(vertx, false, "src/test/resources/certs/server2.jks");
    smtpServer.setDialogue("220 example.com ESMTP",
        "EHLO",
        "250-example.com\n"
            + "250-SIZE 1000000\n"
            + "250 STARTTLS",
        "STARTTLS",
        "220 2.0.0 Ready to start TLS",
        "EHLO",
        "250-example.com\n"
            + "250-SIZE 1000000\n"
            + "250 STARTTLS",
        "MAIL FROM:",
        "250 2.1.0 Ok",
        "RCPT TO:",
        "250 2.1.5 Ok",
        "DATA",
        "354 End data with <CR><LF>.<CR><LF>",
        "250 2.0.0 Ok: queued as ABCDDEF0123456789",
        "QUIT",
        "221 2.0.0 Bye");
  }

}

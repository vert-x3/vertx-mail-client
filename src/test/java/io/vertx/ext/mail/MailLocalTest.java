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

import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

/**
 * this test uses a local SMTP server (wiser from subethasmtp) since this server supports SSL/TLS, the tests relating to
 * that are here
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
@RunWith(VertxUnitRunner.class)
public class MailLocalTest extends SMTPTestWiser {

  @Test
  public void mailTest(TestContext testContext) {
    this.testContext=testContext;
    testSuccess(mailClientLogin(), exampleMessage(), assertExampleMessage());
  }

  @Test
  public void mailTestTLSTrustAll(TestContext testContext) {
    this.testContext=testContext;
    MailClient mailClient = MailClient.create(vertx,
      configLogin().setStarttls(StartTLSOptions.REQUIRED).setTrustAll(true));
    testSuccess(mailClient, exampleMessage(), assertExampleMessage());
  }

  @Test
  public void mailTestTLSNoTrust(TestContext testContext) {
    this.testContext=testContext;
    MailClient mailClient = MailClient.create(vertx, configLogin().setStarttls(StartTLSOptions.REQUIRED));
    testException(mailClient);
  }

  @Test
  public void mailTestTLSCorrectCert(TestContext testContext) {
    this.testContext = testContext;
    final MailConfig config = configLogin().setStarttls(StartTLSOptions.REQUIRED)
        .setKeyStore("src/test/resources/certs/client.jks").setKeyStorePassword("password");
    MailClient mailClient = MailClient.create(vertx, config);
    testSuccess(mailClient, exampleMessage(), assertExampleMessage());
  }

  @Test
  public void mailTestTLSCase(TestContext testContext) {
    this.testContext = testContext;
    final MailConfig config = configLogin().setHostname("LOCALHOST").setStarttls(StartTLSOptions.REQUIRED)
        .setKeyStore("src/test/resources/certs/client.jks").setKeyStorePassword("password");
    MailClient mailClient = MailClient.create(vertx, config);
    testSuccess(mailClient, exampleMessage(), assertExampleMessage());
  }

  @Test
  public void mailTestTLSValidCertWrongHost(TestContext testContext) {
    this.testContext = testContext;
    final MailConfig config = configLogin().setHostname("127.0.0.1").setStarttls(StartTLSOptions.REQUIRED)
        .setKeyStore("src/test/resources/certs/client.jks").setKeyStorePassword("password");
    MailClient mailClient = MailClient.create(vertx, config);
    testException(mailClient);
  }

}

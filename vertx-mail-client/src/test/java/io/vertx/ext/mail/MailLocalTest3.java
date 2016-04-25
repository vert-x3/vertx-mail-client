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
 * this test uses a different server keystore than MailLocalTest, so we need a new test class
 * 
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
@RunWith(VertxUnitRunner.class)
public class MailLocalTest3 extends SMTPTestWiser {

  @Test
  public void mailTestTLSValidCertWrongHost(TestContext testContext) {
    this.testContext = testContext;
    final MailConfig config = configLogin().setHostname("127.0.0.1").setStarttls(StartTLSOptions.REQUIRED)
        .setKeyStore("src/test/resources/certs/client.jks").setKeyStorePassword("password");
    MailClient mailClient = MailClient.createNonShared(vertx, config);
    testException(mailClient);
  }

  protected void startSMTP() {
    super.startSMTP(KeyStoreSSLSocketFactory3.class.getName());
  }

}

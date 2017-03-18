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

import javax.net.ssl.SSLHandshakeException;

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.netty.util.NetUtil;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

/**
 * this tests uses SSL on a local server
 * 
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
@RunWith(VertxUnitRunner.class)
public class MailSslTest extends SMTPTestDummy {

  private static final String SERVER_JKS = "certs/server.jks";
  private static final String SERVER2_JKS = "certs/server2.jks";
  private static final String CLIENT_JKS = "certs/client.jks";

  @Test
  public void mailTestSSLCorrectCert(TestContext testContext) {
    this.testContext = testContext;
    startServer(SERVER2_JKS);
    final MailConfig config = new MailConfig("localhost", 1465, StartTLSOptions.DISABLED, LoginOption.DISABLED)
        .setSsl(true).setKeyStore(CLIENT_JKS).setKeyStorePassword("password");
    MailClient mailClient = MailClient.createNonShared(vertx, config);
    testSuccess(mailClient);
  }

  @Test
  public void mailTestSSLValidCertWrongHost(TestContext testContext) {
    this.testContext = testContext;
    startServer(SERVER2_JKS);
    final MailConfig config = new MailConfig("127.0.0.1", 1465, StartTLSOptions.DISABLED, LoginOption.DISABLED)
        .setSsl(true).setKeyStore(CLIENT_JKS).setKeyStorePassword("password");
    MailClient mailClient = MailClient.createNonShared(vertx, config);
    testException(mailClient);
  }

  @Test
  public void mailTestSSLValidCertIpv6(TestContext testContext) {
    // don't run ipv6 tests when ipv4 is preferred, this should enable running the tests
    // on CI where ipv6 is not configured
    Assume.assumeFalse("no ipv6 support", NetUtil.isIpV4StackPreferred() || "true".equals(System.getProperty("test.disableIpV6")));
    this.testContext = testContext;
    startServer(SERVER_JKS);
    final MailConfig config = new MailConfig("::1", 1465, StartTLSOptions.DISABLED, LoginOption.DISABLED)
        .setSsl(true).setKeyStore(CLIENT_JKS).setKeyStorePassword("password");
    MailClient mailClient = MailClient.createNonShared(vertx, config);
    testSuccess(mailClient);
  }

  @Test
  public void mailTestSSLValidCertIpv6_2(TestContext testContext) {
    Assume.assumeFalse("no ipv6 support", NetUtil.isIpV4StackPreferred() || "true".equals(System.getProperty("test.disableIpV6")));
    this.testContext = testContext;
    startServer(SERVER_JKS);
    final MailConfig config = new MailConfig("[::1]", 1465, StartTLSOptions.DISABLED, LoginOption.DISABLED)
        .setSsl(true).setKeyStore(CLIENT_JKS).setKeyStorePassword("password");
    MailClient mailClient = MailClient.createNonShared(vertx, config);
    testSuccess(mailClient);
  }

  @Test
  public void mailTestSSLValidCertIpv6_3(TestContext testContext) {
    Assume.assumeFalse("no ipv6 support", NetUtil.isIpV4StackPreferred() || "true".equals(System.getProperty("test.disableIpV6")));
    this.testContext = testContext;
    startServer(SERVER_JKS);
    final MailConfig config = new MailConfig("[0000:0000:0000:0000:0000:0000:0000:0001]", 1465, StartTLSOptions.DISABLED, LoginOption.DISABLED)
        .setSsl(true).setKeyStore(CLIENT_JKS).setKeyStorePassword("password");
    MailClient mailClient = MailClient.createNonShared(vertx, config);
    testSuccess(mailClient);
  }

  @Test
  public void mailTestSSLTrustAll(TestContext testContext) {
    this.testContext = testContext;
    startServer(SERVER2_JKS);
    final MailConfig config = new MailConfig("localhost", 1465, StartTLSOptions.DISABLED, LoginOption.DISABLED)
        .setSsl(true).setTrustAll(true);
    MailClient mailClient = MailClient.createNonShared(vertx, config);
    testSuccess(mailClient);
  }

  @Test
  public void mailTestSSLNoTrust(TestContext testContext) {
    this.testContext = testContext;
    startServer(SERVER2_JKS);
    final MailConfig config = new MailConfig("localhost", 1465, StartTLSOptions.DISABLED, LoginOption.DISABLED)
        .setSsl(true);
    MailClient mailClient = MailClient.createNonShared(vertx, config);
    testException(mailClient, SSLHandshakeException.class);
  }

  @Override
  protected void startSMTP() {
    // start server later since the tests use different keystores
  }

  private void startServer(String keystore) {
    smtpServer = new TestSmtpServer(vertx, true, keystore);
  }

}

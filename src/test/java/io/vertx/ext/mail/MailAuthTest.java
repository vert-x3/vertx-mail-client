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

import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * auth examples with failures mostly
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
@RunWith(VertxUnitRunner.class)
public class MailAuthTest extends SMTPTestDummy {

  @Test
  public void authLoginTest(TestContext testContext) {
    this.testContext=testContext;
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
      "MAIL FROM",
      "250 2.1.0 Ok",
      "RCPT TO",
      "250 2.1.5 Ok",
      "DATA",
      "354 End data with <CR><LF>.<CR><LF>",
      "250 2.0.0 Ok: queued as ABCD",
      "QUIT",
      "221 2.0.0 Bye");

    testSuccess(mailClientLogin());
  }

  @Test
  public void authLoginFailTest(TestContext testContext) {
    this.testContext=testContext;
    smtpServer.setDialogue("220 example.com ESMTP",
      "EHLO",
      "250-example.com\n" +
        "250 AUTH LOGIN",
      "AUTH LOGIN",
      "334 VXNlcm5hbWU6",
      "eHh4",
      "334 UGFzc3dvcmQ6",
      "eXl5",
      "435 4.7.8 Error: authentication failed: authentication failure");

    testException(mailClientLogin());
  }

  @Test
  public void authLoginStartFailTest(TestContext testContext) {
    this.testContext=testContext;
    smtpServer.setDialogue("220 example.com ESMTP",
      "EHLO",
      "250-example.com\n" +
        "250 AUTH LOGIN",
      "AUTH LOGIN",
      "555 login is not possible due to some error");

    testException(mailClientLogin());
  }

  @Test
  public void authLoginUsernameFailTest(TestContext testContext) {
    this.testContext=testContext;
    smtpServer.setDialogue("220 example.com ESMTP",
      "EHLO",
      "250-example.com\n" +
        "250 AUTH LOGIN",
      "AUTH LOGIN",
      "334 VXNlcm5hbWU6",
      "eHh4",
      "555 login is not possible due to some error");

    testException(mailClientLogin());
  }

  @Test
  public void authPlainTest(TestContext testContext) {
    this.testContext=testContext;
    smtpServer.setDialogue("220 example.com ESMTP",
      "EHLO",
      "250-example.com\n" +
        "250 AUTH PLAIN",
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
      "221 2.0.0 Bye");

    testSuccess(mailClientLogin());
  }

  @Test
  public void authPlainFailTest(TestContext testContext) {
    this.testContext=testContext;
    smtpServer.setDialogue("220 example.com ESMTP",
      "EHLO",
      "250-example.com\n" +
        "250 AUTH PLAIN",
      "AUTH PLAIN AHh4eAB5eXk=",
      "435 4.7.8 Error: authentication failed: bad protocol / cancel");

    testException(mailClientLogin());
  }

  @Test
  public void authCramMD5Test(TestContext testContext) {
    this.testContext=testContext;
    smtpServer.setDialogue("220 example.com ESMTP",
      "EHLO",
      "250-example.com\n" +
        "250 AUTH CRAM-MD5",
      "AUTH CRAM-MD5",
      "334 PDEyMzQuYWJjZEBleGFtcGxlLmNvbT4=",
      "eHh4IDE2ZGEzMGQ5NmEwNTY4NWQ0MmQ4YzM5ZDlkMDgxOGIx",
      "250 2.1.0 Ok",
      "MAIL FROM",
      "250 2.1.0 Ok",
      "RCPT TO",
      "250 2.1.5 Ok",
      "DATA",
      "354 End data with <CR><LF>.<CR><LF>",
      "250 2.0.0 Ok: queued as ABCD",
      "QUIT",
      "221 2.0.0 Bye");

    testSuccess(mailClientLogin());
  }

  @Test
  public void authCramMD5StartFailTest(TestContext testContext) {
    this.testContext=testContext;
    smtpServer.setDialogue("220 example.com ESMTP",
      "EHLO",
      "250-example.com\n" +
        "250 AUTH CRAM-MD5",
      "AUTH CRAM-MD5",
      "555 login is not possible due to some error");

    testException(mailClientLogin());
  }

  @Test
  public void authCramMD5FailTest(TestContext testContext) {
    this.testContext=testContext;
    smtpServer.setDialogue("220 example.com ESMTP",
      "EHLO",
      "250-example.com\n" +
        "250 AUTH CRAM-MD5",
      "AUTH CRAM-MD5",
      "334 PDEyMzQuYWJjZEBleGFtcGxlLmNvbT4=",
      "eHh4IDE2ZGEzMGQ5NmEwNTY4NWQ0MmQ4YzM5ZDlkMDgxOGIx",
      "435 4.7.8 Error: authentication failed: bad protocol / cancel");

    testException(mailClientLogin());
  }

  @Test
  public void authJunkTest(TestContext testContext) {
    this.testContext=testContext;
    smtpServer.setDialogue("220 example.com ESMTP",
      "EHLO",
      "250-example.com\n" +
        "250 AUTH JUNK");

    testException(mailClientLogin());
  }

  /**
   * test we have Login REQUIRED but no login data in the config
   */
  @Test
  public void authAuthDataMissingTest(TestContext testContext) {
    this.testContext=testContext;
    smtpServer.setDialogue("220 example.com ESMTP",
      "EHLO",
      "250-example.com\n" +
        "250 AUTH PLAIN");

    testException(MailClient.createNonShared(vertx, defaultConfig().setLogin(LoginOption.REQUIRED)));
  }

  @Test
  public void authSelectMethodsTest(TestContext testContext) {
    this.testContext=testContext;
    smtpServer.setDialogue("220 example.com ESMTP",
      "EHLO",
      "250-example.com\n" +
        "250 AUTH PLAIN LOGIN",
      "AUTH LOGIN",
      "334 VXNlcm5hbWU6",
      "eHh4",
      "334 UGFzc3dvcmQ6",
      "eXl5",
      "250 2.1.0 Ok",
      "MAIL FROM",
      "250 2.1.0 Ok",
      "RCPT TO",
      "250 2.1.5 Ok",
      "DATA",
      "354 End data with <CR><LF>.<CR><LF>",
      "250 2.0.0 Ok: queued as ABCD",
      "QUIT",
      "221 2.0.0 Bye");

    testSuccess(MailClient.createNonShared(vertx, configLogin().setAuthMethods("LOGIN CRAM-MD5")));
  }

  @Test
  public void authSelectMethodsNoneTest(TestContext testContext) {
    this.testContext=testContext;
    smtpServer.setDialogue("220 example.com ESMTP",
      "EHLO",
      "250-example.com\n" +
        "250 AUTH PLAIN LOGIN");

    testException(MailClient.createNonShared(vertx, configLogin().setAuthMethods("DIGEST-MD5 CRAM-MD5")));
  }

  @Test
  public void authXOAUTH2Test(TestContext testContext) {
    this.testContext=testContext;

    smtpServer.setDialogue(
      "220 mx.google.com ESMTP 12sm2095603fks.9",
      "EHLO",
      "250-mx.google.com at your service, [172.31.135.47]\n" +
      "250-SIZE 35651584\n" +
      "250-8BITMIME\n" +
      "250-AUTH LOGIN PLAIN XOAUTH XOAUTH2\n" +
      "250-ENHANCEDSTATUSCODES\n" +
      "250 PIPELINING",
      "AUTH XOAUTH2 dXNlcj14eHgBYXV0aD1CZWFyZXIgeXl5AQE=",
      "235 2.7.0 Accepted",
      "MAIL FROM",
      "250 2.1.0 Ok",
      "RCPT TO",
      "250 2.1.5 Ok",
      "DATA",
      "354 End data with <CR><LF>.<CR><LF>",
      "250 2.0.0 Ok: queued as ABCD",
      "QUIT",
      "221 2.0.0 Bye");

    testSuccess(mailClientLogin());
  }
}

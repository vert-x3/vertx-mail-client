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
 * Test a server that doesn't support EHLO and a few errors in the server greeting
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
@RunWith(VertxUnitRunner.class)
public class HeloTest extends SMTPTestDummy {

  @Test
  public void mailEhloMissingTest(TestContext testContext) {
    this.testContext=testContext;
    smtpServer.setDialogue("220 example.com ESMTP",
      "EHLO",
      "402 4.5.2 Error: command not recognized",
      "HELO",
      "250 example.com",
      "MAIL FROM",
      "250 2.1.0 Ok",
      "RCPT TO",
      "250 2.1.5 Ok",
      "DATA",
      "354 End data with <CR><LF>.<CR><LF>",
      "250 2.0.0 Ok: queued as ABCDDEF0123456789",
      "QUIT",
      "221 2.0.0 Bye");
    smtpServer.setCloseImmediately(false);

    testSuccess();
  }

  /*
   * if the server does not even answer correctly to EHLO, we can turn it off
   */
  @Test
  public void mailNoEhloTest(TestContext testContext) {
    this.testContext=testContext;
    smtpServer.setDialogue("220 example.com",
      "HELO",
      "250 example.com",
      "MAIL FROM",
      "250 2.1.0 Ok",
      "RCPT TO",
      "250 2.1.5 Ok",
      "DATA",
      "354 End data with <CR><LF>.<CR><LF>",
      "250 2.0.0 Ok: queued as ABCDDEF0123456789",
      "QUIT",
      "221 2.0.0 Bye");
    smtpServer.setCloseImmediately(false);

    MailConfig mailConfig = defaultConfig();
    mailConfig.setDisableEsmtp(true);
    testSuccess(MailClient.create(vertx, mailConfig), exampleMessage());
  }

  /*
   * if the server does not support ESMTP, but gives an valid error response to EHLO
   */
  @Test
  public void mailNoEsmtpTest(TestContext testContext) {
    this.testContext=testContext;
    smtpServer.setDialogue("220 example.com",
      "EHLO ",
      "502 EHLO command not understood",
      "HELO ",
      "250 example.com",
      "MAIL FROM",
      "250 2.1.0 Ok",
      "RCPT TO",
      "250 2.1.5 Ok",
      "DATA",
      "354 End data with <CR><LF>.<CR><LF>",
      "250 2.0.0 Ok: queued as ABCDDEF0123456789",
      "QUIT",
      "221 2.0.0 Bye");
    smtpServer.setCloseImmediately(false);

    testSuccess();
  }

  /*
   * Test what happens when a reply is sent after the QUIT reply
   */
  @Test
  public void replyAfterQuitTest(TestContext testContext) {
    this.testContext=testContext;
    smtpServer.setDialogue("220 example.com ESMTP",
      "EHLO",
      "250-example.com\n" +
        "250-SIZE 48000000\n" +
        "250 PIPELINING",
      "MAIL FROM",
      "250 2.1.0 Ok",
      "RCPT TO",
      "250 2.1.5 Ok",
      "DATA",
      "354 End data with <CR><LF>.<CR><LF>",
      // message data
      "250 2.0.0 Ok: queued as ABCDDEF0123456789",
      "QUIT",
      "221 2.0.0 Bye",
      "",
      // this should not happen:
      "this is unexpected"
    );
    smtpServer.setCloseImmediately(false);

    testSuccess();
  }

  @Test
  public void serverUnavailableTest(TestContext testContext) {
    this.testContext=testContext;
    smtpServer.setDialogue("400 cannot talk to you right now");
    smtpServer.setCloseImmediately(true);

    testException();
  }

  @Test
  public void connectionRefusedTest(TestContext testContext) {
    this.testContext=testContext;
    testException(MailClient.create(vertx, new MailConfig("localhost", 1588)));
  }

  @Test
  public void stlsMissingTest(TestContext testContext) {
    this.testContext=testContext;
    smtpServer.setDialogue("220 example.com ESMTP multiline",
      "EHLO",
      "250-example.com\n" +
        "250-SIZE 48000000\n" +
        "250 PIPELINING",
      "MAIL FROM",
      "250 2.1.0 Ok",
      "RCPT TO",
      "250 2.1.5 Ok",
      "DATA",
      "354 End data with <CR><LF>.<CR><LF>",
      "250 2.0.0 Ok: queued as ABCDDEF0123456789",
      "QUIT",
      "221 2.0.0 Bye");
    smtpServer.setCloseImmediately(false);

    testException(mailClientTLS());
  }

  @Test
  public void closeOnConnectTest(TestContext testContext) {
    this.testContext = testContext;
    smtpServer.setDialogue("");
    smtpServer.setCloseImmediately(true);

    testException();
  }

  /*
   * test a multiline reply as welcome message. this is done
   * e.g. by the servers from AOL. the service will deny access
   * if the client tries to do PIPELINING before checking the EHLO
   * capabilities
   */
  @Test
  public void mailMultilineWelcomeTest(TestContext testContext) {
    this.testContext=testContext;
    smtpServer.setDialogue("220-example.com ESMTP multiline\n" +
        "220-this server uses a multi-line welcome message\n" +
        "220 this is supposed to confuse spammers",
      "EHLO",
      "250-example.com\n" +
        "250-SIZE 48000000\n" +
        "250 PIPELINING",
      "MAIL FROM",
      "250 2.1.0 Ok",
      "RCPT TO",
      "250 2.1.5 Ok",
      "DATA",
      "354 End data with <CR><LF>.<CR><LF>",
      "250 2.0.0 Ok: queued as ABCDDEF0123456789",
      "QUIT",
      "221 2.0.0 Bye");
    smtpServer.setCloseImmediately(false);

    testSuccess();
  }

  /*
   * simulate the server closes the connection immediately after the
   * banner message
   */
  @Test
  public void closeAfterBannerTest(TestContext testContext) {
    this.testContext=testContext;
    smtpServer.setDialogue("220 example.com ESMTP");
    smtpServer.setCloseImmediately(true);

    testException();
  }

  /*
   * test that we try to use EHLO when the server sends esmtp not all uppercase
   * (https://github.com/vert-x3/vertx-mail-client/issues/46)
   *
   * the check for "ESMTP" is mostly a hack to avoid sending EHLO to a server that doesn't support it, which is not very
   * likely anymore, unless you have a firewall that blocks ESMTP commands. In this case the complete server helo
   * message is probably blocked anyway.
   */
  @Test
  public void esmtpCheckTest(TestContext testContext) {
    this.testContext=testContext;
    smtpServer.setDialogue("220 example.com Esmtp",
      "EHLO",
      "250 example.com",
      "MAIL FROM",
      "250 2.1.0 Ok",
      "RCPT TO",
      "250 2.1.5 Ok",
      "DATA",
      "354 End data with <CR><LF>.<CR><LF>",
      "250 2.0.0 Ok: queued as ABCDDEF0123456789",
      "QUIT",
      "221 2.0.0 Bye");
    smtpServer.setCloseImmediately(false);

    testSuccess();
  }


}

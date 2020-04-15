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
 * test that the SIZE option is added to the MAIL FROM command when ESMTP SIZE is supported
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
@RunWith(VertxUnitRunner.class)
public class MailFromSizeTest extends SMTPTestDummy {

  @Test
  public void mailTest(TestContext testContext) {
    this.testContext=testContext;
    smtpServer.setDialogue(
        "220 example.com ESMTP",
        "EHLO",
        "250-example.com\n" +
            "250 SIZE 1000000",
        "^MAIL FROM:<[^>]+@[^>]+> SIZE=[0-9]+$",
        "250 2.1.0 Ok",
        "RCPT TO:",
        "250 2.1.5 Ok",
        "DATA",
        "354 End data with <CR><LF>.<CR><LF>",
        "250 2.0.0 Ok: queued as ABCDDEF0123456789",
        "QUIT",
        "221 2.0.0 Bye");
    smtpServer.setCloseImmediately(true);

    testSuccess(mailClientDefault(), exampleMessage());
  }

}

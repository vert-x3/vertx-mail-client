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

import javax.mail.internet.MimeMessage;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * test messages containing a few dots
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
@RunWith(VertxUnitRunner.class)
public class MailWithDotsTest extends SMTPTestWiser {

  @Test
  public void mailTest(TestContext testContext) {
    this.testContext=testContext;
    mailTestText(".\n");
  }

  @Test
  public void mailTest2(TestContext testContext) {
    this.testContext=testContext;
    mailTestText(".\n.\n.\n.\n.\n.\n");
  }

  @Test
  public void mailTest3(TestContext testContext) {
    this.testContext=testContext;
    mailTestText(".\n..\n...\n");
  }

  @Test
  public void mailTest4(TestContext testContext) {
    this.testContext=testContext;
    mailTestText(".xxx\n");
  }

  @Test
  public void mailTest5(TestContext testContext) {
    this.testContext=testContext;
    mailTestText(" .\n");
  }

  @Test
  public void mailTest6(TestContext testContext) {
    this.testContext=testContext;
    mailTestText(".\nX\n.\n");
  }

  @Test
  public void mailTestLarge(TestContext testContext) {
    this.testContext=testContext;
    StringBuilder sb = new StringBuilder();
    sb.append("................................................................................\n");
    for (int i = 0; i < 10; i++) {
      sb.append(sb);
    }
    mailTestText(sb.toString());
  }

  @Test
  public void mailTestMissingNL(TestContext testContext) {
    this.testContext=testContext;
    MailMessage message = exampleMessage();
    // the protocol adds a newline at the end if there isn't one
    message.setText(".");
    testSuccess(mailClientLogin(), message, () -> {
      final MimeMessage mimeMessage = wiser.getMessages().get(0).getMimeMessage();
      testContext.assertEquals(".\n", TestUtils.conv2nl(TestUtils.inputStreamToString(mimeMessage.getInputStream())));
    });
  }

  private void mailTestText(final String text) {
    MailMessage message = exampleMessage();
    message.setText(text);
    testSuccess(mailClientLogin(), message, () -> {
      final MimeMessage mimeMessage = wiser.getMessages().get(0).getMimeMessage();
      testContext.assertEquals(text, TestUtils.conv2nl(TestUtils.inputStreamToString(mimeMessage.getInputStream())));
    });
  }

}

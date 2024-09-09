/*
 *  Copyright (c) 2011-2022 The original author or authors
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

import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.subethamail.wiser.WiserMessage;

import javax.mail.internet.MimeMessage;

import static org.hamcrest.core.StringContains.containsString;

/**
 * Test messages which has longer lines than 1000 characters.
 *
 * @author <a href="mailto: aoingl@gmail.com">Lin Gao</a>
 */
@RunWith(VertxUnitRunner.class)
public class MailLongMessageTest extends SMTPTestWiser {

  @Test
  public void mailLongHtmlMessage(TestContext testContext) {
    this.testContext=testContext;
    // each segment length is 50
    final String segment = "this_is_a_very_long_link_than_1000_ascii_character";
    final StringBuilder sb = new StringBuilder("<html>\n<body>\n<a href=\"http://");
    for (int i = 0; i < 20; i ++) {
      sb.append(segment);
    }
    sb.append("\">Link</a>\n</body>\n</html>");
    MailMessage mailMessage = new MailMessage().setFrom("from@example.com").setTo("user@example.com")
      .setSubject("Subject").setHtml(sb.toString());
    testSuccess(mailClientLogin(), mailMessage, () -> {
      final WiserMessage message = wiser.getMessages().get(0);
      testContext.assertEquals("from@example.com", message.getEnvelopeSender());
      final MimeMessage mimeMessage = message.getMimeMessage();
      assertThat(mimeMessage.getContentType(), containsString("text/html"));
      testContext.assertEquals("Subject", mimeMessage.getSubject());
      testContext.assertEquals(sb + "\n", TestUtils.conv2nl(TestUtils.inputStreamToString(mimeMessage.getInputStream())));
    });
  }

}

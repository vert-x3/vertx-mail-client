/*
 *  Copyright (c) 2011-2019 The original author or authors
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

import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.OpenOptions;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.mail.internet.MimeMultipart;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * test messages with a ReadStream specified
 *
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
@RunWith(VertxUnitRunner.class)
public class MailAttachementStreamTest extends SMTPTestWiser {

  @Test
  public void mailWithOneAttachement(TestContext testContext) {
    this.testContext = testContext;
    String text = "This is a message with attachement data specified";
    MailMessage message = exampleMessage().setText(text);
    String path = "log4j.properties";
    Buffer buffer = vertx.fileSystem().readFileBlocking(path);
    MailAttachment attachment = new MailAttachment().setContentType("text/plain").setName("file").setData(buffer);
    message.setAttachment(attachment);
    testSuccess(mailClientLogin(), message, () -> {
      final MimeMultipart multiPart = (MimeMultipart)wiser.getMessages().get(0).getMimeMessage().getContent();
      testContext.assertEquals(2, multiPart.getCount());
      testContext.assertEquals(text, TestUtils.inputStreamToString(multiPart.getBodyPart(0).getInputStream()));
      testContext.assertEquals(buffer.toString(), TestUtils.inputStreamToString(multiPart.getBodyPart(1).getInputStream()));
    });
  }

  @Test
  public void mailWithTwoAttachements(TestContext testContext) {
    this.testContext = testContext;
    String text = "This is a message with attachement specified";
    MailMessage message = exampleMessage().setText(text);
    List<MailAttachment> list = new ArrayList<>();
    Buffer image = vertx.fileSystem().readFileBlocking("logo-white-big.png");
    list.add(new MailAttachment()
      .setData(Buffer.buffer(image.getBytes()))
      .setName("logo-white-big.png")
      .setContentType("image/png")
      .setDisposition("inline")
      .setDescription("logo of vert.x web page"));

    String path = "log4j.properties";
    Buffer logFile = vertx.fileSystem().readFileBlocking(path);
    list.add(new MailAttachment()
      .setData(logFile)
      .setName(path)
      .setContentType("text/plain")
      .setDisposition("attachment")
      .setDescription("This is a log4j properties file")
    );
    message.setAttachment(list);
    testSuccess(mailClientLogin(), message, () -> {
      final MimeMultipart multiPart = (MimeMultipart)wiser.getMessages().get(0).getMimeMessage().getContent();
      testContext.assertEquals(3, multiPart.getCount());
      testContext.assertEquals(text, TestUtils.inputStreamToString(multiPart.getBodyPart(0).getInputStream()));
      testContext.assertTrue(Arrays.equals(image.getBytes(), TestUtils.inputStreamToBytes(multiPart.getBodyPart(1).getInputStream())));
      testContext.assertEquals(logFile.toString(), TestUtils.inputStreamToString(multiPart.getBodyPart(2).getInputStream()));
    });
  }

  @Test
  public void mailWithOneAttachementStream(TestContext testContext) {
    this.testContext = testContext;
    String text = "This is a message with an attachment and with stream specified";
    MailMessage message = exampleMessage().setText(text);
    String path = "log4j.properties";
    Buffer buffer = vertx.fileSystem().readFileBlocking(path);
    MailAttachment attachment = new MailAttachment()
      .setContentType("text/plain")
      .setName("file")
      .setStream(vertx.fileSystem().openBlocking(path, new OpenOptions()));
    message.setAttachment(attachment);
    testSuccess(mailClientLogin(), message, () -> {
      final MimeMultipart multiPart = (MimeMultipart)wiser.getMessages().get(0).getMimeMessage().getContent();
      testContext.assertEquals(2, multiPart.getCount());
      testContext.assertEquals(text, TestUtils.inputStreamToString(multiPart.getBodyPart(0).getInputStream()));
      testContext.assertEquals(buffer.toString(), TestUtils.inputStreamToString(multiPart.getBodyPart(1).getInputStream()));
    });
  }

  @Test
  public void mailWithTwoAttachementStreams(TestContext testContext) {
    this.testContext = testContext;
    String text = "This is a message with 2 attachements specified and with stream specified";
    MailMessage message = exampleMessage().setText(text);
    List<MailAttachment> list = new ArrayList<>();
    String imgPath = "logo-white-big.png";
    Buffer image = vertx.fileSystem().readFileBlocking(imgPath);
    list.add(new MailAttachment()
      .setStream(vertx.fileSystem().openBlocking(imgPath, new OpenOptions()))
      .setName(imgPath)
      .setContentType("image/png")
      .setDisposition("inline")
      .setDescription("logo of vert.x web page"));

    String path = "log4j.properties";
    Buffer logFile = vertx.fileSystem().readFileBlocking(path);
    list.add(new MailAttachment()
      .setStream(vertx.fileSystem().openBlocking(path, new OpenOptions()))
      .setName(path)
      .setContentType("text/plain")
      .setDisposition("attachment")
      .setDescription("This is a log4j properties file")
    );
    message.setAttachment(list);
    testSuccess(mailClientLogin(), message, () -> {
      final MimeMultipart multiPart = (MimeMultipart)wiser.getMessages().get(0).getMimeMessage().getContent();
      testContext.assertEquals(3, multiPart.getCount());
      testContext.assertEquals(text, TestUtils.inputStreamToString(multiPart.getBodyPart(0).getInputStream()));
      testContext.assertTrue(Arrays.equals(image.getBytes(), TestUtils.inputStreamToBytes(multiPart.getBodyPart(1).getInputStream())));
      testContext.assertEquals(logFile.toString(), TestUtils.inputStreamToString(multiPart.getBodyPart(2).getInputStream()));
    });
  }

}

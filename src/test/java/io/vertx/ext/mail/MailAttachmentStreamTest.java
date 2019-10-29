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
public class MailAttachmentStreamTest extends SMTPTestWiser {

  @Test
  public void mailWithOneAttachment(TestContext testContext) {
    this.testContext = testContext;
    String text = "This is a message with attachment data specified";
    MailMessage message = exampleMessage().setText(text);
    String path = "log4j.properties";
    Buffer buffer = vertx.fileSystem().readFileBlocking(path);
    MailAttachment attachment = MailAttachment.create().setContentType("text/plain").setName("file").setData(buffer);
    message.setAttachment(attachment);
    testSuccess(mailClientLogin(), message, () -> {
      final MimeMultipart multiPart = (MimeMultipart)wiser.getMessages().get(0).getMimeMessage().getContent();
      testContext.assertEquals(2, multiPart.getCount());
      testContext.assertEquals(text, TestUtils.conv2nl(TestUtils.inputStreamToString(multiPart.getBodyPart(0).getInputStream())));
      testContext.assertTrue(Arrays.equals(buffer.getBytes(), TestUtils.inputStreamToBytes(multiPart.getBodyPart(1).getInputStream())));
    });
  }

  @Test
  public void mailWithTwoAttachments(TestContext testContext) {
    this.testContext = testContext;
    String text = "This is a message with attachment specified";
    MailMessage message = exampleMessage().setText(text);
    List<MailAttachment> list = new ArrayList<>();
    Buffer image = vertx.fileSystem().readFileBlocking("logo-white-big.png");
    list.add(MailAttachment.create()
      .setData(Buffer.buffer(image.getBytes()))
      .setName("logo-white-big.png")
      .setContentType("image/png")
      .setDisposition("inline")
      .setDescription("logo of vert.x web page"));

    String path = "log4j.properties";
    Buffer logFile = vertx.fileSystem().readFileBlocking(path);
    list.add(MailAttachment.create()
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
      testContext.assertEquals(text, TestUtils.conv2nl(TestUtils.inputStreamToString(multiPart.getBodyPart(0).getInputStream())));
      testContext.assertTrue(Arrays.equals(image.getBytes(), TestUtils.inputStreamToBytes(multiPart.getBodyPart(1).getInputStream())));
      testContext.assertTrue(Arrays.equals(logFile.getBytes(), TestUtils.inputStreamToBytes(multiPart.getBodyPart(2).getInputStream())));
    });
  }

  @Test
  public void mailWithOneAttachmentStream(TestContext testContext) {
    this.testContext = testContext;
    String text = "This is a message with an attachment and with stream specified";
    MailMessage message = exampleMessage().setText(text);
    String path = "log4j.properties";
    Buffer buffer = vertx.fileSystem().readFileBlocking(path);
    MailAttachment attachment = MailAttachment.create()
      .setContentType("text/plain")
      .setName("file")
      .setSize(buffer.length())
      .setStream(vertx.fileSystem().openBlocking(path, new OpenOptions()));
    message.setAttachment(attachment);
    testSuccess(mailClientLogin(), message, () -> {
      final MimeMultipart multiPart = (MimeMultipart)wiser.getMessages().get(0).getMimeMessage().getContent();
      testContext.assertEquals(2, multiPart.getCount());
      testContext.assertEquals(text, TestUtils.conv2nl(TestUtils.inputStreamToString(multiPart.getBodyPart(0).getInputStream())));
      testContext.assertTrue(Arrays.equals(buffer.getBytes(), TestUtils.inputStreamToBytes(multiPart.getBodyPart(1).getInputStream())));
    });
  }

  @Test
  public void mailWithTwoAttachmentStreams(TestContext testContext) {
    this.testContext = testContext;
    String text = "This is a message with 2 attachments specified and with stream specified";
    MailMessage message = exampleMessage().setText(text);
    List<MailAttachment> list = new ArrayList<>();
    String imgPath = "logo-white-big.png";
    Buffer image = vertx.fileSystem().readFileBlocking(imgPath);
    list.add(MailAttachment.create()
      .setStream(vertx.fileSystem().openBlocking(imgPath, new OpenOptions()))
      .setSize(image.length())
      .setName(imgPath)
      .setContentType("image/png")
      .setDisposition("inline")
      .setDescription("logo of vert.x web page"));

    String path = "log4j.properties";
    Buffer logFile = vertx.fileSystem().readFileBlocking(path);
    list.add(MailAttachment.create()
      .setStream(vertx.fileSystem().openBlocking(path, new OpenOptions()))
      .setSize(logFile.length())
      .setName(path)
      .setContentType("text/plain")
      .setDisposition("attachment")
      .setDescription("This is a log4j properties file")
    );
    message.setAttachment(list);
    testSuccess(mailClientLogin(), message, () -> {
      final MimeMultipart multiPart = (MimeMultipart)wiser.getMessages().get(0).getMimeMessage().getContent();
      testContext.assertEquals(3, multiPart.getCount());
      testContext.assertEquals(text, TestUtils.conv2nl(TestUtils.inputStreamToString(multiPart.getBodyPart(0).getInputStream())));
      testContext.assertTrue(Arrays.equals(image.getBytes(), TestUtils.inputStreamToBytes(multiPart.getBodyPart(1).getInputStream())));
      testContext.assertTrue(Arrays.equals(logFile.getBytes(), TestUtils.inputStreamToBytes(multiPart.getBodyPart(2).getInputStream())));
    });
  }

  // This method creates 2 attachments on each call, the first one is the logo image file specified by it's data.
  // the second attachment is the log4j.properties file specified by the stream.
  private List<MailAttachment> mailAttachments(String deposition, Buffer logoBuffer) {
    List<MailAttachment> list = new ArrayList<>();
    String imgPath = "logo-white-big.png";
    list.add(MailAttachment.create()
      .setStream(vertx.fileSystem().openBlocking(imgPath, new OpenOptions()))
      .setSize(logoBuffer.length())
      .setName(imgPath)
      .setContentType("image/png")
      .setContentId("<logo@example.com>")
      .setDisposition(deposition)
      .setDescription("logo of vert.x web page"));

    String path = "log4j.properties";
    list.add(MailAttachment.create()
      .setStream(vertx.fileSystem().openBlocking(path, new OpenOptions()))
      .setName(path)
      .setContentType("text/plain")
      .setDisposition(deposition)
      .setDescription("This is a log4j properties file")
    );
    return list;
  }

  @Test
  public void mailWithOneDataOtherStreamForAttachment(TestContext testContext) {
    this.testContext = testContext;
    Buffer image = vertx.fileSystem().readFileBlocking("logo-white-big.png");
    Buffer logFile = vertx.fileSystem().readFileBlocking("log4j.properties");
    String text = "This is a message with 2 attachments, one is specified by data, and the other is specified by stream";
    MailMessage message = exampleMessage().setText(text);
    message.setAttachment(mailAttachments("attachment", image));
    testSuccess(mailClientLogin(), message, () -> {
      final MimeMultipart multiPart = (MimeMultipart)wiser.getMessages().get(0).getMimeMessage().getContent();
      testContext.assertEquals(3, multiPart.getCount());
      testContext.assertEquals(text, TestUtils.conv2nl(TestUtils.inputStreamToString(multiPart.getBodyPart(0).getInputStream())));
      testContext.assertTrue(Arrays.equals(image.getBytes(), TestUtils.inputStreamToBytes(multiPart.getBodyPart(1).getInputStream())));
      testContext.assertTrue(Arrays.equals(logFile.getBytes(), TestUtils.inputStreamToBytes(multiPart.getBodyPart(2).getInputStream())));
    });
  }

  @Test
  public void testHTMLWithAttachments(TestContext testContext) {
    this.testContext = testContext;
    Buffer image = vertx.fileSystem().readFileBlocking("logo-white-big.png");
    Buffer logFile = vertx.fileSystem().readFileBlocking("log4j.properties");
    String html = "Here is the html email, Click <a href=\"http://vertx.io\">Vert Home</a> to visit.";
    MailMessage message = exampleMessage()
      .setAttachment(mailAttachments("attachment", image))
      .setText(null) // html only
      .setHtml(html);
    testSuccess(mailClientLogin(), message, () -> {
      final MimeMultipart multiPart = (MimeMultipart)wiser.getMessages().get(0).getMimeMessage().getContent();
      testContext.assertEquals(3, multiPart.getCount());
      testContext.assertEquals(html, TestUtils.conv2nl(TestUtils.inputStreamToString(multiPart.getBodyPart(0).getInputStream())));
      testContext.assertTrue(Arrays.equals(image.getBytes(), TestUtils.inputStreamToBytes(multiPart.getBodyPart(1).getInputStream())));
      testContext.assertTrue(Arrays.equals(logFile.getBytes(), TestUtils.inputStreamToBytes(multiPart.getBodyPart(2).getInputStream())));
    });
  }

  @Test
  public void testHTMLWithInlineAttachments(TestContext testContext) {
    this.testContext = testContext;
    Buffer image = vertx.fileSystem().readFileBlocking("logo-white-big.png");
    Buffer logFile = vertx.fileSystem().readFileBlocking("log4j.properties");
    String html = "Here is the html email, Take a look at the Logo: <img src=\"cid:logo@example.com\" /> </br> " +
      "Click <a href=\"http://vertx.io\">Vert Home</a> to visit.";
    MailMessage message = exampleMessage()
      .setInlineAttachment(mailAttachments("inline", image))
      .setText(null) // html only
      .setHtml(html);
    testSuccess(mailClientLogin(), message, () -> {
      final MimeMultipart multiPart = (MimeMultipart)wiser.getMessages().get(0).getMimeMessage().getContent();
      testContext.assertEquals(3, multiPart.getCount());
      testContext.assertEquals(html, TestUtils.conv2nl(TestUtils.inputStreamToString(multiPart.getBodyPart(0).getInputStream())));
      testContext.assertTrue(Arrays.equals(image.getBytes(), TestUtils.inputStreamToBytes(multiPart.getBodyPart(1).getInputStream())));
      testContext.assertTrue(Arrays.equals(logFile.getBytes(), TestUtils.inputStreamToBytes(multiPart.getBodyPart(2).getInputStream())));
    });
  }

  @Test
  public void testHTMLWithAttachmentsAndInlineAttachments(TestContext testContext) {
    this.testContext = testContext;
    Buffer image = vertx.fileSystem().readFileBlocking("logo-white-big.png");
    Buffer logFile = vertx.fileSystem().readFileBlocking("log4j.properties");
    String html = "Here is the html email, Take a look at the Logo: <img src=\"cid:logo@example.com\" /> </br> " +
      "Click <a href=\"http://vertx.io\">Vert Home</a> to visit.";
    MailMessage message = exampleMessage()
      .setAttachment(mailAttachments("attachment", image))
      .setInlineAttachment(mailAttachments("inline", image))
      .setText(null) // html only
      .setHtml(html);
    testSuccess(mailClientLogin(), message, () -> {
      final MimeMultipart multiPart = (MimeMultipart)wiser.getMessages().get(0).getMimeMessage().getContent();
      testContext.assertEquals(3, multiPart.getCount());

      MimeMultipart htmlPart = (MimeMultipart)multiPart.getBodyPart(0).getContent();
      testContext.assertEquals(3, htmlPart.getCount());
      testContext.assertEquals(html, TestUtils.conv2nl(TestUtils.inputStreamToString(htmlPart.getBodyPart(0).getInputStream())));
      testContext.assertTrue(Arrays.equals(image.getBytes(), TestUtils.inputStreamToBytes(htmlPart.getBodyPart(1).getInputStream())));
      testContext.assertTrue(Arrays.equals(logFile.getBytes(), TestUtils.inputStreamToBytes(htmlPart.getBodyPart(2).getInputStream())));

      // left 2 parts in top level
      testContext.assertTrue(Arrays.equals(image.getBytes(), TestUtils.inputStreamToBytes(multiPart.getBodyPart(1).getInputStream())));
      testContext.assertTrue(Arrays.equals(logFile.getBytes(), TestUtils.inputStreamToBytes(multiPart.getBodyPart(2).getInputStream())));
    });
  }

  @Test
  public void testHTMLAndTextWithAttachments(TestContext testContext) {
    this.testContext = testContext;
    Buffer image = vertx.fileSystem().readFileBlocking("logo-white-big.png");
    Buffer logFile = vertx.fileSystem().readFileBlocking("log4j.properties");
    String text = "This is a message with 2 attachments, one is specified by data, and the other is specified by stream";
    String html = "Here is the html email, Take a look at the Logo: <img src=\"cid:logo@example.com\" /> </br> " +
      "Click <a href=\"http://vertx.io\">Vert Home</a> to visit.";
    MailMessage message = exampleMessage()
      .setAttachment(mailAttachments("attachment", image))
      .setText(text)
      .setHtml(html);
    testSuccess(mailClientLogin(), message, () -> {
      final MimeMultipart multiPart = (MimeMultipart)wiser.getMessages().get(0).getMimeMessage().getContent();
      // 1: multipart which contains one text and html as alternative.
      //    1.1: text body
      //    1.2: html body
      // 2: the logo image attachment
      // 3: the log4j properties attachment
      testContext.assertEquals(3, multiPart.getCount());
      MimeMultipart alternative = (MimeMultipart)multiPart.getBodyPart(0).getContent();
      testContext.assertEquals(2, alternative.getCount());
      testContext.assertEquals(text, TestUtils.conv2nl(TestUtils.inputStreamToString(alternative.getBodyPart(0).getInputStream())));
      testContext.assertEquals(html, TestUtils.conv2nl(TestUtils.inputStreamToString(alternative.getBodyPart(1).getInputStream())));

      testContext.assertTrue(Arrays.equals(image.getBytes(), TestUtils.inputStreamToBytes(multiPart.getBodyPart(1).getInputStream())));
      testContext.assertTrue(Arrays.equals(logFile.getBytes(), TestUtils.inputStreamToBytes(multiPart.getBodyPart(2).getInputStream())));
    });
  }

  @Test
  public void testHTMLAndTextWithInlineAttachments(TestContext testContext) {
    this.testContext = testContext;
    Buffer image = vertx.fileSystem().readFileBlocking("logo-white-big.png");
    Buffer logFile = vertx.fileSystem().readFileBlocking("log4j.properties");
    String text = "This is a message with 2 attachments, one is specified by data, and the other is specified by stream";
    String html = "Here is the html email, Take a look at the Logo: <img src=\"cid:logo@example.com\" /> </br> " +
      "Click <a href=\"http://vertx.io\">Vert Home</a> to visit.";
    MailMessage message = exampleMessage()
      .setInlineAttachment(mailAttachments("inline", image))
      .setText(text)
      .setHtml(html);
    testSuccess(mailClientLogin(), message, () -> {
      final MimeMultipart multiPart = (MimeMultipart)wiser.getMessages().get(0).getMimeMessage().getContent();
      // 1: multipart which contains one text and html as alternative.
      //    1.1: text body
      //    1.2: html part as alternative multipart
      //      1.2.1: the html body
      //      1.2.1: log image inline attachment
      //      1.2.2: the log4j properties attachment
      testContext.assertEquals(2, multiPart.getCount());
      testContext.assertEquals(text, TestUtils.conv2nl(TestUtils.inputStreamToString(multiPart.getBodyPart(0).getInputStream())));

      MimeMultipart htmlPart = (MimeMultipart)multiPart.getBodyPart(1).getContent();
      testContext.assertEquals(3, htmlPart.getCount());
      testContext.assertEquals(html, TestUtils.conv2nl(TestUtils.inputStreamToString(htmlPart.getBodyPart(0).getInputStream())));
      testContext.assertTrue(Arrays.equals(image.getBytes(), TestUtils.inputStreamToBytes(htmlPart.getBodyPart(1).getInputStream())));
      testContext.assertTrue(Arrays.equals(logFile.getBytes(), TestUtils.inputStreamToBytes(htmlPart.getBodyPart(2).getInputStream())));
    });
  }

  @Test
  public void testHTMLAndTextWithAttachmentsAndInlineAttachments(TestContext testContext) {
    this.testContext = testContext;
    Buffer image = vertx.fileSystem().readFileBlocking("logo-white-big.png");
    Buffer logFile = vertx.fileSystem().readFileBlocking("log4j.properties");
    String text = "This is a message with 2 attachments, one is specified by data, and the other is specified by stream";
    String html = "Here is the html email, Take a look at the Logo: <img src=\"cid:logo@example.com\" /> </br> " +
      "Click <a href=\"http://vertx.io\">Vert Home</a> to visit.";
    MailMessage message = exampleMessage()
      .setInlineAttachment(mailAttachments("inline", image))
      .setAttachment(mailAttachments("attachment", image))
      .setText(text)
      .setHtml(html);
    testSuccess(mailClientLogin(), message, () -> {
      final MimeMultipart multiPart = (MimeMultipart)wiser.getMessages().get(0).getMimeMessage().getContent();
      // 1: multipart which contains one text and html as alternative.
      //    1.1: an alternative multipart
      //      1.1.1: text body
      //      1.1.2: html part as multipart
      //         1.2.1: the html body
      //         1.2.1: log image inline attachment
      //         1.2.2: the log4j properties attachment
      // 2: the logo image attachment
      // 3: the log4j properties attachment
      testContext.assertEquals(3, multiPart.getCount());

      MimeMultipart alternative = (MimeMultipart)multiPart.getBodyPart(0).getContent();
      testContext.assertEquals(2, alternative.getCount());
      testContext.assertEquals(text, TestUtils.conv2nl(TestUtils.inputStreamToString(alternative.getBodyPart(0).getInputStream())));

      MimeMultipart htmlPart = (MimeMultipart)alternative.getBodyPart(1).getContent();
      testContext.assertEquals(3, htmlPart.getCount());
      testContext.assertEquals(html, TestUtils.conv2nl(TestUtils.inputStreamToString(htmlPart.getBodyPart(0).getInputStream())));
      testContext.assertTrue(Arrays.equals(image.getBytes(), TestUtils.inputStreamToBytes(htmlPart.getBodyPart(1).getInputStream())));
      testContext.assertTrue(Arrays.equals(logFile.getBytes(), TestUtils.inputStreamToBytes(htmlPart.getBodyPart(2).getInputStream())));

      testContext.assertTrue(Arrays.equals(image.getBytes(), TestUtils.inputStreamToBytes(multiPart.getBodyPart(1).getInputStream())));
      testContext.assertTrue(Arrays.equals(logFile.getBytes(), TestUtils.inputStreamToBytes(multiPart.getBodyPart(2).getInputStream())));
    });
  }

}

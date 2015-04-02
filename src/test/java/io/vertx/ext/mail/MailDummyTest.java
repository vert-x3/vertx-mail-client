package io.vertx.ext.mail;

import io.vertx.core.buffer.Buffer;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/*
 first implementation of a SMTP client
 */
/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
public class MailDummyTest extends SMTPTestDummy {

  @Test
  public void mailTest() {
    testSuccess();
  }

  @Test
  public void mailHtml() throws UnsupportedEncodingException {
    Buffer image = vertx.fileSystem().readFileBlocking("logo-white-big.png");

    MailMessage email = new MailMessage()
      .setFrom("user@example.com")
      .setTo("user@example.com")
      .setBounceAddress("bounce@example.com")
      .setSubject("Test email with HTML")
      .setText("this is a message")
      .setHtml("<a href=\"http://vertx.io\">vertx.io</a>");

    List<MailAttachment> list = new ArrayList<MailAttachment>();

    list.add(new MailAttachment()
      .setData(Buffer.buffer(image.getBytes()))
      .setName("logo-white-big.png")
      .setContentType("image/png")
      .setDisposition("inline")
      .setDescription("logo of vert.x web page"));

    list.add(new MailAttachment()
      .setData(Buffer.buffer("this is a text attachment"))
      .setName("file.txt")
      .setContentType("text/plain")
      .setDisposition("attachment")
      .setDescription("some text"));

    email.setAttachment(list);

    testSuccess(email);
  }

  @Test
  public void mailTestNoBody() {
    MailMessage email = new MailMessage()
      .setFrom("user@example.com")
      .setTo("user@example.com")
      .setAttachment(new MailAttachment()
      .setData(Buffer.buffer("\u00ff\u00ff\u00ff\u00ff\u00ff\u00ff")));

    testSuccess(email);
  }

}

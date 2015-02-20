package io.vertx.ext.mail;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.test.core.VertxTestBase;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/*
 first implementation of a SMTP client
 */
/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
public class MailDummyTest extends VertxTestBase {

  private static final Logger log = LoggerFactory.getLogger(MailDummyTest.class);

  @Test
  public void mailTest() {
    log.info("starting");

    MailConfig mailConfig = new MailConfig("localhost", 1587);

    MailService mailService = MailService.create(vertx, mailConfig);

    MailMessage email=new MailMessage()
      .setFrom("user@example.com")
      .setBounceAddress("bounce@example.com")
      .setTo("user@example.com")
      .setSubject("Test email with HTML")
      .setText("this is a message");

    mailService.sendMail(email, result -> {
      log.info("mail finished");
      if (result.succeeded()) {
        log.info(result.result().toString());
        testComplete();
      } else {
        log.warn("got exception", result.cause());
        throw new RuntimeException(result.cause());
      }
    });

    await();
  }

  @Test
  public void mailHtml() throws UnsupportedEncodingException {
    log.info("starting");

    MailConfig mailConfig = new MailConfig("localhost", 1587);

    MailService mailService = MailService.create(vertx, mailConfig);

    Buffer image=vertx.fileSystem().readFileBlocking("logo-white-big.png");

    MailMessage email = new MailMessage()
      .setFrom("user@example.com")
      .setTo("user@example.com")
      .setBounceAddress("bounce@example.com")
      .setSubject("Test email with HTML")
      .setText("this is a message")
      .setHtml("<a href=\"http://vertx.io\">vertx.io</a>");

    List<MailAttachment> list=new ArrayList<MailAttachment>();

    list.add(new MailAttachment()
      .setData(new String(image.getBytes(), "ISO-8859-1"))
      .setName("logo-white-big.png")
      .setContentType("image/png")
      .setDisposition("inline")
      .setDescription("logo of vert.x web page"));

    list.add(new MailAttachment()
      .setData("this is a text attachment")
      .setName("file.txt")
      .setContentType("text/plain")
      .setDisposition("attachment")
      .setDescription("some text"));

    email.setAttachment(list);

    mailService.sendMail(email, result -> {
      log.info("mail finished");
      if (result.succeeded()) {
        log.info(result.result().toString());
        testComplete();
      } else {
        log.warn("got exception", result.cause());
        throw new RuntimeException(result.cause());
      }
    });

    await();
  }

  private TestSmtpServer smtpServer;

  @Before
  public void startSMTP() {
    smtpServer = new TestSmtpServer(vertx);
  }

  @After
  public void stopSMTP() {
    smtpServer.stop();
  }

}

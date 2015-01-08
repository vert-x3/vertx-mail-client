package io.vertx.ext.mail;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.test.core.VertxTestBase;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/*
 first implementation of a SMTP client
 */
/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
public class MailDummyTest extends VertxTestBase {

  Vertx vertx = Vertx.vertx();
  private static final Logger log = LoggerFactory.getLogger(MailDummyTest.class);

  CountDownLatch latch;

  @Ignore
  @Test
  public void mailTest() throws InterruptedException {
    log.info("starting");

    latch = new CountDownLatch(1);

    MailConfig mailConfig = new MailConfig("localhost", 1587);

    MailService mailService = MailService.create(vertx, mailConfig);

    JsonObject email = new JsonObject();
    email.put("from", "lehmann333@arcor.de");
    email.put("recipient", "lehmann333@arcor.de");
    email.put("bounceAddress", "nobody@lehmann.cx");
    email.put("subject", "Test email with HTML");
    email.put("text", "this is a message");

    mailService.sendMail(email, result -> {
      log.info("mail finished");
      if (result.succeeded()) {
        log.info(result.result().toString());
        latch.countDown();
      } else {
        log.warn("got exception", result.cause());
        throw new RuntimeException(result.cause());
      }
    });

    awaitLatch(latch);
  }

  @Test
  public void mailHtml() throws InterruptedException, UnsupportedEncodingException {
    log.info("starting");

    latch = new CountDownLatch(1);

    MailConfig mailConfig = new MailConfig("localhost", 1587);

    MailService mailService = MailService.create(vertx, mailConfig);

    Buffer image=vertx.fileSystem().readFileBlocking("logo-white-big.png");

    JsonObject email = new JsonObject()
      .put("from", "lehmann333@arcor.de")
      .put("recipient", "lehmann333@arcor.de")
      .put("bounceAddress", "nobody@lehmann.cx")
      .put("subject", "Test email with HTML")
      .put("text", "this is a message")
      .put("html", "<a href=\"http://vertx.io\">vertx.io</a>");

    JsonObject attachment=new JsonObject()
      .put("data", image.getBytes())
      .put("name", "logo-white-big.png")
      .put("content-type", "image/png")
      .put("disposition", "inline")
      .put("description", "logo of vert.x web page");

//    JsonObject attachment=new JsonObject()
//      .put("data", "this is a text attachment".getBytes("utf-8"))
//      .put("name", "file.txt")
//      .put("content-type", "text/plain")
//      .put("description", "some text");

    email.put("attachment", attachment);

    mailService.sendMail(email, result -> {
      log.info("mail finished");
      if (result.succeeded()) {
        log.info(result.result().toString());
        latch.countDown();
      } else {
        log.warn("got exception", result.cause());
        throw new RuntimeException(result.cause());
      }
    });

    awaitLatch(latch);
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

package io.vertx.ext.mail;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;

import java.util.concurrent.CountDownLatch;

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
public class MailDummyTest {

  Vertx vertx = Vertx.vertx();
  private static final Logger log = LoggerFactory.getLogger(MailDummyTest.class);

  CountDownLatch latch;

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
      } else {
        log.warn("got exception", result.cause());
      }
      latch.countDown();
    });

    latch.await();
  }

  TestSmtpServer smtpServer;

  @Before
  public void startSMTP() {
    smtpServer=new TestSmtpServer(vertx);
  }

  @After
  public void stopSMTP() {
    smtpServer.stop();
  }

}

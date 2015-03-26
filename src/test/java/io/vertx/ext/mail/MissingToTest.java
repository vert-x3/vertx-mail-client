package io.vertx.ext.mail;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.test.core.VertxTestBase;

import org.junit.Test;

/*
 first implementation of a SMTP client
 */
/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
public class MissingToTest extends VertxTestBase {

  private static final Logger log = LoggerFactory.getLogger(MissingToTest.class);

  @Test
  public void mailMissingToTest() {
    log.info("starting");

    MailConfig mailConfig = new MailConfig("localhost", 1587);

    MailService mailService = MailService.create(vertx, mailConfig);

    MailMessage email=new MailMessage()
    .setFrom("user@example.com");

    mailService.sendMail(email, result -> {
      log.info("mail finished");
      if(result.succeeded()) {
        log.info(result.result().toString());
        fail("this test should throw an Exception");
      } else {
        log.warn("got exception", result.cause());
        testComplete();
      }
    });

    await();
  }

  @Test
  public void mailMissingFromTest() {
    log.info("starting");

    MailConfig mailConfig = new MailConfig("localhost", 1587);

    MailService mailService = MailService.create(vertx, mailConfig);

    MailMessage email=new MailMessage()
      .setTo("user@example.com");

    mailService.sendMail(email, result -> {
      log.info("mail finished");
      if(result.succeeded()) {
        log.info(result.result().toString());
        fail("this test should throw an Exception");
      } else {
        log.warn("got exception", result.cause());
        testComplete();
      }
    });

    await();
  }

}

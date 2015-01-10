package io.vertx.ext.mail;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.test.core.VertxTestBase;

import java.util.concurrent.CountDownLatch;

import org.junit.Ignore;
import org.junit.Test;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 * this test uses google but connects without tls
 */
public class MissingAuthTest extends VertxTestBase {

  Vertx vertx = Vertx.vertx();
  private static final Logger log = LoggerFactory.getLogger(MissingAuthTest.class);

  CountDownLatch latch;

  @Ignore
  @Test
  public void mailTest() throws InterruptedException {
    log.info("starting");

    latch = new CountDownLatch(1);

    MailConfig mailConfig = ServerConfigs.configGoogle();
    mailConfig.setStarttls(StarttlsOption.DISABLED);
    mailConfig.setUsername("xxx");
    mailConfig.setPassword("xxx");

    MailService mailService = MailService.create(vertx, mailConfig);

    MailMessage email = new MailMessage("lehmann333@arcor.de", "lehmann333@arcor.de", "Subject", "Message");

    mailService.sendMail(email, result -> {
      log.info("mail finished");
      if(result.succeeded()) {
        log.info(result.result().toString());
        fail("this test should throw an Exception");
      } else {
        log.warn("got exception", result.cause());
        latch.countDown();
      }
    });

    awaitLatch(latch);
  }
}

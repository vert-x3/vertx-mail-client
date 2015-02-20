package io.vertx.ext.mail;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.test.core.VertxTestBase;

import org.junit.Test;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 * this test uses google but connects without tls
 * the EHLO will not contain AUTH in this case
 */
public class MissingAuthTest extends VertxTestBase {

  private static final Logger log = LoggerFactory.getLogger(MissingAuthTest.class);

  @Test
  public void mailTest() {
    MailConfig mailConfig = new MailConfig("smtp.googlemail.com", 587, StarttlsOption.DISABLED, LoginOption.REQUIRED);
    mailConfig.setUsername("xxx")
      .setPassword("xxx");

    MailService mailService = MailService.create(vertx, mailConfig);

    MailMessage email = new MailMessage("user@example.com", "user@example.com", "Subject", "Message");

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

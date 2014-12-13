package io.vertx.ext.mail;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;

import java.util.concurrent.CountDownLatch;

import org.junit.Test;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 * this test uses google but connects without tls
 */
public class MissingAuthTest {

  Vertx vertx = Vertx.vertx();
  private static final Logger log = LoggerFactory.getLogger(MissingAuthTest.class);

  CountDownLatch latch;

  @Test
  public void mailTest() {
    log.info("starting");

    latch = new CountDownLatch(1);

    try {
      MailConfig mailConfig = ServerConfigs.configGoogle();
      mailConfig.setStarttls(StarttlsOption.DISABLED);
      mailConfig.setUsername("xxx");
      mailConfig.setPassword("xxx");

      MailService mailService = MailService.create(vertx, mailConfig);

      JsonObject email = new JsonObject();
      email.put("from", "lehmann333@arcor.de");
      email.put("recipient", "lehmann333@arcor.de");
      email.put("subject", "Test email with HTML");
      email.put("text", "this is a test email");

      mailService.sendMail(email, v -> {
        log.info("mail finished");
        if(v!=null) {
          if(v.succeeded()) {
            log.info(v.result().toString());
          } else {
            log.warn("got exception", v.cause());
          }
        }
        latch.countDown();
      });

      latch.await();
    } catch (InterruptedException ioe) {
      log.error("IOException", ioe);
    }
  }
}

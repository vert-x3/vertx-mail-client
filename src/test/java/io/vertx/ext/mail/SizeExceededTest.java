package io.vertx.ext.mail;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;

import java.util.concurrent.CountDownLatch;

import org.junit.Ignore;
import org.junit.Test;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 * this test uses google but connects without tls
 */
public class SizeExceededTest {

  Vertx vertx = Vertx.vertx();
  private static final Logger log = LoggerFactory.getLogger(SizeExceededTest.class);

  CountDownLatch latch;

  @Ignore
  @Test
  public void mailTest() throws InterruptedException {
    log.info("starting");

    latch = new CountDownLatch(1);

    MailConfig mailConfig = new MailConfig("mail.arcor.de", 587, StarttlsOption.REQUIRED, LoginOption.DISABLED);
    mailConfig.setLogin(LoginOption.DISABLED);

    MailService mailService = MailService.create(vertx, mailConfig);

    JsonObject email = new JsonObject();
    email.put("from", "lehmann333@arcor.de");
    email.put("recipient", "lehmann333@arcor.de");
    email.put("subject", "Test email with HTML");

    // message to exceed SIZE limit (48000000 for our server)
    // 46 Bytes
    StringBuilder sb=new StringBuilder("*********************************************\n");
    // multiply by 2**20
    for(int i=0;i<20;i++) {
      sb.append(sb);
    }
    String message=sb.toString();

    email.put("text", message);

    mailService.sendMail(email, result -> {
      log.info("mail finished");
      if(result.succeeded()) {
        log.info(result.result().toString());
      } else {
        log.warn("got exception", result.cause());
      }
      latch.countDown();
    });

    latch.await();
  }
}

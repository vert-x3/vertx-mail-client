package io.vertx.ext.mail;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.test.core.VertxTestBase;

import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 * this test uses a message that exceeds the SIZE limit of the smtp server
 * (uses the mockup server that just plays a file)
 */
public class SizeExceededTest extends VertxTestBase {

  Vertx vertx = Vertx.vertx();
  private static final Logger log = LoggerFactory.getLogger(SizeExceededTest.class);

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
    email.put("subject", "Test email with HTML");

    // message to exceed SIZE limit (1000000 for our server)
    // 32 Bytes
    StringBuilder sb=new StringBuilder("*******************************\n");
    // multiply by 2**15
    for(int i=0;i<15;i++) {
      sb.append(sb);
    }
    String message=sb.toString();

    log.info("message size is "+message.length());

    email.put("text", message);

    mailService.sendMail(email, result -> {
      log.info("mail finished");
      if(result.succeeded()) {
        log.info(result.result().toString());
        fail("this test should throw an Exception");
      } else {
        log.info("got exception", result.cause());
        latch.countDown();
      }
    });

    awaitLatch(latch);
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

package io.vertx.ext.mail;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.test.core.VertxTestBase;

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
public class MailEBTest extends VertxTestBase {

  private static final Logger log = LoggerFactory.getLogger(MailEBTest.class);

  @Test
  public void mailTest() throws InterruptedException {
    log.info("starting");

    MailService mailService = MailService.createEventBusProxy(vertx, "vertx.mail");

    MailMessage email=new MailMessage()
    .setFrom("lehmann333@arcor.de")
    .setBounceAddress("nobody@lehmann.cx")
    .setTo("lehmann333@arcor.de")
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

  private TestSmtpServer smtpServer;

  @Before
  public void startSMTP() {
    smtpServer = new TestSmtpServer(vertx);
    CountDownLatch latch = new CountDownLatch(1);
    JsonObject config = new JsonObject("{\"config\":{\"address\":\"vertx.mail\",\"hostname\":\"localhost\",\"port\":1587}}");
    DeploymentOptions deploymentOptions = new DeploymentOptions(config);
    vertx.deployVerticle("io.vertx.ext.mail.MailServiceVerticle", deploymentOptions ,r -> {
      if(r.succeeded()) {
        log.info(r.result());
      } else {
        log.info("exception", r.cause());
      }
      latch.countDown();
    });
    try {
      latch.await();
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @After
  public void stopSMTP() {
    smtpServer.stop();
  }

}

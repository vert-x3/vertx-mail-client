package io.vertx.ext.mail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;

import java.util.concurrent.CountDownLatch;

import javax.mail.MessagingException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.subethamail.wiser.Wiser;
import org.subethamail.wiser.WiserMessage;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 * this test uses a local smtp server mockup
 */
public class MailLocalTest {

  Vertx vertx = Vertx.vertx();
  Logger log = LoggerFactory.getLogger(this.getClass());

  CountDownLatch latch;

  @Test
  public void mailTest() {
    log.info("starting");

    latch = new CountDownLatch(1);

    try {
      JsonObject mailConfig = new JsonObject();

      mailConfig.put("host", "localhost");
      mailConfig.put("port", 1587);
      mailConfig.put("username", "username");
      mailConfig.put("password", "asdf");

      MailService mailService = MailService.create(vertx, mailConfig);

      JsonObject email = new JsonObject();
      email.put("from", "lehmann333@arcor.de");
      email.put("recipient", "lehmann333@arcor.de");
      email.put("bounceAddress", "user@example.com");
      email.put("subject", "Test email with HTML");
      email.put("text", "this is a test email");

      mailService.sendMail(email, v -> {
        log.info("mail finished");
        latch.countDown();
      });

      latch.await();
      final WiserMessage message = wiser.getMessages().get(0);
      String sender = message.getEnvelopeSender();
      String messageText = message.getMimeMessage().getContentType();
      assertEquals("user@example.com", sender);
      assertTrue(messageText, messageText.contains("text/plain"));
    } catch (InterruptedException | MessagingException ioe) {
      log.error("IOException", ioe);
    }
  }

  Wiser wiser;

  @Before
  public void startSMTP() throws Exception {
    wiser = new Wiser();
    wiser.setPort(1587);
    wiser.start();
  }

  @After
  public void stopSMTP() {
    if (wiser != null) {
      wiser.stop();
    }
  }
}

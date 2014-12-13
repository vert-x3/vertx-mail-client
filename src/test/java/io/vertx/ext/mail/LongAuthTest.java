package io.vertx.ext.mail;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;

import java.util.concurrent.CountDownLatch;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.subethamail.wiser.Wiser;
import org.subethamail.wiser.WiserMessage;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 * this test uses a local smtp server mockup
 * 
 * this tests uses more than 57 bytes as auth plain string which would break
 * the authentication if the base64 were chunked
 */
public class LongAuthTest {

  Vertx vertx = Vertx.vertx();
  private static final Logger log = LoggerFactory.getLogger(LongAuthTest.class);

  CountDownLatch latch;

  @Test
  public void mailTest() {
    log.info("starting");

    latch = new CountDownLatch(1);

    try {
      MailConfig mailConfig = new MailConfig("localhost", 1587, StarttlsOption.DISABLED, LoginOption.REQUIRED);

      mailConfig.setUsername("*************************************************");
      mailConfig.setPassword("*************************************************");

      MailService mailService = MailService.create(vertx, mailConfig);

      JsonObject email = new JsonObject();
      email.put("from", "lehmann333@arcor.de");
      email.put("recipient", "lehmann333@arcor.de");
      email.put("bounceAddress", "user@example.com");
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

      final WiserMessage message = wiser.getMessages().get(0);
      String sender = message.getEnvelopeSender();
      final MimeMessage mimeMessage = message.getMimeMessage();
      assertEquals("user@example.com", sender);
      assertThat(mimeMessage.getContentType(), containsString("text/plain"));
      assertThat(mimeMessage.getSubject() , equalTo("Test email with HTML"));
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

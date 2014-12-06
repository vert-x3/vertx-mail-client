package io.vertx.ext.mail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.mail.mailutil.MyHtmlEmail;

import java.util.concurrent.CountDownLatch;

import javax.mail.MessagingException;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
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
      sendMail("username", "password");
      latch.await();
      final WiserMessage message = wiser.getMessages().get(0);
      String sender = message.getEnvelopeSender();
      String messageText = message.getMimeMessage().getContentType();
      assertEquals("user@example.com", sender);
      assertTrue(messageText, messageText.contains("multipart/mixed"));
    } catch (InterruptedException | MessagingException ioe) {
      log.error("IOException", ioe);
    }
  }

  private void sendMail(String username, String pw) {
    try {
      HtmlEmail email = new MyHtmlEmail(); // this provides the missing getter for bounceAddress

      email.setCharset("UTF-8");

      email.setHostName("localhost");
      email.setSmtpPort(1587);
      email.setStartTLSRequired(false);

      // Create the email message
      email.addTo("lehmann333@arcor.de", "User");
      email.setFrom("lehmann333@arcor.de", "Sender");
      email.setBounceAddress("user@example.com");
      email.setSubject("Test email with HTML");

      // set the html message
      email.setHtmlMsg("<html>Hello</html>");

      // set the alternative message
      email.setTextMsg("Hello text");

      // OK, its not really a verticle, we will get that right later ...
      MailVerticle mailVerticle = new MailVerticle(vertx, v -> {
        log.info("mail finished");
        latch.countDown();
      });
      mailVerticle.sendMail(email, username, pw);

    } catch (EmailException e) {
      log.error("Exception", e);
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

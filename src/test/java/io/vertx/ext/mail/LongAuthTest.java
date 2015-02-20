package io.vertx.ext.mail;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.test.core.VertxTestBase;

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
 *         this test uses a local smtp server mockup
 * 
 *         this tests uses more than 57 bytes as auth plain string which would
 *         break the authentication if the base64 were chunked
 */
public class LongAuthTest extends VertxTestBase {

  private static final Logger log = LoggerFactory.getLogger(LongAuthTest.class);

  @Test
  public void mailTest() throws MessagingException {
    MailConfig mailConfig = new MailConfig("localhost", 1587, StarttlsOption.DISABLED, LoginOption.REQUIRED);

    mailConfig.setUsername("*************************************************");
    mailConfig.setPassword("*************************************************");

    MailService mailService = MailService.create(vertx, mailConfig);

    MailMessage email = new MailMessage("user@example.com", "user@example.com", "Subject", "Message");

    mailService.sendMail(email, result -> {
      log.info("mail finished");
      if (result.succeeded()) {
        log.info(result.result().toString());
        assertEquals("success", result.result().getValue("result"));
        testComplete();
      } else {
        log.warn("got exception", result.cause());
        throw new RuntimeException(result.cause());
      }
    });

    await();

    final WiserMessage message = wiser.getMessages().get(0);
    String sender = message.getEnvelopeSender();
    final MimeMessage mimeMessage = message.getMimeMessage();
    assertEquals("user@example.com", sender);
    assertThat(mimeMessage.getContentType(), containsString("text/plain"));
    assertThat(mimeMessage.getSubject(), equalTo("Subject"));
  }

  Wiser wiser;

  @Before
  public void startSMTP() {
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

package io.vertx.groovy.ext.mail;

import static org.hamcrest.core.IsEqual.equalTo
import static org.hamcrest.core.StringContains.containsString
import io.vertx.core.logging.Logger
import io.vertx.core.logging.impl.LoggerFactory
import io.vertx.ext.mail.LoginOption
import io.vertx.ext.mail.MailConfig
import io.vertx.ext.mail.MailMessage
import io.vertx.ext.mail.StarttlsOption
import io.vertx.groovy.core.Vertx
import io.vertx.test.core.VertxTestBase

import java.util.concurrent.CountDownLatch

import javax.mail.MessagingException
import javax.mail.internet.MimeMessage

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.subethamail.wiser.Wiser
import org.subethamail.wiser.WiserMessage

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 * this test uses a local smtp server mockup
 */
public class GroovyMailTest extends VertxTestBase {

  private static final Logger log = LoggerFactory.getLogger(GroovyMailTest.class);

  CountDownLatch latch;

  Vertx vertx = Vertx.vertx();

  @Test
  public void mailTest() throws MessagingException, InterruptedException {
    log.info("starting");

    latch = new CountDownLatch(1);

    MailConfig mailConfig = new MailConfig("localhost", 1587, StarttlsOption.DISABLED, LoginOption.REQUIRED);

    mailConfig.username="username";
    mailConfig.password="asdf";

    MailService mailService = MailService.create(vertx, mailConfig.toJson().map);

    MailMessage email = new MailMessage();

    email.from="lehmann333@arcor.de";
    email.to=["lehmann333@arcor.de (User Name)", "user@example.com (Another User)"];
    email.bounceAddress="user@example.com";
    email.subject="Test email";
    email.text="this is a test email";

    mailService.sendMail(email.toJson().map, { result ->
      log.info("mail finished");
      if (result.succeeded()) {
        log.info(result.result());
        latch.countDown();
      } else {
        log.warn("got exception", result.cause());
        throw new RuntimeException(result.cause());
      }
    });

    awaitLatch(latch);

    final WiserMessage message = wiser.messages[0];
    String sender = message.envelopeSender;
    final MimeMessage mimeMessage = message.mimeMessage;
    assertEquals("user@example.com", sender);
    assertThat(mimeMessage.contentType, containsString("text/plain"));
    assertThat(mimeMessage.subject, equalTo("Test email"));
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

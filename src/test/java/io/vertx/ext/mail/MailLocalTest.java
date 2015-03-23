package io.vertx.ext.mail;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.test.core.VertxTestBase;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

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
 */
public class MailLocalTest extends VertxTestBase {

  private static final Logger log = LoggerFactory.getLogger(MailLocalTest.class);

  @Test
  public void mailTest() throws MessagingException, IOException {
    log.info("starting");

    MailConfig mailConfig = new MailConfig("localhost", 1587, StarttlsOption.DISABLED, LoginOption.REQUIRED);

    mailConfig.setUsername("username");
    mailConfig.setPassword("asdf");

    MailService mailService = MailService.create(vertx, mailConfig);

    MailMessage email = new MailMessage();

    email.setFrom("user@example.com (Sender)")
      .setTo(Arrays.asList(
        "user@example.com (User Name)",
        "other@example.com (Another User)"))
      .setBounceAddress("user@example.com (Bounce)")
      .setSubject("Test email")
      .setText("this is a test email");

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

    final WiserMessage message = wiser.getMessages().get(0);
    String sender = message.getEnvelopeSender();
    final MimeMessage mimeMessage = message.getMimeMessage();
    assertEquals("user@example.com", sender);
    assertThat(mimeMessage.getContentType(), containsString("text/plain"));
    assertThat(mimeMessage.getSubject(), equalTo("Test email"));
    assertThat(inputStreamToString(mimeMessage.getInputStream()), equalTo("this is a test email"));
  }

  private String inputStreamToString(InputStream inputStream) throws IOException {
    StringBuilder sb = new StringBuilder();
    int ch;
    while((ch=inputStream.read())!=-1) {
      sb.append((char) ch);
    }
    return sb.toString();
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

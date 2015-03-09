package io.vertx.ext.mail;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.test.core.VertxTestBase;

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
 *         this test uses a local smtp server mockup
 */
public class MailStringTest extends VertxTestBase {

  private static final Logger log = LoggerFactory.getLogger(MailStringTest.class);

  @Test
  public void mailTest() throws MessagingException {
    log.info("starting");

    MailConfig mailConfig = new MailConfig("localhost", 1587, StarttlsOption.DISABLED, LoginOption.DISABLED);

    MailService mailService = MailService.create(vertx, mailConfig);

    MailMessage email = new MailMessage();

    email.setFrom("user@example.com")
      .setTo(Arrays.asList(
        "user@example.com (User Name)",
        "other@example.com (Another User)"))
      .setBounceAddress("user@example.org");

    String messageString = "Message-ID: <12345@example.com>\n" + 
        "Date: Mon, 09 Mar 2015 22:10:48 +0100\n" + 
        "From: User Name <user@example.com>\n" + 
        "MIME-Version: 1.0\n" + 
        "To: User Name <user@example.com>\n" + 
        "Subject: pregenerated message\n" + 
        "Content-Type: text/plain; charset=US-ASCII\n" + 
        "Content-Transfer-Encoding: 7bit\n" + 
        "\n" + 
        "this is an example mail\n" + 
        "\n";

    // note that the to and from fields from the string are not
    // evaluated at all

    mailService.sendMailString(email, messageString, result -> {
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
    assertEquals("user@example.org", sender);
    assertThat(mimeMessage.getContentType(), containsString("text/plain"));
    assertThat(mimeMessage.getSubject(), equalTo("pregenerated message"));
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

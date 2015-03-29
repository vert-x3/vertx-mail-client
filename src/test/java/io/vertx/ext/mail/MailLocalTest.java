package io.vertx.ext.mail;

import static org.hamcrest.core.StringContains.containsString;

import java.io.IOException;
import java.util.Arrays;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.junit.Test;
import org.subethamail.wiser.WiserMessage;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 * this test uses a local smtp server mockup
 */
public class MailLocalTest extends SMTPTestWiser {

  @Test
  public void mailTest() throws MessagingException, IOException {

    MailService mailService = mailServiceLogin();

    MailMessage email = new MailMessage();

    email.setFrom("user@example.com (Sender)")
      .setTo(Arrays.asList(
        "user@example.com (User Name)",
        "other@example.com (Another User)"))
      .setBounceAddress("user@example.com (Bounce)")
      .setSubject("Test email")
      .setText("this is a test email");

    testSuccess(mailService, email);

    final WiserMessage message = wiser.getMessages().get(0);
    assertEquals("user@example.com", message.getEnvelopeSender());
    final MimeMessage mimeMessage = message.getMimeMessage();
    assertThat(mimeMessage.getContentType(), containsString("text/plain"));
    assertEquals("Test email", mimeMessage.getSubject());
    assertEquals("this is a test email", inputStreamToString(mimeMessage.getInputStream()));
  }

}

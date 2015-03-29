package io.vertx.ext.mail;

import static org.hamcrest.core.StringContains.containsString;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.junit.Test;
import org.subethamail.wiser.WiserMessage;

/**
 * this tests uses more than 57 bytes as AUTH PLAIN string which would break the
 * authentication if the base64 data were chunked
 * 
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
public class LongAuthTest extends SMTPTestWiser {

  @Test
  public void mailTest() throws MessagingException {
    testSuccess(mailServiceLogin("*************************************************",
        "*************************************************"));

    final WiserMessage message = wiser.getMessages().get(0);
    assertEquals("from@example.com", message.getEnvelopeSender());
    final MimeMessage mimeMessage = message.getMimeMessage();
    assertThat(mimeMessage.getContentType(), containsString("text/plain"));
    assertEquals("Subject", mimeMessage.getSubject());
  }

}

package io.vertx.ext.mail;

import org.junit.Test;

import javax.mail.MessagingException;

/**
 * this tests uses more than 57 bytes as AUTH PLAIN string which would break the
 * authentication if the base64 data were chunked
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
public class LongAuthTest extends SMTPTestWiser {

  @Test
  public void mailTest() throws MessagingException {
    testSuccess(
      mailClientLogin("*************************************************",
        "*************************************************"), exampleMessage(), assertExampleMessage());
  }

}

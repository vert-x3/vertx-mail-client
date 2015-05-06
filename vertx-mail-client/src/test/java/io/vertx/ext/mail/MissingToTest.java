package io.vertx.ext.mail;

import org.junit.Test;

/**
 * test sending message with either missing from or to
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
public class MissingToTest extends SMTPTestDummy {

  @Test
  public void mailMissingToTest() {
    testException(new MailMessage().setFrom("user@example.com"));
  }

  @Test
  public void mailMissingFromTest() {
    testException(new MailMessage().setTo("user@example.com"));
  }

  @Test
  public void mailBounceAddrOnlyTest() {
    testSuccess(new MailMessage().setBounceAddress("from@example.com").setBcc("user@example.com"));
  }

}

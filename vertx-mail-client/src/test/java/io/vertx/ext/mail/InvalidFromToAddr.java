package io.vertx.ext.mail;

import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * test sending message with invalid from or to
 *
 * the exception wasn't reported when the message was created inside the checkSize method before the
 * mailFromCmd/rcptToCmd methods. This issue was fixed by pr#44 already.
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
@RunWith(VertxUnitRunner.class)
public class InvalidFromToAddr extends SMTPTestDummy {

  @Test
  public void mailInvalidToTest(TestContext testContext) {
    this.testContext = testContext;
    testException(new MailMessage().setFrom("user@example.com").setTo("user @example.com"));
  }

  @Test
  public void mailInvalidFromTest(TestContext testContext) {
    this.testContext = testContext;
    testException(new MailMessage().setFrom("user @example.com").setTo("user@example.com"));
  }

}

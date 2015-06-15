package io.vertx.ext.mail;

import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * test sending message with either missing from or to
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
@RunWith(VertxUnitRunner.class)
public class MissingToTest extends SMTPTestDummy {

  @Test
  public void mailMissingToTest(TestContext testContext) {
    this.testContext=testContext;
    testException(new MailMessage().setFrom("user@example.com"));
  }

  @Test
  public void mailMissingFromTest(TestContext testContext) {
    this.testContext=testContext;
    testException(new MailMessage().setTo("user@example.com"));
  }

  @Test
  public void mailBounceAddrOnlyTest(TestContext testContext) {
    this.testContext=testContext;
    testSuccess(new MailMessage().setBounceAddress("from@example.com").setBcc("user@example.com"));
  }

}

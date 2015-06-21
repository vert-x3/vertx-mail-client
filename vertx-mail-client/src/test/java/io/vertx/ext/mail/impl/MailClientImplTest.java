/**
 *
 */
package io.vertx.ext.mail.impl;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.test.core.VertxTestBase;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
@RunWith(VertxUnitRunner.class)
public class MailClientImplTest extends VertxTestBase {

  private static final Logger log = LoggerFactory.getLogger(MailClientImplTest.class);

  /**
   * Test method for
   * {@link MailClientImpl#MailClientImpl(io.vertx.core.Vertx, io.vertx.ext.mail.MailConfig)}
   * .
   */
  @Test
  public final void testMailClientImpl(TestContext testContext) {
    MailClient mailClient = new MailClientImpl(vertx, new MailConfig(), true);
    testContext.assertNotNull(mailClient);
  }

  /**
   * Test method for {@link MailClientImpl#close()}.
   */
  @Test
  public final void testClose(TestContext testContext) {
    MailClient mailClient = new MailClientImpl(vertx, new MailConfig(), true);
    mailClient.close();
  }

  @Test(expected=IllegalStateException.class)
  public final void test2xClose(TestContext testContext) {
    MailClient mailClient = new MailClientImpl(vertx, new MailConfig(), true);
    mailClient.close();
    mailClient.close();
  }

  @Test
  public final void testClosedSend(TestContext testContext) {
    Async async = testContext.async();
    MailClient mailClient = new MailClientImpl(vertx, new MailConfig(), true);
    mailClient.close();
    mailClient.sendMail(new MailMessage(), result -> {
      if (result.succeeded()) {
        log.info(result.result().toString());
        testContext.fail("this test should throw an Exception");
      } else {
        log.warn("got exception", result.cause());
        async.complete();
      }
    });
  }

}

/**
 *
 */
package io.vertx.ext.mail.impl;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.test.core.VertxTestBase;
import org.junit.Test;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
public class MailClientImplTest extends VertxTestBase {

  private static final Logger log = LoggerFactory.getLogger(MailClientImplTest.class);

  /**
   * Test method for
   * {@link MailClientImpl#MailClientImpl(io.vertx.core.Vertx, io.vertx.ext.mail.MailConfig)}
   * .
   */
  @Test
  public final void testMailClientImpl() {
    MailClient mailClient = new MailClientImpl(vertx, new MailConfig());
    assertNotNull(mailClient);
  }

  /**
   * Test method for {@link MailClientImpl#close()}.
   */
  @Test
  public final void testClose() {
    MailClient mailClient = new MailClientImpl(vertx, new MailConfig());
    mailClient.close();
  }

  @Test(expected=IllegalStateException.class)
  public final void test2xClose() {
    MailClient mailClient = new MailClientImpl(vertx, new MailConfig());
    mailClient.close();
    mailClient.close();
  }

  @Test
  public final void testClosedSend() {
    MailClient mailClient = new MailClientImpl(vertx, new MailConfig());
    mailClient.close();
    mailClient.sendMail(new MailMessage(), result -> {
      if (result.succeeded()) {
        log.info(result.result().toString());
        fail("this test should throw an Exception");
      } else {
        log.warn("got exception", result.cause());
        testComplete();
      }
    });

    await();
  }

}

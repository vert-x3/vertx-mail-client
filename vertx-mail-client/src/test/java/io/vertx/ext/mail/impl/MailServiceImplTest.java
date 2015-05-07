/**
 *
 */
package io.vertx.ext.mail.impl;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.test.core.VertxTestBase;
import org.junit.Test;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
public class MailServiceImplTest extends VertxTestBase {

  private static final Logger log = LoggerFactory.getLogger(MailServiceImplTest.class);

  /**
   * Test method for
   * {@link MailClientImpl#MailClientImpl(io.vertx.core.Vertx, io.vertx.ext.mail.MailConfig)}
   * .
   */
  @Test
  public final void testMailServiceImpl() {
    MailClient mailService = new MailClientImpl(vertx, new MailConfig());
    assertNotNull(mailService);
  }

  /**
   * Test method for {@link MailClientImpl#close()}.
   */
  @Test
  public final void testClose() {
    MailClient mailService = new MailClientImpl(vertx, new MailConfig());
    mailService.close();
  }

  @Test
  public final void test2xClose() {
    MailClient mailService = new MailClientImpl(vertx, new MailConfig());
    mailService.close();
    mailService.close();
  }

  @Test
  public final void testClosedSend() {
    MailClient mailService = new MailClientImpl(vertx, new MailConfig());
    mailService.close();
    mailService.sendMail(new MailMessage(), result -> {
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

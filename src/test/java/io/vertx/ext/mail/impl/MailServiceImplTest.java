/**
 * 
 */
package io.vertx.ext.mail.impl;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.MailService;
import io.vertx.test.core.VertxTestBase;

import org.junit.Test;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
public class MailServiceImplTest extends VertxTestBase {

  private static final Logger log = LoggerFactory.getLogger(MailServiceImplTest.class);

  /**
   * Test method for
   * {@link io.vertx.ext.mail.impl.MailServiceImpl#MailServiceImpl(io.vertx.core.Vertx, io.vertx.ext.mail.MailConfig)}
   * .
   */
  @Test
  public final void testMailServiceImpl() {
    MailService mailService = new MailServiceImpl(vertx, new MailConfig());
    assertNotNull(mailService);
  }

  /**
   * Test method for {@link io.vertx.ext.mail.impl.MailServiceImpl#start()}.
   */
  @Test
  public final void testStart() {
    MailService mailService = new MailServiceImpl(vertx, new MailConfig());
    mailService.start(); // currently no-op
  }

  /**
   * Test method for {@link io.vertx.ext.mail.impl.MailServiceImpl#stop()}.
   */
  @Test
  public final void testStop() {
    MailService mailService = new MailServiceImpl(vertx, new MailConfig());
    mailService.start();
    mailService.stop();
  }

  @Test
  public final void test2xStop() {
    MailService mailService = new MailServiceImpl(vertx, new MailConfig());
    mailService.start();
    mailService.stop();
    mailService.stop();
  }

  @Test
  public final void testStoppedSend() {
    MailService mailService = new MailServiceImpl(vertx, new MailConfig());
    mailService.start();
    mailService.stop();
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

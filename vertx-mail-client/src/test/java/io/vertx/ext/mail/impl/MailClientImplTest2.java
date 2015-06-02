package io.vertx.ext.mail.impl;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.mail.SMTPTestWiser;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
@RunWith(VertxUnitRunner.class)
public class MailClientImplTest2 extends SMTPTestWiser {

  private static final Logger log = LoggerFactory.getLogger(MailClientImplTest2.class);

  /**
   * test if we can shut down the connection pool while a send operation is still running the active connection will be
   * shut down when the mail has finished sending (this is basically the same test as
   * {@link SMTPConnectionPoolShutdownTest#testCloseWhileMailActive(TestContext)} but it goes through the MailClient
   * interface and actually sends a mail)
   */
  @Test
  public final void testCloseWhileMailActive(TestContext testContext) {
    Async async = testContext.async();
    Async async2 = testContext.async();

    MailClientImpl mailClient = new MailClientImpl(vertx, configNoSSL());

    testContext.assertEquals(0, mailClient.getConnectionPool().connCount());

    mailClient.sendMail(largeMessage(), result -> {
      log.info("mail finished");
      if (result.succeeded()) {
        log.info(result.result().toString());
        vertx.setTimer(1000, v -> {
          testContext.assertEquals(0, mailClient.getConnectionPool().connCount());
          async.complete();
        });
      } else {
        log.warn("got exception", result.cause());
        testContext.fail(result.cause());
      }
    });
    // wait a short while to allow the mail send to start
    // otherwise we shut down the connection pool before sending even starts
    vertx.setTimer(100, v1 -> {
      log.info("closing mail service");
      mailClient.close();
      // this doesn't wait for close operation, so we are still at 1 here
        testContext.assertEquals(1, mailClient.getConnectionPool().connCount());
        async2.complete();
      });
  }
}

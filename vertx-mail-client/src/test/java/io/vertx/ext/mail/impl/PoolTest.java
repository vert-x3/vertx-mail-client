package io.vertx.ext.mail.impl;

import java.util.concurrent.atomic.AtomicReference;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.PassOnce;
import io.vertx.ext.mail.SMTPTestWiser;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests with a pool with more than 1 slot
 * <p>
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
@RunWith(VertxUnitRunner.class)
public class PoolTest extends SMTPTestWiser {

  private static final Logger log = LoggerFactory.getLogger(PoolTest.class);

  /**
   * send two mails in parallel
   *
   * we assert that the conn count reaches 2 and that the message-id is not the same
   * (that was not thread-safe)
   *
   * @param context
   */
  @Test
  public void mailTwiceTest(TestContext context) {
    final MailConfig config = configNoSSL();
    final TestMailClient mailClient = new TestMailClient(vertx, config);
    Async async = context.async();
    Async async2 = context.async();

    MailMessage email = largeMessage();

    PassOnce pass1 = new PassOnce(s -> context.fail(s));
    PassOnce pass2 = new PassOnce(s -> context.fail(s));

    context.assertEquals(0, mailClient.getConnectionPool().connCount());

    final AtomicReference<String> messageID1 = new AtomicReference<String>();

    mailClient.sendMail(email, result -> {
      context.assertEquals(2, mailClient.getConnectionPool().connCount());
      log.info("mail finished");
      pass1.passOnce();
      if (result.succeeded()) {
        log.info(result.result());
        messageID1.set(result.result().getMessageID());
        async2.complete();
      } else {
        log.warn("got exception", result.cause());
        context.fail(result.cause());
      }
    });
    mailClient.sendMail(email, result -> {
      context.assertEquals(2, mailClient.getConnectionPool().connCount());
      log.info("mail finished");
      pass2.passOnce();
      mailClient.close();
      if (result.succeeded()) {
        log.info(result.result());
        context.assertNotEquals(messageID1.get(), result.result().getMessageID());
        vertx.setTimer(1000, v -> {
          context.assertEquals(0, mailClient.getConnectionPool().connCount());
          async.complete();
        });
      } else {
        log.warn("got exception", result.cause());
        context.fail(result.cause());
      }
    });
  }
}

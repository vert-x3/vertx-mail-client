package io.vertx.ext.mail.impl;

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
 * this test uses a local SMTP server (wiser from subethasmtp) since this server supports SSL/TLS, the tests relating to
 * that are here
 * <p>
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
@RunWith(VertxUnitRunner.class)
public class Pool1SlotTest extends SMTPTestWiser {

  private static final Logger log = LoggerFactory.getLogger(Pool1SlotTest.class);

  /**
   * send two mails after one another with a pool with size 1 we try to assert that the pool doesn't reach size > 1 and
   * that it is closed after the idle wait time
   *
   * @param context
   */
  @Test
  public void mailTest(TestContext context) {
    final MailConfig config = configNoSSL().setMaxPoolSize(1).setIdleTimeout(1);
    final TestMailClient mailClient = new TestMailClient(vertx, config);
    Async async = context.async();

    MailMessage email = exampleMessage();

    PassOnce pass1 = new PassOnce(s -> context.fail(s));
    PassOnce pass2 = new PassOnce(s -> context.fail(s));

    mailClient.sendMail(email, result -> {
      log.info("mail finished");
      pass1.passOnce();
      if (result.succeeded()) {
        log.info(result.result());
        mailClient.sendMail(email, result2 -> {
          log.info("mail finished");
          pass2.passOnce();
          if (result2.succeeded()) {
            log.info(result2.result());
            context.assertEquals(1, mailClient.getConnectionPool().connCount());
            // give the pool 1,5s to time the connection out
            vertx.setTimer(1500, v -> {
              context.assertEquals(0, mailClient.getConnectionPool().connCount());
              mailClient.close();
              async.complete();
            });
          } else {
            log.warn("got exception", result2.cause());
            context.fail(result2.cause());
          }
        });
      } else {
        log.warn("got exception", result.cause());
        context.fail(result.cause());
      }
    });
  }

  /**
   * send two mails in parallel with a pool with size 1
   *
   * we try to assert that the pool doesn't start both send operations in parallel by checking that the pool size stays
   * at 1
   *
   * @param context
   */
  @Test
  public void mailTwiceTest(TestContext context) {
    final MailConfig config = configNoSSL().setMaxPoolSize(1).setIdleTimeout(1);
    final TestMailClient mailClient = new TestMailClient(vertx, config);
    Async async = context.async();
    Async async2 = context.async();

    MailMessage email = exampleMessage();

    PassOnce pass1 = new PassOnce(s -> context.fail(s));
    PassOnce pass2 = new PassOnce(s -> context.fail(s));

    mailClient.sendMail(email, result -> {
      context.assertEquals(1, mailClient.getConnectionPool().connCount());
      log.info("mail finished");
      pass1.passOnce();
      if (result.succeeded()) {
        log.info(result.result());
        async2.complete();
      } else {
        log.warn("got exception", result.cause());
        context.fail(result.cause());
      }
    });
    mailClient.sendMail(email, result2 -> {
      context.assertEquals(1, mailClient.getConnectionPool().connCount());
      log.info("mail finished");
      pass2.passOnce();
      mailClient.close();
      if (result2.succeeded()) {
        log.info(result2.result());
        vertx.setTimer(1000, v -> {
          context.assertEquals(0, mailClient.getConnectionPool().connCount());
          async.complete();
        });
      } else {
        log.warn("got exception", result2.cause());
        context.fail(result2.cause());
      }
    });
  }
}

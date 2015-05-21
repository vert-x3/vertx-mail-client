package io.vertx.ext.mail.impl;

import java.util.concurrent.atomic.AtomicLong;

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

    MailMessage email = largeMessage();

    PassOnce pass1 = new PassOnce(s -> context.fail(s));
    PassOnce pass2 = new PassOnce(s -> context.fail(s));

    mailClient.sendMail(email, result -> {
      log.info("mail finished");
      pass1.passOnce();
      if (result.succeeded()) {
        log.info(result.result().toString());
        mailClient.sendMail(email, result2 -> {
          log.info("mail finished");
          pass2.passOnce();
          if (result2.succeeded()) {
            log.info(result2.result().toString());
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
   * send two mails in parallel with a pool with size 1 we try to assert that the pool doesn't start both send
   * operations in parallel
   * TODO: check if this test really makes sense
   *
   * @param context
   */
  @Test
  public void mailTwiceTest(TestContext context) {
    final MailConfig config = configNoSSL().setMaxPoolSize(1).setIdleTimeout(1);
    final TestMailClient mailClient = new TestMailClient(vertx, config);
    Async async = context.async();

    MailMessage email = largeMessage();

    PassOnce pass1 = new PassOnce(s -> context.fail(s));
    PassOnce pass2 = new PassOnce(s -> context.fail(s));

    final long startTime = System.currentTimeMillis();
    AtomicLong finishtimeMail1 = new AtomicLong(0);
    AtomicLong finishtimeMail2 = new AtomicLong(0);

    mailClient.sendMail(email, result -> {
      log.info("mail finished");
      pass1.passOnce();
      if (result.succeeded()) {
        finishtimeMail1.set(System.currentTimeMillis());
        log.info(result.result().toString());
      } else {
        log.warn("got exception", result.cause());
        context.fail(result.cause());
      }
    });
    mailClient.sendMail(email, result2 -> {
      log.info("mail finished");
      pass2.passOnce();
      mailClient.close();
      if (result2.succeeded()) {
        log.info(result2.result().toString());
        finishtimeMail2.set(System.currentTimeMillis());
        // to assert that the pool size limit works, we assume that the send
        // operations did not finish
        // in about the same time (i.e. the mails were sent in sequence and not
        // in parallel)
        long time1 = finishtimeMail1.get() - startTime;
        long time2 = finishtimeMail2.get() - startTime;
        long diff = Math.abs(time1 - time2);
        log.info("running time " + time1 + "/" + time2 + " diff " + diff);
        if (diff < 100) {
          context.fail("time difference is too small (" + diff + "ms), connection pool limit not enforced");
        }
        async.complete();
      } else {
        log.warn("got exception", result2.cause());
        context.fail(result2.cause());
      }
    });
  }
}

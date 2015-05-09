package io.vertx.ext.mail;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 * this test uses a local SMTP server (wiser from subethasmtp)
 * since this server supports SSL/TLS, the tests relating to that are here
 */
@RunWith(VertxUnitRunner.class)
public class Pool1SlotTest extends SMTPTestWiser {

  private static final Logger log = LoggerFactory.getLogger(Pool1SlotTest.class);

  @Test
  public void mailTest(TestContext context) {
    final MailConfig config = configNoSSL().setMaxPoolSize(1);
    final MailService mailService = MailService.create(vertx, config);
    Async async = context.async();

    MailMessage email = exampleMessage();

    PassOnce pass1 = new PassOnce(s -> context.fail(s));
    PassOnce pass2 = new PassOnce(s -> context.fail(s));

    mailService.sendMail(email, result -> {
      log.info("mail finished");
      pass1.passOnce();
      if (result.succeeded()) {
        log.info(result.result().toString());
        mailService.sendMail(email, result2 -> {
          log.info("mail finished");
          pass2.passOnce();
          mailService.stop();
          if (result2.succeeded()) {
            log.info(result2.result().toString());
            async.complete();
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

  @Test
  public void mailTwiceTest(TestContext context) {
    final MailConfig config = configNoSSL().setMaxPoolSize(1);
    final MailService mailService = MailService.create(vertx, config);
    Async async = context.async();

    MailMessage email = exampleMessage();

    PassOnce pass1 = new PassOnce(s -> context.fail(s));
    PassOnce pass2 = new PassOnce(s -> context.fail(s));

    mailService.sendMail(email, result -> {
      log.info("mail finished");
      pass1.passOnce();
      if (result.succeeded()) {
        log.info(result.result().toString());
      } else {
        log.warn("got exception", result.cause());
        context.fail(result.cause());
      }
    });
    mailService.sendMail(email, result2 -> {
      log.info("mail finished");
      pass2.passOnce();
      mailService.stop();
      if (result2.succeeded()) {
        log.info(result2.result().toString());
        async.complete();
      } else {
        log.warn("got exception", result2.cause());
        context.fail(result2.cause());
      }
    });
  }

}

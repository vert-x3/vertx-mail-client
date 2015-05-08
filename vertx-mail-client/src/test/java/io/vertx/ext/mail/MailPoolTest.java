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
 */
@RunWith(VertxUnitRunner.class)
public class MailPoolTest extends SMTPTestWiser {

  private static final Logger log = LoggerFactory.getLogger(MailPoolTest.class);

  MailClient mailClient;

  @Test
  public void mailTest(TestContext context) {
    log.info("starting");

    Async async = context.async();

    MailClient mailClient = MailClient.create(vertx, configNoSSL());

    MailMessage email = exampleMessage();

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
          mailClient.close();
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
  public void mailConcurrentTest(TestContext context) {
    log.info("starting");

    Async mail1 = context.async();
    Async mail2 = context.async();

    MailClient mailClient = MailClient.create(vertx, configNoSSL());

    MailMessage email = exampleMessage();

    PassOnce pass1 = new PassOnce(s -> context.fail(s));
    PassOnce pass2 = new PassOnce(s -> context.fail(s));

    mailClient.sendMail(email, result -> {
      log.info("mail finished");
      pass1.passOnce();
      if (result.succeeded()) {
        log.info(result.result().toString());
        mail1.complete();
      } else {
        log.warn("got exception", result.cause());
        context.fail(result.cause());
      }
    });

    mailClient.sendMail(email, result2 -> {
      log.info("mail finished");
      pass2.passOnce();
      if (result2.succeeded()) {
        log.info(result2.result().toString());
        mail2.complete();
      } else {
        log.warn("got exception", result2.cause());
        context.fail(result2.cause());
      }
    });
  }

  @Test
  public void mailConcurrent2Test(TestContext context) {
    Async mail1 = context.async();
    Async mail2 = context.async();

    vertx.getOrCreateContext().runOnContext(v -> {

      log.info("starting");

      MailClient mailClient = MailClient.create(vertx, configNoSSL());

      MailMessage email = exampleMessage();

      PassOnce pass1 = new PassOnce(s -> context.fail(s));
      PassOnce pass2 = new PassOnce(s -> context.fail(s));
      PassOnce pass3 = new PassOnce(s -> context.fail(s));
      PassOnce pass4 = new PassOnce(s -> context.fail(s));

      log.info("starting mail 1");
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
              mail1.complete();
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

      log.info("starting mail 2");
      mailClient.sendMail(email, result -> {
        log.info("mail finished");
        pass3.passOnce();
        if (result.succeeded()) {
          log.info(result.result().toString());
          mailClient.sendMail(email, result2 -> {
            log.info("mail finished");
            pass4.passOnce();
            if (result2.succeeded()) {
              log.info(result2.result().toString());
              mail2.complete();
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
    });
  }

}

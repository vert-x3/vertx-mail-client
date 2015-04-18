package io.vertx.ext.mail;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

/*
 first implementation of a SMTP client
 */
/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
@RunWith(VertxUnitRunner.class)
public class MailPoolTest extends SMTPTestWiser {

  private static final Logger log = LoggerFactory.getLogger(MailPoolTest.class);

  Vertx vertx = Vertx.vertx();

  MailService mailService;

  @Test
  public void mailTest(TestContext context) {
    log.info("starting");

    Async async = context.async();

    MailService mailService = MailService.create(vertx, configNoSSL());

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
  public void mailConcurrentTest(TestContext context) {
    log.info("starting");

    Async mail1 = context.async();
    Async mail2 = context.async();

    MailService mailService = MailService.create(vertx, configNoSSL());

    MailMessage email = exampleMessage();

    PassOnce pass1 = new PassOnce(s -> context.fail(s));
    PassOnce pass2 = new PassOnce(s -> context.fail(s));

    mailService.sendMail(email, result -> {
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

    mailService.sendMail(email, result2 -> {
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

      MailService mailService = MailService.create(vertx, configNoSSL());

      MailMessage email = exampleMessage();

      PassOnce pass1 = new PassOnce(s -> context.fail(s));
      PassOnce pass2 = new PassOnce(s -> context.fail(s));
      PassOnce pass3 = new PassOnce(s -> context.fail(s));
      PassOnce pass4 = new PassOnce(s -> context.fail(s));

      log.info("starting mail 1");
      mailService.sendMail(email, result -> {
        log.info("mail finished");
        pass1.passOnce();
        if (result.succeeded()) {
          log.info(result.result().toString());
          mailService.sendMail(email, result2 -> {
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
      mailService.sendMail(email, result -> {
        log.info("mail finished");
        pass3.passOnce();
        if (result.succeeded()) {
          log.info(result.result().toString());
          mailService.sendMail(email, result2 -> {
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

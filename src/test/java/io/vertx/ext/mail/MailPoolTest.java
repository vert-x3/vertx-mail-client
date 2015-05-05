package io.vertx.ext.mail;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.subethamail.wiser.Wiser;

/*
 first implementation of a SMTP client
 */
/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
@RunWith(VertxUnitRunner.class)
public class MailPoolTest {

  private static final Logger log = LoggerFactory.getLogger(MailPoolTest.class);

  Vertx vertx = Vertx.vertx();

  MailClient mailClient;

  @Test
  public void mailTest(TestContext context) {
    log.info("starting");

    Async async = context.async();

    MailClient mailClient = MailClient.create(vertx, mailConfig());

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
          mailClient.stop();
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

  /**
   * @return
   */
  private MailMessage exampleMessage() {
    return new MailMessage().setFrom("user@example.com").setTo("user@example.com")
        .setSubject("Test email").setText("this is a message");
  }

  @Test
  public void mailConcurrentTest(TestContext context) {
    log.info("starting");

    Async mail1 = context.async();
    Async mail2 = context.async();

    MailClient mailClient = MailClient.create(vertx, mailConfig());

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

      MailClient mailClient = MailClient.create(vertx, mailConfig());

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

  /**
   * @return
   */
  private MailConfig mailConfig() {
    return new MailConfig("localhost", 1587, StarttlsOption.DISABLED, LoginOption.DISABLED);
  }

  Wiser wiser;

  @Before
  public void startSMTP() {
    mailClient = MailClient.create(vertx, mailConfig());
    wiser = new Wiser();
    wiser.setPort(1587);
    wiser.start();
  }

  @After
  public void stopSMTP() {
    mailClient.stop();
    if (wiser != null) {
      wiser.stop();
    }
  }

}

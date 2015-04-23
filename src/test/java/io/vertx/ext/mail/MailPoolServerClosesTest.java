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

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 * test what happens when the server closes the connection if we use a pooled connection
 * (doesn't work currently since the pool doesn't check if the connections are still working)
 */
@RunWith(VertxUnitRunner.class)
public class MailPoolServerClosesTest extends SMTPTestDummy {

  private static final Logger log = LoggerFactory.getLogger(MailPoolServerClosesTest.class);

  Vertx vertx = Vertx.vertx();

  /**
   * send two mails after each other when the server closes the connection immediately after the data
   * send was successful
   *  
   * @param context
   */
  @Test
  public void mailConnectionCloseImmediatelyTest(TestContext context) {
    log.info("starting");

    smtpServer.setCloseImmediately(true);
    Async mail1 = context.async();
    Async mail2 = context.async();

    vertx.getOrCreateContext().runOnContext(v -> {

      MailService mailService = MailService.create(vertx, mailConfig());

      MailMessage email = new MailMessage().setFrom("user@example.com").setTo("user@example.com")
          .setSubject("Test email").setText("this is a message");

      PassOnce pass1 = new PassOnce(s -> context.fail(s));
      PassOnce pass2 = new PassOnce(s -> context.fail(s));

      log.info("starting mail 1");
      mailService.sendMail(email, result -> {
        log.info("mail finished 1");
        pass1.passOnce();
        if (result.succeeded()) {
          log.info(result.result().toString());
          mail1.complete();
          log.info("starting mail 2");
          mailService.sendMail(email, result2 -> {
            pass2.passOnce();
            log.info("mail finished 2");
            if (result2.succeeded()) {
              log.info(result2.result().toString());
              mail2.complete();
            } else {
              log.warn("got exception 2", result2.cause());
              context.fail(result2.cause());
            }
          });
        } else {
          log.warn("got exception 1", result.cause());
          context.fail(result.cause());
        }
      });
    });
  }

  /**
   * send two mails after each other when the server waits a time after the after the data
   * send was successful and closes the connection
   *
   * @param context
   */
  @Test
  public void mailConnectionCloseWaitTest(TestContext context) {
    log.info("starting");

    smtpServer.setCloseImmediately(false);
    Async mail1 = context.async();
    Async mail2 = context.async();

    vertx.getOrCreateContext().runOnContext(v -> {

      MailService mailService = MailService.create(vertx, mailConfig());

      MailMessage email = new MailMessage().setFrom("user@example.com").setTo("user@example.com")
          .setSubject("Test email").setText("this is a message");

      PassOnce pass1 = new PassOnce(s -> context.fail(s));
      PassOnce pass2 = new PassOnce(s -> context.fail(s));

      log.info("starting mail 1");
      mailService.sendMail(email, result -> {
        pass1.passOnce();
        log.info("mail finished 1");
        if (result.succeeded()) {
          log.info(result.result().toString());
          mail1.complete();
          log.info("starting mail 2");
          mailService.sendMail(email, result2 -> {
            pass2.passOnce();
            log.info("mail finished 2");
            if (result2.succeeded()) {
              log.info(result2.result().toString());
              mail2.complete();
            } else {
              log.warn("got exception 2", result2.cause());
              context.fail(result2.cause());
            }
          });
        } else {
          log.warn("got exception 1", result.cause());
          context.fail(result.cause());
        }
      });
    });
  }

  /**
   * send two mails after each other when the server fails the RSET operation after sending the first
   * mail
   *
   */
  @Test
  public void mailConnectionRsetFailTest(TestContext context) {
    log.info("starting");

    smtpServer.setCloseImmediately(false);
    smtpServer.setDialogue("220 example.com ESMTP",
        "EHLO",
        "250-example.com\n" +
            "250-SIZE 1000000\n" +
            "250 PIPELINING",
            "MAIL FROM",
            "250 2.1.0 Ok",
            "RCPT TO",
            "250 2.1.5 Ok",
            "DATA",
            "354 End data with <CR><LF>.<CR><LF>",
            "250 2.0.0 Ok: queued as ABCDDEF0123456789",
            "RSET",
        "500 xxx");

    Async mail1 = context.async();
    Async mail2 = context.async();

    vertx.getOrCreateContext().runOnContext(v -> {

      MailService mailService = MailService.create(vertx, mailConfig());

      MailMessage email = new MailMessage().setFrom("user@example.com").setTo("user@example.com")
          .setSubject("Test email").setText("this is a message");

      PassOnce pass1 = new PassOnce(s -> context.fail(s));
      PassOnce pass2 = new PassOnce(s -> context.fail(s));

      log.info("starting mail 1");
      mailService.sendMail(email, result -> {
        pass1.passOnce();
        log.info("mail finished 1");
        if (result.succeeded()) {
          log.info(result.result().toString());
          mail1.complete();
          log.info("starting mail 2");
          mailService.sendMail(email, result2 -> {
            pass2.passOnce();
            log.info("mail finished 2");
            if (result2.succeeded()) {
              log.info(result2.result().toString());
              mail2.complete();
            } else {
              log.warn("got exception 2", result2.cause());
              context.fail(result2.cause());
            }
          });
        } else {
          log.warn("got exception 1", result.cause());
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

  private TestSmtpServer smtpServer;

  @Before
  public void startSMTP() {
    smtpServer = new TestSmtpServer(vertx);
    smtpServer.setDialogue("220 example.com ESMTP",
        "EHLO",
        "250-example.com\n" +
            "250-SIZE 1000000\n" +
            "250 PIPELINING",
            "MAIL FROM",
            "250 2.1.0 Ok",
            "RCPT TO",
            "250 2.1.5 Ok",
            "DATA",
            "354 End data with <CR><LF>.<CR><LF>",
            "250 2.0.0 Ok: queued as ABCDDEF0123456789");
  }

  @After
  public void stopSMTP() {
    smtpServer.stop();
  }

}

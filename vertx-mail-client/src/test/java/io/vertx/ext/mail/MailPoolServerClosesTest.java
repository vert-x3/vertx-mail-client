package io.vertx.ext.mail;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * test what happens when the server closes the connection if we use a pooled connection
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
@RunWith(VertxUnitRunner.class)
public class MailPoolServerClosesTest extends SMTPTestDummy {

  private static final Logger log = LoggerFactory.getLogger(MailPoolServerClosesTest.class);

  /**
   * send two mails after each other when the server closes the connection immediately after the data send was
   * successfully
   *
   * @param context
   */
  @Test
  public void mailConnectionCloseImmediatelyTest(TestContext context) {
    smtpServer.setCloseImmediately(true);
    Async mail1 = context.async();
    Async mail2 = context.async();

    MailClient mailClient = MailClient.createShared(vertx, configNoSSL());

    MailMessage email = exampleMessage();

    PassOnce pass1 = new PassOnce(s -> context.fail(s));
    PassOnce pass2 = new PassOnce(s -> context.fail(s));

    log.info("starting mail 1");
    mailClient.sendMail(email, result -> {
      log.info("mail finished 1");
      pass1.passOnce();
      if (result.succeeded()) {
        log.info(result.result().toString());
        mail1.complete();
        log.info("starting mail 2");
        mailClient.sendMail(email, result2 -> {
          pass2.passOnce();
          log.info("mail finished 2");
          if (result2.succeeded()) {
            log.info(result2.result().toString());
            mailClient.close();
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
  }

  /**
   * send two mails after each other when the server waits a time after the after the data send was successful and
   * closes the connection
   *
   * @param context
   */
  @Test
  public void mailConnectionCloseWaitTest(TestContext context) {
    smtpServer.setCloseImmediately(false);
    smtpServer.setCloseWaitTime(1);
    Async mail1 = context.async();
    Async mail2 = context.async();

    MailClient mailClient = MailClient.createShared(vertx, configNoSSL());

    MailMessage email = exampleMessage();

    PassOnce pass1 = new PassOnce(s -> context.fail(s));
    PassOnce pass2 = new PassOnce(s -> context.fail(s));

    log.info("starting mail 1");
    mailClient.sendMail(email, result -> {
      pass1.passOnce();
      log.info("mail finished 1");
      if (result.succeeded()) {
        log.info(result.result().toString());
        mail1.complete();
        log.info("starting mail 2");
        mailClient.sendMail(email, result2 -> {
          pass2.passOnce();
          log.info("mail finished 2");
          if (result2.succeeded()) {
            log.info(result2.result().toString());
            mailClient.close();
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
  }

  /**
   * send two mails after each other when the server fails the RSET operation after sending the first mail
   */
  @Test
  public void mailConnectionRsetFailTest(TestContext context) {
    smtpServer.setCloseImmediately(false)
      .setDialogue("220 example.com ESMTP",
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
      "QUIT",
      "220 bye bye");

    Async mail1 = context.async();
    Async mail2 = context.async();

    MailClient mailClient = MailClient.createShared(vertx, configNoSSL());

    MailMessage email = exampleMessage();

    PassOnce pass1 = new PassOnce(s -> context.fail(s));
    PassOnce pass2 = new PassOnce(s -> context.fail(s));

    log.info("starting mail 1");
    mailClient.sendMail(email, result -> {
      pass1.passOnce();
      log.info("mail finished 1");
      if (result.succeeded()) {
        log.info(result.result().toString());
        mail1.complete();
        log.info("starting mail 2");
        mailClient.sendMail(email, result2 -> {
          pass2.passOnce();
          log.info("mail finished 2");
          if (result2.succeeded()) {
            log.info(result2.result().toString());
            mailClient.close();
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
  }

  /**
   * this test creates an error in the 2nd mail
   */
  @Test
  public void error2ndMail(TestContext context) {
    smtpServer.setCloseImmediately(true)
      .setDialogue("220 example.com ESMTP",
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
      "220 reset ok");

    Async mail1 = context.async();
    Async mail2 = context.async();

    MailClient mailClient = MailClient.createShared(vertx, configNoSSL());

    MailMessage email = exampleMessage();

    PassOnce pass1 = new PassOnce(s -> context.fail(s));
    PassOnce pass2 = new PassOnce(s -> context.fail(s));

    log.info("starting mail 1");
    mailClient.sendMail(email, result -> {
      pass1.passOnce();
      log.info("mail finished 1");
      if (result.succeeded()) {
        log.info(result.result().toString());
        mail1.complete();
        log.info("starting mail 2");
        mailClient.sendMail(email, result2 -> {
          pass2.passOnce();
          log.info("mail finished 2");
          if (result2.succeeded()) {
            log.info(result2.result().toString());
            mailClient.close();
            context.fail("this test should fail");
          } else {
            log.info("(as expected) got exception 2", result2.cause());
            mailClient.close();
            mail2.complete();
          }
        });
      } else {
        log.warn("got exception 1", result.cause());
        context.fail(result.cause());
      }
    });
  }

  @Override
  public void startSMTP() {
    super.startSMTP();
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

}

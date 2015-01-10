package io.vertx.ext.mail;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.test.core.VertxTestBase;

import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/*
 * Test a server that doesn't support EHLO
 */
/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
public class HeloTest extends VertxTestBase {

  Vertx vertx = Vertx.vertx();
  private static final Logger log = LoggerFactory.getLogger(HeloTest.class);

  CountDownLatch latch;

  @Test
  public void mailEhloMissingTest() throws InterruptedException {
    log.info("starting");

    smtpServer.setAnswers("220 example.com ESMTP",
        "402 4.5.2 Error: command not recognized", 
        "250 example.com",
        "250 2.1.0 Ok", 
        "250 2.1.5 Ok",
        "354 End data with <CR><LF>.<CR><LF>",
        "250 2.0.0 Ok: queued as ABCDDEF0123456789",
        "221 2.0.0 Bye");

    latch = new CountDownLatch(1);

    MailConfig mailConfig = new MailConfig("localhost", 1587);

    MailService mailService = MailService.create(vertx, mailConfig);

    MailMessage email = new MailMessage("lehmann333@arcor.de", "lehmann333@arcor.de", "Subject", "Message");

    mailService.sendMail(email, result -> {
      log.info("mail finished");
      if (result.succeeded()) {
        log.info(result.result().toString());
        latch.countDown();
      } else {
        log.warn("got exception", result.cause());
        throw new RuntimeException(result.cause());
      }
    });

    awaitLatch(latch);
  }

  @Test
  public void mailNoEsmtpTest() throws InterruptedException {
    log.info("starting");

    smtpServer.setAnswers("220 example.com",
        "250 example.com",
        "250 2.1.0 Ok", 
        "250 2.1.5 Ok",
        "354 End data with <CR><LF>.<CR><LF>",
        "250 2.0.0 Ok: queued as ABCDDEF0123456789",
        "221 2.0.0 Bye");

    latch = new CountDownLatch(1);

    MailConfig mailConfig = new MailConfig("localhost", 1587);

    MailService mailService = MailService.create(vertx, mailConfig);

    MailMessage message = new MailMessage("lehmann333@arcor.de", "lehmann333@arcor.de", "Subject", "Message");

    mailService.sendMail(message, result -> {
      log.info("mail finished");
      if (result.succeeded()) {
        log.info(result.result().toString());
        latch.countDown();
      } else {
        log.warn("got exception", result.cause());
        throw new RuntimeException(result.cause());
      }
    });

    awaitLatch(latch);
  }

  /*
   * Test what happens when a reply is sent after the QUIT reply
   * I got a "Result has already been set" exception before
   * but I cannot reproduce this right now
   */
  @Test
  public void replyAfterQuitTest() throws InterruptedException {
    log.info("starting");

    smtpServer.setAnswers("220 example.com ESMTP",
        // EHLO
        "250-example.com", 
        "250-SIZE 48000000", 
        "250 PIPELINING",
        // MAIL FROM
        "250 2.1.0 Ok",
        // RCPT TO
        "250 2.1.5 Ok",
        // DATA
        "354 End data with <CR><LF>.<CR><LF>",
        // message data
        "250 2.0.0 Ok: queued as ABCDDEF0123456789",
        // QUIT
        "221 2.0.0 Bye",
        // this should not happen:
        "this is unexpected"
        );

    latch = new CountDownLatch(1);

    MailConfig mailConfig = new MailConfig("localhost", 1587);

    MailService mailService = MailService.create(vertx, mailConfig);

    MailMessage message = new MailMessage("lehmann333@arcor.de", "lehmann333@arcor.de", "Subject", "Message");

    mailService.sendMail(message, result -> {
      log.info("mail finished");
      if (result.succeeded()) {
        log.info(result.result().toString());
        latch.countDown();
      } else {
        log.warn("got exception", result.cause());
        throw new RuntimeException(result.cause());
      }
    });

    awaitLatch(latch);
  }

  @Test
  public void serverUnavailableTest() throws InterruptedException {
    log.info("starting");

    smtpServer.setAnswers("400 cannot talk to you right now\n");

    latch = new CountDownLatch(1);

    MailConfig mailConfig = new MailConfig("localhost", 1587);

    MailService mailService = MailService.create(vertx, mailConfig);

    MailMessage message = new MailMessage("lehmann333@arcor.de", "lehmann333@arcor.de", "Subject", "Message");

    mailService.sendMail(message, result -> {
      log.info("mail finished");
      if (result.succeeded()) {
        log.info(result.result().toString());
        fail("this test should throw an Exception");
      } else {
        log.warn("got exception", result.cause());
        latch.countDown();
      }
    });

    awaitLatch(latch);
  }

  @Test
  public void connectionRefusedTest() throws InterruptedException {
    log.info("starting");

    latch = new CountDownLatch(1);

    MailConfig mailConfig = new MailConfig("localhost", 1588);

    MailService mailService = MailService.create(vertx, mailConfig);

    MailMessage message = new MailMessage("lehmann333@arcor.de", "lehmann333@arcor.de", "Subject", "Message");

    mailService.sendMail(message, result -> {
      log.info("mail finished");
      if (result.succeeded()) {
        log.info(result.result().toString());
        fail("this test should throw an Exception");
      } else {
        log.warn("got exception", result.cause());
        latch.countDown();
      }
    });

    awaitLatch(latch);
  }

  @Test
  public void tlsMissingTest() throws InterruptedException {
    log.info("starting");

    smtpServer.setAnswers("220 example.com ESMTP multiline",
        "250-example.com", 
        "250-SIZE 48000000", 
        "250 PIPELINING",
        "250 2.1.0 Ok", 
        "250 2.1.5 Ok",
        "354 End data with <CR><LF>.<CR><LF>",
        "250 2.0.0 Ok: queued as ABCDDEF0123456789",
        "221 2.0.0 Bye");

    latch = new CountDownLatch(1);

    MailConfig mailConfig = new MailConfig("localhost", 1587, StarttlsOption.REQUIRED, LoginOption.DISABLED);

    MailService mailService = MailService.create(vertx, mailConfig);

    MailMessage message = new MailMessage("lehmann333@arcor.de", "lehmann333@arcor.de", "Subject", "Message");

    mailService.sendMail(message, result -> {
      log.info("mail finished");
      if (result.succeeded()) {
        log.info(result.result().toString());
        fail("this test should throw an Exception");
      } else {
        log.warn("got exception", result.cause());
        latch.countDown();
      }
    });

    awaitLatch(latch);
  }

  @Ignore
  @Test
  public void closeOnConnectTest() throws InterruptedException {
    log.info("starting");

    smtpServer.setAnswers("");

    latch = new CountDownLatch(1);

    MailConfig mailConfig = new MailConfig("localhost", 1587, StarttlsOption.REQUIRED, LoginOption.DISABLED);

    MailService mailService = MailService.create(vertx, mailConfig);

    MailMessage message = new MailMessage("lehmann333@arcor.de", "lehmann333@arcor.de", "Subject", "Message");

    mailService.sendMail(message, result -> {
      log.info("mail finished");
      if (result.succeeded()) {
        log.info(result.result().toString());
        latch.countDown();
      } else {
        log.warn("got exception", result.cause());
        throw new RuntimeException(result.cause());
      }
    });

    awaitLatch(latch);
  }

  /*
   * test a multiline reply as welcome message. this is done
   * e.g. by the servers from AOL. the service will deny access
   * if the client tries to do PIPELINING before checking the EHLO
   * capabilities
   */
  @Test
  public void mailMultilineWelcomeTest() throws InterruptedException {
    log.info("starting");

    smtpServer.setAnswers("220-example.com ESMTP multiline",
        "220-this server uses a long welcome message",
        "220 this is supposed to confuse spammers",
        "250-example.com", 
        "250-SIZE 48000000", 
        "250 PIPELINING",
        "250 2.1.0 Ok", 
        "250 2.1.5 Ok",
        "354 End data with <CR><LF>.<CR><LF>",
        "250 2.0.0 Ok: queued as ABCDDEF0123456789",
        "221 2.0.0 Bye");

    latch = new CountDownLatch(1);

    MailConfig mailConfig = new MailConfig("localhost", 1587);

    MailService mailService = MailService.create(vertx, mailConfig);

    MailMessage message = new MailMessage("lehmann333@arcor.de", "lehmann333@arcor.de", "Subject", "Message");

    mailService.sendMail(message, result -> {
      log.info("mail finished");
      if (result.succeeded()) {
        log.info(result.result().toString());
        latch.countDown();
      } else {
        log.warn("got exception", result.cause());
        throw new RuntimeException(result.cause());
      }
    });

    awaitLatch(latch);
  }

  TestSmtpServer smtpServer;

  @Before
  public void startSMTP() {
    smtpServer=new TestSmtpServer(vertx);
  }

  @After
  public void stopSMTP() {
    smtpServer.stop();
  }

}

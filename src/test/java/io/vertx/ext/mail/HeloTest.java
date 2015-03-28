package io.vertx.ext.mail;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.test.core.VertxTestBase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/*
 * Test a server that doesn't support EHLO
 */
/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
public class HeloTest extends VertxTestBase {

  private static final Logger log = LoggerFactory.getLogger(HeloTest.class);

  @Test
  public void mailEhloMissingTest() {
    smtpServer.setAnswers("220 example.com ESMTP",
        "402 4.5.2 Error: command not recognized", 
        "250 example.com",
        "250 2.1.0 Ok", 
        "250 2.1.5 Ok",
        "354 End data with <CR><LF>.<CR><LF>",
        "250 2.0.0 Ok: queued as ABCDDEF0123456789",
        "221 2.0.0 Bye");
    smtpServer.setCloseImmediately(false);

    runTestSuccess(mailServiceDefault());
  }

  @Test
  public void mailNoEsmtpTest() {
    smtpServer.setAnswers("220 example.com",
        "250 example.com",
        "250 2.1.0 Ok", 
        "250 2.1.5 Ok",
        "354 End data with <CR><LF>.<CR><LF>",
        "250 2.0.0 Ok: queued as ABCDDEF0123456789",
        "221 2.0.0 Bye");
    smtpServer.setCloseImmediately(false);

    runTestSuccess(mailServiceDefault());
  }

  /**
   * @return
   */
  private MailService mailServiceDefault() {
    return MailService.create(vertx, defaultConfig());
  }

  /*
   * Test what happens when a reply is sent after the QUIT reply
   */
  @Test
  public void replyAfterQuitTest() {
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
    smtpServer.setCloseImmediately(false);

    runTestSuccess(mailServiceDefault());
  }

  @Test
  public void serverUnavailableTest() {
    smtpServer.setAnswers("400 cannot talk to you right now\r\n");
    smtpServer.setCloseImmediately(true);

    runTestException(mailServiceDefault());
  }

  @Test
  public void connectionRefusedTest() {
    runTestException(MailService.create(vertx, new MailConfig("localhost", 1588)));
  }

  @Test
  public void stlsMissingTest() {
    smtpServer.setAnswers("220 example.com ESMTP multiline",
        "250-example.com", 
        "250-SIZE 48000000", 
        "250 PIPELINING",
        "250 2.1.0 Ok", 
        "250 2.1.5 Ok",
        "354 End data with <CR><LF>.<CR><LF>",
        "250 2.0.0 Ok: queued as ABCDDEF0123456789",
        "221 2.0.0 Bye");
    smtpServer.setCloseImmediately(false);

    runTestException(mailServiceTLS());
  }

  /**
   * @param mailService
   */
  private void runTestException(final MailService mailService) {
    PassOnce pass = new PassOnce(s -> fail(s));

    mailService.sendMail(exampleMessage(), result -> {
      log.info("mail finished");
      pass.passOnce();
      if (result.succeeded()) {
        log.info(result.result().toString());
        fail("this test should throw an Exception");
      } else {
        log.warn("got exception", result.cause());
        testComplete();
      }
    });

    await();
  }

  /**
   * @return
   */
  private MailService mailServiceTLS() {
    return MailService.create(vertx, defaultConfigTLS());
  }

  /**
   * @return
   */
  private MailConfig defaultConfigTLS() {
    return new MailConfig("localhost", 1587, StarttlsOption.REQUIRED, LoginOption.DISABLED);
  }

  @Test
  public void closeOnConnectTest() {
    smtpServer.setAnswers("");
    smtpServer.setCloseImmediately(true);

    runTestException(mailServiceDefault());
  }

  /**
   * 
   */
  private void runTestSuccess(MailService mailService) {
    PassOnce pass = new PassOnce(s -> fail(s));

    mailService.sendMail(exampleMessage(), result -> {
      log.info("mail finished");
      pass.passOnce();
      if (result.succeeded()) {
        log.info(result.result().toString());
        testComplete();
      } else {
        log.warn("got exception", result.cause());
        throw new RuntimeException(result.cause());
      }
    });

    await();
  }

  /*
   * test a multiline reply as welcome message. this is done
   * e.g. by the servers from AOL. the service will deny access
   * if the client tries to do PIPELINING before checking the EHLO
   * capabilities
   */
  @Test
  public void mailMultilineWelcomeTest() {
    smtpServer.setAnswers("220-example.com ESMTP multiline",
        "220-this server uses a multi-line welcome message",
        "220 this is supposed to confuse spammers",
        "250-example.com", 
        "250-SIZE 48000000", 
        "250 PIPELINING",
        "250 2.1.0 Ok", 
        "250 2.1.5 Ok",
        "354 End data with <CR><LF>.<CR><LF>",
        "250 2.0.0 Ok: queued as ABCDDEF0123456789",
        "221 2.0.0 Bye");
    smtpServer.setCloseImmediately(false);

    runTestSuccess(mailServiceDefault());
  }

  /*
   * simulate the server closes the connection immediately after the
   * banner message
   */
  @Test
  public void closeAfterBannerTest() {
    smtpServer.setAnswers("220 example.com ESMTP\r\n");
    smtpServer.setCloseImmediately(true);

    runTestException(mailServiceDefault());
  }

  /**
   * @return
   */
  private MailConfig defaultConfig() {
    return new MailConfig("localhost", 1587);
  }

  /**
   * @return
   */
  private MailMessage exampleMessage() {
    return new MailMessage("from@example.com", "user@example.com", "Subject", "Message");
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

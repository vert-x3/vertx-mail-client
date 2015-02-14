package io.vertx.ext.mail;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.test.core.VertxTestBase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 * auth examples with failures mostly 
 */
public class MailAuthTest extends VertxTestBase {

  private static final Logger log = LoggerFactory.getLogger(MailAuthTest.class);

  @Test
  public void authLoginTest() {
    smtpServer.setAnswers("220 example.com ESMTP",
        "250-example.com",
        "250 AUTH LOGIN",
        "334 VXNlcm5hbWU6",
        "334 UGFzc3dvcmQ6",
        "250 2.1.0 Ok",
        "250 2.1.0 Ok",
        "250 2.1.5 Ok",
        "354 End data with <CR><LF>.<CR><LF>",
        "250 2.0.0 Ok: queued as ABCD",
        "221 2.0.0 Bye");

    runTestSuccess(mailServiceDefault());
  }

  /**
   * @param mailService
   */
  private void runTestSuccess(MailService mailService) {
    mailService.sendMail(exampleMessage(), result -> {
      log.info("mail finished");
      if (result.succeeded()) {
        log.info(result.result().toString());
        testComplete();
      } else {
        final Throwable cause = result.cause();
        log.warn("got exception", cause);
        throw new RuntimeException("unexpected exception", cause);
      }
    });

    await();
  }

  /**
   * @return
   */
  private MailMessage exampleMessage() {
    return new MailMessage()
      .setFrom("user@example.com")
      .setTo("user@example.com")
      .setSubject("Test email")
      .setText("this is a message");
  }

  /**
   * @return
   */
  private MailConfig defaultConfigLogin() {
    return new MailConfig("localhost", 1587)
    .setUsername("username")
    .setPassword("password");
  }

  @Test
  public void authPlainTest() {
    smtpServer.setAnswers("220 example.com ESMTP",
        "250-example.com",
        "250 AUTH PLAIN",
        "250 2.1.0 Ok",
        "250 2.1.0 Ok",
        "250 2.1.5 Ok",
        "354 End data with <CR><LF>.<CR><LF>",
        "250 2.0.0 Ok: queued as ABCD",
        "221 2.0.0 Bye");

    runTestSuccess(mailServiceDefault());
  }

  @Test
  public void authCramMD5Test() {
    smtpServer.setAnswers("220 example.com ESMTP",
        "250-example.com",
        "250 AUTH CRAM-MD5",
        "334 PDEyMzQuYWJjZEBleGFtcGxlLmNvbT4=",
        "250 2.1.0 Ok",
        "250 2.1.0 Ok",
        "250 2.1.5 Ok",
        "354 End data with <CR><LF>.<CR><LF>",
        "250 2.0.0 Ok: queued as ABCD",
        "221 2.0.0 Bye");

    runTestSuccess(mailServiceDefault());
  }

  @Test
  public void authCramMD5FailTest() {
    smtpServer.setAnswers("220 example.com ESMTP",
        "250-example.com",
        "250 AUTH CRAM-MD5",
        "334 PDEyMzQuYWJjZEBleGFtcGxlLmNvbT4=",
        "435 4.7.8 Error: authentication failed: bad protocol / cancel", 
        "250 2.1.0 Ok",
        "250 2.1.5 Ok",
        "354 End data with <CR><LF>.<CR><LF>",
        "250 2.0.0 Ok: queued as ABCD",
        "221 2.0.0 Bye");

    runTestException(mailServiceDefault());
  }

  /**
   * @return
   */
  private MailService mailServiceDefault() {
    return MailService.create(vertx, defaultConfigLogin());
  }

  @Test
  public void authJunkTest() throws InterruptedException {
    smtpServer.setAnswers("220 example.com ESMTP",
        "250-example.com",
        "250 AUTH JUNK");

    runTestException(mailServiceDefault());
  }

  /**
   * @param mailService
   */
  private void runTestException(MailService mailService) {
    mailService.sendMail(exampleMessage(), result -> {
      log.info("mail finished");
      if (result.succeeded()) {
        log.info(result.result().toString());
        fail("expected an exception");
      } else {
        log.warn("got exception", result.cause());
        testComplete();
      }
    });

    await();
  }

  private TestSmtpServer smtpServer;

  @Before
  public void startSMTP() {
    smtpServer = new TestSmtpServer(vertx);
  }

  @After
  public void stopSMTP() {
    smtpServer.stop();
  }

}

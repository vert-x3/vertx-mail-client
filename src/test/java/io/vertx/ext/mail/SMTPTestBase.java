package io.vertx.ext.mail;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.test.core.VertxTestBase;

/**
 * Support functions for SMTP tests
 *
 * the actual tests should extend a subclass
 * that starts and stops a fake smtp server
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
public class SMTPTestBase extends VertxTestBase {

  private static final Logger log = LoggerFactory.getLogger(SMTPTestBase.class);

  /**
   * @return
   */
  protected MailClient mailServiceDefault() {
    return MailClient.create(vertx, defaultConfig());
  }

  /**
   * @return
   */
  protected MailClient mailServiceLogin() {
    return MailClient.create(vertx, configLogin());
  }

  /**
   * @return
   */
  protected MailClient mailServiceLogin(String user, String pw) {
    return MailClient.create(vertx, configLogin(user, pw));
  }

  /**
   * @param mailClient
   */
  protected void runTestException(final MailClient mailClient) {
    testException(mailClient, exampleMessage());
  }

  /**
   * @return
   */
  protected MailClient mailServiceTLS() {
    return MailClient.create(vertx, configTLS());
  }

  /**
   * @return
   */
  protected MailClient mailServiceNoSSL() {
    return MailClient.create(vertx, configNoSSL());
  }

  /**
   * @return
   */
  private MailConfig configTLS() {
    return new MailConfig("localhost", 1587, StarttlsOption.REQUIRED, LoginOption.DISABLED);
  }

  /**
   * @return
   */
  private MailConfig configNoSSL() {
    return new MailConfig("localhost", 1587, StarttlsOption.DISABLED, LoginOption.DISABLED);
  }

  /**
   * @return
   */
  protected MailConfig defaultConfig() {
    return new MailConfig("localhost", 1587);
  }

  /**
   * @return
   */
  private MailConfig configLogin() {
    return configLogin("xxx", "xxx");
  }

  /**
   * @return
   */
  private MailConfig configLogin(String user, String pw) {
    return new MailConfig("localhost", 1587, StarttlsOption.DISABLED, LoginOption.REQUIRED)
      .setUsername(user)
      .setPassword(pw);
  }

  /**
   * @return
   */
  protected MailMessage exampleMessage() {
    return new MailMessage("from@example.com", "user@example.com", "Subject", "Message");
  }

  protected void testException(MailClient mailClient, MailMessage email) {
    PassOnce pass = new PassOnce(s -> fail(s));

    mailClient.sendMail(email, result -> {
      log.info("mail finished");
      pass.passOnce();
      mailClient.stop();
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

  protected void testSuccess(MailClient mailClient, MailMessage email) {
    PassOnce pass = new PassOnce(s -> fail(s));

    mailClient.sendMail(email, result -> {
      log.info("mail finished");
      pass.passOnce();
      mailClient.stop();
      if (result.succeeded()) {
        log.info(result.result().toString());
        testComplete();
      } else {
        log.warn("got exception", result.cause());
        fail(result.cause().toString());
      }
    });

    await();
  }

  protected void testSuccess(MailClient mailClient, MailMessage email, String message) {
    PassOnce pass = new PassOnce(s -> fail(s));

    mailClient.sendMailString(email, message, result -> {
      log.info("mail finished");
      pass.passOnce();
      mailClient.stop();
      if (result.succeeded()) {
        log.info(result.result().toString());
        testComplete();
      } else {
        log.warn("got exception", result.cause());
        fail(result.cause().toString());
      }
    });

    await();
  }

  protected void testSuccess(MailClient mailClient) {
    testSuccess(mailClient, exampleMessage());
  }

  protected void testException(MailMessage email) {
    testException(mailServiceDefault(), email);
  }

  protected void testException(MailClient mailClient) {
    testException(mailClient, exampleMessage());
  }

  protected void testSuccess(MailMessage email) {
    testSuccess(mailServiceDefault(), email);
  }

  protected void testSuccess() {
    testSuccess(mailServiceDefault());
  }

  protected void testException() {
    runTestException(mailServiceDefault());
  }

}

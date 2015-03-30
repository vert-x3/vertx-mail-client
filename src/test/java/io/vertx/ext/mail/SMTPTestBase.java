package io.vertx.ext.mail;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.test.core.VertxTestBase;

/**
 * Support functions for SMTP tests
 *
 * this is an abstract class, the actual tests should extend a subclass
 * that starts and stops a fake smtp server
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
public abstract class SMTPTestBase extends VertxTestBase {

  private static final Logger log = LoggerFactory.getLogger(SMTPTestBase.class);

  /**
   * @return
   */
  protected MailService mailServiceDefault() {
    return MailService.create(vertx, defaultConfig());
  }

  /**
   * @return
   */
  protected MailService mailServiceLogin() {
    return MailService.create(vertx, configLogin());
  }

  /**
   * @return
   */
  protected MailService mailServiceLogin(String user, String pw) {
    return MailService.create(vertx, configLogin(user, pw));
  }

  /**
   * @param mailService
   */
  protected void runTestException(final MailService mailService) {
    testException(mailService, exampleMessage());
  }

  /**
   * @return
   */
  protected MailService mailServiceTLS() {
    return MailService.create(vertx, configTLS());
  }

  /**
   * @return
   */
  protected MailService mailServiceNoSSL() {
    return MailService.create(vertx, configNoSSL());
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

  protected void testException(MailService mailService, MailMessage email) {
    PassOnce pass = new PassOnce(s -> fail(s));

    mailService.sendMail(email, result -> {
      log.info("mail finished");
      pass.passOnce();
      mailService.stop();
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

  protected void testSuccess(MailService mailService, MailMessage email) {
    PassOnce pass = new PassOnce(s -> fail(s));

    mailService.sendMail(email, result -> {
      log.info("mail finished");
      pass.passOnce();
      mailService.stop();
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

  protected void testSuccess(MailService mailService, MailMessage email, String message) {
    PassOnce pass = new PassOnce(s -> fail(s));

    mailService.sendMailString(email, message, result -> {
      log.info("mail finished");
      pass.passOnce();
      mailService.stop();
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

  protected void testSuccess(MailService mailService) {
    testSuccess(mailService, exampleMessage());
  }

  protected void testException(MailMessage email) {
    testException(mailServiceDefault(), email);
  }

  protected void testException(MailService mailService) {
    testException(mailService, exampleMessage());
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

  public abstract void startSMTP();

  public abstract void stopSMTP();

}

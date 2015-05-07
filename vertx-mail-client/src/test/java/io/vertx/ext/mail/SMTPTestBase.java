package io.vertx.ext.mail;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.test.core.VertxTestBase;

/**
 * Support functions for SMTP tests
 * <p>
 * the actual tests should extend a subclass
 * that starts and stops a fake smtp server
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
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
   * @param mailService
   */
  protected void runTestException(final MailClient mailService) {
    testException(mailService, exampleMessage());
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
  protected MailClient mailServiceTLSTrustAll() {
    return MailClient.create(vertx, configTLSTrustAll());
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
    return new MailConfig("localhost", 1587, StartTLSOptions.REQUIRED, LoginOption.DISABLED);
  }

  /**
   * @return
   */
  private MailConfig configTLSTrustAll() {
    return new MailConfig("localhost", 1587, StartTLSOptions.REQUIRED, LoginOption.DISABLED).setTrustAll(true);
  }

  /**
   * @return
   */
  protected MailConfig configNoSSL() {
    return new MailConfig("localhost", 1587, StartTLSOptions.DISABLED, LoginOption.DISABLED);
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
  protected MailConfig configLogin() {
    return configLogin("xxx", "yyy");
  }

  /**
   * @return
   */
  private MailConfig configLogin(String user, String pw) {
    return new MailConfig("localhost", 1587, StartTLSOptions.DISABLED, LoginOption.REQUIRED)
      .setUsername(user)
      .setPassword(pw);
  }

  /**
   * @return
   */
  protected MailMessage exampleMessage() {
    return new MailMessage("from@example.com", "user@example.com", "Subject", "Message");
  }

  protected void testException(MailClient mailService, MailMessage email) {
    PassOnce pass = new PassOnce(s -> fail(s));

    mailService.sendMail(email, result -> {
      log.info("mail finished");
      pass.passOnce();
      mailService.close();
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

  protected void testSuccess(MailClient mailService, MailMessage email) {
    testSuccess(mailService, email, (AdditionalAsserts) null);
  }


  /**
   * support running additional asserts after the sending was successfull
   * so we do not fail after we have called testComplete()
   *
   * @param mailService
   * @param email
   * @param asserts
   */
  protected void testSuccess(MailClient mailService, MailMessage email, AdditionalAsserts asserts) {
    PassOnce pass = new PassOnce(s -> fail(s));

    mailService.sendMail(email, result -> {
      log.info("mail finished");
      pass.passOnce();
      mailService.close();
      if (result.succeeded()) {
        log.info(result.result().toString());
        if (asserts != null) {
          try {
            asserts.doAsserts();
          } catch (Exception e) {
            fail(e.toString());
          }
        }
        testComplete();
      } else {
        log.warn("got exception", result.cause());
        fail(result.cause().toString());
      }
    });

    await();
  }

  protected void testSuccess(MailClient mailService) {
    testSuccess(mailService, exampleMessage());
  }

  protected void testException(MailMessage email) {
    testException(mailServiceDefault(), email);
  }

  protected void testException(MailClient mailService) {
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

}

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
  protected MailClient mailClientDefault() {
    return MailClient.create(vertx, defaultConfig());
  }

  /**
   * @return
   */
  protected MailClient mailClientLogin() {
    return MailClient.create(vertx, configLogin());
  }

  /**
   * @return
   */
  protected MailClient mailClientLogin(String user, String pw) {
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
  protected MailClient mailClientTLS() {
    return MailClient.create(vertx, configTLS());
  }

  /**
   * @return
   */
  protected MailClient mailClientTLSTrustAll() {
    return MailClient.create(vertx, configTLSTrustAll());
  }

  /**
   * @return
   */
  protected MailClient mailClientNoSSL() {
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

  static MailMessage largeMessage;

  /**
   * create a large message to be able to test timing of operations
   * the message is about 10MB and will take a few seconds to send on the fake server
   */
  protected MailMessage largeMessage() {
    // this is not thread safe, but we do not have to be
    if(largeMessage == null) {
      StringBuilder sb = new StringBuilder(50*1024*1024);
      sb.append("*************************************************\n");
      for(int i=0; i<20;i++) {
        sb.append(sb);
      }
      largeMessage =new MailMessage("from@example.com", "user@example.com", "Subject", sb.toString()); 
    }
    return largeMessage;
  }

  protected void testException(MailClient mailClient, MailMessage email) {
    PassOnce pass = new PassOnce(s -> fail(s));

    mailClient.sendMail(email, result -> {
      log.info("mail finished");
      pass.passOnce();
      mailClient.close();
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
    testSuccess(mailClient, email, (AdditionalAsserts) null);
  }


  /**
   * support running additional asserts after the sending was successfull
   * so we do not fail after we have called testComplete()
   *
   * @param mailClient
   * @param email
   * @param asserts
   */
  protected void testSuccess(MailClient mailClient, MailMessage email, AdditionalAsserts asserts) {
    PassOnce pass = new PassOnce(s -> fail(s));

    mailClient.sendMail(email, result -> {
      log.info("mail finished");
      pass.passOnce();
      mailClient.close();
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

  protected void testSuccess(MailClient mailClient) {
    testSuccess(mailClient, exampleMessage());
  }

  protected void testException(MailMessage email) {
    testException(mailClientDefault(), email);
  }

  protected void testException(MailClient mailClient) {
    testException(mailClient, exampleMessage());
  }

  protected void testSuccess(MailMessage email) {
    testSuccess(mailClientDefault(), email);
  }

  protected void testSuccess() {
    testSuccess(mailClientDefault());
  }

  protected void testException() {
    runTestException(mailClientDefault());
  }

}

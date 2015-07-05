package io.vertx.ext.mail;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.test.core.VertxTestBase;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.rules.Timeout;
import org.junit.runner.Description;

/**
 * Support functions for SMTP tests
 * <p>
 * the actual tests should extend a subclass
 * that starts and stops a fake smtp server
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
public abstract class SMTPTestBase extends VertxTestBase {

  @Rule
  public Timeout rule = Timeout.seconds(10);

  private static final Logger log = LoggerFactory.getLogger(SMTPTestBase.class);

  private long testStartTime;

  /*
   * test context the unit test this is running with
   * this is used to construct the Async objects and do the
   * assert operations
   */
  TestContext testContext;

  /**
   * @return
   */
  protected MailClient mailClientDefault() {
    return MailClient.createNonShared(vertx, defaultConfig());
  }

  /**
   * @return
   */
  protected MailClient mailClientLogin() {
    return MailClient.createNonShared(vertx, configLogin());
  }

  /**
   * @return
   */
  protected MailClient mailClientLogin(String user, String pw) {
    return MailClient.createNonShared(vertx, configLogin(user, pw));
  }

  /**
   * @param mailClient
   */
  protected void testException(final MailClient mailClient) {
    testException(mailClient, exampleMessage());
  }

  /**
   * @return
   */
  protected MailClient mailClientTLS() {
    return MailClient.createNonShared(vertx, configTLS());
  }

  /**
   * @return
   */
  protected MailClient mailClientTLSTrustAll() {
    return MailClient.createNonShared(vertx, configTLSTrustAll());
  }

  /**
   * @return
   */
  protected MailClient mailClientNoSSL() {
    return MailClient.createNonShared(vertx, configNoSSL());
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

  static volatile MailMessage largeMessage;

  /**
   * create a large message to be able to test timing of operations
   * the message is about 1MB and will take about a second to send on the fake server
   */
  protected static MailMessage largeMessage() {
    // this is not thread safe, but we do not have to be
    if (largeMessage == null) {
      StringBuilder sb = new StringBuilder(1024 * 1024);
      // 64*2^14 = 2^20
      sb.append("****************************************************************\n");
      for (int i = 0; i < 14; i++) {
        sb.append(sb);
      }
      largeMessage = new MailMessage("from@example.com", "user@example.com", "Subject", sb.toString());
    }
    return largeMessage;
  }

  protected void testException(MailClient mailClient, MailMessage email) {
    Async async = testContext.async();
    PassOnce pass = new PassOnce(s -> testContext.fail(s));

    mailClient.sendMail(email, result -> {
      log.info("mail finished");
      pass.passOnce();
      mailClient.close();
      if (result.succeeded()) {
        log.info(result.result().toString());
        testContext.fail("this test should throw an Exception");
      } else {
        log.warn("got exception", result.cause());
        async.complete();
      }
    });
  }

  protected void testSuccess(MailClient mailClient, MailMessage email) {
    testSuccess(mailClient, email, (AdditionalAsserts) null);
  }


  /**
   * support running additional asserts after sending was successful
   * so we do not fail after we have called async.complete()
   */
  protected void testSuccess(MailClient mailClient, MailMessage email, AdditionalAsserts asserts) {
    Async async = testContext.async();
    PassOnce pass = new PassOnce(s -> testContext.fail(s));

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
            testContext.fail(e);
          }
        }
        async.complete();
      } else {
        log.warn("got exception", result.cause());
        testContext.fail(result.cause());
      }
    });
  }

  protected void testSuccess(MailClient mailClient) {
    testSuccess(mailClient, exampleMessage());
  }

  protected void testException(MailMessage email) {
    testException(mailClientDefault(), email);
  }

  protected void testSuccess(MailMessage email) {
    testSuccess(mailClientDefault(), email);
  }

  protected void testSuccess() {
    testSuccess(mailClientDefault());
  }

  protected void testException() {
    testException(mailClientDefault());
  }

  @Before
  public void startCounter() {
    testStartTime = System.currentTimeMillis();
  }

  @After
  public void stopCounter() {
    final long runtime = System.currentTimeMillis() - testStartTime;
    if (runtime > 2000) {
      log.warn(this.getClass().getName()+"."+ methodName + "() test took " + runtime + "ms");
    }
  }

  private String methodName;

  @Rule
  public TestRule watcher = new TestWatcher() {
     protected void starting(Description description) {
       methodName = description.getMethodName();
     }
  };

  /**
   * Matcher assertThat, this is usually in VertxTestBase, we pass the failure to testContext
   */
  protected <T> void assertThat(T actual, Matcher<T> matcher) {
    try {
      super.assertThat(actual, matcher);
    } catch (AssertionError e) {
      testContext.fail(e);
    }
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    startSMTP();
  }

  protected abstract void startSMTP();

  @Override
  public void tearDown() throws Exception {
    stopSMTP();
    super.tearDown();
  }

  protected abstract void stopSMTP();

}

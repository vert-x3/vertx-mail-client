/*
 *  Copyright (c) 2011-2015 The original author or authors
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *       The Eclipse Public License is available at
 *       http://www.eclipse.org/legal/epl-v10.html
 *
 *       The Apache License v2.0 is available at
 *       http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 */

package io.vertx.ext.mail;

import io.vertx.core.Future;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.test.core.VertxTestBase;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Support functions for SMTP tests
 * <p>
 * the actual tests should extend a subclass
 * that starts and stops a fake smtp server
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
public abstract class SMTPTestBase extends VertxTestBase {

  // run all smtp tests with a timeout of 10 seconds
  @Rule
  public Timeout rule = Timeout.seconds(10);

  private static final Logger log = LoggerFactory.getLogger(SMTPTestBase.class);

  private long testStartTime;

  /*
   * test context the unit test this is running with
   * this is used to construct the Async objects and do the
   * assert operations
   */
  protected TestContext testContext;

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

  protected MailClient mailClientLoginWithCredentialsSupplier() {
    MailConfig config = configLogin();
    Supplier<Future<UsernamePasswordCredentials>> supplier = new Supplier<Future<UsernamePasswordCredentials>>() {

      final String username = Objects.requireNonNull(config.getUsername());
      final String password = Objects.requireNonNull(config.getPassword());

      @Override
      public Future<UsernamePasswordCredentials> get() {
        return Future.succeededFuture(new UsernamePasswordCredentials(username, password));
      }
    };
    config.setUsername(null).setPassword(null);
    return MailClient.builder(vertx).with(config).withCredentialsSupplier(supplier).build();
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
  protected void testException(final MailClient mailClient) {
    testException(mailClient, exampleMessage());
  }

  protected void testException(final MailClient mailClient, Class<? extends Exception> exceptionClass) {
    testException(mailClient, exampleMessage(), exceptionClass);
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
    testException(mailClient, email, null);
  }

  protected void testException(MailClient mailClient, MailMessage email, Class<? extends Exception> exceptionClass) {
    PassOnce pass = new PassOnce(s -> testContext.fail(s));
    mailClient.sendMail(email, testContext.asyncAssertFailure(cause -> {
      log.info("mail finished");
      pass.passOnce();
      if (exceptionClass != null && !exceptionClass.equals(cause.getClass())) {
        log.warn("got exception", cause);
        testContext.fail("didn't get expected exception " + exceptionClass + " but " + cause.getClass());
      }
      mailClient.close(testContext.asyncAssertSuccess());
    }));
  }

  protected void testSuccess(MailClient mailClient, MailMessage email) {
    testSuccess(mailClient, email, null);
  }


  /**
   * support running additional asserts after sending was successful
   * so we do not fail after we have called async.complete()
   */
  protected void testSuccess(MailClient mailClient, MailMessage email, AdditionalAsserts asserts) {
    PassOnce pass = new PassOnce(s -> testContext.fail(s));

    mailClient.sendMail(email, testContext.asyncAssertSuccess(result -> {
      log.info("mail finished");
      pass.passOnce();
      log.info(result.toString());
      if (asserts != null) {
        try {
          asserts.doAsserts();
        } catch (Exception e) {
          testContext.fail(e);
        }
      }
      mailClient.close(testContext.asyncAssertSuccess());
    }));
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

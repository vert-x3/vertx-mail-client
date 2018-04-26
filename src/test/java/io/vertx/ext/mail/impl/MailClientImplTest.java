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

/**
 *
 */
package io.vertx.ext.mail.impl;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.test.core.VertxTestBase;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
@RunWith(VertxUnitRunner.class)
public class MailClientImplTest extends VertxTestBase {

  private static final Logger log = LoggerFactory.getLogger(MailClientImplTest.class);

  @Test
  public final void testMailClientImpl(TestContext testContext) {
    MailClient mailClient = new MailClientImpl(vertx, new MailConfig(), "foo");
    testContext.assertNotNull(mailClient);
  }

  /**
   * Test method for {@link MailClientImpl#close()}.
   */
  @Test
  public final void testClose(TestContext testContext) {
    MailClient mailClient = new MailClientImpl(vertx, new MailConfig(), "foo");
    mailClient.close();
  }

  @Test(expected=IllegalStateException.class)
  public final void test2xClose(TestContext testContext) {
    MailClient mailClient = new MailClientImpl(vertx, new MailConfig(), "foo");
    mailClient.close();
    mailClient.close();
  }

  @Test
  public final void testClosedSend(TestContext testContext) {
    Async async = testContext.async();
    MailClient mailClient = new MailClientImpl(vertx, new MailConfig(), "foo");
    mailClient.close();
    mailClient.sendMail(new MailMessage(), result -> {
      if (result.succeeded()) {
        log.info(result.result().toString());
        testContext.fail("this test should throw an Exception");
      } else {
        log.warn("got exception", result.cause());
        async.complete();
      }
    });
  }

}

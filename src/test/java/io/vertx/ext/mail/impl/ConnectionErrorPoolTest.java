/*
 *  Copyright (c) 2011-2016 The original author or authors
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

package io.vertx.ext.mail.impl;

import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.SMTPTestDummy;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

/**
 * this is a test for issue https://github.com/vert-x3/vertx-mail-client/issues/77 when a connection fails (e.g. with
 * connection refused), the pool size count is not decreased so that the connection pool is full after e.g. 10 failed
 * connections. The test will just time out when the issue is present since the waiting connection never gets to run
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
@RunWith(VertxUnitRunner.class)
public class ConnectionErrorPoolTest extends SMTPTestDummy {

  @Test
  public void poolCountRisesConnRefusedTest(TestContext testContext) {
    Async async = testContext.async();

    MailClient mailClient = MailClient.create(vertx, new MailConfig("localhost", 20025).setMaxPoolSize(1));

    mailClient.sendMail(exampleMessage()).onComplete(result -> {
      testContext.assertTrue(result.failed());
      mailClient.sendMail(exampleMessage()).onComplete(result2 -> {
        testContext.assertTrue(result2.failed());
        mailClient.close().onComplete(testContext.asyncAssertSuccess(v -> async.complete()));
      });
    });
  }

  /**
   * when counting down the closed connection, make sure that we don't decrease the count twice for one connection
   */
  @Test
  public void countLessThan0Test(TestContext testContext) {
    smtpServer.setDialogue("500 connection rejected", "QUIT", "220 bye");

    // since we want to spy on connCount, we have to use MailClientImpl directly
    MailClientImpl mailClient = (MailClientImpl) MailClient.create(vertx, defaultConfig().setMaxPoolSize(1));
    SMTPConnectionPool pool = mailClient.getConnectionPool();

    testContext.assertTrue(pool.connCount()>=0, "connCount() is " + pool.connCount());

    mailClient.sendMail(exampleMessage(), testContext.asyncAssertFailure(result -> {
      testContext.assertTrue(pool.connCount()>=0, "connCount() is " + pool.connCount());
      mailClient.sendMail(exampleMessage(), testContext.asyncAssertFailure(result2 -> {
        testContext.assertTrue(pool.connCount()>=0, "connCount() is " + pool.connCount());
        mailClient.close(testContext.asyncAssertSuccess());
      }));
    }));
  }

}

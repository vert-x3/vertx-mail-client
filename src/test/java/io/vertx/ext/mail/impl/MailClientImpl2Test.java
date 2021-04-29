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

package io.vertx.ext.mail.impl;

import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.ext.mail.SMTPTestWiser;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
@RunWith(VertxUnitRunner.class)
public class MailClientImpl2Test extends SMTPTestWiser {

  private static final Logger log = LoggerFactory.getLogger(MailClientImpl2Test.class);

  /**
   * test if we can shut down the connection pool while a send operation is still running the active connection will be
   * shut down when the mail has finished sending (this is basically the same test as
   * {@link SMTPConnectionPoolShutdownTest#testCloseWhileMailActive(TestContext)} but it goes through the MailClient
   * interface and actually sends a mail)
   */
  @Test
  public final void testCloseWhileMailActive(TestContext testContext) {
    Async async = testContext.async();
    Async async2 = testContext.async();

    MailClientImpl mailClient = new MailClientImpl(vertx, configNoSSL(), "foo");

    testContext.assertEquals(0, mailClient.getConnectionPool().connCount());

    mailClient.sendMail(largeMessage(), result -> {
      log.info("mail finished");
      if (result.succeeded()) {
        log.info(result.result().toString());
        vertx.setTimer(1000, v -> {
          testContext.assertEquals(0, mailClient.getConnectionPool().connCount());
          async.complete();
        });
      } else {
        log.warn("got exception", result.cause());
        testContext.fail(result.cause());
      }
    });
    // wait a short while to allow the mail send to start
    // otherwise we shut down the connection pool before sending even starts
    vertx.setTimer(100, v1 -> {
      testContext.assertEquals(1, mailClient.getConnectionPool().connCount());
      log.info("closing mail service");
      mailClient.close();
      // after using new connection pool, pool.close() will reset size to 0 directly
      // but it won't close the SMTPConnections before current mail transaction is finished.
      // it can be tested that the sendMail still succeeds.
      testContext.assertEquals(0, mailClient.getConnectionPool().connCount());
      async2.complete();
    });
  }
}

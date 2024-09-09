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
package io.vertx.tests.mail.internal;

import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.impl.SMTPConnectionPool;
import io.vertx.tests.mail.client.SMTPTestDummy;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * a few tests using the dummy server
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
@RunWith(VertxUnitRunner.class)
public class SMTPConnectionPoolDummySMTPTest extends SMTPTestDummy {

  /**
   *
   */
  private static final String HOSTNAME = "my.hostname.com";

  private static final Logger log = LoggerFactory.getLogger(SMTPConnectionPoolDummySMTPTest.class);

  private final MailConfig config = configNoSSL();

  @Test
  public final void testGetConnectionAfterReturn(TestContext testContext) {

    smtpServer.setDialogue("220 example.com ESMTP",
        "EHLO",
        "250-example.com\n" +
          "250 SIZE 1000000",
        "RSET",
        "500 command failed");

    SMTPConnectionPool pool = new SMTPConnectionPool(vertx, config);
    Async async = testContext.async();

    testContext.assertEquals(0, pool.connCount());

    pool.getConnection(HOSTNAME).onComplete(result -> {
      if (result.succeeded()) {
        log.debug("got 1st connection");
        testContext.assertEquals(1, pool.connCount());
        result.result().returnToPool().onComplete(v -> {
          testContext.assertEquals(1, pool.connCount());
          pool.getConnection(HOSTNAME).onComplete(result2 -> {
            if (result2.succeeded()) {
              log.debug("got 2nd connection");
              testContext.assertEquals(1, pool.connCount());
              result2.result().returnToPool().onComplete(c -> pool.doClose().onComplete(vv -> {
                testContext.assertEquals(0, pool.connCount());
                async.complete();
              }));
            } else {
              log.info(result2.cause());
              testContext.fail(result2.cause());
            }
          });
        });
      } else {
        log.info(result.cause());
        testContext.fail(result.cause());
      }
    });
  }

}

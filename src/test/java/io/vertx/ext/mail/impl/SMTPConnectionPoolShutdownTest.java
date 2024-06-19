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

import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.SMTPTestWiser;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * test if we can shut down the connection pool while a send operation is still running the active connection will be
 * shut down when the mail has finished sending
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
@RunWith(VertxUnitRunner.class)
public class SMTPConnectionPoolShutdownTest extends SMTPTestWiser {

  private static final Logger log = LoggerFactory.getLogger(SMTPConnectionPoolShutdownTest.class);

  @Test
  public final void testCloseWhileMailActive(TestContext testContext) {
    Async async = testContext.async();

    MailConfig config = configNoSSL();

    SMTPConnectionPool pool = new SMTPConnectionPool(vertx, config);

    AtomicBoolean closeFinished = new AtomicBoolean(false);

    testContext.assertEquals(0, pool.connCount());

    pool.getConnection("hostname").onComplete(result -> {
      if (result.succeeded()) {
        log.debug("got connection");
        testContext.assertEquals(1, pool.connCount());
        pool.doClose().onComplete(v1 -> {
          log.debug("pool.close finished");
          closeFinished.set(true);
        });
        testContext.assertFalse(closeFinished.get(), "connection closed though it was still active");
        // the new connection pool resets size to 0 on close() directly
        testContext.assertEquals(0, pool.connCount());
        result.result().returnToPool();
        vertx.setTimer(1000, v1 -> {
          testContext.assertTrue(closeFinished.get(), "connection not closed by pool.close()");
          testContext.assertEquals(0, pool.connCount());
          async.complete();
        });
      } else {
        log.info("exception", result.cause());
        testContext.fail(result.cause());
      }
    });
  }

}

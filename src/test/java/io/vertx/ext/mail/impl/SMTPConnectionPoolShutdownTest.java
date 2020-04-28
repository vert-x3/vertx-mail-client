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

import io.vertx.core.impl.ContextInternal;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.SMTPTestWiser;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

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
    ContextInternal ctx = (ContextInternal)vertx.getOrCreateContext();
    SMTPConnectionPool pool = new SMTPConnectionPool(ctx, config);
    testContext.assertEquals(0, pool.connCount());
    pool.getConnection(ctx).onComplete(testContext.asyncAssertSuccess(conn -> {
      log.debug("got connection");
      testContext.assertEquals(1, pool.connCount());
      pool.close();
      log.debug("pool close finished");
      testContext.assertEquals(1, pool.connCount());
      // pool has been closed, so next getConnection will fail
      pool.getConnection(ctx).onComplete(testContext.asyncAssertFailure());
      conn.returnToPool();
      vertx.setTimer(100, l -> {
        testContext.assertEquals(0, pool.connCount());
        async.complete();
      });
    }));
  }

}

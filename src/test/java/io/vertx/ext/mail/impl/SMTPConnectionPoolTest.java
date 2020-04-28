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

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
@RunWith(VertxUnitRunner.class)
public class SMTPConnectionPoolTest extends SMTPTestWiser {

  private static final Logger log = LoggerFactory.getLogger(SMTPConnectionPoolTest.class);

  private final MailConfig config = configNoSSL();

  @Test
  public final void testConnectionPool(TestContext testContext) {
    Async async = testContext.async();
    ContextInternal ctx = (ContextInternal)vertx.getOrCreateContext();
    SMTPConnectionPool pool = new SMTPConnectionPool(ctx, config);
    pool.close();
    async.complete();
  }

  /**
   * Test method for get connection
   */
  @Test
  public final void testgetConnection(TestContext testContext) {
    Async async = testContext.async();
    ContextInternal ctx = (ContextInternal)vertx.getOrCreateContext();
    SMTPConnectionPool pool = new SMTPConnectionPool(ctx, config);
    testContext.assertEquals(0, pool.connCount());
    pool.getConnection(ctx).onComplete(testContext.asyncAssertSuccess(conn -> {
      testContext.assertEquals(1, pool.connCount());
      conn.returnToPool();
      testContext.assertEquals(1, pool.connCount());
      pool.close();
      vertx.setTimer(100, l -> {
        testContext.assertEquals(0, pool.connCount());
        async.complete();
      });
    }));
  }

  /**
   * test closing an used connection pool
   */
  @Test
  public final void testClose(TestContext testContext) {
    Async async = testContext.async();
    ContextInternal ctx = (ContextInternal)vertx.getOrCreateContext();
    SMTPConnectionPool pool = new SMTPConnectionPool(ctx, config);
    testContext.assertEquals(0, pool.connCount());
    pool.getConnection(ctx).onComplete(testContext.asyncAssertSuccess(conn -> {
      log.debug("have got a connection");
      testContext.assertEquals(1, pool.connCount());
      conn.returnToPool();
      testContext.assertEquals(1, pool.connCount());
      pool.close();
      vertx.setTimer(100, l -> {
        testContext.assertEquals(0, pool.connCount());
        async.complete();
      });
    }));
  }

  /**
   * test closing an empty connection pool with handler
   */
  @Test
  public final void testCloseEmptyWithHandler(TestContext testContext) {
    Async async = testContext.async();
    ContextInternal ctx = (ContextInternal)vertx.getOrCreateContext();
    SMTPConnectionPool pool = new SMTPConnectionPool(ctx, config);
    testContext.assertEquals(0, pool.connCount());
    pool.close();
    vertx.setTimer(100, l -> {
      testContext.assertEquals(0, pool.connCount());
      async.complete();
    });
  }

  /**
   * test closing an used connection pool with handler
   */
  @Test
  public final void testCloseWithHandler(TestContext testContext) {
    Async async = testContext.async();
    ContextInternal ctx = (ContextInternal)vertx.getOrCreateContext();
    SMTPConnectionPool pool = new SMTPConnectionPool(ctx, config);
    testContext.assertEquals(0, pool.connCount());
    pool.getConnection(ctx).onComplete(testContext.asyncAssertSuccess(conn -> {
      testContext.assertEquals(1, pool.connCount());
      conn.returnToPool();
      pool.close();
      vertx.setTimer(100, l -> {
        testContext.assertEquals(0, pool.connCount());
        async.complete();
      });
    }));
  }

  @Test
  public final void testAlreadyClosedgetConnection(TestContext testContext) {
    ContextInternal ctx = (ContextInternal)vertx.getOrCreateContext();
    SMTPConnectionPool pool = new SMTPConnectionPool(ctx, config);
    pool.close();
    pool.getConnection(ctx).onComplete(testContext.asyncAssertFailure());
  }

  @Test
  public final void testGet2Connections(TestContext testContext) {
    Async async = testContext.async();
    ContextInternal ctx = (ContextInternal)vertx.getOrCreateContext();
    SMTPConnectionPool pool = new SMTPConnectionPool(ctx, config);
    testContext.assertEquals(0, pool.connCount());
    pool.getConnection(ctx).onComplete(testContext.asyncAssertSuccess(conn -> {
      testContext.assertEquals(1, pool.connCount());
      pool.getConnection(ctx).onComplete(testContext.asyncAssertSuccess(conn2 -> {
        testContext.assertNotEquals(conn, conn2);
        testContext.assertEquals(2, pool.connCount());
        conn.returnToPool();
        conn2.returnToPool();
        testContext.assertEquals(2, pool.connCount());
        pool.close();
        vertx.setTimer(100, l -> {
          testContext.assertEquals(0, pool.connCount());
          async.complete();
        });
      }));
    }));
  }

  @Test
  public final void testGetConnectionAfterReturn(TestContext testContext) {
    Async async = testContext.async();
    ContextInternal ctx = (ContextInternal)vertx.getOrCreateContext();
    SMTPConnectionPool pool = new SMTPConnectionPool(ctx, config);
    testContext.assertEquals(0, pool.connCount());
    pool.getConnection(ctx).onComplete(testContext.asyncAssertSuccess(conn -> {
      log.debug("got 1st connection");
      testContext.assertEquals(1, pool.connCount());
      conn.returnToPool();
      testContext.assertEquals(1, pool.connCount());
      pool.getConnection(ctx).onComplete(testContext.asyncAssertSuccess(conn2 -> {
        log.debug("got 2nd connection");
        testContext.assertEquals(conn, conn2);
        testContext.assertEquals(1, pool.connCount());
        conn2.returnToPool();
        pool.close();
        vertx.setTimer(100, l -> {
          testContext.assertEquals(0, pool.connCount());
          async.complete();
        });
      }));
    }));
  }

  /**
   * test that we are really waiting if the connection pool is full
   */
  @Test
  public final void testWaitingForConnection(TestContext testContext) {
    final MailConfig config = configNoSSL().setMaxPoolSize(1);
    Async async = testContext.async();
    AtomicBoolean haveGotConnection = new AtomicBoolean(false);
    ContextInternal ctx = (ContextInternal)vertx.getOrCreateContext();
    SMTPConnectionPool pool = new SMTPConnectionPool(ctx, config);
    testContext.assertEquals(0, pool.connCount());
    pool.getConnection(ctx).onComplete(testContext.asyncAssertSuccess(conn -> {
      log.debug("got connection 1st tme");
      testContext.assertEquals(1, pool.connCount());
      pool.getConnection(ctx).onComplete(testContext.asyncAssertSuccess(conn2 -> {
        haveGotConnection.set(true);
        log.debug("got connection 2nd time");
        testContext.assertEquals(1, pool.connCount());
        conn2.returnToPool();
        pool.close();
        vertx.setTimer(100, l -> {
          testContext.assertEquals(0, pool.connCount());
          async.countDown();
        });
      }));
      testContext.assertFalse(haveGotConnection.get(), "got a connection on the 2nd try already");
      log.debug("didn't get a connection 2nd time yet");
      conn.returnToPool();
      vertx.setTimer(100, v -> {
        testContext.assertTrue(haveGotConnection.get(), "didn't get a connection on the 2nd try");
        log.debug("got a connection 2nd time");
        async.countDown();
      });
    }));
  }

  /**
   * test what happens if the server closes the connection while idle
   * <p>
   * (the close exception was sent to the errorHandler even though we returned the connection to the pool)
   */
  @Test
  public final void testGetConnectionClosePool(TestContext testContext) {
    ContextInternal ctx = (ContextInternal)vertx.getOrCreateContext();
    SMTPConnectionPool pool = new SMTPConnectionPool(ctx, config);
    testContext.assertEquals(0, pool.connCount());
    Async async = testContext.async();
    pool.getConnection(ctx).onComplete(testContext.asyncAssertSuccess(conn -> {
      log.debug("got connection");
      testContext.assertEquals(1, pool.connCount());
      conn.returnToPool();
      testContext.assertEquals(1, pool.connCount());
      stopSMTP(); // this closes our dummy server and consequently our connection at the server end
      pool.close();
      vertx.setTimer(100, l -> {
        testContext.assertEquals(0, pool.connCount());
        async.complete();
      });
    }));
  }

  /**
   * test that pool.close closes the idle connections
   */
  @Test
  public final void testClosePoolClosesConnection(TestContext testContext) {
    Async async = testContext.async();
    ContextInternal ctx = (ContextInternal)vertx.getOrCreateContext();
    SMTPConnectionPool pool = new SMTPConnectionPool(ctx, config);
    testContext.assertEquals(0, pool.connCount());
    pool.getConnection(ctx).onComplete(testContext.asyncAssertSuccess(conn -> {
      log.debug("got connection");
      testContext.assertEquals(1, pool.connCount());
      conn.returnToPool();
      testContext.assertEquals(1, pool.connCount());
      pool.close();
      vertx.setTimer(100, v -> {
        testContext.assertEquals(0, pool.connCount());
        async.complete();
      });
    }));
  }

  /**
   * test that we are really waiting if the connection pool is full this test has the connection pool disabled
   *
   * @param testContext
   */
  @Test
  public final void testWaitingForConnectionPoolDisabled(TestContext testContext) {
    final MailConfig config = configNoSSL().setMaxPoolSize(1).setKeepAlive(false);
    Async async = testContext.async(2);
    AtomicBoolean haveGotConnection = new AtomicBoolean(false);
    ContextInternal ctx = (ContextInternal)vertx.getOrCreateContext();
    SMTPConnectionPool pool = new SMTPConnectionPool(ctx, config);
    testContext.assertEquals(0, pool.connCount());
    pool.getConnection(ctx).onComplete(testContext.asyncAssertSuccess(conn -> {
      log.debug("got connection 1st tme");
      testContext.assertEquals(1, pool.connCount());
      pool.getConnection(ctx).onComplete(testContext.asyncAssertSuccess(conn2 -> {
        haveGotConnection.set(true);
        log.debug("got connection 2nd time");
        testContext.assertEquals(1, pool.connCount());
        conn2.returnToPool();
        pool.close();
        vertx.setTimer(100, l -> {
          testContext.assertEquals(0, pool.connCount());
          async.countDown();
        });
      }));
      testContext.assertFalse(haveGotConnection.get(), "got a connection on the 2nd try already");
      testContext.assertEquals(1, pool.connCount());
      log.debug("didn't get a connection 2nd time yet");
      conn.returnToPool();
      vertx.setTimer(100, v -> {
        testContext.assertTrue(haveGotConnection.get(), "didn't get a connection on the 2nd try");
        log.debug("got a connection 2nd time");
        testContext.assertEquals(0, pool.connCount());
        async.countDown();
      });
    }));
  }

  /**
   * test that pool.close doesn't close active connections before the operation is finished
   *
   * @param testContext
   */
  @Test
  public final void testClosePoolWaitsToCloseActive(TestContext testContext) {
    final MailConfig config = configNoSSL().setMaxPoolSize(1);
    Async async = testContext.async(2);
    ContextInternal ctx = (ContextInternal)vertx.getOrCreateContext();
    SMTPConnectionPool pool = new SMTPConnectionPool(ctx, config);
    testContext.assertEquals(0, pool.connCount());
    pool.getConnection(ctx).onComplete(testContext.asyncAssertSuccess(conn -> {
      log.debug("got connection");
      testContext.assertEquals(1, pool.connCount());
      pool.close();
      vertx.setTimer(100, l -> {
        testContext.assertEquals(0, pool.connCount());
        async.countDown();
      });
      conn.returnToPool();
      vertx.setTimer(100, v -> {
        testContext.assertEquals(0, pool.connCount());
        log.debug("connection is closed");
        async.countDown();
      });
    }));
  }
}

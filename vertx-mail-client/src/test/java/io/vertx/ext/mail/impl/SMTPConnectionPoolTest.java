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

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
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
    SMTPConnectionPool pool = new SMTPConnectionPool(vertx, config);
    pool.close(v -> async.complete());
  }

  /**
   * Test method for
   * {@link io.vertx.ext.mail.impl.SMTPConnectionPool#getConnection(io.vertx.core.Handler, io.vertx.core.Handler)} .
   */
  @Test
  public final void testGetConnection(TestContext testContext) {
    SMTPConnectionPool pool = new SMTPConnectionPool(vertx, config);
    testContext.assertEquals(0, pool.connCount());
    Async async = testContext.async();
    pool.getConnection(result -> {
      if (result.succeeded()) {
        testContext.assertEquals(1, pool.connCount());
        result.result().returnToPool();
        testContext.assertEquals(1, pool.connCount());
        pool.close(v -> {
          testContext.assertEquals(0, pool.connCount());
          async.complete();
        });
      } else {
        testContext.fail(result.cause());
      }
    });
  }

  /**
   * test closing an empty connection pool
   */
  @Test
  public final void testCloseEmpty(TestContext testContext) {
    SMTPConnectionPool pool = new SMTPConnectionPool(vertx, config);
    Async async = testContext.async();
    testContext.assertEquals(0, pool.connCount());
    pool.close();
    vertx.setTimer(1000, v -> {
      testContext.assertEquals(0, pool.connCount());
      async.complete();
    });
  }

  /**
   * test closing an used connection pool
   */
  @Test
  public final void testClose(TestContext testContext) {
    SMTPConnectionPool pool = new SMTPConnectionPool(vertx, config);
    Async async = testContext.async();
    testContext.assertEquals(0, pool.connCount());
    pool.getConnection(result -> {
      if (result.succeeded()) {
        log.debug("have got a connection");
        testContext.assertEquals(1, pool.connCount());
        result.result().returnToPool();
        testContext.assertEquals(1, pool.connCount());
        pool.close();
        vertx.setTimer(1000, v -> {
          testContext.assertEquals(0, pool.connCount());
          async.complete();
        });
      } else {
        testContext.fail(result.cause());
      }
    });
  }

  /**
   * test closing an empty connection pool with handler
   */
  @Test
  public final void testCloseEmptyWithHandler(TestContext testContext) {
    SMTPConnectionPool pool = new SMTPConnectionPool(vertx, config);
    Async async = testContext.async();
    testContext.assertEquals(0, pool.connCount());
    pool.close(v -> {
      log.info("connection pool stopped");
      testContext.assertEquals(0, pool.connCount());
      async.complete();
    });
  }

  /**
   * test closing an used connection pool with handler
   */
  @Test
  public final void testCloseWithHandler(TestContext testContext) {
    SMTPConnectionPool pool = new SMTPConnectionPool(vertx, config);
    Async async = testContext.async();
    testContext.assertEquals(0, pool.connCount());
    pool.getConnection(result -> {
      if (result.succeeded()) {
        testContext.assertEquals(1, pool.connCount());
        result.result().returnToPool();
        pool.close(v -> {
          testContext.assertEquals(0, pool.connCount());
          log.info("connection pool stopped");
          async.complete();
        });
      } else {
        testContext.fail(result.cause());
      }
    });
  }

  @Test
  public final void testAlreadyClosedGetConnection(TestContext testContext) {
    SMTPConnectionPool pool = new SMTPConnectionPool(vertx, config);
    pool.close();
    Async async = testContext.async();
    pool.getConnection(result -> {
      if (result.succeeded()) {
        testContext.fail("getConnection() should fail");
      } else {
        log.info(result.cause());
        async.complete();
      }
    });
  }

  @Test
  public final void testGet2Connections(TestContext testContext) {
    SMTPConnectionPool pool = new SMTPConnectionPool(vertx, config);
    Async async = testContext.async();
    testContext.assertEquals(0, pool.connCount());
    pool.getConnection(result -> {
      if (result.succeeded()) {
        testContext.assertEquals(1, pool.connCount());
        pool.getConnection(result2 -> {
          if (result2.succeeded()) {
            testContext.assertNotEquals(result.result(), result2.result());
            testContext.assertEquals(2, pool.connCount());
            result.result().returnToPool();
            result2.result().returnToPool();
            testContext.assertEquals(2, pool.connCount());
            pool.close(v -> {
              testContext.assertEquals(0, pool.connCount());
              async.complete();
            });
          } else {
            log.info(result2.cause());
            testContext.fail(result2.cause());
          }
        });
      } else {
        log.info(result.cause());
        testContext.fail(result.cause());
      }
    });
  }

  @Test
  public final void testGetConnectionAfterReturn(TestContext testContext) {
    SMTPConnectionPool pool = new SMTPConnectionPool(vertx, config);
    Async async = testContext.async();
    testContext.assertEquals(0, pool.connCount());
    pool.getConnection(result -> {
      if (result.succeeded()) {
        log.debug("got 1st connection");
        testContext.assertEquals(1, pool.connCount());
        result.result().returnToPool();
        testContext.assertEquals(1, pool.connCount());
        pool.getConnection(result2 -> {
          if (result.succeeded()) {
            log.debug("got 2nd connection");
            testContext.assertEquals(result.result(), result2.result());
            testContext.assertEquals(1, pool.connCount());
            result2.result().returnToPool();
            pool.close(v -> {
              testContext.assertEquals(0, pool.connCount());
              async.complete();
            });
          } else {
            log.info(result2.cause());
            testContext.fail(result2.cause());
          }
        });
      } else {
        log.info(result.cause());
        testContext.fail(result.cause());
      }
    });
  }

  /**
   * test that the pool returns the same connection when returning it once
   *
   * @param testContext
   */
  @Test
  public final void testReturnConnection(TestContext testContext) {
    SMTPConnectionPool pool = new SMTPConnectionPool(vertx, config);
    Async async = testContext.async();
    testContext.assertEquals(0, pool.connCount());
    pool.getConnection(result -> {
      if (result.succeeded()) {
        testContext.assertEquals(1, pool.connCount());
        result.result().returnToPool();
        testContext.assertEquals(1, pool.connCount());
        pool.getConnection(result2 -> {
          if (result.succeeded()) {
            testContext.assertEquals(result.result(), result2.result());
            testContext.assertEquals(1, pool.connCount());
            result2.result().returnToPool();
            pool.close(v -> {
              testContext.assertEquals(0, pool.connCount());
              async.complete();
            });
          } else {
            log.info(result2.cause());
            testContext.fail(result2.cause());
          }
        });
      } else  {
        log.info(result.cause());
        testContext.fail(result.cause());
      }
    });
  }

  /**
   * test that we are really waiting if the connection pool is full
   *
   * @param testContext
   */
  @Test
  public final void testWaitingForConnection(TestContext testContext) {
    final MailConfig config = configNoSSL().setMaxPoolSize(1);
    Async async = testContext.async();
    AtomicBoolean haveGotConnection = new AtomicBoolean(false);
    SMTPConnectionPool pool = new SMTPConnectionPool(vertx, config);
    testContext.assertEquals(0, pool.connCount());
    pool.getConnection(result -> {
      if (result.succeeded()) {
        log.debug("got connection 1st tme");
        testContext.assertEquals(1, pool.connCount());
        pool.getConnection(result2 -> {
          if (result.succeeded()) {
            haveGotConnection.set(true);
            log.debug("got connection 2nd time");
            testContext.assertEquals(1, pool.connCount());
            result2.result().returnToPool();
            pool.close(v -> {
              testContext.assertEquals(0, pool.connCount());
              async.complete();
            });
          } else {
            log.info(result2.cause());
            testContext.fail(result2.cause());
          }
        });
        testContext.assertFalse(haveGotConnection.get(), "got a connection on the 2nd try already");
        log.debug("didn't get a connection 2nd time yet");
        result.result().returnToPool();
        testContext.assertTrue(haveGotConnection.get(), "didn't get a connection on the 2nd try");
        log.debug("got a connection 2nd time");
      } else {
        log.info(result.cause());
        testContext.fail(result.cause());
      }
    });
  }

  /**
   * test what happens if the server closes the connection while idle
   * <p>
   * (the close exception was sent to the errorHandler even though we returned the connection to the pool)
   *
   * @param testContext
   */
  @Test
  public final void testGetConnectionClosePool(TestContext testContext) {
    Async async = testContext.async();
    SMTPConnectionPool pool = new SMTPConnectionPool(vertx, config);
    testContext.assertEquals(0, pool.connCount());
    pool.getConnection(result -> {
      if (result.succeeded()) {
        log.debug("got connection");
        testContext.assertEquals(1, pool.connCount());
        result.result().returnToPool();
        testContext.assertEquals(1, pool.connCount());
        stopSMTP(); // this closes our dummy server and consequently our
        // connection at the server end
        vertx.setTimer(1, v -> {
          pool.close(v2 -> {
            testContext.assertEquals(0, pool.connCount());
            async.complete();
          });
        });
      } else {
        log.info(result.cause());
        testContext.fail(result.cause());
      }
    });
  }

  /**
   * test that pool.close closes the idle connections
   *
   * @param testContext
   */
  @Test
  public final void testClosePoolClosesConnection(TestContext testContext) {
    Async async = testContext.async();
    SMTPConnectionPool pool = new SMTPConnectionPool(vertx, config);
    testContext.assertEquals(0, pool.connCount());
    pool.getConnection(result -> {
      if (result.succeeded()) {
        log.debug("got connection");
        testContext.assertEquals(1, pool.connCount());
        result.result().returnToPool();
        testContext.assertEquals(1, pool.connCount());
        pool.close();
        vertx.setTimer(1000, v -> {
          testContext.assertEquals(0, pool.connCount());
          async.complete();
        });
      } else {
        log.info(result.cause());
        testContext.fail(result.cause());
      }
    });
  }

  /**
   * test that we are really waiting if the connection pool is full this test has the connection pool disabled
   *
   * @param testContext
   */
  @Test
  public final void testWaitingForConnectionPoolDisabled(TestContext testContext) {
    final MailConfig config = configNoSSL().setMaxPoolSize(1).setKeepAlive(false);
    Async async = testContext.async();
    Async async2 = testContext.async();
    AtomicBoolean haveGotConnection = new AtomicBoolean(false);
    SMTPConnectionPool pool = new SMTPConnectionPool(vertx, config);
    testContext.assertEquals(0, pool.connCount());
    pool.getConnection(result -> {
      if (result.succeeded()) {
        log.debug("got connection 1st tme");
        testContext.assertEquals(1, pool.connCount());
        pool.getConnection(result2 -> {
          if (result2.succeeded()) {
            haveGotConnection.set(true);
            log.debug("got connection 2nd time");
            testContext.assertEquals(1, pool.connCount());
            result2.result().returnToPool();
            pool.close(v -> {
              testContext.assertEquals(0, pool.connCount());
              async.complete();
            });
          } else {
            log.info(result2.cause());
            testContext.fail(result2.cause());
          }
        });
        testContext.assertFalse(haveGotConnection.get(), "got a connection on the 2nd try already");
        testContext.assertEquals(1, pool.connCount());
        log.debug("didn't get a connection 2nd time yet");
        result.result().returnToPool();
        vertx.setTimer(1000, v -> {
          testContext.assertTrue(haveGotConnection.get(), "didn't get a connection on the 2nd try");
          log.debug("got a connection 2nd time");
          testContext.assertEquals(0, pool.connCount());
          async2.complete();
        });
      } else {
        log.info(result.cause());
        testContext.fail(result.cause());
      }
    });
  }

  /**
   * test that pool.close doesn't close active connections before the operation is finished
   *
   * @param testContext
   */
  @Test
  public final void testClosePoolWaitsToCloseActive(TestContext testContext) {
    final MailConfig config = configNoSSL().setMaxPoolSize(1);
    Async async = testContext.async();
    SMTPConnectionPool pool = new SMTPConnectionPool(vertx, config);
    testContext.assertEquals(0, pool.connCount());
    pool.getConnection(result -> {
      if (result.succeeded()) {
        log.debug("got connection");
        testContext.assertEquals(1, pool.connCount());
        pool.close();
        testContext.assertEquals(1, pool.connCount());
        final SMTPConnection conn = result.result();
        conn.returnToPool();
        vertx.setTimer(1000, v -> {
          testContext.assertTrue(conn.isClosed(), "connection was not closed");
          testContext.assertEquals(0, pool.connCount());
          log.debug("connection is closed");
          async.complete();
        });
      } else {
        log.info(result.cause());
        testContext.fail(result.cause());
      }
    });
  }
}

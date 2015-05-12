/**
 *
 */
package io.vertx.ext.mail.impl;

import java.util.concurrent.atomic.AtomicBoolean;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.mail.MailConfig;
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
public class SMTPConnectionPoolTest extends SMTPTestWiser {

  private static final Logger log = LoggerFactory.getLogger(SMTPConnectionPoolTest.class);

  private final MailConfig config = configNoSSL();

  @Test
  public final void testConnectionPool() {
    SMTPConnectionPool pool = new SMTPConnectionPool(vertx, config);
    pool.close();
  }

  /**
   * Test method for
   * {@link io.vertx.ext.mail.impl.SMTPConnectionPool#getConnection(io.vertx.core.Handler, io.vertx.core.Handler)}
   * .
   */
  @Test
  public final void testGetConnection(TestContext testContext) {
    SMTPConnectionPool pool = new SMTPConnectionPool(vertx, config);
    testContext.assertEquals(0, pool.connCount());
    Async async = testContext.async();
    pool.getConnection(conn -> {
      testContext.assertEquals(1, pool.connCount());
      pool.close();
      async.complete();
    }, throwable -> {
      testContext.fail(throwable);
    });
  }

  // FIXME - need more tests that test with different values of maxSockets, and
  // assert connCount
  // also closing connections, returning to the pool, etc, etc

  /**
   * Test method for {@link io.vertx.ext.mail.impl.SMTPConnectionPool#close()}.
   */
  @Test
  public final void testClose(TestContext testContext) {
    SMTPConnectionPool pool = new SMTPConnectionPool(vertx, config);
    Async async = testContext.async();
    pool.getConnection(conn -> {
      log.debug("have got a connection");
      conn.returnToPool();
      pool.close();
      // have to assert something here
      async.complete();
    }, th -> {
      testContext.fail(th);
    });
  }

  /**
   * Test method for
   * {@link io.vertx.ext.mail.impl.SMTPConnectionPool#close(io.vertx.core.Handler)}
   * .
   */
  @Test
  public final void testCloseWithHandler(TestContext testContext) {
    SMTPConnectionPool pool = new SMTPConnectionPool(vertx, config);
    Async async = testContext.async();
    pool.getConnection(conn -> {
      conn.returnToPool();
      pool.close(v -> {
        log.info("connection pool stopped");
        async.complete();
      });
    }, th -> {
      testContext.fail(th);
    });
  }

  @Test
  public final void testAlreadyClosedGetConnection(TestContext testContext) {
    SMTPConnectionPool pool = new SMTPConnectionPool(vertx, config);
    pool.close();
    Async async = testContext.async();
    pool.getConnection(conn -> {
      testContext.fail("this operation should fail");
    }, th -> {
      log.info(th);
      async.complete();
    });
  }

  @Test
  public final void testGet2Connections(TestContext testContext) {
    SMTPConnectionPool pool = new SMTPConnectionPool(vertx, config);
    Async async = testContext.async();
    pool.getConnection(conn -> {
      pool.getConnection(conn2 -> {
        testContext.assertNotEquals(conn, conn2);
        testContext.assertEquals(2, pool.connCount());
        pool.close();
        async.complete();
      }, th -> {
        log.info(th);
        testContext.fail(th);
      });
    }, th -> {
      log.info(th);
      testContext.fail(th);
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
    pool.getConnection(conn -> {
      conn.returnToPool();
      pool.getConnection(conn2 -> {
        testContext.assertEquals(conn, conn2);
        testContext.assertEquals(1, pool.connCount());
        conn2.returnToPool();
        pool.close();
        async.complete();
      }, th -> {
        log.info(th);
        testContext.fail(th);
      });
    }, th -> {
      log.info(th);
      testContext.fail(th);
    });
  }

  /**
   * test that we are really waiting if the connection pool is full
   * @param testContext
   */
  @Test
  public final void testWaitingForConnection(TestContext testContext) {
    final MailConfig config = configNoSSL().setMaxPoolSize(1);
    Async async = testContext.async();
    AtomicBoolean haveGotConnection = new AtomicBoolean(false);
    SMTPConnectionPool pool = new SMTPConnectionPool(vertx, config);
    pool.getConnection(conn -> {
      log.debug("got connection 1st tme");
      pool.getConnection(conn2 -> {
        haveGotConnection.set(true);
        log.debug("got connection 2nd time");
        testContext.assertEquals(1, pool.connCount());
        pool.close();
        async.complete();
      }, th -> {
        log.info(th);
        testContext.fail(th);
      });
      testContext.assertFalse(haveGotConnection.get(), "got a connection on the 2nd try already");
      log.debug("didn't get a connection 2nd time yet");
      conn.returnToPool();
      testContext.assertTrue(haveGotConnection.get(), "didn't get a connection on the 2nd try");
      log.debug("got a connection 2nd time");
    }, th -> {
      log.info(th);
      testContext.fail(th);
    });
  }

  @Test
  public final void testIdleConnectionTimeout(TestContext testContext) {
    final MailConfig config = configNoSSL().setIdleTimeout(1);
    Async async = testContext.async();
    SMTPConnectionPool pool = new SMTPConnectionPool(vertx, config);
    pool.getConnection(conn -> {
      log.debug("1st connection");
      conn.returnToPool();
      vertx.setTimer(1500, v -> {
        testContext.assertEquals(0, pool.connCount());
        log.debug("connection pool is empty");
        async.complete();
      });
    }, th -> {
      log.info(th);
      testContext.fail(th);
    });
  }

  /**
   * the close exception was sent to the errorHandler
   * even though we have returned the connection to the pool
   * @param testContext
   */
  @Test
  public final void testGetConnectionClosePool(TestContext testContext) {
    Async async = testContext.async();
    SMTPConnectionPool pool = new SMTPConnectionPool(vertx, config);
    pool.getConnection(conn -> {
      log.debug("got connection");
      conn.returnToPool();
      stopSMTP(); // this closes our dummy server and consequently our connection at the server end
      vertx.setTimer(1, v -> {
        pool.close();
        async.complete();
      });
    }, th -> {
      log.info(th);
      testContext.fail(th);
    });
  }

  @Test
  public final void testClosePoolClosesConnection(TestContext testContext) {
    Async async = testContext.async();
    SMTPConnectionPool pool = new SMTPConnectionPool(vertx, config);
    pool.getConnection(conn -> {
      log.debug("got connection");
      conn.returnToPool();
      pool.close();
      vertx.setTimer(1000, v -> {
        testContext.assertEquals(0, pool.connCount());
        async.complete();
      });
    }, th -> {
      log.info(th);
      testContext.fail(th);
    });
  }

}

/**
 *
 */
package io.vertx.ext.mail.impl;

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
   * Test method for {@link io.vertx.ext.mail.impl.SMTPConnectionPool#getConnection(io.vertx.core.Handler, io.vertx.core.Handler)}.
   */
  @Test
  public final void testGetConnection(TestContext testContext) {
    SMTPConnectionPool pool = new SMTPConnectionPool(vertx, config);
    testContext.assertEquals(0, pool.connCount());
    Async async = testContext.async();
    pool.getConnection(conn -> {
      testContext.assertEquals(1, pool.connCount());
      async.complete();
    }, throwable -> {
      testContext.fail(throwable);
    });
  }

  // FIXME - need more tests that test with different values of maxSockets, and assert connCount
  // also closing connections, returning to the pool, etc, etc

  /**
   * Test method for {@link io.vertx.ext.mail.impl.SMTPConnectionPool#close()}.
   */
  @Test
  public final void testStop(TestContext testContext) {
    SMTPConnectionPool pool = new SMTPConnectionPool(vertx, config);
    Async async = testContext.async();
    pool.getConnection(conn -> {
      log.debug("have got a connection");
      conn.returnToPool();
      pool.close();
      async.complete();
    }, th -> {
      testContext.fail(th);
    });
  }

  /**
   * Test method for {@link io.vertx.ext.mail.impl.SMTPConnectionPool#close(io.vertx.core.Handler)}.
   */
  @Test
  public final void testStopHandlerOfVoid(TestContext testContext) {
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
  public final void testStoppedGetConnection(TestContext testContext) {
    SMTPConnectionPool pool = new SMTPConnectionPool(vertx, config);
    pool.close();
    Async async = testContext.async();
    pool.getConnection(conn -> {
      testContext.fail("this operation should fail");
      async.complete();
    }, th -> {
      log.info(th);
      async.complete();
    });
  }

}

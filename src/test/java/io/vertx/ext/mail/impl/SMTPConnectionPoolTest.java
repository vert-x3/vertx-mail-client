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
 *
 */
@RunWith(VertxUnitRunner.class)
public class SMTPConnectionPoolTest extends SMTPTestWiser {

  private static final Logger log = LoggerFactory.getLogger(SMTPConnectionPoolTest.class);

  private final MailConfig config = configNoSSL();

  /**
   * Test method for {@link io.vertx.ext.mail.impl.SMTPConnectionPool#ConnectionPool(io.vertx.core.Vertx, io.vertx.ext.mail.MailConfig, io.vertx.core.Context)}.
   */
  @Test
  public final void testConnectionPool() {
    SMTPConnectionPool pool = new SMTPConnectionPool(vertx, config, vertx.getOrCreateContext());
    pool.stop();
  }

  /**
   * Test method for {@link io.vertx.ext.mail.impl.SMTPConnectionPool#getConnection(io.vertx.core.Handler, io.vertx.core.Handler)}.
   */
  @Test
  public final void testGetConnection(TestContext testContext) {
    SMTPConnectionPool pool = new SMTPConnectionPool(vertx, config, vertx.getOrCreateContext());
    Async async = testContext.async();
    pool.getConnection(r -> {
      async.complete();
    }, throwable -> {
      testContext.fail(throwable);
    });
  }

  /**
   * Test method for {@link io.vertx.ext.mail.impl.SMTPConnectionPool#stop()}.
   */
  @Test
  public final void testStop(TestContext testContext) {
    SMTPConnectionPool pool = new SMTPConnectionPool(vertx, config, vertx.getOrCreateContext());
    Async async = testContext.async();
    pool.getConnection(conn -> {
      log.debug("have got a connection");
      conn.returnToPool();
      pool.stop();
      async.complete();
    }, th -> {
      testContext.fail(th);
    });
  }

  /**
   * Test method for {@link io.vertx.ext.mail.impl.SMTPConnectionPool#stop(io.vertx.core.Handler)}.
   */
  @Test
  public final void testStopHandlerOfVoid(TestContext testContext) {
    SMTPConnectionPool pool = new SMTPConnectionPool(vertx, config, vertx.getOrCreateContext());
    Async async = testContext.async();
    pool.getConnection(conn -> {
      conn.returnToPool();
      pool.stop(v -> {
        log.info("connection pool stopped");
        async.complete();
      });
    }, th -> {
      testContext.fail(th);
    });
  }

  @Test
  public final void testStoppedGetConnection(TestContext testContext) {
    SMTPConnectionPool pool = new SMTPConnectionPool(vertx, config, vertx.getOrCreateContext());
    pool.stop();
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

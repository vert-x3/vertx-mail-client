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

    pool.getConnection(result -> {
      if (result.succeeded()) {
        log.debug("got connection");
        testContext.assertEquals(1, pool.connCount());
        pool.close(v1 -> {
          log.debug("pool.close finished");
          closeFinished.set(true);
        });
        testContext.assertFalse(closeFinished.get(), "connection closed though it was still active");
        testContext.assertEquals(1, pool.connCount());
        result.result().returnToPool();
        vertx.setTimer(1000, v -> {
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

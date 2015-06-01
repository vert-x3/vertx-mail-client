/**
 *
 */
package io.vertx.ext.mail.impl;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.SMTPTestDummy;
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

  private static final Logger log = LoggerFactory.getLogger(SMTPConnectionPoolDummySMTPTest.class);

  private final MailConfig config = configNoSSL();

  @Test
  public final void testGetConnectionAfterReturn(TestContext testContext) {

    smtpServer.setDialogue("220 example.com ESMTP",
        "EHLO",
        "250-example.com\n" +
          "250-SIZE 1000000\n" +
          "250 PIPELINING",
        "MAIL FROM:",
        "250 2.1.0 Ok",
        "RCPT TO:",
        "250 2.1.5 Ok",
        "DATA",
        "354 End data with <CR><LF>.<CR><LF>",
        "250 2.0.0 Ok: queued as ABCDDEF0123456789",
        "RSET",
        "500 command failed");

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
          if (result2.succeeded()) {
            log.debug("got 2nd connection");
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

}

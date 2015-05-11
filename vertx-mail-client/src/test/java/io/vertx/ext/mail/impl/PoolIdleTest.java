package io.vertx.ext.mail.impl;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.PassOnce;
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
public class PoolIdleTest extends SMTPTestWiser {

  private static final Logger log = LoggerFactory.getLogger(PoolIdleTest.class);

  @Test
  public void mailTest(TestContext context) {
    final MailConfig config = configNoSSL().setMaxPoolSize(1).setIdleTimeout(1);
    final TestMailClient mailClient = new TestMailClient(vertx, config);
    Async async = context.async();

    MailMessage email = exampleMessage();

    PassOnce pass = new PassOnce(s -> context.fail(s));

    mailClient.sendMail(email, result -> {
      log.info("mail finished");
      pass.passOnce();
      if (result.succeeded()) {
        log.info(result.result().toString());
        // connection pool has 1 second idle timeout so the connection should be closed after 1,5s
        vertx.setTimer(1500, v -> {
          context.assertEquals(0, mailClient.getConnectionPool().connCount());
          mailClient.close();
          async.complete();
        });
      } else {
        log.warn("got exception", result.cause());
        context.fail(result.cause());
      }
    });
  }

}

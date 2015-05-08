package io.vertx.ext.mail;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
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
    final MailConfig config = configNoSSL().setMaxPoolSize(1);
    final MailClient mailClient = MailClient.create(vertx, config);
    Async async = context.async();

    MailMessage email = exampleMessage();

    PassOnce pass = new PassOnce(s -> context.fail(s));

    mailClient.sendMail(email, result -> {
      log.info("mail finished");
      pass.passOnce();
      if (result.succeeded()) {
        log.info(result.result().toString());
        log.debug("waiting for 20 seconds");
        //FIXME - 20 seconds waits in testsuites suck!
        vertx.setTimer(2000, v -> {
          log.debug("timer finished");
          async.complete();
        });
      } else {
        log.warn("got exception", result.cause());
        context.fail(result.cause());
      }
    });
  }

}

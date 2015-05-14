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
 * do pool tests via the MailClient interface
 * its a bit difficult to assert state of the implementation from the interface so we use a delegate implementation
 * that exposes a few getters for unit tests
 * this class is in the impl package so we can use package local classes and methods
 * <p>
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
@RunWith(VertxUnitRunner.class)
public class PoolDisabledTest extends SMTPTestWiser {

  private static final Logger log = LoggerFactory.getLogger(PoolDisabledTest.class);

  @Test
  public void mailTest(TestContext context) {
    final MailConfig config = configNoSSL().setKeepAlive(false);
    final TestMailClient mailClient = new TestMailClient(vertx, config);
    Async async = context.async();

    MailMessage email = exampleMessage();

    PassOnce pass = new PassOnce(s -> context.fail(s));

    mailClient.sendMail(email, result -> {
      log.info("mail finished");
      pass.passOnce();
      if (result.succeeded()) {
        log.info(result.result().toString());
        if (mailClient.getConnectionPool().connCount() > 0) {
          vertx.setTimer(1000, v -> {
            if (mailClient.getConnectionPool().connCount() > 0) {
              context.fail("smtp connection wasn't closed");
            } else {
              log.debug("connection has been closed after wait");
              mailClient.close();
              async.complete();
            }
          });
        } else {
          log.debug("connection has been closed");
          async.complete();
        }
      } else {
        log.warn("got exception", result.cause());
        context.fail(result.cause());
      }
    });
  }

}

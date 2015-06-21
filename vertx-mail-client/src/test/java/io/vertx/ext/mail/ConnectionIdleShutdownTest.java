package io.vertx.ext.mail;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.mail.impl.TestMailClient;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test that idle timeout occurs on an idle connection
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
@RunWith(VertxUnitRunner.class)
public class ConnectionIdleShutdownTest extends SMTPTestDummy {

  private static final Logger log = LoggerFactory.getLogger(ConnectionIdleShutdownTest.class);

  MailClient mailClient;

  @Test
  public void mailIdleTimemout(TestContext context) {
    smtpServer.setCloseImmediately(true);
    Async async = context.async();

    MailConfig mailConfig = configNoSSL().setIdleTimeout(1);

    TestMailClient mailClient = new TestMailClient(vertx, mailConfig);

    MailMessage email = exampleMessage();

    PassOnce passOnce = new PassOnce(s -> context.fail(s));

    context.assertEquals(0, mailClient.connCount());

    mailClient.sendMail(email, result -> {
      context.assertEquals(1, mailClient.connCount());
      log.info("mail finished");
      passOnce.passOnce();
      if (result.succeeded()) {
        log.info(result.result().toString());
        vertx.setTimer(1100, v -> {
          context.assertEquals(0, mailClient.connCount());
          mailClient.close();
          log.info("client closed");
          async.complete();
        });
      } else {
        log.warn("got exception", result.cause());
        context.fail(result.cause());
      }
    });
  }

}

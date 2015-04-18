/**
 * 
 */
package io.vertx.ext.mail.impl;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.MailService;
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
public class MailServiceImplTest2 extends SMTPTestWiser {

  private static final Logger log = LoggerFactory.getLogger(MailServiceImplTest2.class);

  /**
   * test if we can shut down the connection pool while a send operation is
   * still running the active connection will be shut down when the mail has
   * finished sending (this is basically the same test as
   * {@link ConnectionPoolShutdownTest#testStopWhileMailActive(TestContext)} but
   * it goes through the MailService interface and it actually sends a mail)
   */
  @Test
  public final void testStopWhileMailActive(TestContext testContext) {
    Async async = testContext.async();
    Vertx vertx = Vertx.vertx();

    vertx.runOnContext(v -> {
      MailConfig config = configNoSSL();

      MailService mailService = MailService.create(vertx, config);

      // send large mail so we some time to call the .stop() method
      StringBuilder sb = new StringBuilder();
      sb.append("*************************************************\n");
      for (int i = 0; i < 20; i++) {
        sb.append(sb);
      }
      String text = sb.toString();

      MailMessage email = new MailMessage("from@example.com", "user@example.com", "Subject", text);

      mailService.sendMail(email, result -> {
        log.info("mail finished");
        if (result.succeeded()) {
          log.info(result.result().toString());
          async.complete();
        } else {
          log.warn("got exception", result.cause());
          testContext.fail(result.cause().toString());
        }
      });
      // wait a short while to allow the mail send to start
      // otherwise we shut down the connection pool before sending even starts
      vertx.setTimer(100, v1 -> {
        log.info("stopping mail service");
        mailService.stop();
      });
    });
  }

}

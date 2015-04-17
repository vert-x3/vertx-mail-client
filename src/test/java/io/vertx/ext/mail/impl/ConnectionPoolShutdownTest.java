package io.vertx.ext.mail.impl;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.mail.LoginOption;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.MailService;
import io.vertx.ext.mail.SMTPTestWiser;
import io.vertx.ext.mail.StarttlsOption;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * test if we can shut down the connection pool while a send operation is still running
 * the active connection will be shut down when the mail has finished sending
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
@RunWith(VertxUnitRunner.class)
public class ConnectionPoolShutdownTest extends SMTPTestWiser {

  private static final Logger log = LoggerFactory.getLogger(ConnectionPoolShutdownTest.class);

  @Test
  public final void testStopWhileMailActive(TestContext testContext) {

    Vertx vertx = Vertx.vertx();
    MailConfig config = configNoSSL();

    MailService mailService = MailService.create(vertx, config);

    Async async = testContext.async();

    StringBuilder sb = new StringBuilder();
    sb.append("*************************************************\n");
    for(int i=0; i<20;i++) {
      sb.append(sb);
    }
    String text=sb.toString();

    MailMessage email = new MailMessage("from@example.com", "user@example.com", "Subject", text);

    mailService.sendMail(email, result -> {
      log.info("mail finished");
      mailService.stop();
      if (result.succeeded()) {
        log.info(result.result().toString());
        async.complete();
      } else {
        log.warn("got exception", result.cause());
        testContext.fail(result.cause().toString());
      }
    });
    // wait a short while to allow the mail send to start
    // otherwise we have shut down the connection pool before it even starts
    vertx.setTimer(100, v -> {
      log.info("stopping mailService");
      mailService.stop();
    });
  }

}

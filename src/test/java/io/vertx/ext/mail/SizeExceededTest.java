package io.vertx.ext.mail;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.test.core.VertxTestBase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 *         this test uses a message that exceeds the SIZE limit of the smtp
 *         server (uses the mockup server that just plays a file)
 */
public class SizeExceededTest extends VertxTestBase {

  private static final Logger log = LoggerFactory.getLogger(SizeExceededTest.class);

  @Test
  public void mailTest() throws InterruptedException {
    log.info("starting");

    MailConfig mailConfig = new MailConfig("localhost", 1587);

    MailService mailService = MailService.create(vertx, mailConfig);

    // message to exceed SIZE limit (1000000 for our server)
    // 32 Bytes
    StringBuilder sb = new StringBuilder("*******************************\n");
    // multiply by 2**15
    for (int i = 0; i < 15; i++) {
      sb.append(sb);
    }
    String message = sb.toString();

    log.info("message size is " + message.length());

    MailMessage email = new MailMessage("user@example.com", "user@example.com", "Subject", message);

    PassOnce pass = new PassOnce(s -> fail(s));

    mailService.sendMail(email, result -> {
      log.info("mail finished");
      pass.passOnce();
      if (result.succeeded()) {
        log.info(result.result().toString());
        fail("this test should throw an Exception");
      } else {
        log.info("got exception", result.cause());
        testComplete();
      }
    });

    await();
  }

  TestSmtpServer smtpServer;

  @Before
  public void startSMTP() {
    smtpServer = new TestSmtpServer(vertx);
  }

  @After
  public void stopSMTP() {
    smtpServer.stop();
  }

}

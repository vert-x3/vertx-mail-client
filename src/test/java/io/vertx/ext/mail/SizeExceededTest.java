package io.vertx.ext.mail;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;

import org.junit.Test;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 *         this test uses a message that exceeds the SIZE limit of the smtp
 *         server (uses the mockup server that just plays a file)
 */
public class SizeExceededTest extends SMTPTestDummy {

  private static final Logger log = LoggerFactory.getLogger(SizeExceededTest.class);

  @Test
  public void mailTest() throws InterruptedException {
    log.info("starting");

    // message to exceed SIZE limit (1000000 for our server)
    // 32 Bytes
    StringBuilder sb = new StringBuilder("*******************************\n");
    // multiply by 2**15
    for (int i = 0; i < 15; i++) {
      sb.append(sb);
    }
    String message = sb.toString();

    log.info("message size is " + message.length());

    testException(new MailMessage("user@example.com", "user@example.com", "Subject", message));
  }

}

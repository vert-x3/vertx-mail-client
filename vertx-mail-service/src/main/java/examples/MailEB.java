package examples;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;

/**
 * send a mail via event bus to the mail service running on another machine
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
public class MailEB extends AbstractVerticle {

  private static final Logger log = LoggerFactory.getLogger(MailEB.class);

  public void start() {
//    MailService mailService = MailService.createEventBusProxy(vertx, "vertx.mail");
//
//    MailMessage email = new MailMessage()
//      .setBounceAddress("bounce@example.com")
//      .setTo("user@example.com")
//      .setSubject("this message has no content at all");
//
//    mailService.sendMail(email, result -> {
//      log.info("mail finished");
//      if (result.succeeded()) {
//        log.info(result.result().toString());
//      } else {
//        log.warn("got exception", result.cause());
//      }
//    });
  }

}

package examples;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.MailService;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
public class Examples {

  Logger log=LoggerFactory.getLogger(Examples.class);

  public void example1(Vertx vertx) {
    MailConfig mailConfig = new MailConfig("localhost", 587)
        .setUsername("username")
        .setPassword("password");

    MailService mailService = MailService.create(vertx, mailConfig);

    MailMessage email = new MailMessage()
      .setFrom("address@example.com")
      .setTo("address@example.com")
      .setSubject("meaningful subject")
      .setText("this is a message");

    mailService.sendMail(email, result -> {
      if(result.succeeded()) {
        log.info(result.result().toString());
      } else {
        log.warn("got exception", result.cause());
      }
    });
  }
}

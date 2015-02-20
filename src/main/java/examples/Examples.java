package examples;

import io.vertx.core.Vertx;
import io.vertx.docgen.Source;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.MailService;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
@Source
public class Examples {

  public void example1(Vertx vertx) {
    MailConfig mailConfig = new MailConfig()
        .setHostname("mail.example.com")
        .setPort(587)
        .setUsername("user")
        .setPassword("pw");

    MailService mailService = MailService.create(vertx, mailConfig);

    MailMessage email = new MailMessage()
        .setFrom("address@example.com")
        .setTo("address@example.com")
        .setSubject("meaningful subject")
        .setText("this is a message")
        .setHtml("HTML message <a href=\"http://vertx.io\">vertx</a>");

    mailService.sendMail(email, result -> {
      if (result.succeeded()) {
        System.out.println(result.result());
      } else {
        System.out.println("got exception");
        result.cause().printStackTrace();
      }
    });
  }
}

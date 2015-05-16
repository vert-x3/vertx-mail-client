package examples;

import io.vertx.core.Vertx;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.MailService;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class Examples {

  public void createService(Vertx vertx) {
    MailService mailService = MailService.createEventBusProxy(vertx, "vertx.mail");
  }

  public void example3(Vertx vertx) {
    MailService mailService = MailService.createEventBusProxy(vertx, "vertx.mail");

    MailMessage email=new MailMessage()
    .setFrom("user@example.com")
    .setBounceAddress("bounce@example.com")
    .setTo("user@example.com");

    mailService.sendMail(email, result -> {
      System.out.println("mail finished");
      if (result.succeeded()) {
        System.out.println(result.result());
      } else {
        System.out.println("got exception");
        result.cause().printStackTrace();
      }
    });
  }

}

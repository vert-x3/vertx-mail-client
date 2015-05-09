package examples;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.docgen.Source;
import io.vertx.ext.mail.MailAttachment;
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

  public void example2(Vertx vertx) {
    // default config will use localhost:25
    MailConfig mailConfig = new MailConfig();

    MailService mailService = MailService.create(vertx, mailConfig);

    MailMessage email = new MailMessage()
    .setFrom("address@example.com")
    .setTo("address@example.com")
    .setSubject("your file")
    .setText("please take a look at the attached file");

    MailAttachment attachment = new MailAttachment()
    .setName("file.dat")
    .setData(Buffer.buffer("ASDF1234\0\u0001\u0080\u00ff\n"));

    email.setAttachment(attachment);

    mailService.sendMail(email, result -> {
      if (result.succeeded()) {
        System.out.println(result.result());
      } else {
        System.out.println("got exception");
        result.cause().printStackTrace();
      }
    });
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

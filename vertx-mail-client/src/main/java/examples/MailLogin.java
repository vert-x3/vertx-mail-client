package examples;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.mail.*;

import java.util.ArrayList;
import java.util.List;

/**
 * send a mail via a smtp service requiring TLS and Login
 * we use an attachment and a text/html alternative mail body
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
public class MailLogin extends AbstractVerticle {

  private static final Logger log = LoggerFactory.getLogger(MailLogin.class);

  public void start() {
    MailConfig mailConfig = new MailConfig("smtp.example.com", 587, StartTLSOptions.REQUIRED, LoginOption.REQUIRED)
      .setUsername("username")
      .setPassword("password");

    MailClient mailClient = MailClient.create(vertx, mailConfig);

    Buffer image = vertx.fileSystem().readFileBlocking("logo-white-big.png");

    MailMessage email = new MailMessage()
      .setFrom("user1@example.com")
      .setTo("user2@example.com")
      .setCc("user3@example.com")
      .setBcc("user4@example.com")
      .setBounceAddress("bounce@example.com")
      .setSubject("Test email with HTML")
      .setText("this is a message")
      .setHtml("<a href=\"http://vertx.io\">vertx.io</a>");

    List<MailAttachment> list = new ArrayList<MailAttachment>();

    list.add(new MailAttachment()
      .setData(image)
      .setName("logo-white-big.png")
      .setContentType("image/png")
      .setDisposition("inline")
      .setDescription("logo of vert.x web page"));

    email.setAttachment(list);

    mailClient.sendMail(email, result -> {
      log.info("mail finished");
      if (result.succeeded()) {
        log.info(result.result().toString());
      } else {
        log.warn("got exception", result.cause());
      }
    });
  }

}

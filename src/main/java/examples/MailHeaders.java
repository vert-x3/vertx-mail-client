package examples;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.CaseInsensitiveHeaders;
import io.vertx.ext.mail.LoginOption;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.MailService;
import io.vertx.ext.mail.StarttlsOption;

import java.util.Arrays;

/**
 * send a mail via a smtp service using SSL
 *
 * we add a header line to message header
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
public class MailHeaders extends AbstractVerticle {

  public void start() {
    MailConfig mailConfig = new MailConfig("smtp.example.com", 465, StarttlsOption.DISABLED, LoginOption.DISABLED)
      .setSsl(true);

    MailService mailService = MailService.create(vertx, mailConfig);

    MailMessage email = new MailMessage()
      .setFrom("user1@example.com")
      .setTo(Arrays.asList("user2@example.com",
          "user3@example.com",
          "user4@example.com"))
      .setHeaders(new CaseInsensitiveHeaders().add("X-Header", "this header probably means something special"))
      .setText("this is a test mail from vertx <http://vertx.io/>\n");

    mailService.sendMail(email, result -> {
      System.out.println("mail is finished");
    });
  }

}

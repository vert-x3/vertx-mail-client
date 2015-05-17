package examples;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.docgen.Source;
import io.vertx.ext.mail.MailAttachment;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;

/**
 * code chunks for the adoc documentation 
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
@Source
public class Examples {

  public void createClient(Vertx vertx) {
    MailConfig config = new MailConfig();
    MailClient mailClient = MailClient.create(vertx, config);
  }

//  public void createService(Vertx vertx) {
//    MailService mailService = MailService.createEventBusProxy(vertx, "vertx.mail");
//  }

  public void mailMessage(Vertx vertx) {
    MailMessage message = new MailMessage();
    message.setFrom("user@example.com (Example User)");
    message.setTo("recipient@example.org");
    message.setCc("Another User <another@example.net>");
    message.setText("this is the plain message text");
    message.setHtml("this is html text <a href=\"\">vertx.io</a>");
  }

  public void attachment(Vertx vertx, MailMessage message) {
    MailAttachment attachment = new MailAttachment();
    attachment.setContentType("text/plain");
    attachment.setData(Buffer.buffer("attachment file"));

    message.setAttachment(attachment);
  }

  public void sendMail(Vertx vertx, MailMessage message, MailClient mailClient) {
    mailClient.sendMail(message, result -> {
      if(result.succeeded()) {
        System.out.println(result.result());
      } else {
        result.cause().printStackTrace();
      }
    });
  }

}

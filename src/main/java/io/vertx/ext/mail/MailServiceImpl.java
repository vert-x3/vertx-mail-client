package io.vertx.ext.mail;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.mail.mailutil.MyHtmlEmail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.activation.DataSource;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
public class MailServiceImpl implements MailService {

  private static final Logger log = LoggerFactory.getLogger(MailServiceImpl.class);

  private Vertx vertx;
  private MailConfig config;

  private String username;
  private String password;

  private String hostname;

  private int port;

  public MailServiceImpl(Vertx vertx, MailConfig config) {
    this.vertx = vertx;
    this.config = config;
    username = config.getUsername();
    password = config.getPassword();
    hostname = config.getHostname();
    port = config.getPort();
  }

  @Override
  public void sendMailString(String email, Handler<AsyncResult<JsonObject>> resultHandler) {
    // not yet implemented
  }

  @Override
  public void start() {
    // may take care of validating the options
    // and configure a queue if we implement one
    log.debug("mail service started");
  }

  @Override
  public void stop() {
    // may shut down the queue, if we implement one
    log.debug("mail service stopped");
  }

  @SuppressWarnings("unchecked")
  @Override
  public void sendMail(JsonObject emailJson, Handler<AsyncResult<JsonObject>> resultHandler) {
    try {
      String text = emailJson.getString("text");
      String html = emailJson.getString("html");
      String bounceAddress = emailJson.getString("bounceAddress");
      String from = emailJson.getString("from");
      List<InternetAddress> tos = new ArrayList<InternetAddress>();
      if (emailJson.containsKey("recipient")) {
        tos.add(new InternetAddress(emailJson.getString("recipient")));
      } else if (emailJson.containsKey("recipients")) {
        for (String r : (List<String>) emailJson.getJsonArray("recipients").getList()) {
          tos.add(new InternetAddress(r));
        }
      }

      if (from == null || tos.size() == 0) {
        throw new EmailException("from or to addresses missing");
      }

      HtmlEmail email = new MyHtmlEmail();
      email.setFrom(from);
      email.setTo(tos);
      email.setSubject(emailJson.getString("subject"));
      email.setBounceAddress(bounceAddress);
      if(text!=null) {
        email.setTextMsg(text);
      }
      if(html!=null) {
        email.setHtmlMsg(html);
      }

      JsonObject attachment=emailJson.getJsonObject("attachment");

      if(attachment!=null) {
        DataSource attachmentDS=createDS(attachment);
        final String disposition=attachment.getString("disposition");
        final String name = attachment.getString("name");
        final String description = attachment.getString("description");
        if(disposition!=null) {
          email.attach(attachmentDS, name, description, disposition);
        } else {
          email.attach(attachmentDS, name, description);
        }
      }

      // use optional as default, this way we do opportunistic TLS unless
      // disabled
      final StarttlsOption starttls = config.getStarttls();
      if (starttls == StarttlsOption.OPTIONAL) {
        email.setStartTLSEnabled(true);
      }
      if (starttls == StarttlsOption.REQUIRED) {
        email.setStartTLSRequired(true);
      }

      if (config.isSsl()) {
        email.setSSLOnConnect(true);
      }

      email.setHostName(hostname);
      email.setSmtpPort(port);

      MailVerticle mailVerticle = new MailVerticle(vertx, resultHandler);
      mailVerticle.sendMail(email, username, password, config.getLogin());
    } catch (EmailException | AddressException e) {
      resultHandler.handle(Future.failedFuture(e));
    }
  }

  private DataSource createDS(JsonObject attachment) {
    String name=attachment.getString("name");
    String contentType=attachment.getString("content-type");
    byte[] bytes=attachment.getBinary("data");
    return new DataSource() {
      
      @Override
      public OutputStream getOutputStream() throws IOException {
        throw new IOException("read only");
      }
      
      @Override
      public String getName() {
        return name;
      }
      
      @Override
      public InputStream getInputStream() throws IOException {
        // TODO Auto-generated method stub
        return new ByteArrayInputStream(bytes);
      }
      
      @Override
      public String getContentType() {
        return contentType;
      }
    };
  }

  // @Override
  // public void sendMail(Email email, Handler<AsyncResult<String>>
  // resultHandler) {
  // }

}

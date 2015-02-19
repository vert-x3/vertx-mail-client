package io.vertx.ext.mail;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.activation.DataSource;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;

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

  private Email createPlainMail(MailMessage message) throws EmailException {
    MySimpleEmail email = new MySimpleEmail();
    String text = message.getText();
    if (text != null) {
      email.setMsg(text);
    }
    return email;
  }

  private Email createHtmlMail(MailMessage message) throws EmailException {
    MyHtmlEmail email = new MyHtmlEmail();
    String text = message.getText();
    String html = message.getHtml();
    if (text != null) {
      email.setTextMsg(text);
    }
    if (html != null) {
      email.setHtmlMsg(html);
    }

    List<MailAttachment> list = message.getAttachment();
    if (list != null) {
      for (MailAttachment attachment : list) {
        DataSource attachmentDS = createDS(attachment);
        final String disposition = attachment.getDisposition();
        final String name = attachment.getName();
        final String description = attachment.getDescription();
        if (disposition != null) {
          email.attach(attachmentDS, name, description, disposition);
        } else {
          email.attach(attachmentDS, name, description);
        }
      }
    }

    return email;
  }

  private boolean isPlainMail(MailMessage message) {
    return message.getHtml() == null && message.getAttachment() == null;
  }

  private DataSource createDS(MailAttachment attachment) {
    final String name = attachment.getName() == null ? "" : attachment.getName();
    final String contentType = attachment.getContentType() == null ? "application/octet-stream" : attachment
        .getContentType();

    String bytes = attachment.getData();

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
        return new ByteArrayInputStream(bytes.getBytes("iso-8859-1"));
      }

      @Override
      public String getContentType() {
        return contentType;
      }
    };
  }

  @Override
  public MailService sendMail(MailMessage message, Handler<AsyncResult<JsonObject>> resultHandler) {
    try {
      String bounceAddress = message.getBounceAddress();
      String from = message.getFrom();
      List<InternetAddress> tos = new ArrayList<InternetAddress>();
      if (message.getTo() != null) {
        for (String r : message.getTo()) {
          tos.add(new InternetAddress(r));
        }
      }
      List<InternetAddress> ccs = new ArrayList<InternetAddress>();
      if (message.getCc() != null) {
        for (String r : message.getCc()) {
          ccs.add(new InternetAddress(r));
        }
      }
      List<InternetAddress> bccs = new ArrayList<InternetAddress>();
      if (message.getBcc() != null) {
        for (String r : message.getBcc()) {
          bccs.add(new InternetAddress(r));
        }
      }

      if (from == null || tos.isEmpty() && ccs.isEmpty() && bccs.isEmpty()) {
        throw new EmailException("from or to addresses missing");
      }

      Email email;

      // create a Email object that can handle the elements we want
      if (isPlainMail(message)) {
        email = createPlainMail(message);
      } else {
        email = createHtmlMail(message);
      }
      // the rest of the settings are all available in the Email base class
      email.setFrom(from);
      if (!tos.isEmpty()) {
        email.setTo(tos);
      }
      if (!ccs.isEmpty()) {
        email.setCc(ccs);
      }
      if (!bccs.isEmpty()) {
        email.setBcc(bccs);
      }
      email.setSubject(message.getSubject());
      email.setBounceAddress(bounceAddress);

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

      MailMain mailVerticle = new MailMain(vertx, resultHandler);
      mailVerticle.sendMail(email, username, password, config.getLogin());
    } catch (EmailException | AddressException e) {
      resultHandler.handle(Future.failedFuture(e));
    }
    return this;
  }

}

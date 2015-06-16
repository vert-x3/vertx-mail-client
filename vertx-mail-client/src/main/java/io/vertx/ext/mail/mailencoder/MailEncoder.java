package io.vertx.ext.mail.mailencoder;

import io.vertx.core.MultiMap;
import io.vertx.core.http.CaseInsensitiveHeaders;
import io.vertx.ext.mail.MailAttachment;
import io.vertx.ext.mail.MailMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * create MIME mail messages from a MailMessage object
 * <p>
 * example usage is:
 * <p>
 * 
 * <pre>
 * {@code
 * MailMessage = new MailMessage();
 * (set elements and attachments ...)
 * String message = new MailEncoder(mailmessage).encode();
 * }
 * </pre>
 * <p>
 * usually you are not using this class directly, rather it will be used by {@code sendMail()} in MailClientImpl
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
public class MailEncoder {

  private final MailMessage message;
  private final String hostname;

  private String messageID;

  /**
   * create a MailEncoder for the message
   * <p>
   * The class will probably get a few setters for optional features of the SMTP protocol later e.g. 8BIT or SMTPUTF
   * (this is not yet supported)
   *
   * @param message the message to encode later
   * @param hostname the hostname to be used in message-id or null to get hostname from OS network config
   */
  public MailEncoder(MailMessage message, String hostname) {
    this.message = message;
    this.hostname = hostname;
  }

  /**
   * encode the MailMessage to a String
   *
   * @return the encoded message
   */
  public String encode() {
    EncodedPart completeMessage;
    EncodedPart mainPart;

    String text = message.getText();
    String html = message.getHtml();

    if (text != null && html != null) {
      mainPart = new MultiPart(Arrays.asList(new TextPart(text, "plain"), new TextPart(html, "html")), "alternative");
    } else if (text != null) {
      mainPart = new TextPart(text, "plain");
    } else if (html != null) {
      mainPart = new TextPart(html, "html");
    } else {
      // message with only attachments
      mainPart = null;
    }

    List<MailAttachment> attachments = message.getAttachment();
    if (attachments != null) {
      List<EncodedPart> parts = new ArrayList<>();
      if (mainPart != null) {
        parts.add(mainPart);
      }
      for (MailAttachment a : attachments) {
        parts.add(new AttachmentPart(a));
      }
      completeMessage = new MultiPart(parts, "mixed");
    } else {
      completeMessage = mainPart;
    }

    if (completeMessage == null) {
      // if we have neither a text part nor attachments, create
      // an empty message with the default headers
      completeMessage = new TextPart("", "plain");
    }
    completeMessage.headers = createHeaders(completeMessage.headers);

    return completeMessage.asString();
  }

  /**
   * create the headers of the MIME message by combining the headers the user has supplied with the ones necessary for
   * the message
   *
   * @return MultiMap of final headers
   */
  private MultiMap createHeaders(MultiMap additionalHeaders) {
    MultiMap headers = new CaseInsensitiveHeaders();

    if (!message.isFixedHeaders()) {
      headers.set("MIME-Version", "1.0");
      headers.set("Message-ID", Utils.generateMessageID(hostname));
      headers.set("Date", Utils.generateDate());

      if (message.getSubject() != null) {
        headers.set("Subject", Utils.encodeHeader(message.getSubject(), 9));
      }

      if (message.getFrom() != null) {
        headers.set("From", Utils.encodeHeaderEmail(message.getFrom(), 6));
      }
      if (message.getTo() != null) {
        headers.set("To", Utils.encodeEmailList(message.getTo(), 4));
      }
      if (message.getCc() != null) {
        headers.set("Cc", Utils.encodeEmailList(message.getCc(), 4));
      }

      headers.addAll(additionalHeaders);
    }

    // add the user-supplied headers as last step, this way it is possible
    // to supply a custom Message-ID for example.
    MultiMap headersToSet = message.getHeaders();
    if (headersToSet != null) {
      for (String key : headersToSet.names()) {
        headers.remove(key);
      }
      headers.addAll(headersToSet);
    }

    messageID = headers.get("Message-ID");
    
    return headers;
  }

  /**
   * @return the messageId
   */
  public String getMessageID() {
    return messageID;
  }
}

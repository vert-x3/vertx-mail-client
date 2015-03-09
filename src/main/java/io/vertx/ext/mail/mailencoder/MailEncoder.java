package io.vertx.ext.mail.mailencoder;

import io.vertx.core.MultiMap;
import io.vertx.core.http.CaseInsensitiveHeaders;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.mail.MailAttachment;
import io.vertx.ext.mail.MailMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MailEncoder {

  private static final Logger log = LoggerFactory.getLogger(MailEncoder.class);

  private MailMessage message;

  public MailEncoder(MailMessage message) {
    this.message = message;
  }

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
      List<EncodedPart> parts = new ArrayList<EncodedPart>();
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
   * @return
   */
  private MultiMap createHeaders(MultiMap additionalHeaders) {
    CaseInsensitiveHeaders headers = new CaseInsensitiveHeaders();

    headers.set("MIME-Version", "1.0");
    headers.set("Message-ID", Utils.generateMessageId());
    headers.set("Date", Utils.generateDate());
    
    if (message.getSubject() != null) {
      headers.set("Subject", Utils.encodeHeader(message.getSubject(), 8));
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

    return headers;
  }

}

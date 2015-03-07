package io.vertx.ext.mail.mailencoder;

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

    CaseInsensitiveHeaders headers = new CaseInsensitiveHeaders();

    if(message.getSubject()!=null) {
      headers.set("Subject", Utils.encodeHeader(message.getSubject()));
    }
    headers.set("MIME-Version", "1.0");
    headers.set("Message-ID", Utils.generateMessageId());

    if(message.getFrom()!=null) {
      headers.set("From", Utils.encodeHeaderEmail(message.getFrom()));
    }
    if(message.getTo()!=null) {
      headers.set("To", Utils.encodeEmailList(message.getTo()));
    }
    if(message.getCc()!=null) {
      headers.set("Cc", Utils.encodeEmailList(message.getCc()));
    }

    EncodedPart completeParts;
    EncodedPart mainPart;

    String text = message.getText();
    String html = message.getHtml();

    if (text != null && html != null) {
      mainPart = new MultiPart(Arrays.asList(new PlainPart(text), new HtmlPart(html)), "alternative");
    } else if (text != null) {
      mainPart = new PlainPart(text);
    } else if (html != null) {
      mainPart = new HtmlPart(html);
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
      completeParts = new MultiPart(parts, "mixed");
    } else {
      completeParts = mainPart;
    }

    if(completeParts==null) {
      // if we have either a text part nor attachments, create
      // an empty message with the headers
      completeParts=new PlainPart("");
      completeParts.headers=headers.addAll(completeParts.headers);
    } else {
      completeParts.headers=headers.addAll(completeParts.headers);
    }

    return completeParts.asString();
  }

}

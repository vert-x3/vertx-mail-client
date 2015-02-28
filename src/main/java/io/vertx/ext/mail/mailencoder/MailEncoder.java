package io.vertx.ext.mail.mailencoder;

import io.vertx.core.http.CaseInsensitiveHeaders;
import io.vertx.ext.mail.MailAttachment;
import io.vertx.ext.mail.MailMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MailEncoder {

  private MailMessage message;

  public MailEncoder(MailMessage message) {
    this.message = message;
  }

  public String encode() {

    CaseInsensitiveHeaders headers = new CaseInsensitiveHeaders();

    headers.set("Subject", message.getSubject());
    headers.set("MIME-Version", "1.0");
    headers.set("Message-ID", "<12345@mail.vertx.io>");

    EncodedPart completeParts;
    EncodedPart mainPart;

    String text = message.getText();
    String html = message.getHtml();

    if (text != null && html != null) {
      mainPart = new MultiPart(Arrays.asList(new PlainPart(text), new HtmlPart(html)));
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
      completeParts = new MultiPart(parts);
    } else {
      completeParts = mainPart;
    }

    // TODO: may be easier to construct a part with all headers
    // and convert that to String
    headers.addAll(completeParts.headers);

    return headers.toString() + "\n" + completeParts.part;
  }

}

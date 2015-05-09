package io.vertx.ext.mail.mailencoder;

import io.vertx.core.http.CaseInsensitiveHeaders;
import io.vertx.ext.mail.MailAttachment;

class AttachmentPart extends EncodedPart {

  public AttachmentPart(MailAttachment attachment) {
    headers = new CaseInsensitiveHeaders();
    String name = attachment.getName();
    String contentType;
    if (attachment.getContentType() != null) {
      contentType = attachment.getContentType();
    } else {
      contentType = "application/octet-stream";
    }
    if (name != null) {
      contentType += "; name=" + name;
    }
    headers.set("Content-Type", contentType);
    headers.set("Content-Transfer-Encoding", "base64");

    if (attachment.getDescription() != null) {
      headers.set("Content-Description", attachment.getDescription());
    }
    String disposition;
    if (attachment.getDisposition() != null) {
      disposition = attachment.getDisposition();
    } else {
      disposition = "attachment";
    }
    if (name != null) {
      disposition += "; filename=" + name;
    }
    headers.set("Content-Disposition", disposition);

    part = Utils.base64(attachment.getData().getBytes());
  }

}

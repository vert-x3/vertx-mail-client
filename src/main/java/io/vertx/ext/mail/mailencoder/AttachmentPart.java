package io.vertx.ext.mail.mailencoder;

import io.vertx.core.http.CaseInsensitiveHeaders;
import io.vertx.ext.mail.MailAttachment;

public class AttachmentPart extends EncodedPart {

  public AttachmentPart(MailAttachment attachment) {
    headers=new CaseInsensitiveHeaders();
    headers.set("Content-Type", "application/octet-stream");
    headers.set("Content-Transfer-Encoding", "base64");

    part=Utils.base64(attachment.getData());
  }

}

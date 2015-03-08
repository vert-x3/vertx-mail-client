package io.vertx.ext.mail.mailencoder;

import io.vertx.core.http.CaseInsensitiveHeaders;

public class TextPart extends EncodedPart {

  public TextPart(String text, String mode) {
    if (Utils.mustEncode(text)) {
      headers = new CaseInsensitiveHeaders();
      headers.set("Content-Type", "text/" + mode + "; charset=utf-8");
      headers.set("Content-Transfer-Encoding", "quoted-printable");
      part = Utils.encodeQP(text);
    } else {
      headers = new CaseInsensitiveHeaders();
      headers.set("Content-Type", "text/" + mode);
      headers.set("Content-Transfer-Encoding", "7bit");
      part = text;
    }
  }

}

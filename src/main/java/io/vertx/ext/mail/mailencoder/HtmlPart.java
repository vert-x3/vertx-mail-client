package io.vertx.ext.mail.mailencoder;

import io.vertx.core.http.CaseInsensitiveHeaders;

public class HtmlPart extends EncodedPart {

  public HtmlPart(String text) {
    if(Utils.mustEncode(text)) {
      headers=new CaseInsensitiveHeaders();
      headers.set("Content-Type", "text/html; charset=utf-8");
      headers.set("Content-Transfer-Encoding", "quoted-printable");
      part=Utils.encodeQP(text);
    } else {
      headers=new CaseInsensitiveHeaders();
      headers.set("Content-Type", "text/html");
      headers.set("Content-Transfer-Encoding", "7bit");
      part=text;
    }
  }
}

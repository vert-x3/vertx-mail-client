package io.vertx.ext.mail.mailencoder;

import io.vertx.core.http.CaseInsensitiveHeaders;

public class PlainPart extends EncodedPart {

  public PlainPart(String text) {
    headers=new CaseInsensitiveHeaders();
    headers.set("Content-Type", "text/plain; charset=utf-8");
    headers.set("Content-Transfer-Encoding", "quoted-printable");

    part=Utils.encodeQP(text);
  }

}

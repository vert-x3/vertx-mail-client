package io.vertx.ext.mail.mailencoder;

import io.vertx.core.http.CaseInsensitiveHeaders;

import java.util.List;

class MultiPart extends EncodedPart {

  public MultiPart(List<EncodedPart> parts, String mode) {

    String boundary = Utils.generateBoundary();

    headers = new CaseInsensitiveHeaders();
    headers.set("Content-Type", "multipart/" + mode + "; boundary=\"" + boundary + "\"");

    StringBuilder sb = new StringBuilder();

    for (EncodedPart part : parts) {
      sb.append("--");
      sb.append(boundary);
      sb.append('\n');
      sb.append(part.asString());
      sb.append('\n');
    }
    sb.append("--");
    sb.append(boundary);
    sb.append("--\n");
    part = sb.toString();
  }

}

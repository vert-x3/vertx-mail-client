package io.vertx.ext.mail.mailencoder;

import java.util.List;

import io.vertx.core.http.CaseInsensitiveHeaders;

public class MultiPart extends EncodedPart {

  public MultiPart(List<EncodedPart> parts) {

    String boundary=Utils.generateBoundary();

    headers=new CaseInsensitiveHeaders();
    headers.set("Content-Type", "multipart/mixed; boundary=\""+boundary+"\"");

    StringBuilder sb = new StringBuilder();

    
    for(EncodedPart part : parts) {
      sb.append(boundary);
      sb.append('\n');
      sb.append(part.asString());
      sb.append('\n');
    }
    sb.append(boundary);
    sb.append("--\n");
    part=sb.toString();
  }

}

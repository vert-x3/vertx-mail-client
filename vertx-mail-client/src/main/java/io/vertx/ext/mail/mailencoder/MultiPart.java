/*
 *  Copyright (c) 2011-2015 The original author or authors
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *       The Eclipse Public License is available at
 *       http://www.eclipse.org/legal/epl-v10.html
 *
 *       The Apache License v2.0 is available at
 *       http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 */

package io.vertx.ext.mail.mailencoder;

import java.util.List;

import io.vertx.core.http.CaseInsensitiveHeaders;

class MultiPart extends EncodedPart {

  public MultiPart(List<EncodedPart> parts, String mode) {

    String boundary = Utils.generateBoundary();

    headers = new CaseInsensitiveHeaders();
    headers.set("Content-Type", "multipart/" + mode + "; boundary=\"" + boundary + "\"\n\n");

    StringBuilder sb = new StringBuilder();
    if (isInline(parts)) {
      sb.append('\n');
      sb.append("--");
      sb.append(boundary);
      sb.append('\n');
      boundary = Utils.generateBoundary();
      sb.append("Content-Type: multipart/related; boundary=\"").append(boundary).append("\"\n\n\n");

      // --=--vertx_mail_1468357786_1454504463948_0
      // Content-Type: multipart/related; boundary="-------------...B128803765634796"

    }

    for (EncodedPart part : parts) {
      sb.append("--");
      sb.append(boundary);
      sb.append('\n');
      sb.append(part.asString());
      sb.append("\n\n");
    }
    sb.append("--");
    sb.append(boundary);
    sb.append("--\n");
    part = sb.toString();
  }

  private boolean isInline(List<EncodedPart> parts) {
    for (EncodedPart part : parts) {
      String cd = part.headers.get("Content-Disposition");
      if (cd != null && cd.contains("inline"))
        return true;
    }
    return false;
  }

}

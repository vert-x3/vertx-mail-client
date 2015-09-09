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

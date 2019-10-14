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

  private final List<EncodedPart> parts;
  private final String boundary;

  public MultiPart(List<EncodedPart> parts, String mode, String userAgent) {
    this.parts = parts;
    this.boundary = Utils.generateBoundary(userAgent);

    headers = new CaseInsensitiveHeaders();
    headers.set("Content-Type", "multipart/" + mode + "; boundary=\"" + boundary + "\"");

  }

  @Override
  String asString() {
    return partAsString(this);
  }

  private String partAsString(EncodedPart part) {
    StringBuilder sb = new StringBuilder(part.headers().toString());
    sb.append("\n");
    if (part.parts() != null) {
      for(EncodedPart thePart: part.parts()) {
        sb.append("--");
        sb.append(part.boundary());
        sb.append("\n");
        sb.append(partAsString(thePart));
        sb.append("\n\n");
      }
    } else {
      sb.append(part.body());
    }
    return sb.toString();
  }

  @Override
  public int size() {
    int size = 0;
    for (EncodedPart part: parts) {
      size += part.size();
    }
    return size;
  }

  @Override
  public List<EncodedPart> parts() {
    return this.parts;
  }

  @Override
  public String boundary() {
    return this.boundary;
  }

}

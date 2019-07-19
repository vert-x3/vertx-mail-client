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

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;

import java.util.List;

public abstract class EncodedPart {
  MultiMap headers;
  String part;

  String asString() {
    StringBuilder sb = new StringBuilder(headers().toString());
    if (body() != null) {
      sb.append("\n");
      sb.append(body());
    }
    return sb.toString();
  }

  public MultiMap headers() {
    return headers;
  }

  public String body() {
    return part;
  }

  public int size() {
    return asString().length();
  }

  public ReadStream<Buffer> bodyStream() {
    return null;
  }

  public List<EncodedPart> parts() {
    return null;
  }

  public String boundary() {
    return null;
  }

}

package io.vertx.ext.mail.mailencoder;

import io.vertx.core.MultiMap;

abstract class EncodedPart {
  MultiMap headers;
  String part;

  public String asString() {
    return headers.toString() + "\n"
        + part;
  }
}

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

class TextPart extends EncodedPart {

  public TextPart(String text, String mode) {
    if (Utils.mustEncode(text)) {
      headers = MultiMap.caseInsensitiveMultiMap();;
      headers.set("Content-Type", "text/" + mode + "; charset=utf-8");
      headers.set("Content-Transfer-Encoding", "quoted-printable");
      part = Utils.encodeQP(text);
    } else {
      headers = MultiMap.caseInsensitiveMultiMap();;
      headers.set("Content-Type", "text/" + mode);
      headers.set("Content-Transfer-Encoding", "7bit");
      part = text;
    }
  }

}

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

package io.vertx.ext.mail.impl.sasl;

import io.vertx.ext.auth.PRNG;

import java.util.Set;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
public final class AuthOperationFactory {

  // algorithms are sorted by preference
  private static final String[] ALGORITHMS = {
    "XOAUTH2",
    "DIGEST-MD5",
    "CRAM-SHA256",
    "CRAM-SHA1",
    "CRAM-MD5",
    "PLAIN",
    "LOGIN"
  };

  private final PRNG prng;

  public AuthOperationFactory(PRNG prng) {
    this.prng = prng;
  }

  public AuthOperation createAuth(String username, String password, Set<String> allowedMethods) {
    for (String algorithm : ALGORITHMS) {
      if (allowedMethods.contains(algorithm)) {
        switch (algorithm) {
          case "XOAUTH2":
            return new AuthXOAUTH2(username, password);
          case "CRAM-MD5":
            return new AuthCram("CRAM-MD5",username, password);
          case "CRAM-SHA1":
            return new AuthCram("CRAM-SHA1", username, password);
          case "CRAM-SHA256":
            return new AuthCram("CRAM-SHA256", username, password);
          case "DIGEST-MD5":
            return new AuthDigest("DIGEST-MD5", prng, username, password);
          case "PLAIN":
            return new AuthPlain(username, password);
          case "LOGIN":
            return new AuthLogin(username, password);
        }
      }
    }
    return null;
  }
}

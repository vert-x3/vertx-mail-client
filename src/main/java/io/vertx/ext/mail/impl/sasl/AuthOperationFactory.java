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
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.impl.Utils;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
public final class AuthOperationFactory {

  // algorithms are sorted by preference
  private static final String[] ALGORITHMS = {
    "XOAUTH2",
    "NTLM",
    "DIGEST-MD5",
    "CRAM-SHA256",
    "CRAM-SHA1",
    "CRAM-MD5",
    "LOGIN",
    "PLAIN"
  };

  private final PRNG prng;

  private String authMethod;

  public AuthOperationFactory(PRNG prng) {
    this.prng = prng;
  }

  public List<String> supportedAuths(MailConfig config) {
    List<String> supported = Stream.of(ALGORITHMS).collect(Collectors.toList());
    final String authMethods = config.getAuthMethods();
    if (authMethods != null && !authMethods.trim().isEmpty()) {
      supported.retainAll(Utils.parseCapaAuth(authMethods));
    }
    return supported;
  }

  public synchronized String getAuthMethod() {
    return authMethod;
  }

  public synchronized AuthOperationFactory setAuthMethod(String authMethod) {
    this.authMethod = authMethod;
    return this;
  }

  public AuthOperation createAuth(MailConfig mailConfig, String authMethod, UsernamePasswordCredentials credentials) {
    String username = credentials != null ? credentials.getUsername() : mailConfig.getUsername();
    String password = credentials != null ? credentials.getPassword() : mailConfig.getPassword();
    switch (authMethod) {
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
      case "LOGIN":
        return new AuthLogin(username, password);
      case "PLAIN":
        return new AuthPlain(username, password);
      case "NTLM":
        int idx = username.indexOf('\\');
        String ntDomain = mailConfig.getNtDomain();
        if (idx != -1) {
          ntDomain = username.substring(0, idx).toUpperCase(Locale.ENGLISH);
          username = username.substring(idx + 1);
        }
        return new NTLMAuth(username, password, ntDomain, mailConfig.getWorkstation());
    }
    throw new IllegalArgumentException("Unsupported Authentication Method: " + authMethod);
  }
}

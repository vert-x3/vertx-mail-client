/*
 *  Copyright (c) 2011-2019 The original author or authors
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

package io.vertx.ext.mail;

import io.vertx.codegen.annotations.VertxGen;

/**
 *
 * Signing Algorithm specified by DKIM spec.
 *
 * @author <a href="mailto: aoingl@gmail.com">Lin Gao</a>
 */
@VertxGen
public enum DKIMSignAlgorithm {
  RSA_SHA1("sha1", "rsa", "SHA-1"), // rsa-sha1
  RSA_SHA256("sha256", "rsa", "SHA-256"); // rsa-sha256

  /**
   * The hash algorithm id used by {@link io.vertx.ext.auth.HashingAlgorithm} to distinguish from others.
   */
  private final String hashAlgoId;

  /**
   * The Hash type. It is: <code>rsa</code> now.
   */
  private final String type;

  /**
   * The actual algorithm that can be used by the {@link java.security.MessageDigest} to calculate the digest.
   */
  private final String hashAlgo;

  DKIMSignAlgorithm(String hashAlgoId, String type, String hashAlgo) {
    this.hashAlgoId = hashAlgoId;
    this.type = type;
    this.hashAlgo = hashAlgo;
  }

  /**
   * Gets the algorithm name specified by the DKIM specification.
   *
   * See: https://tools.ietf.org/html/rfc6376#section-3.3
   *
   * @return the algorithm name
   */
  public String dkimAlgoName() {
    return this.type + "-" + this.hashAlgoId;
  }

  /**
   * Gets the hash algorithm to produce the hash of the message.
   *
   * @return the hash algorithm
   */
  public String hashAlgorithm() {
    return this.hashAlgo;
  }

  /**
   * Gets the Hash Algorithm ID that can be identified by the {@link io.vertx.ext.auth.HashingStrategy}.
   *
   * @return the id of the Hash Algorithm.
   */
  public String hashAlgoId() {
    return hashAlgoId;
  }

  /**
   * Gets the Signature Algorithm, like: SHA256withRSA, SHA1withRSA.
   *
   * @return the signature algorithm
   */
  public String signatureAlgorithm() {
    return this.hashAlgoId.toUpperCase() + "with" + this.type.toUpperCase();
  }

}

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

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
public class CryptUtils {

  private CryptUtils() {
  }

  public static String hmacHex(String keyString, String message, String hmac) {
    try {
      SecretKey key = new SecretKeySpec(keyString.getBytes(StandardCharsets.UTF_8), hmac);
      Mac mac = Mac.getInstance(key.getAlgorithm());
      mac.init(key);
      return encodeHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      // doesn't happen, auth will fail in that case
      return "";
    }
  }

  /**
   * @param bytes
   * @return
   */
  public static String encodeHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      final int v = ((int) b) & 0xff;
      if (v < 16) {
        sb.append('0');
      }
      sb.append(Integer.toHexString(v));
    }
    return sb.toString();
  }

  public static String base64(String string) {
    // this call does not create multi-line base64 data
    // (if someone uses a password longer than 57 chars or
    // one of the other SASL replies is longer than 76 chars)
    return Base64.getEncoder().encodeToString(string.getBytes(StandardCharsets.UTF_8));
  }

  public static String base64(byte[] data) {
    return Base64.getEncoder().encodeToString(data);
  }

  public static String decodeb64(String string) {
    return new String(Base64.getDecoder().decode(string), StandardCharsets.UTF_8);
  }

}

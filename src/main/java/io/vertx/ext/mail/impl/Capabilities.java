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

package io.vertx.ext.mail.impl;

import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * holds the capabilities of an ESMTP server.
 * <p>
 * e.g. SIZE, AUTH, PIPELINING
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
class Capabilities {

  private static final Logger log = LoggerFactory.getLogger(Capabilities.class);

  /**
   * hold that parsed list of possible authentication mechanisms
   * <p>
   * e.g. PLAIN, LOGIN, CRAM-MD5
   */
  private Set<String> capaAuth;
  /**
   * if supported, the maximum size of a mail
   * 0 if not supported
   */
  private int capaSize;
  /**
   * if the server supports STARTTLS
   */
  private boolean capaStartTLS;

  /**
   * if the server supports PIPELINING
   */
  private boolean capaPipelining;

  /**
   * if the server supports ENHANCEDSTATUSCODES
   */
  private boolean capaEnhancedStatusCodes;

  /**
   * @return Set of Strings of capabilities
   */
  Set<String> getCapaAuth() {
    return capaAuth;
  }

  /**
   * @return size of allowed message
   */
  int getSize() {
    return capaSize;
  }

  /**
   * @return if the server supports PIPELINING
   */
  boolean isCapaPipelining() {
    return capaPipelining;
  }

  /**
   * @return if the server supports ENHANCEDSTATUSCODES
   */
  boolean isCapaEnhancedStatusCodes() {
    return capaEnhancedStatusCodes;
  }

  /**
   * @return if the server supports STARTTLS
   */
  boolean isStartTLS() {
    return capaStartTLS;
  }

  Capabilities() {
    capaAuth = Collections.emptySet();
  }

  /**
   * parse the capabilities as returned by the server (i.e. multi-line SMTP reply)
   * and set the capabities in the class fields
   *
   * @param message multi-line SMTP reply
   */
  void parseCapabilities(final String message) {
    List<String> capabilities = parseEhlo(message);
    for (String c : capabilities) {
      if (c.equals("STARTTLS")) {
        capaStartTLS = true;
      }
      if (c.equals("PIPELINING")) {
        capaPipelining = true;
      }
      if (c.equals("ENHANCEDSTATUSCODES")) {
        capaEnhancedStatusCodes = true;
      }
      if (c.startsWith("AUTH ")) {
        capaAuth = Utils.parseCapaAuth(c.substring(5));
      }
      // if (c.equals("8BITMIME")) {
      // capa8BitMime = true;
      // }
      if (c.startsWith("SIZE ")) {
        try {
          capaSize = Integer.parseInt(c.substring(5));
        } catch (NumberFormatException n) {
          capaSize = 0;
        }
      }
    }
  }

  /**
   * parse a multi-line EHLO reply string into a List of lines
   *
   * @param message the EHLO response message
   * @return List of lines
   */
  private List<String> parseEhlo(String message) {
    // parse ehlo and other multiline replies
    List<String> result = new ArrayList<>();

    String resultCode = message.substring(0, 3);

    for (String line : Utils.splitByChar(message, '\n')) {
      if (!line.startsWith(resultCode) || line.charAt(3) != '-' && line.charAt(3) != ' ') {
        log.error("format error in multiline response");
        handleError("format error in multiline response");
      } else {
        result.add(line.substring(4));
      }
    }

    return result;
  }

  /**
   * handle errors (this currently doesn't call a handler but just logs the error)
   *
   * @param msg the error message
   */
  private void handleError(String msg) {
    log.error(msg);
  }

}

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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
public final class Utils {

  private static final Logger log = LoggerFactory.getLogger(Utils.class);

  /**
   * utility class only
   */
  private Utils() {
  }

  /**
   * parse the capabilities string (single line) into a Set of auth String
   *
   * @param auths list of auth methods as String (e.g. "PLAIN LOGIN CRAM-MD5")
   * @return Set of supported auth methods
   */
  public static Set<String> parseCapaAuth(final String auths) {
    Set<String> authSet = new HashSet<String>();
    for (String a : splitByChar(auths, ' ')) {
      authSet.add(a);
    }
    return authSet;
  }

  /**
   * split string at each occurrence of a character (e.g. \n)
   *
   * @param message the string to split
   * @param ch      the char between which we split
   * @return the list lines
   */
  static List<String> splitByChar(final String message, final char ch) {
    List<String> lines = new ArrayList<>();
    int index = 0;
    int nextIndex;
    while ((nextIndex = message.indexOf(ch, index)) != -1) {
      lines.add(message.substring(index, nextIndex));
      index = nextIndex + 1;
    }
    lines.add(message.substring(index));
    return lines;
  }

  /**
   * check if the welcome line of the SMTP server advertises ESMTP support
   *
   * this is mostly a hack to avoid trying EHLO command on servers that do not support it (e.g. a server behind a
   * firewall with policy enforcement that overwrites all unknown text with ***).
   *
   * Some servers use "Esmtp" so we do a case-insensitive check. The Locale.ENGLISH parameter is required to avoid
   * issues with locale settings that change ASCII chars (and to keep pmd happy)
   *
   * @param message
   *          the initial reply from the server (e.g. "220 example.com ESMTP Postfix")
   * @return true if ESMTP is (probably) supported
   */
  static boolean isEsmtpSupported(String message) {
    final boolean esmtpSupported = message.toUpperCase(Locale.ENGLISH).contains("ESMTP");
    log.debug("isEsmtpSupported:" + esmtpSupported);
    return esmtpSupported;
  }

  /**
   * get the hostname by resolving our own address
   *
   * this method is not async due to dns call, we run this with executeBlocking
   *
   * @return the hostname
   */
  public static String getHostname() {
    try {
      InetAddress ip = InetAddress.getLocalHost();
      return ip.getCanonicalHostName();
    } catch (UnknownHostException e) {
      // as a last resort, use localhost
      return "localhost";
    }
  }

}

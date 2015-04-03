package io.vertx.ext.mail.impl;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * holds the capabilities of an ESMTP server.
 * 
 * e.g. SIZE, AUTH, PIPELINING
 * 
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
class Capabilities {

  private static final Logger log = LoggerFactory.getLogger(Capabilities.class);

  /**
   * hold that parsed list of possible authentication mechanisms
   * 
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
      if (c.startsWith("AUTH ")) {
        capaAuth = parseCapaAuth(c);
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
   * parse the capabilities string (single line) into a Set of auth String
   * @param auths list of auth methods as String (e.g. "PLAIN LOGIN CRAM-MD5")
   * @return Set of supported auth methods
   */
  private Set<String> parseCapaAuth(String auths) {
    Set<String> authSet = new HashSet<String>();
    for (String a : splitByChar(auths.substring(5), ' ')) {
      authSet.add(a);
    }
    return authSet;
  }

  /**
   * parse a multi-line EHLO reply string into a List of lines
   * 
   * @param message
   * @return List of lines
   */
  private List<String> parseEhlo(String message) {
    // parse ehlo and other multiline replies
    List<String> result = new ArrayList<String>();

    String resultCode = message.substring(0, 3);

    for (String line : splitByChar(message, '\n')) {
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
   * split string at each occurrence of a character (e.g. \n)
   * 
   * @param message
   *          the string to split
   * @param ch
   *          the char between which we split
   * @return List<String> of the split lines
   */
  private List<String> splitByChar(String message, char ch) {
    List<String> lines = new ArrayList<String>();
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
   * handle errors (this currently doesn't call a handler but just logs the error)
   *
   * @param msg the error message
   */
  private void handleError(String msg) {
    log.error(msg);
  }

}

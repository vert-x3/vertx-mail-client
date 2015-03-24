package io.vertx.ext.mail;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class Capabilities {

  private static final Logger log = LoggerFactory.getLogger(Capabilities.class);

  private Set<String> capaAuth;
  private int capaSize;
  private boolean capaStartTLS;

  /**
   * @return the capaAuth
   */
  Set<String> getCapaAuth() {
    return capaAuth;
  }

  /**
   * @return the capaSize
   */
  int getSize() {
    return capaSize;
  }

  /**
   * @return the capaStartTLS
   */
  boolean isStartTLS() {
    return capaStartTLS;
  }

  Capabilities() {
    capaAuth = Collections.emptySet();
  }

  /**
   * @param message
   */
  void parseCapabilities(String message) {
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
   * @param c
   * @return
   */
  private Set<String> parseCapaAuth(String c) {
    Set<String> authSet = new HashSet<String>();
    for (String a : splitByChar(c.substring(5), ' ')) {
      authSet.add(a);
    }
    return authSet;
  }

  private List<String> parseEhlo(String message) {
    // parse ehlo and other multiline replies
    List<String> v = new ArrayList<String>();

    String resultCode = message.substring(0, 3);

    for (String l : splitByChar(message, '\n')) {
      if (!l.startsWith(resultCode) || l.charAt(3) != '-' && l.charAt(3) != ' ') {
        log.error("format error in multiline response");
        throwError("format error in multiline response");
      } else {
        v.add(l.substring(4));
      }
    }

    return v;
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

  private void throwError(String msg) {
    log.error(msg);
  }
  
}

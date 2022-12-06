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

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class Utils {

  private Utils() {
  }

  public static String encodeQP(String text) {
    byte[] utf8 = text.getBytes(StandardCharsets.UTF_8);
    StringBuilder sb = new StringBuilder();

    int column = 0;
    for (int i = 0; i < utf8.length; i++) {
      char ch = (char) utf8[i];
      if (ch == '\n') {
        sb.append(ch);
        column = 0;
      } else {
        boolean nextIsEOL = i == utf8.length - 1 || utf8[i + 1] == '\n';
        String encChar;
        if (mustEncode(ch) || nextIsEOL && ch == ' ') {
          encChar = encodeChar(ch);
        } else {
          encChar = String.valueOf(ch);
        }
        int newColumn = column + encChar.length();
        if (newColumn <= 75 || nextIsEOL && newColumn == 76) {
          sb.append(encChar);
          column = newColumn;
        } else {
          sb.append("=\n").append(encChar);
          column = encChar.length();
        }
      }
    }
    return sb.toString();
  }

  private static String encodeChar(char ch) {
    if (ch < 16) {
      return "=0" + Integer.toHexString(ch).toUpperCase(Locale.ENGLISH);
    } else {
      return '=' + Integer.toHexString(ch & 0xff).toUpperCase(Locale.ENGLISH);
    }
  }

  /*
   * check if a single char must be encoded as qp, this assumes we
   * already have decided that we have to encode the text
   */
  static boolean mustEncode(char ch) {
    return ch >= 128 || ch < 10 || ch >= 11 && ch < 32 || ch == '=';
  }

  /*
   * check if a String must be encoded as qp, this allows tab and = chars
   * if these are there are no other chars to encode
   */
  static boolean mustEncode(String s) {
    int lineLen = 0;
    for (int i = 0; i < s.length(); i++) {
      final char ch = s.charAt(i);
      if (ch != '=' && ch != '\t' && mustEncode(ch)) {
        return true;
      }
      if (ch == '\n') {
        lineLen = 0;
      } else {
        // line limit is 1000 - CRLF = 998
        // https://www.rfc-editor.org/rfc/rfc5322#section-2.1.1
        if (++lineLen > 998) {
          return true;
        }
      }
    }
    return false;
  }

  private static final AtomicInteger count = new AtomicInteger(0);

  private static String nomaliseUserAgent(String userAgent) {
    return userAgent.replace(" ", "_");
  }

  static String generateBoundary(String userAgent) {
    return "=--" +
      nomaliseUserAgent(userAgent) + "_" +
      Thread.currentThread().hashCode() + "_" +
      System.currentTimeMillis() + "_" +
      count.getAndIncrement();
  }

  static String generateMessageID(String hostname, String userAgent) {
    return "<msg." +
      System.currentTimeMillis() + "." +
      nomaliseUserAgent(userAgent) + "." +
      count.getAndIncrement() +
      "@" + hostname +
      ">";
  }

  /*
   * encode subject if necessary. we assume that the string is encoded as whole
   * and do mime compliant line wrapping
   * index is the offset of the line that is already used (i.e. the length of the header including ": ")
   */
  static String encodeHeader(String subject, int index) {
    if (mustEncode(subject)) {
      byte[] utf8 = subject.getBytes(StandardCharsets.UTF_8);
      StringBuilder sb = new StringBuilder();
      sb.append("=?UTF-8?Q?");
      int column = 10 + index;
      for (byte b : utf8) {
        char ch = (char) b;
        if (ch == '\n') {
          column = 1;
        } else {
          String encChar;
          if (mustEncode(ch) || ch == '_' || ch == '?') {
            encChar = encodeChar(ch);
          } else if (ch == ' ') {
            encChar = "_";
          } else {
            encChar = String.valueOf(ch);
          }
          int newColumn = column + encChar.length();
          if (newColumn <= 74) {
            sb.append(encChar);
            column = newColumn;
          } else {
            sb.append("?=\n =?UTF-8?Q?").append(encChar);
            column = 11 + encChar.length();
          }
        }
      }
      sb.append("?=");
      return sb.toString();
    } else {
      return subject;
    }
  }

  static String encodeHeaderEmail(String address, int index) {
    EmailAddress adr = new EmailAddress(address);

    if (mustEncode(adr.getName())) {
      return adr.getEmail() + " (" + encodeHeader(adr.getName(), index + adr.getEmail().length() + 2) + ")";
    } else {
      return address;
    }
  }

  static String encodeEmailList(List<String> addresses, int index) {
    StringBuilder sb = new StringBuilder();
    boolean firstAddress = true;
    for (String addr : addresses) {
      if (firstAddress) {
        firstAddress = false;
      } else {
        sb.append(',');
        index++;
      }
      final String email, name;
      // check postmaster against RCPT TO
      if (EmailAddress.POSTMASTER.equalsIgnoreCase(addr)) {
        email = addr;
        name = "";
      } else {
        EmailAddress adr = new EmailAddress(addr);
        email = adr.getEmail();
        name = adr.getName();
      }
      if (index + email.length() >= 76) {
        sb.append("\n ");
        index = 1;
      }
      sb.append(email);
      index += email.length();
      if (!name.isEmpty()) {
        if (mustEncode(name)) {
          boolean hadSpace = false;
          if (index + 12 >= 71) {
            sb.append("\n ");
            index = 1;
            hadSpace = true;
          }
          if (!hadSpace) {
            sb.append(' ');
            index++;
          }
          sb.append('(');
          index++;
          String encoded = encodeHeader(name, index);
          sb.append(encoded);
          if (encoded.contains("\n")) {
            index = encoded.length() - encoded.lastIndexOf('\n');
          } else {
            index += encoded.length();
          }
          sb.append(')');
          index++;
        } else {
          boolean hadSpace = false;
          if (index + name.length() + 3 >= 76) {
            sb.append("\n ");
            index = 1;
            hadSpace = true;
          }
          if (!hadSpace) {
            sb.append(' ');
            index++;
          }
          sb.append('(');
          index++;
          sb.append(name);
          sb.append(')');
          index += email.length() + 3;
        }
      }
    }
    return sb.toString();
  }

  static String generateDate() {
    SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z (z)", Locale.ENGLISH);
    return format.format(new Date());
  }

  private final static byte[] lf = { 10 };

  /*
   * base64 with lf line terminators, the crlf will be added in the write operation
   * in DATA
   */
  public static String base64(byte[] bytes) {
    return Base64.getMimeEncoder(76, lf).encodeToString(bytes);
  }

}

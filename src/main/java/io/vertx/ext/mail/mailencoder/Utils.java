package io.vertx.ext.mail.mailencoder;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Base64;
import java.util.List;
import java.util.StringJoiner;

class Utils {

  private Utils() {
  }

  static String encodeQP(String text) {
    try {
      byte[] utf8 = text.getBytes("UTF-8");
      StringBuilder sb = new StringBuilder();

      int column = 0;
      for (int i = 0; i < utf8.length; i++) {
        char ch = (char) utf8[i];
        if (ch == '\n') {
          column = 0;
        } else {
          boolean nextIsEOL = (i == utf8.length - 1 || utf8[i + 1] == '\n');
          String encChar;
          if (mustEncode(ch) || nextIsEOL && ch==' ') {
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
    } catch (UnsupportedEncodingException e) {
      return "";
    }
  }

  /**
   * @param ch
   * @return
   */
  private static String encodeChar(char ch) {
    return String.format("=%02X", ch & 0xff);
  }

  static boolean mustEncode(char ch) {
    return (ch & 0xff) >= 128 || ch >= 0 && ch < 10 || ch >= 11 && ch < 32 || ch == '=';
  }

  static boolean mustEncode(String s) {
    for (int i = 0; i < s.length(); i++) {
      if (s.charAt(i) != '=' && mustEncode(s.charAt(i))) {
        return true;
      }
    }
    return false;
  }

  static String base64(String string) {
    try {
      return Base64.getMimeEncoder().encodeToString(string.getBytes("ISO-8859-1"));
    } catch (UnsupportedEncodingException e) {
      // doesn't happen
      return "";
    }
  }

  // TODO: make this smarter
  static int count = 0;

  static String generateBoundary() {
    return "=--vertx_mail_" + Thread.currentThread().hashCode() + "_" + System.currentTimeMillis() + "_" + (count++);
  }

  static String generateMessageId() {
    return "<msg." + System.currentTimeMillis() + ".vertxmail." + (count++) + "@" + getMyHostname() + ">";
  }

  static private String getMyHostname() {
    try {
      InetAddress ip = InetAddress.getLocalHost();
      return ip.getCanonicalHostName();
    } catch (UnknownHostException e) {
      return "unknown";
    }
  }

  /*
   * encode subject if necessary we assume that the string is encoded as whole
   * and do mime compliant line wrapping
   */
  static String encodeHeader(String subject) {
    if (mustEncode(subject)) {
      try {
        byte[] utf8 = subject.getBytes("UTF-8");
        StringBuilder sb = new StringBuilder();
        sb.append("=?UTF-8?Q?");
        for (int i = 0; i < utf8.length; i++) {
          char ch = (char) utf8[i];
          if (mustEncode(ch) || ch == '_' || ch == '?') {
            sb.append(encodeChar(ch));
          } else if (ch == ' ') {
            sb.append('_');
          } else {
            sb.append(ch);
          }
        }
        sb.append("?=");
        return sb.toString();
      } catch (UnsupportedEncodingException e) {
        return "";
      }
    } else {
      return subject;
    }
  }

  static String encodeHeaderEmail(String address) {
    EmailAddress adr = new EmailAddress(address);

    if (mustEncode(adr.getName())) {
      return adr.getAddress() + " (" + encodeHeader(adr.getName()) + ")";
    } else {
      return address;
    }
  }

  static String encodeEmailList(List<String> addresses) {
    StringJoiner joiner = new StringJoiner(",");
    for (String addr : addresses) {
      joiner.add(encodeHeaderEmail(addr));
    }
    return joiner.toString();
  }

}

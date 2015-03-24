package io.vertx.ext.mail;

public class StatusCode {

  private StatusCode() {
  }

  static private int getStatusCode(String message) {
    if (message.length() < 4) {
      return 500;
    }
    if (message.charAt(3) != ' ' && message.charAt(3) != '-') {
      return 500;
    }
    try {
      return Integer.parseInt(message.substring(0, 3));
    } catch (NumberFormatException n) {
      return 500;
    }
  }

  static boolean isStatusOk(String message) {
    int statusCode = getStatusCode(message);
    return statusCode >= 200 && statusCode < 400;
  }

  static boolean isStatusFatal(String message) {
    return getStatusCode(message) >= 500;
  }

  static boolean isStatusTemporary(String message) {
    int statusCode = getStatusCode(message);
    return statusCode >= 400 && statusCode < 500;
  }

}

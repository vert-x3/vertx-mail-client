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

class StatusCode {

  private StatusCode() {
  }

  private static int getStatusCode(String message) {
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

  static boolean isStatusContinue(String message) {
    int statusCode = getStatusCode(message);
    return statusCode >= 300 && statusCode < 400;
  }

  static boolean isStatusFatal(String message) {
    return getStatusCode(message) >= 500;
  }

  static boolean isStatusTemporary(String message) {
    int statusCode = getStatusCode(message);
    return statusCode >= 400 && statusCode < 500;
  }

}

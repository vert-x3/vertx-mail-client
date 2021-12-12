/*
 *  Copyright (c) 2011-2021 The original author or authors
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

import io.vertx.ext.mail.SMTPException;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * This represents the response from SMTP server.
 *
 * See: https://datatracker.ietf.org/doc/html/rfc5321#section-4.2
 *
 * @author <a href="mailto: aoingl@gmail.com">Lin Gao</a>
 */
class SMTPResponse {

  private final int replyCode;
  private final List<String> lines;

  SMTPResponse(String line) {
    Objects.requireNonNull(line, "SMTP response should not be null.");
    this.replyCode = getStatusCode(line);
    this.lines = Arrays.asList(line.split("\n"));
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

  boolean isStatusOk() {
    return replyCode >= 200 && replyCode < 400;
  }

  boolean isStatusContinue() {
    return replyCode >= 300 && replyCode < 400;
  }

  SMTPException toException(String message) {
    return toException(message, false);
  }

  SMTPException toException(String message, boolean supportEnhancementStatusCode) {
    if (isStatusOk()) {
      throw new IllegalStateException("Status is OK, no exceptions");
    }
    return new SMTPException(message, replyCode, lines, supportEnhancementStatusCode);
  }

}

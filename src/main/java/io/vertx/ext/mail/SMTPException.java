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

package io.vertx.ext.mail;

import io.vertx.core.impl.NoStackTraceThrowable;

import java.util.List;
import java.util.Objects;

/**
 * This represents an Exception during communication with SMTP server.
 *
 * @author <a href="mailto: aoingl@gmail.com">Lin Gao</a>
 */
public class SMTPException extends NoStackTraceThrowable {

  private final int replyCode;
  private final List<String> replyMessages;

  /**
   * Constructor of SMTPException.
   *
   * @param message the informative message prepending the reply messages
   * @param replyCode the SMTP reply code
   * @param replyMessages the SMTP reply messages
   */
  public SMTPException(String message, int replyCode, List<String> replyMessages) {
    super(message + ": " + String.join("\n", Objects.requireNonNull(replyMessages)));
    this.replyCode = replyCode;
    this.replyMessages = replyMessages;
  }

  /**
   * Gets the SMTP reply status code.
   *
   * @return the SMTP reply status code.
   */
  public int getReplyCode() {
    return replyCode;
  }

  /**
   * Gets the SMTP reply messages. It maybe multi lines messages splits by <code>\n</code>
   *
   * @return the SMTP reply messages
   */
  public List<String> getReplyMessages() {
    return replyMessages;
  }

  /**
   * Gets the SMTP reply message. In case of multi lines messages, returns the first line.
   *
   * @return the SMTP reply message
   */
  public String getReplyMessage() {
    return replyMessages.get(0);
  }

  /**
   * Checks if it is a permanent failure, that the reply status code <code>&gt;=</code> 500
   *
   * @return true if it is a permanent failure, false otherwise.
   */
  public boolean isPermanent() {
    return replyCode >= 500;
  }

  /**
   * Checks if it is a transient failure, that the reply status code <code>&gt;=</code> 400 and <code>&lt;</code> 500
   *
   * @return true if it is a transient failure, false otherwise.
   */
  public boolean isTransient() {
    return replyCode >= 400 && replyCode < 500;
  }

}

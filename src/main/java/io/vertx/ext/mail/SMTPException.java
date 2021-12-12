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

  /**
   * The Enhanced Status codes.
   *<p>
   * See: https://datatracker.ietf.org/doc/html/rfc3463#section-3
   *</p>
   */
  public enum EnhancedStatus {

    /**
     * <b>x.0.x</b> for Undefined errors
     */
    OTHER_UNDEFINED(0),
    /**
     * <b>x.1.x</b> for Address Status errors
     */
    OTHER_ADDRESS(1),
    /**
     * <b>x.2.x</b> for Mailbox Status errors
     */
    OTHER_MAILBOX(2),
    /**
     * <b>x.3.x</b> for Mail System errors
     */
    OTHER_MAIL_SYSTEM(3),
    /**
     * <b>x.4.x</b> for Network and Routing errors
     */
    OTHER_NETWORK(4),
    /**
     * <b>x.5.x</b> for Mail Delivery Protocol errors
     */
    OTHER_MAIL_DELIVERY(5),
    /**
     * <b>x.6.x</b> for Message Content or Message Media errors
     */
    OTHER_MAIL_MESSAGE(6),
    /**
     * <b>x.7.x</b> for Security or Policy errors
     */
    OTHER_SECURITY(7),
    /**
     * Anything else is unknown, like failures happen before knowing the capabilities or smtp server reply maliciously.
     */
    OTHER_UNKNOWN(-1);

    private final int subject;
    private int detail;

    EnhancedStatus(int subject) {
      this.subject = subject;
    }

    /**
     * Gets the subject of the EnhancedStatus.
     *
     * @return the subject of the EnhancedStatus
     */
    public int getSubject() {
      return subject;
    }

    /**
     * Gets detail number of the EnhancedStatus.
     *
     * @return the detail of the EnhancedStatus
     */
    public int getDetail() {
      return detail;
    }

    private void setDetail(int detail) {
      this.detail = detail;
    }

    /**
     * Gets the EnhancedStatus from the subject.
     *
     * @param subject the subject
     * @return the associated EnhancedStatus
     */
    static EnhancedStatus fromSubject(int subject) {
      if (subject >= 0 && subject < values().length) {
        return values()[subject];
      }
      return OTHER_UNKNOWN;
    }

  }

  private final int replyCode;
  private final List<String> replyMessages;
  private final EnhancedStatus enhancedStatus;

  /**
   * Constructor of SMTPException.
   *
   * @param message the informative message prepending the reply messages
   * @param replyCode the SMTP reply code
   * @param supportEnhancementStatusCode if <code>ENHANCEDSTATUSCODES</code> is supported or not
   * @param replyMessages the SMTP reply messages
   */
  public SMTPException(String message, int replyCode, List<String> replyMessages, boolean supportEnhancementStatusCode) {
    super(message + ": " + String.join("\n", Objects.requireNonNull(replyMessages)));
    this.replyCode = replyCode;
    this.replyMessages = replyMessages;
    if (supportEnhancementStatusCode) {
      int subject;
      int detail;
      try {
        final String line = replyMessages.get(0).substring(4);
        final String statusStr = line.substring(0, line.indexOf(" "));
        subject = Integer.parseInt(statusStr.substring(statusStr.indexOf(".") + 1, statusStr.lastIndexOf(".")));
        detail = Integer.parseInt(statusStr.substring(statusStr.lastIndexOf(".") + 1));
      } catch (Exception e) {
        subject = -1;
        detail = 0;
      }
      this.enhancedStatus = EnhancedStatus.fromSubject(subject);
      this.enhancedStatus.setDetail(detail);
    } else {
      this.enhancedStatus = EnhancedStatus.OTHER_UNKNOWN;
    }
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

  /**
   * EnhancedStatus of the exception.
   *
   * @return the EnhancedStatus.
   */
  public EnhancedStatus getEnhancedStatus() {
    return enhancedStatus;
  }

}

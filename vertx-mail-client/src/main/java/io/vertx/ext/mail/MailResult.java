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

package io.vertx.ext.mail;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Represent the result of the sendMail operation
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
@DataObject
public class MailResult {

  private String messageID;
  private List<String> recipients;

  public MailResult() {
    messageID = null;
    recipients = new ArrayList<>();
  }

  public MailResult(MailResult other) {
    messageID = other.messageID;
    recipients = new ArrayList<>(other.recipients);
  }

  @SuppressWarnings("unchecked")
  public MailResult(JsonObject json) {
    messageID = json.getString("messageId");
    JsonArray jsonArray = json.getJsonArray("recipients");
    recipients = jsonArray == null ? new ArrayList<>() : (List<String>) jsonArray.getList();
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    if (messageID != null) {
      json.put("messageId", messageID);
    }
    if (recipients != null) {
      json.put("recipients", recipients);
    }
    return json;
  }

  /**
   * @return the messageID
   */
  public String getMessageID() {
    return messageID;
  }

  /**
   * @param messageID the messageID to set
   */
  public MailResult setMessageID(String messageID) {
    this.messageID = messageID;
    return this;
  }

  /**
   * @return the recipients
   */
  public List<String> getRecipients() {
    return recipients;
  }

  /**
   * @param recipients the recipients to set
   */
  public MailResult setRecipients(List<String> recipients) {
    this.recipients = recipients;
    return this;
  }

  public String toString() {
    return toJson().encode();
  }

}

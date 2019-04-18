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
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.core.MultiMap;
import io.vertx.core.http.CaseInsensitiveHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.*;

/**
 * represent a mail message that can be sent via the MailClient
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
@DataObject
public class MailMessage {

  private String bounceAddress;
  private String from;
  private List<String> to = null;
  private List<String> cc = null;
  private List<String> bcc = null;
  private String subject;
  private String text;
  private String html;
  private List<MailAttachment> inlineAttachment;
  private List<MailAttachment> attachment;
  private MultiMap headers = null;
  private boolean fixedHeaders = false;

  /**
   * construct empty mail message that can be used with the setters
   */
  public MailMessage() {
  }

  /**
   * copy object to another @link MailMessage object
   *
   * @param other object to copy
   */
  public MailMessage(MailMessage other) {
    Objects.requireNonNull(other);
    this.bounceAddress = other.bounceAddress;
    this.from = other.from;
    this.to = copyList(other.to);
    this.cc = copyList(other.cc);
    this.bcc = copyList(other.bcc);
    this.subject = other.subject;
    this.text = other.text;
    this.html = other.html;
    this.fixedHeaders = other.fixedHeaders;
    if (other.attachment != null) {
      this.attachment = copyAttachments(other.attachment);
    }
    if (other.inlineAttachment != null) {
      this.inlineAttachment = copyAttachments(other.inlineAttachment);
    }
    if (other.headers != null) {
      headers = new CaseInsensitiveHeaders().addAll(other.headers);
    }
  }

  /**
   * @return
   */
  private List<MailAttachment> copyAttachments(List<MailAttachment> attachment) {
    List<MailAttachment> newList = new ArrayList<>(attachment.size());
    for (MailAttachment a : attachment) {
      newList.add(new MailAttachment(a));
    }
    return newList;
  }

  /**
   * construct object from a JsonObject representation
   *
   * @param json json object to copy
   */
  public MailMessage(JsonObject json) {
    Objects.requireNonNull(json);
    bounceAddress = json.getString("bounceAddress");
    from = json.getString("from");
    to = Utils.getKeyAsStringOrList(json, "to");
    cc = Utils.getKeyAsStringOrList(json, "cc");
    bcc = Utils.getKeyAsStringOrList(json, "bcc");
    subject = json.getString("subject");
    text = json.getString("text");
    html = json.getString("html");
    if (json.containsKey("inline_attachment")) {
      inlineAttachment = copyJsonAttachment(json.getValue("inline_attachment"));
    }
    if (json.containsKey("attachment")) {
      attachment = copyJsonAttachment(json.getValue("attachment"));
    }
    if (json.containsKey("headers")) {
      headers = Utils.jsonToMultiMap(json.getJsonObject("headers"));
    }
  }

  /**
   * @return
   * @throws IllegalArgumentException
   */
  private List<MailAttachment> copyJsonAttachment(Object object) throws IllegalArgumentException {
    List<MailAttachment> list;
    if (object instanceof JsonObject) {
      list = Collections.singletonList(new MailAttachment((JsonObject) object));
    } else if (object instanceof JsonArray) {
      list = new ArrayList<>();
      for (Object attach : (JsonArray) object) {
        list.add(new MailAttachment((JsonObject) attach));
      }
    } else {
      throw new IllegalArgumentException("invalid attachment type");
    }
    return list;
  }

  /**
   * construct a simple message with text/plain
   *
   * @param from    from email address
   * @param to      string to email address
   * @param subject subject of the mail
   * @param text    plain text of the message body
   */
  public MailMessage(String from, String to, String subject, String text) {
    this.from = from;
    this.to = Utils.asList(to);
    this.subject = subject;
    this.text = text;
  }

  /**
   * get bounce address of this mail
   *
   * @return bounce address
   */
  public String getBounceAddress() {
    return bounceAddress;
  }

  /**
   * set bounce address of this mail
   *
   * @param bounceAddress bounce address
   * @return this to be able to use it fluently
   */
  public MailMessage setBounceAddress(String bounceAddress) {
    this.bounceAddress = bounceAddress;
    return this;
  }

  /**
   * get from address of this mail
   *
   * @return from address
   */
  public String getFrom() {
    return from;
  }

  /**
   * set from address of this mail
   *
   * @param from from addrss
   * @return this to be able to use it fluently
   */
  public MailMessage setFrom(String from) {
    this.from = from;
    return this;
  }

  /**
   * get list of to addresses
   *
   * @return List of to addresses
   */
  public List<String> getTo() {
    return to;
  }

  /**
   * set list of to addresses
   *
   * @param to List of to addresses
   * @return this to be able to use it fluently
   */
  public MailMessage setTo(List<String> to) {
    this.to = to;
    return this;
  }

  /**
   * helper method for single recipient
   *
   * @param to to address
   * @return this to be able to use it fluently
   */
  @GenIgnore
  public MailMessage setTo(String to) {
    this.to = Utils.asList(to);
    return this;
  }

  /**
   * get list of cc addresses
   *
   * @return List of cc addresses
   */
  public List<String> getCc() {
    return cc;
  }

  /**
   * set list of cc addresses
   *
   * @param cc List of cc addresses
   * @return this to be able to use it fluently
   */
  public MailMessage setCc(List<String> cc) {
    this.cc = cc;
    return this;
  }

  /**
   * helper method for single recipient
   *
   * @param cc cc address
   * @return this to be able to use it fluently
   */
  @GenIgnore
  public MailMessage setCc(String cc) {
    this.cc = Utils.asList(cc);
    return this;
  }

  /**
   * get list of bcc addresses
   *
   * @return List of bcc addresses
   */
  public List<String> getBcc() {
    return bcc;
  }

  /**
   * set list of bcc addresses
   *
   * @param bcc List of bcc addresses
   * @return this to be able to use it fluently
   */
  public MailMessage setBcc(List<String> bcc) {
    this.bcc = bcc;
    return this;
  }

  /**
   * helper method for single recipient
   *
   * @param bcc bcc address
   * @return this to be able to use it fluently
   */
  @GenIgnore
  public MailMessage setBcc(String bcc) {
    this.bcc = Utils.asList(bcc);
    return this;
  }

  /**
   * get the subject of this mail
   *
   * @return the subject
   */
  public String getSubject() {
    return subject;
  }

  /**
   * set the subject of this mail
   *
   * @param subject the subject
   * @return this to be able to use it fluently
   */
  public MailMessage setSubject(String subject) {
    this.subject = subject;
    return this;
  }

  /**
   * get the plain text of this mail
   *
   * @return the text
   */
  public String getText() {
    return text;
  }

  /**
   * set the plain text of this mail
   *
   * @param text the text
   * @return this to be able to use it fluently
   */
  public MailMessage setText(String text) {
    this.text = text;
    return this;
  }

  /**
   * get the html text of this mail
   *
   * @return the text
   */
  public String getHtml() {
    return html;
  }

  /**
   * set the html text of this mail
   *
   * @param html the text
   * @return this to be able to use it fluently
   */
  public MailMessage setHtml(String html) {
    this.html = html;
    return this;
  }

  /**
   * get the list of attachments of this mail
   *
   * @return List of attachment
   */
  public List<MailAttachment> getAttachment() {
    return attachment;
  }

  /**
   * set the list of attachments of this mail
   *
   * @param attachment List of attachment
   * @return this to be able to use it fluently
   */
  public MailMessage setAttachment(List<MailAttachment> attachment) {
    this.attachment = attachment;
    return this;
  }

  /**
   * set a single attachment of this mail
   *
   * @param attachment the attachment to add
   * @return this to be able to use it fluently
   */
  @GenIgnore
  public MailMessage setAttachment(MailAttachment attachment) {
    this.attachment = Utils.asList(attachment);
    return this;
  }

  /**
   * get the list of inline attachments of this mail
   *
   * @return List of attachment
   */
  public List<MailAttachment> getInlineAttachment() {
    return inlineAttachment;
  }

  /**
   * set the list of inline attachments of this mail
   *
   * @param inlineAttachment List of attachment
   * @return this to be able to use it fluently
   */
  public MailMessage setInlineAttachment(List<MailAttachment> inlineAttachment) {
    this.inlineAttachment = inlineAttachment;
    return this;
  }

  /**
   * set a single inline attachment of this mail
   *
   * @param inlineAttachment the attachment to add
   * @return this to be able to use it fluently
   */
  @GenIgnore
  public MailMessage setInlineAttachment(MailAttachment inlineAttachment) {
    this.inlineAttachment = Utils.asList(inlineAttachment);
    return this;
  }

  /**
   * Add a message header.
   *
   * @param key  the header key
   * @param value  the header value
   * @return  a reference to this, so the API can be used fluently
   */
  public MailMessage addHeader(String key, String value) {
    if (headers == null) {
      headers = new CaseInsensitiveHeaders();
    }
    Objects.requireNonNull(key, "no null key accepted");
    Objects.requireNonNull(value, "no null value accepted");
    headers.add(key, value);
    return this;
  }

  /**
   * Get the headers.
   *
   * @return the headers
   */
  @GenIgnore
  public MultiMap getHeaders() {
    return headers;
  }

  /**
   * Set the headers.
   *
   * @param headers the headers to set
   * @return this to be able to use it fluently
   */
  @GenIgnore
  public MailMessage setHeaders(MultiMap headers) {
    this.headers = headers;
    return this;
  }

  /**
   * get whether our own headers should be the only headers added to the message
   *
   * @return the fixedHeaders
   */
  public boolean isFixedHeaders() {
    return fixedHeaders;
  }

  /**
   * set whether our own headers should be the only headers added to the message
   *
   * @param fixedHeaders the fixedHeaders to set
   * @return this to be able to use it fluently
   */
  public MailMessage setFixedHeaders(boolean fixedHeaders) {
    this.fixedHeaders = fixedHeaders;
    return this;
  }

  /**
   * convert the mail message to Json representation
   *
   * @return the json object
   */
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    Utils.putIfNotNull(json, "bounceAddress", bounceAddress);
    Utils.putIfNotNull(json, "from", from);
    Utils.putIfNotNull(json, "to", to);
    Utils.putIfNotNull(json, "cc", cc);
    Utils.putIfNotNull(json, "bcc", bcc);
    Utils.putIfNotNull(json, "subject", subject);
    Utils.putIfNotNull(json, "text", text);
    Utils.putIfNotNull(json, "html", html);
    if (attachment != null) {
      json.put("attachment", attachmentsToJson(attachment));
    }
    if (inlineAttachment != null) {
      json.put("inline_attachment", attachmentsToJson(inlineAttachment));
    }
    if (headers != null) {
      json.put("headers", Utils.multiMapToJson(headers));
    }
    if (fixedHeaders) {
      json.put("fixedheaders", true);
    }
    return json;
  }

  private JsonArray attachmentsToJson(List<MailAttachment> attachments) {
    JsonArray array = new JsonArray();
    for (MailAttachment a : attachments) {
      array.add(a.toJson());
    }
    return array;
  }
  
  private List<Object> getList() {
    return Arrays.asList(bounceAddress, from, to, cc, bcc, subject, text, html, attachment, inlineAttachment, headers, fixedHeaders);
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MailMessage)) {
      return false;
    }
    final MailMessage message = (MailMessage) o;

    return getList().equals(message.getList());
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return getList().hashCode();
  }

  private List<String> copyList(List<String> list) {
    if (list == null) {
      return null;
    } else {
      return new ArrayList<>(list);
    }
  }
}

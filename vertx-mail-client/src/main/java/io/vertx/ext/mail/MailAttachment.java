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
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Represent a mail attachment that can be used in a MailMessage.
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */

@DataObject
public class MailAttachment {

  private Buffer data;
  private String name;
  private String contentType;
  private String disposition;
  private String description;

  /**
   * construct an empty MailAttachment object that can be filled with the
   * setters
   */
  public MailAttachment() {
  }

  /**
   * create a copy of a MailAttachment object
   *
   * @param other object to be copied
   */
  public MailAttachment(final MailAttachment other) {
    Objects.requireNonNull(other);
    this.data = other.data == null ? null : other.data.copy();
    this.name = other.name;
    this.contentType = other.contentType;
    this.disposition = other.disposition;
    this.description = other.description;
  }

  /**
   * create a MailAttachment object from a JsonObject representation
   *
   * @param json object to be copied
   */
  public MailAttachment(final JsonObject json) {
    Objects.requireNonNull(json);
    this.data = json.getBinary("data") == null ? null : Buffer.buffer(json.getBinary("data"));
    this.name = json.getString("name");
    this.contentType = json.getString("contentType");
    this.disposition = json.getString("disposition");
    this.description = json.getString("description");
  }

  /**
   * get the data
   *
   * @return the data
   */
  public Buffer getData() {
    return data;
  }

  /**
   * set the data
   *
   * @param data Buffer of bytes to be used at attachment
   * @return this to be able to use it fluently
   */
  public MailAttachment setData(final Buffer data) {
    this.data = data;
    return this;
  }

  /**
   * get the name
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * set the name
   * @param name name of the attachment file
   * @return this to be able to use it fluently
   * <p>
   * name is the descriptive filename that will be put into the mail
   * i.e. usually a local filename without path
   * this can be set to "" to omit the filename attribute
   */
  public MailAttachment setName(final String name) {
    this.name = name;
    return this;
  }

  /**
   * get the Content-Type
   *
   * @return the contentType
   */
  public String getContentType() {
    return contentType;
  }

  /**
   * set the Content-Type
   *
   * @param contentType the contentType
   * @return this to be able to use it fluently
   */
  public MailAttachment setContentType(final String contentType) {
    this.contentType = contentType;
    return this;
  }

  /**
   * get the disposition field
   *
   * @return the disposition
   */
  public String getDisposition() {
    return disposition;
  }

  /**
   * set the disposition field to be used in the attachment
   *
   * @param disposition the disposition
   * @return this to be able to use it fluently
   */
  public MailAttachment setDisposition(final String disposition) {
    this.disposition = disposition;
    return this;
  }

  /**
   * get the description field
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * set the description field to be used in the attachment
   *
   * @param description the description
   * @return this to be able to use it fluently
   */
  public MailAttachment setDescription(final String description) {
    this.description = description;
    return this;
  }

  /**
   * convert this object to JSON representation
   *
   * @return the JSON object
   */
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    if (data != null) {
      putIfNotNull(json, "data", data.getBytes());
    }
    putIfNotNull(json, "name", name);
    putIfNotNull(json, "contentType", contentType);
    putIfNotNull(json, "disposition", disposition);
    putIfNotNull(json, "description", description);
    return json;
  }

  private List<Object> getList() {
    return Arrays.asList(data, name, disposition, description);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || !(o instanceof MailAttachment)) {
      return false;
    }
    final MailAttachment attachment = (MailAttachment) o;
    return getList().equals(attachment.getList());
  }

  @Override
  public int hashCode() {
    return getList().hashCode();
  }

  private void putIfNotNull(final JsonObject json, final String key, final Object value) {
    if (value != null) {
      json.put(key, value);
    }
  }

}

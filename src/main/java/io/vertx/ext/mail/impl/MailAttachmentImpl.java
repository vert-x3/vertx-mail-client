/*
 *  Copyright (c) 2011-2019 The original author or authors
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

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;
import io.vertx.ext.mail.MailAttachment;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * MailAttachment implementation.
 *
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
public class MailAttachmentImpl implements MailAttachment {

  private ReadStream<Buffer> stream;
  private int size = -1;
  private Buffer data;
  private String name;
  private String contentType;
  private String disposition;
  private String description;
  private String contentId;
  private MultiMap headers;

  /**
   * construct an empty MailAttachmentImpl object that can be filled with the
   * setters
   */
  public MailAttachmentImpl() {}

  /**
   * create a copy of a MailAttachmentImpl object
   *
   * @param otherMailAttachment object to be copied
   */
  public MailAttachmentImpl(MailAttachment otherMailAttachment) {
    Objects.requireNonNull(otherMailAttachment);
    MailAttachmentImpl other = (MailAttachmentImpl)otherMailAttachment;
    this.data = other.data == null ? null : other.data.copy();
    this.name = other.name;
    this.contentType = other.contentType;
    this.disposition = other.disposition;
    this.description = other.description;
    this.contentId = other.contentId;
    this.headers = other.headers == null ? null : MultiMap.caseInsensitiveMultiMap().addAll(other.headers);
    this.size = other.size;
    this.stream = other.stream;
  }

  /**
   * create a MailAttachmentImpl object from a JsonObject representation
   *
   * @param json object to be copied
   */
  public MailAttachmentImpl(final JsonObject json) {
    Objects.requireNonNull(json);
    this.data = json.getBinary("data") == null ? null : Buffer.buffer(json.getBinary("data"));
    this.name = json.getString("name");
    this.contentType = json.getString("contentType");
    this.disposition = json.getString("disposition");
    this.description = json.getString("description");
    this.contentId = json.getString("contentId");
    JsonObject headers = json.getJsonObject("headers");
    if (headers != null) {
      this.headers = Utils.jsonToMultiMap(headers);
    }
    this.size = json.getInteger("size", -1);
  }

  @Override
  public ReadStream<Buffer> getStream() {
    return this.stream;
  }

  @Override
  public MailAttachment setStream(ReadStream<Buffer> stream) {
    this.stream = stream;
    return this;
  }

  @Override
  public int getSize() {
    return this.size;
  }

  @Override
  public MailAttachment setSize(int size) {
    if (size < 0) {
      throw new IllegalArgumentException("Size of the Attachment cannot be smaller than 0");
    }
    this.size = size;
    return this;
  }

  @Override
  public Buffer getData() {
    return data;
  }

  @Override
  public MailAttachment setData(final Buffer data) {
    this.data = data;
    return this;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public MailAttachment setName(final String name) {
    this.name = name;
    return this;
  }

  @Override
  public String getContentType() {
    return contentType;
  }

  @Override
  public MailAttachment setContentType(final String contentType) {
    this.contentType = contentType;
    return this;
  }

  @Override
  public String getDisposition() {
    return disposition;
  }

  @Override
  public MailAttachment setDisposition(final String disposition) {
    this.disposition = disposition;
    return this;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public MailAttachment setDescription(final String description) {
    this.description = description;
    return this;
  }

  @Override
  public String getContentId() {
    return contentId;
  }

  @Override
  public MailAttachment setContentId(final String contentId) {
    this.contentId = contentId;
    return this;
  }

  @Override
  public MailAttachment addHeader(String key, String value) {
    if (headers == null) {
      headers = MultiMap.caseInsensitiveMultiMap();;
    }
    Objects.requireNonNull(key, "no null key accepted");
    Objects.requireNonNull(value, "no null value accepted");
    headers.add(key, value);
    return this;
  }

  @Override
  public MultiMap getHeaders() {
    return headers;
  }

  @Override
  public MailAttachment setHeaders(final MultiMap headers) {
    this.headers = headers;
    return this;
  }

  @Override
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    if (data != null) {
      json.put("data", data.getBytes());
    }
    Utils.putIfNotNull(json, "name", name);
    Utils.putIfNotNull(json, "contentType", contentType);
    Utils.putIfNotNull(json, "disposition", disposition);
    Utils.putIfNotNull(json, "description", description);
    Utils.putIfNotNull(json, "contentId", contentId);
    if (headers != null) {
      json.put("headers", Utils.multiMapToJson(headers));
    }
    if (this.size >= 0) {
      json.put("size", this.size);
    }
    return json;
  }

  private List<Object> getList() {
    return Arrays.asList(data, name, disposition, description, contentId, headers, size);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MailAttachmentImpl)) {
      return false;
    }
    final MailAttachmentImpl attachment = (MailAttachmentImpl) o;
    return getList().equals(attachment.getList());
  }

  @Override
  public int hashCode() {
    return getList().hashCode();
  }

}

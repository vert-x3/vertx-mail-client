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

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;

import io.vertx.ext.mail.impl.MailAttachmentImpl;

/**
 * Represent a mail attachment that can be used in a MailMessage.
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */

@VertxGen
public interface MailAttachment {

  /**
   * construct an empty MailAttachment object that can be filled with the
   * setters
   */
  static MailAttachment create() {
    return new MailAttachmentImpl();
  }

  /**
   * create a MailAttachment object from a JsonObject representation
   *
   * @param json object to be copied
   */
  static MailAttachment create(JsonObject json) {return new MailAttachmentImpl(json); }

   /**
    * create a copy of a MailAttachment object
    *
    * @param other object to be copied
    */
  static MailAttachment create(MailAttachment other) {
    return new MailAttachmentImpl(other);
   }

  /**
   * get the data
   *
   * @return the data
   */
  Buffer getData();

  /**
   * set the data
   *
   * @param data Buffer of bytes to be used at attachment
   * @return this to be able to use it fluently
   */
  @Fluent
  MailAttachment setData(Buffer data);

  /**
   * Gets the data stream.
   *
   * @return the data stream
   */
  ReadStream<Buffer> getStream();

  /**
   * Sets the data stream.
   *
   * @param stream data stream to be used at attachment
   * @return this to be able to use it fluently
   */
  @Fluent
  MailAttachment setStream(ReadStream<Buffer> stream);

  /**
   * Gets the size of the attachment.
   *
   * @return the size of the attachment
   */
  int getSize();

  /**
   * Sets the size of the attachment.
   *<p>
   * It is needed when using ReadStream for the MailAttachement.
   *</p>
   *
   * @param size the size of the attachment
   * @return this to be able to use it fluently
   */
  @Fluent
  MailAttachment setSize(int size);

  /**
   * get the name
   *
   * @return the name
   */
  String getName();

  /**
   * set the name
   * @param name name of the attachment file
   * @return this to be able to use it fluently
   * <p>
   * name is the descriptive filename that will be put into the mail
   * i.e. usually a local filename without path
   * this can be set to "" to omit the filename attribute
   */
  @Fluent
  MailAttachment setName(String name);

  /**
   * get the Content-Type
   *
   * @return the contentType
   */
  String getContentType();

  /**
   * set the Content-Type
   *
   * @param contentType the contentType
   * @return this to be able to use it fluently
   */
  @Fluent
  MailAttachment setContentType(String contentType);

  /**
   * get the disposition field
   *
   * @return the disposition
   */
  String getDisposition();

  /**
   * set the disposition field to be used in the attachment
   *
   * @param disposition the disposition
   * @return this to be able to use it fluently
   */
  @Fluent
  MailAttachment setDisposition(String disposition);

  /**
   * get the description field
   *
   * @return the description
   */
  String getDescription();

  /**
   * set the description field to be used in the attachment
   *
   * @param description the description
   * @return this to be able to use it fluently
   */
  @Fluent
  MailAttachment setDescription(String description);

  /**
   * get the Content-ID field
   *
   * @return the content id
   */
  String getContentId();

  /**
   * set the Content-ID field to be used in the attachment
   *
   * @param contentId the content id
   * @return this to be able to use it fluently
   */
  @Fluent
  MailAttachment setContentId(String contentId);

  /**
   * Add an header to this attachment.
   *
   * @param key  the header key
   * @param value  the header value
   * @return  a reference to this, so the API can be used fluently
   */
  @Fluent
  MailAttachment addHeader(String key, String value);

  /**
   * Get the headers to be added for this attachment.
   *
   * @return the headers
   */
  MultiMap getHeaders();

  /**
   * Set the headers to be added for this attachment.
   *
   * @param headers the headers to be added
   * @return this to be able to use it fluently
   */
  @Fluent
  MailAttachment setHeaders(MultiMap headers);

  /**
   * convert this object to JSON representation
   *
   * @return the JSON object
   */
  JsonObject toJson();

}

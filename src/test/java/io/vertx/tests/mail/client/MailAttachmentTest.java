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

package io.vertx.tests.mail.client;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mail.MailAttachment;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class MailAttachmentTest {

  @Test
  public void testConstructor() {
    final MailAttachment attachment = MailAttachment.create();
    assertNotNull(attachment);
  }

  @Test
  public void testToJson() {
    assertEquals("{}", MailAttachment.create().toJson().encode());
    assertEquals("{\"data\":\"ZGF0YQ\",\"contentType\":\"text/plain\",\"disposition\":\"inline\",\"description\":\"description\",\"contentId\":\"randomstring\",\"headers\":{}}",
      MailAttachment.create()
        .setData(Buffer.buffer("data"))
        .setContentType("text/plain")
        .setDescription("description")
        .setDisposition("inline")
        .setContentId("randomstring")
        .setHeaders(MultiMap.caseInsensitiveMultiMap())
        .toJson().encode());

    JsonObject json = MailAttachment.create()
      .setData(Buffer.buffer("hello\"\0\u0001\t\r\n\u00ffx\u00a0\u00a1<>")).toJson();
    assertEquals("{\"data\":\"aGVsbG8iAAEJDQrDv3jCoMKhPD4\"}", json.encode());
  }

  @Test
  public void testConstructorFromClass() {
    MailAttachment message = MailAttachment.create();
    assertEquals(message, MailAttachment.create(message));
  }

  @Test
  public void testConstructorFromClassDoesCopy() {
    MailAttachment message = MailAttachment.create();

    Buffer data = Buffer.buffer("asdf");
    message.setData(data);

    final MailAttachment copy = MailAttachment.create(message);
    assertEquals(message.getData(), copy.getData());
    assertFalse("Buffer not copied", message.getData()==copy.getData());
  }

  // TODO: this test is too complicated since CaseInsensitiveHeaders does not have equals currently
  @Test
  public void testConstructorFromClassHeaders() {
    MailAttachment message = MailAttachment.create();
    message.setHeaders(MultiMap.caseInsensitiveMultiMap().add("Header", "Value"));
    assertEquals("Value", MailAttachment.create(message).getHeaders().get("Header"));
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorFromJsonNull() {
    final MailAttachment attachment = MailAttachment.create((JsonObject) null);
    assertNotNull(attachment);
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorFromMailAttachmentNull() {
    final MailAttachment attachment = MailAttachment.create((MailAttachment) null);
    assertNotNull(attachment);
  }

  @Test
  public void testConstructorFromJsonEmpty() {
    assertEquals(MailAttachment.create(), MailAttachment.create(new JsonObject()));
  }

  @Test
  public void testConstructorFromJson() {
    final String jsonString = "{\"data\":\"YXNkZmc\",\"name\":\"filename.jpg\",\"headers\":{\"Content-ID\":[\"<image1@example.org\"],\"Header\":[\"Value\"]}}";
    JsonObject json = new JsonObject(jsonString);

    MailAttachment message = MailAttachment.create(json);

    assertEquals(jsonString, message.toJson().encode());
  }

  @Test
  public void testEquals() {
    MailAttachment mailAttachment = MailAttachment.create();
    assertEquals(mailAttachment, mailAttachment);
    assertEquals(mailAttachment, MailAttachment.create());
    assertFalse(mailAttachment.equals(null));
    assertFalse(mailAttachment.equals(new Object()));
  }

  @Test
  public void testHashcode() {
    MailAttachment mailAttachment = MailAttachment.create();
    assertEquals(mailAttachment.hashCode(), MailAttachment.create().hashCode());
  }

  @Test
  public void testName() {
    MailAttachment mailMessage = MailAttachment.create();
    mailMessage.setName("file.jpg");
    assertEquals("file.jpg", mailMessage.getName());
  }

  @Test
  public void testData() {
    MailAttachment mailMessage = MailAttachment.create();
    mailMessage.setData(Buffer.buffer("xxxx"));
    assertEquals("xxxx", mailMessage.getData().toString());
  }

  @Test
  public void testContentType() {
    MailAttachment mailMessage = MailAttachment.create();
    mailMessage.setContentType("text/plain");
    assertEquals("text/plain", mailMessage.getContentType());
  }

  @Test
  public void testDescription() {
    MailAttachment mailMessage = MailAttachment.create();
    mailMessage.setDescription("attachment");
    assertEquals("attachment", mailMessage.getDescription());
  }

  @Test
  public void testDispostion() {
    MailAttachment mailMessage = MailAttachment.create();
    mailMessage.setDisposition("inline");
    assertEquals("inline", mailMessage.getDisposition());
  }

  @Test
  public void testContentId() {
    MailAttachment mailMessage = MailAttachment.create();
    mailMessage.setContentId("id@example.org");
    assertEquals("id@example.org", mailMessage.getContentId());
  }

  @Test
  public void testHeaders() {
    MailAttachment mailMessage = MailAttachment.create();
    mailMessage.setHeaders(MultiMap.caseInsensitiveMultiMap().add("Header", "Value"));
    assertEquals("Value", mailMessage.getHeaders().get("Header"));
  }

}

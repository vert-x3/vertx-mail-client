/*
 *  Copyright (c) 2011-2026 The original author or authors
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

import io.vertx.core.MultiMap;
import io.vertx.ext.mail.MailAttachment;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.mailencoder.Utils;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Tests for CRLF header injection prevention (CWE-93).
 */
public class CRLFInjectionTest {

  // --- MailMessage.addHeader ---

  @Test(expected = IllegalArgumentException.class)
  public void testAddHeaderRejectsCRLFInValue() {
    new MailMessage().addHeader("X-Custom", "value\r\nInjected: header");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddHeaderRejectsBareNewlineInValue() {
    new MailMessage().addHeader("X-Custom", "value\nInjected: header");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddHeaderRejectsBareCarriageReturnInValue() {
    new MailMessage().addHeader("X-Custom", "value\rInjected: header");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddHeaderRejectsNewlineInKey() {
    new MailMessage().addHeader("X-Custom\nInjected", "value");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddHeaderRejectsCarriageReturnInKey() {
    new MailMessage().addHeader("X-Custom\rInjected", "value");
  }

  // --- MailMessage.setHeaders ---

  @Test(expected = IllegalArgumentException.class)
  public void testSetHeadersRejectsCRLFInValue() {
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    headers.add("X-Custom", "value\r\nInjected: header");
    new MailMessage().setHeaders(headers);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSetHeadersRejectsCRLFInKey() {
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    headers.add("X-Custom\nBad", "value");
    new MailMessage().setHeaders(headers);
  }

  // --- MailMessage.setSubject (regression) ---

  @Test(expected = IllegalArgumentException.class)
  public void testSetSubjectRejectsNewline() {
    new MailMessage().setSubject("Hello\nWorld");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSetSubjectRejectsCarriageReturn() {
    new MailMessage().setSubject("Hello\rWorld");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSetSubjectRejectsCRLF() {
    new MailMessage().setSubject("Hello\r\nWorld");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorSubjectRejectsNewline() {
    new MailMessage("from@example.com", "to@example.com", "Hello\nWorld", "body");
  }

  // --- MailAttachment.addHeader ---

  @Test(expected = IllegalArgumentException.class)
  public void testAttachmentAddHeaderRejectsCRLFInValue() {
    MailAttachment.create().addHeader("X-Custom", "value\r\nInjected: header");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAttachmentAddHeaderRejectsNewlineInKey() {
    MailAttachment.create().addHeader("X-Custom\nInjected", "value");
  }

  // --- MailAttachment.setContentType ---

  @Test(expected = IllegalArgumentException.class)
  public void testAttachmentSetContentTypeRejectsNewline() {
    MailAttachment.create().setContentType("text/plain\nInjected: header");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAttachmentSetContentTypeRejectsCarriageReturn() {
    MailAttachment.create().setContentType("text/plain\rInjected: header");
  }

  // --- MailAttachment.setDisposition ---

  @Test(expected = IllegalArgumentException.class)
  public void testAttachmentSetDispositionRejectsNewline() {
    MailAttachment.create().setDisposition("attachment\nInjected: header");
  }

  // --- MailAttachment.setDescription ---

  @Test(expected = IllegalArgumentException.class)
  public void testAttachmentSetDescriptionRejectsNewline() {
    MailAttachment.create().setDescription("A file\nInjected: header");
  }

  // --- MailAttachment.setContentId ---

  @Test(expected = IllegalArgumentException.class)
  public void testAttachmentSetContentIdRejectsNewline() {
    MailAttachment.create().setContentId("<id>\nInjected: header");
  }

  // --- MailAttachment.setHeaders ---

  @Test(expected = IllegalArgumentException.class)
  public void testAttachmentSetHeadersRejectsCRLFInValue() {
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    headers.add("X-Custom", "value\r\nInjected: header");
    MailAttachment.create().setHeaders(headers);
  }

  // --- Valid inputs still work ---

  @Test
  public void testValidHeadersAccepted() {
    MailMessage msg = new MailMessage();
    msg.addHeader("X-Custom", "valid value");
    msg.addHeader("X-Another", "also valid");
    assertTrue(msg.getHeaders().contains("X-Custom"));
    assertTrue(msg.getHeaders().contains("X-Another"));
  }

  @Test
  public void testValidSubjectAccepted() {
    MailMessage msg = new MailMessage();
    msg.setSubject("A perfectly valid subject");
    assertTrue("A perfectly valid subject".equals(msg.getSubject()));
  }

  @Test
  public void testNullSubjectAccepted() {
    MailMessage msg = new MailMessage();
    msg.setSubject(null);
    assertTrue(msg.getSubject() == null);
  }

  @Test
  public void testValidAttachmentFieldsAccepted() {
    MailAttachment attachment = MailAttachment.create();
    attachment.setContentType("text/plain");
    attachment.setDisposition("attachment");
    attachment.setDescription("A test file");
    attachment.setContentId("<unique-id@example.com>");
    attachment.addHeader("X-Custom", "value");
    assertTrue(attachment.getHeaders().contains("X-Custom"));
  }

  @Test
  public void testNullAttachmentFieldsAccepted() {
    MailAttachment attachment = MailAttachment.create();
    attachment.setContentType(null);
    attachment.setDisposition(null);
    attachment.setDescription(null);
    attachment.setContentId(null);
    // Should not throw
  }

  // --- Utils.mustEncode ---

  @Test
  public void testMustEncodeNewline() {
    assertTrue("LF (char 10) must be encoded", Utils.mustEncode('\n'));
  }

  @Test
  public void testMustEncodeCarriageReturn() {
    assertTrue("CR (char 13) must be encoded", Utils.mustEncode('\r'));
  }
}

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

package io.vertx.ext.mail.mailencoder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.CaseInsensitiveHeaders;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.ext.mail.MailAttachment;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.TestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.mail.BodyPart;
import javax.mail.internet.MimeMultipart;

import org.junit.Test;

public class MailEncoderTest {

  private static final String HOSTNAME = "my.hostname.com";
  private static final Logger log = LoggerFactory.getLogger(MailEncoderTest.class);

  @Test
  public void testEncode() {
    MailMessage message = new MailMessage();
    message.setSubject("this is the subject_äöü");
    message.setTo("user@example.com");
    message.setCc(Arrays.asList("user@example.com (User Name)", "user2@example.com (User with Ü)",
      "user3@example.com (ÄÖÜ)"));
    message.setFrom("from@example.com (User with Ü)");
    message
      .setText("asdf=\n\näöüÄÖÜ\u00ff\n\t=======================================================================================\n");
    message.setHtml("<a href=\"http://vertx.io\">vertx.io</a>\n");

    List<MailAttachment> attachments = new ArrayList<MailAttachment>();

    attachments
      .add(MailAttachment.create()
        .setData(Buffer.buffer("****************************************************************************************"))
        .setName("file.txt"));

    attachments.add(MailAttachment.create().setData(Buffer.buffer("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX")));

    attachments.add(MailAttachment.create()
      .setData(Buffer.buffer("испытание", "UTF-8")));

    attachments.add(MailAttachment.create().setData(TestUtils.asBuffer(new int[]{0xD0, 0xB8, 0xD1, 0x81, 0xD0, 0xBF,
      0xD1, 0x8B, 0xD1, 0x82, 0xD0, 0xB0, 0xD0, 0xBD, 0xD0, 0xB8, 0xD0, 0xB5})));

    message.setAttachment(attachments);

    MailEncoder encoder = new MailEncoder(message, HOSTNAME);
    log.info(encoder.encode());
  }


  /*
   * test completely empty message, doesn't make much sense but should not give a
   * NPE at least
   */
  @Test
  public void testEmptyMsg() throws Exception {
    MailMessage message = new MailMessage();
    String mime = new MailEncoder(message, HOSTNAME).encode();
    assertThat(mime, containsString("Message-ID:"));
    assertThat(TestUtils.getMessage(mime).getHeader("Message-ID")[0], containsString(".vertxmail."));
  }

  @Test
  public void testSubject() throws Exception {
    MailMessage message = new MailMessage();
    final String subject = "this is the subject";
    message.setSubject(subject);
    String mime = new MailEncoder(message, HOSTNAME).encode();
    assertThat(mime, containsString("Subject: this is the subject\n"));
    assertEquals(subject, TestUtils.getMessage(mime).getSubject());
  }

  @Test
  public void testFrom() throws Exception {
    MailMessage message = new MailMessage();
    message.setFrom("user@example.com (Username)");
    String mime = new MailEncoder(message, HOSTNAME).encode();
    assertThat(mime, containsString("From: user@example.com (Username)\n"));
    assertEquals("Username <user@example.com>", TestUtils.getMessage(mime).getFrom()[0].toString());
  }

  @Test
  public void testTo() throws Exception {
    MailMessage message = new MailMessage();
    message.setTo("user@example.com (Username)");
    String mime = new MailEncoder(message, HOSTNAME).encode();
    assertThat(mime, containsString("To: user@example.com (Username)\n"));
    assertEquals("Username <user@example.com>", TestUtils.getMessage(mime).getAllRecipients()[0].toString());
  }

  @Test
  public void testTo1() throws Exception {
    MailMessage message = new MailMessage();
    message.setTo(Arrays.asList("user@example.com (Username)"));
    String mime = new MailEncoder(message, HOSTNAME).encode();
    assertThat(mime, containsString("To: user@example.com (Username)\n"));
    assertEquals("Username <user@example.com>", TestUtils.getMessage(mime).getAllRecipients()[0].toString());
  }

  @Test
  public void testTo2() throws Exception {
    MailMessage message = new MailMessage();
    message.setTo(Arrays.asList("user@example.com (Username)", "user2@example.com"));
    String mime = new MailEncoder(message, HOSTNAME).encode();
    assertThat(mime, containsString("To: user@example.com (Username),user2@example.com\n"));
    assertEquals("Username <user@example.com>", TestUtils.getMessage(mime).getAllRecipients()[0].toString());
    assertEquals("user2@example.com", TestUtils.getMessage(mime).getAllRecipients()[1].toString());
  }

  @Test
  public void testToEncoded() throws Exception {
    MailMessage message = new MailMessage();
    message.setTo("user@example.com (Username ÄÖÜ)");
    String mime = new MailEncoder(message, HOSTNAME).encode();
    assertThat(mime, containsString("To: user@example.com (=?UTF-8?Q?Username_=C3=84=C3=96=C3=9C?=)\n"));
    assertEquals("=?UTF-8?Q?Username_=C3=84=C3=96=C3=9C?= <user@example.com>", TestUtils.getMessage(mime).getAllRecipients()[0].toString());
  }

  @Test
  public void testToMany() throws Exception {
    MailMessage message = new MailMessage();
    List<String> to = new ArrayList<String>();
    for (int i = 0; i < 20; i++) {
      to.add("user" + i + "@example.com");
    }
    message.setTo(to);
    String mime = new MailEncoder(message, HOSTNAME).encode();
    assertThat(mime, containsString("To: user0@example.com,user1@example.com,user2@example.com,user3@example.com,\n"
      + " user4@example.com,user5@example.com,user6@example.com,user7@example.com,\n"
      + " user8@example.com,user9@example.com,user10@example.com,user11@example.com,\n"
      + " user12@example.com,user13@example.com,user14@example.com,\n"
      + " user15@example.com,user16@example.com,user17@example.com,\n"
      + " user18@example.com,user19@example.com\n"));

    assertEquals(
        "[user0@example.com, user1@example.com, user2@example.com, user3@example.com, user4@example.com, user5@example.com, user6@example.com, user7@example.com, user8@example.com, user9@example.com, user10@example.com, user11@example.com, user12@example.com, user13@example.com, user14@example.com, user15@example.com, user16@example.com, user17@example.com, user18@example.com, user19@example.com]",
        Arrays.asList(TestUtils.getMessage(mime).getAllRecipients()).toString());
  }

  @Test
  public void testToManyName() throws Exception {
    MailMessage message = new MailMessage();
    List<String> to = new ArrayList<String>();
    for (int i = 0; i < 20; i++) {
      to.add("user" + i + "@example.com (Some User Name)");
    }
    message.setTo(to);
    String mime = new MailEncoder(message, HOSTNAME).encode();
    assertThat(mime, containsString("To: user0@example.com (Some User Name),user1@example.com\n"
      + " (Some User Name),user2@example.com (Some User Name),\n"
      + " user3@example.com (Some User Name),user4@example.com (Some User Name),\n"
      + " user5@example.com (Some User Name),user6@example.com (Some User Name),\n"
      + " user7@example.com (Some User Name),user8@example.com (Some User Name),\n"
      + " user9@example.com (Some User Name),user10@example.com\n"
      + " (Some User Name),user11@example.com (Some User Name),\n"
      + " user12@example.com (Some User Name),user13@example.com\n"
      + " (Some User Name),user14@example.com (Some User Name),\n"
      + " user15@example.com (Some User Name),user16@example.com\n"
      + " (Some User Name),user17@example.com (Some User Name),\n"
      + " user18@example.com (Some User Name),user19@example.com\n" + " (Some User Name)\n"));

    assertEquals(
        "[Some User Name <user0@example.com>, Some User Name <user1@example.com>, Some User Name <user2@example.com>, Some User Name <user3@example.com>, Some User Name <user4@example.com>, Some User Name <user5@example.com>, Some User Name <user6@example.com>, Some User Name <user7@example.com>, Some User Name <user8@example.com>, Some User Name <user9@example.com>, Some User Name <user10@example.com>, Some User Name <user11@example.com>, Some User Name <user12@example.com>, Some User Name <user13@example.com>, Some User Name <user14@example.com>, Some User Name <user15@example.com>, Some User Name <user16@example.com>, Some User Name <user17@example.com>, Some User Name <user18@example.com>, Some User Name <user19@example.com>]",
        Arrays.asList(TestUtils.getMessage(mime).getAllRecipients()).toString());
  }

  @Test
  public void testToManyEncoded() throws Exception {
    MailMessage message = new MailMessage();
    List<String> to = new ArrayList<String>();
    for (int i = 0; i < 20; i++) {
      to.add("user" + i + "@example.com (Äa)");
    }
    message.setTo(to);
    String mime = new MailEncoder(message, HOSTNAME).encode();
    assertThat(mime, containsString("To: user0@example.com (=?UTF-8?Q?=C3=84a?=),user1@example.com\n"
      + " (=?UTF-8?Q?=C3=84a?=),user2@example.com (=?UTF-8?Q?=C3=84a?=),\n"
      + " user3@example.com (=?UTF-8?Q?=C3=84a?=),user4@example.com (=?UTF-8?Q?=C3?=\n"
      + " =?UTF-8?Q?=84a?=),user5@example.com (=?UTF-8?Q?=C3=84a?=),\n"
      + " user6@example.com (=?UTF-8?Q?=C3=84a?=),user7@example.com (=?UTF-8?Q?=C3?=\n"
      + " =?UTF-8?Q?=84a?=),user8@example.com (=?UTF-8?Q?=C3=84a?=),\n"
      + " user9@example.com (=?UTF-8?Q?=C3=84a?=),user10@example.com\n"
      + " (=?UTF-8?Q?=C3=84a?=),user11@example.com (=?UTF-8?Q?=C3=84a?=),\n"
      + " user12@example.com (=?UTF-8?Q?=C3=84a?=),user13@example.com\n"
      + " (=?UTF-8?Q?=C3=84a?=),user14@example.com (=?UTF-8?Q?=C3=84a?=),\n"
      + " user15@example.com (=?UTF-8?Q?=C3=84a?=),user16@example.com\n"
      + " (=?UTF-8?Q?=C3=84a?=),user17@example.com (=?UTF-8?Q?=C3=84a?=),\n"
      + " user18@example.com (=?UTF-8?Q?=C3=84a?=),user19@example.com\n" + " (=?UTF-8?Q?=C3=84a?=)\n"));
  }

  @Test
  public void testToLong() {
    MailMessage message = new MailMessage();
    message
      .setTo("user@example.com (this email has an insanely long username just to check that the text is correctly wrapped into multiple lines)");
    String mime = new MailEncoder(message, HOSTNAME).encode();
    assertThat(
      mime,
      containsString("To: user@example.com\n"
        + " (this email has an insanely long username just to check that the text is correctly wrapped into multiple lines)\n"));
  }

  @Test
  public void testToLongEncoded() {
    MailMessage message = new MailMessage();
    message
      .setTo("user@example.com (ä this email has an insanely long username just to check that the text is correctly wrapped into multiple lines)");
    String mime = new MailEncoder(message, HOSTNAME).encode();
    assertThat(mime, containsString("To: user@example.com (=?UTF-8?Q?=C3=A4_this_email_has_an_insanely_long_use?=\n"
      + " =?UTF-8?Q?rname_just_to_check_that_the_text_is_correctly_wrapped_into_mul?=\n"
      + " =?UTF-8?Q?tiple_lines?=)\n"));
  }

  @Test
  public void testTextPlain() throws Exception {
    MailMessage message = new MailMessage();
    final String text = "the quick brown fox jumps over the lazy dog";
    message.setText(text);
    String mime = new MailEncoder(message, HOSTNAME).encode();
    assertThat(mime, containsString("Content-Type: text/plain"));
    assertThat(mime, containsString(text));
    assertEquals(text, TestUtils.getMessage(mime).getContent());
  }

  @Test
  public void testTextHtml() throws Exception {
    MailMessage message = new MailMessage();
    final String text = "the <b>quick brown fox</b> jumps over the lazy dog";
    message.setHtml(text);
    String mime = new MailEncoder(message, HOSTNAME).encode();
    assertThat(mime, containsString("Content-Type: text/html"));
    assertThat(mime, containsString(text));
    assertEquals(text, TestUtils.getMessage(mime).getContent());
  }

  @Test
  public void testTextPlainEncoded() throws Exception {
    MailMessage message = new MailMessage();
    final String text = "Zwölf Boxkämpfer jagen Viktor quer über den großen Sylter Deich";
    final String encodedtext = "Zw=C3=B6lf Boxk=C3=A4mpfer jagen Viktor quer =C3=BCber den gro=C3=9Fen Sylt=\n"
      + "er Deich";
    message.setHtml(text);
    String mime = new MailEncoder(message, HOSTNAME).encode();
    assertThat(mime, containsString("Content-Type: text/html"));
    assertThat(mime, containsString(encodedtext));
    assertEquals(text, TestUtils.getMessage(mime).getContent());
  }

  @Test
  public void testTextHtmlEncoded() throws Exception {
    MailMessage message = new MailMessage();
    final String text = "<a href=\"http://vertx.io/\">go\u00a0to\u00a0vertx.io</a>";
    final String encodedtext = "<a href=3D\"http://vertx.io/\">go=C2=A0to=C2=A0vertx.io</a>";
    message.setHtml(text);
    String mime = new MailEncoder(message, HOSTNAME).encode();
    assertThat(mime, containsString("Content-Type: text/html"));
    assertThat(mime, containsString(encodedtext));
    assertEquals(text, TestUtils.getMessage(mime).getContent());
  }

  @Test
  public void testSubjectEncoded() throws Exception {
    MailMessage message = new MailMessage();
    final String subject = "subject with äöü_=??=";
    final String encodedSubject = "=?UTF-8?Q?subject_with_=C3=A4=C3=B6=C3=BC=5F=3D=3F=3F=3D?=";
    message.setSubject(subject);
    String mime = new MailEncoder(message, HOSTNAME).encode();
    assertThat(mime, containsString(encodedSubject));
    assertEquals(subject, TestUtils.getMessage(mime).getSubject());
  }

  @Test
  public void testSubjectEncodedLong() throws Exception {
    MailMessage message = new MailMessage();
    final String subject = "ä=======================================================================================";
    final String encodedSubject = "Subject: =?UTF-8?Q?=C3=A4=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D?=\n"
      + " =?UTF-8?Q?=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D?=\n"
      + " =?UTF-8?Q?=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D?=\n"
      + " =?UTF-8?Q?=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D?=\n"
      + " =?UTF-8?Q?=3D=3D=3D=3D=3D=3D=3D=3D?=";
    message.setSubject(subject);
    String mime = new MailEncoder(message, HOSTNAME).encode();
    assertThat(mime, containsString(encodedSubject));
    assertEquals(subject, TestUtils.getMessage(mime).getSubject());
  }

  @Test
  public void testSubjectEncodedLong2() throws Exception {
    MailMessage message = new MailMessage();
    final String subject = "ä****************************************************************************************************************";
    final String encodedSubject = "Subject: =?UTF-8?Q?=C3=A4*************************************************?=\n"
      + " =?UTF-8?Q?***************************************************************?=\n";
    message.setSubject(subject);
    String mime = new MailEncoder(message, HOSTNAME).encode();
    assertThat(mime, containsString(encodedSubject));
    assertEquals(subject, TestUtils.getMessage(mime).getSubject());
  }

  @Test
  public void testSubjectEncodedNul() throws Exception {
    MailMessage message = new MailMessage();
    final String subject = "\0";
    final String encodedSubject = "=00";
    message.setSubject(subject);
    String mime = new MailEncoder(message, HOSTNAME).encode();
    assertThat(mime, containsString(encodedSubject));
    assertEquals(subject, TestUtils.getMessage(mime).getSubject());
  }

  @Test
  public void testTextPlain76Chars() throws Exception {
    MailMessage message = new MailMessage();
    final String text = "ä**********************************************************************";
    final String encodedSubject = "=C3=A4**********************************************************************";
    message.setText(text);
    String mime = new MailEncoder(message, HOSTNAME).encode();
    assertThat(mime, containsString(encodedSubject));
    assertEquals(text, TestUtils.getMessage(mime).getContent());
  }

  @Test
  public void testTextPlainEOLSpace() throws Exception {
    MailMessage message = new MailMessage();
    final String text = "ä ";
    final String encodedSubject = "=C3=A4=20";
    message.setText(text);
    String mime = new MailEncoder(message, HOSTNAME).encode();
    assertThat(mime, containsString(encodedSubject));
    assertEquals(text, TestUtils.getMessage(mime).getContent());
  }

  /**
   * if the subject is already encoded, do not encode it again. this leads to a
   * non-reversible operation, but it works around mail clients that do not
   * support MIME encoded headers e.g. if the subject is
   * "Re: =?UTF-8?Q?something_encoded?=" the reply should keep that and not
   * create a twice encoded subject
   */
  @Test
  public void testSubjectAlreadyEncoded() throws Exception {
    MailMessage message = new MailMessage();
    final String text = "Re: =?ISO-8859-1?Q?Hello_=FC?=";
    final String encodedSubject = "Re: =?ISO-8859-1?Q?Hello_=FC?=";
    message.setSubject(text);
    String mime = new MailEncoder(message, HOSTNAME).encode();
    assertThat(mime, containsString(encodedSubject));
    assertEquals("Re: Hello ü", TestUtils.getMessage(mime).getSubject());
  }

  @Test
  public void testToEmailOnly() throws Exception {
    MailMessage message = new MailMessage();
    final String to = "user@example.com";
    message.setTo(to);
    String mime = new MailEncoder(message, HOSTNAME).encode();
    assertThat(mime, containsString(to));
    assertEquals(to, TestUtils.getMessage(mime).getAllRecipients()[0].toString());
  }

  @Test
  public void testAttachment() throws Exception {
    MailMessage message = new MailMessage();
    MailAttachment attachment = MailAttachment.create();
    attachment.setContentType("application/x-something")
      .setData(Buffer.buffer("***"))
      .setDescription("description")
      .setDisposition("attachment")
      .setName("file.txt");
    message.setAttachment(attachment);
    String mime = new MailEncoder(message, HOSTNAME).encode();
    assertThat(mime, containsString("Content-Type: application/x-something; name=\"file.txt\""));
    assertThat(mime, containsString("Content-Description: description"));
    assertThat(mime, containsString("Content-Disposition: attachment; filename=\"file.txt\""));

    BodyPart part = ((MimeMultipart) TestUtils.getMessage(mime).getContent()).getBodyPart(0);
    assertEquals("***", TestUtils.inputStreamToString(part.getInputStream()));
    assertEquals("attachment", part.getDisposition());
    assertEquals("file.txt", part.getFileName());
    assertEquals("description", part.getDescription());
    assertEquals("application/x-something; name=\"file.txt\"", part.getContentType());
  }

  @Test
  public void testRealnameComma() throws Exception {
    MailMessage message = new MailMessage();
    message.setTo("Last, First <user@example.com>");
    String mime = new MailEncoder(message, HOSTNAME).encode();
    assertThat(mime, containsString("user@example.com (Last, First)"));
    assertEquals("\"Last, First\" <user@example.com>", TestUtils.getMessage(mime).getAllRecipients()[0].toString());
  }

  @Test
  public void testHeaders() {
    MailMessage message = new MailMessage();
    MultiMap headers = new CaseInsensitiveHeaders();
    headers.set("X-Header", "value");
    message.setHeaders(headers);
    String mime = new MailEncoder(message, HOSTNAME).encode();
    assertThat(mime, containsString("X-Header: value"));
  }

  @Test
  public void testHeadersExist() {
    MailMessage message = new MailMessage();
    MultiMap headers = new CaseInsensitiveHeaders();
    headers.set("mime-version", "2.1");
    message.setHeaders(headers);
    String mime = new MailEncoder(message, HOSTNAME).encode().toLowerCase(Locale.ENGLISH);
    assertThat(mime, containsString("mime-version: 2.1"));
    assertThat(mime, not(containsString("mime-version: 1.0")));
  }

  @Test
  public void testFixedHeaders() {
    MailMessage message = new MailMessage();
    MultiMap headers = new CaseInsensitiveHeaders();
    headers.set("X-Header", "value");
    message.setHeaders(headers);
    message.setFixedHeaders(true);
    String mime = new MailEncoder(message, HOSTNAME).encode();
    assertThat(mime, containsString("X-Header: value"));
    assertThat(mime, not(containsString("MIME-Version: 1.0")));
  }

  @Test
  public void testFixedHeadersExist() {
    MailMessage message = new MailMessage();
    MultiMap headers = new CaseInsensitiveHeaders();
    headers.set("Content-Type", "type");
    message.setHeaders(headers);
    message.setFixedHeaders(true);
    String mime = new MailEncoder(message, HOSTNAME).encode();
    assertThat(mime, containsString("Content-Type: type"));
    assertThat(mime, not(containsString("Content-Type: text/plain")));
  }

  @Test
  public void testFixedHeadersMessage() {
    MailMessage message = new MailMessage();
    message.setHeaders(new CaseInsensitiveHeaders());
    message.setFixedHeaders(true);
    message.setText("message text");
    String mime = new MailEncoder(message, HOSTNAME).encode();
    assertEquals("\nmessage text", TestUtils.conv2nl(mime));
  }

  @Test
  public void testFixedHeadersMultiple() {
    MailMessage message = new MailMessage();
    final MultiMap headers = new CaseInsensitiveHeaders();
    headers.add("Header", "value1");
    headers.add("Header", "value2");
    headers.add("Header2", "value3");
    headers.add("Header", "value4");
    message.setHeaders(headers);
    message.setFixedHeaders(true);
    message.setText("message text");
    String mime = new MailEncoder(message, HOSTNAME).encode();
    assertEquals("Header: value1\n" +
      "Header: value2\n" +
      "Header2: value3\n" +
      "Header: value4\n" +
      "\n" +
      "message text", TestUtils.conv2nl(mime));
  }

  @Test
  public void testSetHostname() {
    MailMessage message = new MailMessage();
    String mime = new MailEncoder(message, "myhostname.example.com").encode();
    assertThat(mime, containsString("Message-ID:"));
    assertThat(mime, containsString("@myhostname.example.com"));
  }

  /*
   *  test that tabs not not turn on quoted-printable encoding unless there are
   *  other chars that make it necessary 
   */
  @Test
  public void testMailTextContainsTabs() throws Exception {
    MailMessage message = new MailMessage();
    message.setText("\t\n");
    String mime = new MailEncoder(message, HOSTNAME).encode();
    assertThat(mime, containsString("\t"));
    assertThat(mime, not(containsString("=09")));
  }

  @Test
  public void testMailTextContainsTabs2() throws Exception {
    MailMessage message = new MailMessage();
    message.setText("\tä\n");
    String mime = new MailEncoder(message, HOSTNAME).encode();
    assertThat(mime, not(containsString("\t")));
    assertThat(mime, containsString("=09"));
  }

  @Test
  public void testAdditionalHeadersAttachment() {
    MailMessage message = new MailMessage();
    MailAttachment attachment = MailAttachment.create();
    attachment.setData(Buffer.buffer("XXX"))
      .setHeaders(new CaseInsensitiveHeaders().add("X-Header", "value"));
    message.setAttachment(attachment);
    String mime = new MailEncoder(message, HOSTNAME).encode();
    assertThat(mime, containsString("X-Header: value"));
  }

  @Test
  public void testInlineAttachment() {
    MailMessage message = new MailMessage();
    message.setHtml("this is a html message");
    MailAttachment attachment = MailAttachment.create();
    attachment.setData(Buffer.buffer("XXX"))
      .setDisposition("inline");
    message.setInlineAttachment(attachment);
    String mime = new MailEncoder(message, HOSTNAME).encode();
    assertThat(mime, containsString("multipart/related"));
  }

  @Test
  public void testGetMessageID() {
    MailMessage message = new MailMessage();
    final MailEncoder encoder = new MailEncoder(message, HOSTNAME);
    encoder.encode();
    assertThat(encoder.getMessageID(), containsString(HOSTNAME));
  }

  @Test
  public void testHtmlWithInlineImg() {
    MailMessage email = new MailMessage()
        .setFrom("user@example.net")
        .setTo("user@example.net")
        .setSubject("Test email")
        .setText("this mail is readable as html only")
        .setHtml("<img src=\"cid:image1@localhost\">");

    List<MailAttachment> list=new ArrayList<MailAttachment>();
    MailAttachment attachment = MailAttachment.create();
    attachment.setData(Buffer.buffer("******"));
    attachment.setContentType("image/jpg");
    attachment.setName("image1.jpg");
    attachment.setDisposition("inline");
    attachment.setContentId("image1@localhost");
    list.add(attachment);
    email.setInlineAttachment(list);

    String encoded = new MailEncoder(email, HOSTNAME).encode();
    System.out.println(encoded);
    assertThat(encoded, containsString("Content-ID: image1@localhost"));
  }

  /*
   * https://github.com/vert-x3/vertx-mail-client/issues/82
   * attachment filename must be encoded
   */
  @Test
  public void testAttachmentUtf8() {
    MailMessage message = new MailMessage();
    MailAttachment attachment = MailAttachment.create();
    attachment.setData(Buffer.buffer("test"));
    attachment.setName("你好.txt");
    message.setAttachment(attachment);
    final MailEncoder encoder = new MailEncoder(message, HOSTNAME);
    String mime = encoder.encode();
    System.out.println(mime);
    assertThat(mime, not(containsString("你好")));
  }

}

package io.vertx.ext.mail.mailencoder;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.CaseInsensitiveHeaders;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.mail.MailAttachment;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.TestUtils;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;

public class MailEncoderTest {

  private static final Logger log = LoggerFactory.getLogger(MailEncoder.class);

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
      .add(new MailAttachment()
        .setData(Buffer.buffer("****************************************************************************************"))
        .setName("file.txt"));

    attachments.add(new MailAttachment().setData(Buffer.buffer("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX")));

    attachments.add(new MailAttachment()
      .setData(Buffer.buffer("испытание", "UTF-8")));

    attachments.add(new MailAttachment().setData(TestUtils.asBuffer(new int[]{0xD0, 0xB8, 0xD1, 0x81, 0xD0, 0xBF,
      0xD1, 0x8B, 0xD1, 0x82, 0xD0, 0xB0, 0xD0, 0xBD, 0xD0, 0xB8, 0xD0, 0xB5})));

    message.setAttachment(attachments);

    MailEncoder encoder = new MailEncoder(message, null);
    log.info(encoder.encode());
  }


  /*
   * test completely empty message, doesn't make much sense but should not give a
   * NPE at least
   */
  @Test
  public void testEmptyMsg() {
    MailMessage message = new MailMessage();
    String mime = new MailEncoder(message, null).encode();
    assertThat(mime, containsString("Message-ID:"));
  }

  @Test
  public void testSubject() {
    MailMessage message = new MailMessage();
    message.setSubject("this is the subject");
    String mime = new MailEncoder(message, null).encode();
    assertThat(mime, containsString("Subject: this is the subject\n"));
  }

  @Test
  public void testFrom() {
    MailMessage message = new MailMessage();
    message.setFrom("user@example.com (Username)");
    String mime = new MailEncoder(message, null).encode();
    assertThat(mime, containsString("From: user@example.com (Username)\n"));
  }

  @Test
  public void testTo() {
    MailMessage message = new MailMessage();
    message.setTo("user@example.com (Username)");
    String mime = new MailEncoder(message, null).encode();
    assertThat(mime, containsString("To: user@example.com (Username)\n"));
  }

  @Test
  public void testTo1() {
    MailMessage message = new MailMessage();
    message.setTo(Arrays.asList("user@example.com (Username)"));
    String mime = new MailEncoder(message, null).encode();
    assertThat(mime, containsString("To: user@example.com (Username)\n"));
  }

  @Test
  public void testTo2() {
    MailMessage message = new MailMessage();
    message.setTo(Arrays.asList("user@example.com (Username)", "user2@example.com"));
    String mime = new MailEncoder(message, null).encode();
    assertThat(mime, containsString("To: user@example.com (Username),user2@example.com\n"));
  }

  @Test
  public void testToMany() {
    MailMessage message = new MailMessage();
    List<String> to = new ArrayList<String>();
    for (int i = 0; i < 20; i++) {
      to.add("user" + i + "@example.com");
    }
    message.setTo(to);
    String mime = new MailEncoder(message, null).encode();
    assertThat(mime, containsString("To: user0@example.com,user1@example.com,user2@example.com,user3@example.com,\n"
      + " user4@example.com,user5@example.com,user6@example.com,user7@example.com,\n"
      + " user8@example.com,user9@example.com,user10@example.com,user11@example.com,\n"
      + " user12@example.com,user13@example.com,user14@example.com,\n"
      + " user15@example.com,user16@example.com,user17@example.com,\n" + " user18@example.com,user19@example.com\n"));
  }

  @Test
  public void testToManyName() {
    MailMessage message = new MailMessage();
    List<String> to = new ArrayList<String>();
    for (int i = 0; i < 20; i++) {
      to.add("user" + i + "@example.com (Some User Name)");
    }
    message.setTo(to);
    String mime = new MailEncoder(message, null).encode();
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
  }

  @Test
  public void testToManyEncoded() {
    MailMessage message = new MailMessage();
    List<String> to = new ArrayList<String>();
    for (int i = 0; i < 20; i++) {
      to.add("user" + i + "@example.com (Äa)");
    }
    message.setTo(to);
    String mime = new MailEncoder(message, null).encode();
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
    String mime = new MailEncoder(message, null).encode();
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
    String mime = new MailEncoder(message, null).encode();
    assertThat(mime, containsString("To: user@example.com (=?UTF-8?Q?=C3=A4_this_email_has_an_insanely_long_use?=\n"
      + " =?UTF-8?Q?rname_just_to_check_that_the_text_is_correctly_wrapped_into_mul?=\n"
      + " =?UTF-8?Q?tiple_lines?=)\n"));
  }

  @Test
  public void testTextPlain() {
    MailMessage message = new MailMessage();
    final String text = "the quick brown fox jumps over the lazy dog";
    message.setText(text);
    String mime = new MailEncoder(message, null).encode();
    assertThat(mime, containsString("Content-Type: text/plain"));
    assertThat(mime, containsString(text));
  }

  @Test
  public void testTextHtml() {
    MailMessage message = new MailMessage();
    final String text = "the <b>quick brown fox</b> jumps over the lazy dog";
    message.setHtml(text);
    String mime = new MailEncoder(message, null).encode();
    assertThat(mime, containsString("Content-Type: text/html"));
    assertThat(mime, containsString(text));
  }

  // TODO would be better to check the decoded text?
  // Issue #29
  @Test
  public void testTextPlainEncoded() {
    MailMessage message = new MailMessage();
    final String text = "Zwölf Boxkämpfer jagen Viktor quer über den großen Sylter Deich";
    final String encodedtext = "Zw=C3=B6lf Boxk=C3=A4mpfer jagen Viktor quer =C3=BCber den gro=C3=9Fen Sylt=\n"
      + "er Deich";
    message.setHtml(text);
    String mime = new MailEncoder(message, null).encode();
    assertThat(mime, containsString("Content-Type: text/html"));
    assertThat(mime, containsString(encodedtext));
  }

  @Test
  public void testTextHtmlEncoded() {
    MailMessage message = new MailMessage();
    final String text = "<a href=\"http://vertx.io/\">go\u00a0to\u00a0vertx.io</a>";
    final String encodedtext = "<a href=3D\"http://vertx.io/\">go=C2=A0to=C2=A0vertx.io</a>";
    message.setHtml(text);
    String mime = new MailEncoder(message, null).encode();
    assertThat(mime, containsString("Content-Type: text/html"));
    assertThat(mime, containsString(encodedtext));
  }

  @Test
  public void testSubjectEncoded() {
    MailMessage message = new MailMessage();
    final String subject = "subject with äöü_=??=";
    final String encodedSubject = "=?UTF-8?Q?subject_with_=C3=A4=C3=B6=C3=BC=5F=3D=3F=3F=3D?=";
    message.setSubject(subject);
    String mime = new MailEncoder(message, null).encode();
    assertThat(mime, containsString(encodedSubject));
  }

  @Test
  public void testSubjectEncodedLong() {
    MailMessage message = new MailMessage();
    final String subject = "ä=======================================================================================";
    final String encodedSubject = "Subject: =?UTF-8?Q?=C3=A4=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D?=\n"
      + " =?UTF-8?Q?=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D?=\n"
      + " =?UTF-8?Q?=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D?=\n"
      + " =?UTF-8?Q?=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D?=\n"
      + " =?UTF-8?Q?=3D=3D=3D=3D=3D=3D=3D=3D?=";
    message.setSubject(subject);
    String mime = new MailEncoder(message, null).encode();
    assertThat(mime, containsString(encodedSubject));
  }

  @Test
  public void testSubjectEncodedLong2() {
    MailMessage message = new MailMessage();
    final String subject = "ä****************************************************************************************************************";
    final String encodedSubject = "Subject: =?UTF-8?Q?=C3=A4*************************************************?=\n"
      + " =?UTF-8?Q?***************************************************************?=\n";
    message.setSubject(subject);
    String mime = new MailEncoder(message, null).encode();
    assertThat(mime, containsString(encodedSubject));
  }

  @Test
  public void testSubjectEncodedNul() {
    MailMessage message = new MailMessage();
    final String subject = "\0";
    final String encodedSubject = "=00";
    message.setSubject(subject);
    String mime = new MailEncoder(message, null).encode();
    assertThat(mime, containsString(encodedSubject));
  }

  @Test
  public void testTextPlain76Chars() {
    MailMessage message = new MailMessage();
    final String text = "ä**********************************************************************";
    final String encodedSubject = "=C3=A4**********************************************************************";
    message.setText(text);
    String mime = new MailEncoder(message, null).encode();
    assertThat(mime, containsString(encodedSubject));
  }

  @Test
  public void testTextPlainEOLSpace() {
    MailMessage message = new MailMessage();
    final String text = "ä ";
    final String encodedSubject = "=C3=A4=20";
    message.setText(text);
    String mime = new MailEncoder(message, null).encode();
    assertThat(mime, containsString(encodedSubject));
  }

  /**
   * if the subject is already encoded, do not encode it again this leads to a
   * non-reversible operation, but it works around mail clients that do not
   * support MIME encoded headers e.g. if the subject is
   * "Re: =?UTF-8?Q?something_encoded?=" the reply should keep that and not
   * create a twice encoded subject
   */
  @Test
  public void testSubjectAlreadyEncoded() {
    MailMessage message = new MailMessage();
    final String text = "Re: =?ISO-8859-1?Q?Hello_=FC?=";
    final String encodedSubject = "Re: =?ISO-8859-1?Q?Hello_=FC?=";
    message.setText(text);
    String mime = new MailEncoder(message, null).encode();
    assertThat(mime, containsString(encodedSubject));
  }

  @Test
  public void testToEmailOnly() {
    MailMessage message = new MailMessage();
    message.setTo("user@example.com");
    String mime = new MailEncoder(message, null).encode();
    assertThat(mime, containsString("user@example.com"));
  }

  @Test
  public void testAttachment() {
    MailMessage message = new MailMessage();
    MailAttachment attachment = new MailAttachment();
    attachment.setContentType("application/x-something")
      .setData(Buffer.buffer("***"))
      .setDescription("description")
      .setDisposition("attachment")
      .setName("file.txt");
    message.setAttachment(attachment);
    String mime = new MailEncoder(message, null).encode();
    assertThat(mime, containsString("Content-Type: application/x-something; name=file.txt"));
    assertThat(mime, containsString("Content-Description: description"));
    assertThat(mime, containsString("Content-Disposition: attachment; filename=file.txt"));
  }

  @Test
  public void testRealnameComma() {
    MailMessage message = new MailMessage();
    message.setTo("Last, First <user@example.com>");
    String mime = new MailEncoder(message, null).encode();
    assertThat(mime, containsString("user@example.com (Last, First)"));
  }

  @Test
  public void testHeaders() {
    MailMessage message = new MailMessage();
    MultiMap headers = new CaseInsensitiveHeaders();
    headers.set("X-Header", "value");
    message.setHeaders(headers);
    String mime = new MailEncoder(message, null).encode();
    assertThat(mime, containsString("X-Header: value"));
  }

  @Test
  public void testHeadersExist() {
    MailMessage message = new MailMessage();
    MultiMap headers = new CaseInsensitiveHeaders();
    headers.set("mime-version", "2.1");
    message.setHeaders(headers);
    String mime = new MailEncoder(message, null).encode().toLowerCase(Locale.ENGLISH);
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
    String mime = new MailEncoder(message, null).encode();
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
    String mime = new MailEncoder(message, null).encode();
    assertThat(mime, containsString("Content-Type: type"));
    assertThat(mime, not(containsString("Content-Type: text/plain")));
  }

  @Test
  public void testFixedHeadersMessage() {
    MailMessage message = new MailMessage();
    message.setHeaders(new CaseInsensitiveHeaders());
    message.setFixedHeaders(true);
    message.setText("message text");
    String mime = new MailEncoder(message, null).encode();
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
    String mime = new MailEncoder(message, null).encode();
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

}

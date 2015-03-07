package io.vertx.ext.mail.mailencoder;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.mail.MailAttachment;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.mailencoder.MailEncoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.core.StringContains.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

public class MailEncoderTest {

  private static final Logger log = LoggerFactory.getLogger(MailEncoder.class);

  @Ignore
  @Test
  public void test() {
    MailMessage message = new MailMessage();
    message.setSubject("this is the subject_äöü");
    message.setTo("user@example.com");
    message.setCc(Arrays.asList("user@example.com (User Name)", "user2@example.com (User with Ü)", "user3@example.com (ÄÖÜ)"));
    message.setFrom("from@example.com (User with Ü)");
    message.setText("asdf=\n\näöüÄÖÜ\u00ff\n\t=======================================================================================\n");
    message.setHtml("<a href=\"http://vertx.io\">vertx.io</a>\n");

    List<MailAttachment> attachments = new ArrayList<MailAttachment>();

    attachments.add(new MailAttachment()
      .setData("****************************************************************************************")
      .setName("file.txt"));

    attachments.add(new MailAttachment()
      .setData("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"));

    // this one is incorrect since the data has to be values between 0x00-0xff
    attachments.add(new MailAttachment()
    .setData("\u0417\u043d\u0430\u043c\u0435\u043d\u0438\u0442\u043e\u0441\u0442\u0438"));

    attachments.add(new MailAttachment()
    .setData("\u00D0\u0097\u00D0\u00BD\u00D0\u00B0\u00D0\u00BC\u00D0\u00B5\u00D0\u00BD\u00D0\u00B8\u00D1\u0082\u00D0\u00BE\u00D1\u0081\u00D1\u0082\u00D0\u00B8"));

    message.setAttachment(attachments);

    MailEncoder encoder=new MailEncoder(message);
    System.out.println(encoder.encode());
  }

  /*
   * test completely empty message
   * doesn't make much sense but should not give a NPE of course
   */
  @Test
  public void testEmptyMsg() {
    MailMessage message = new MailMessage();
    String mime = new MailEncoder(message).encode();
//    log.info("\""+mime+"\"");
    assertThat(mime, containsString("Message-ID:"));
  }

  @Test
  public void testSubject() {
    MailMessage message = new MailMessage();
    message.setSubject("this is the subject");
    String mime = new MailEncoder(message).encode();
    log.info("\""+mime+"\"");
    assertThat(mime, containsString("Subject: this is the subject\n"));
  }

  @Test
  public void testFrom() {
    MailMessage message = new MailMessage();
    message.setFrom("user@example.com (Username)");
    String mime = new MailEncoder(message).encode();
    log.info("\""+mime+"\"");
    assertThat(mime, containsString("From: user@example.com (Username)\n"));
  }

  @Test
  public void testTo() {
    MailMessage message = new MailMessage();
    message.setTo("user@example.com (Username)");
    String mime = new MailEncoder(message).encode();
    log.info("\""+mime+"\"");
    assertThat(mime, containsString("To: user@example.com (Username)\n"));
  }

  @Test
  public void testTo1() {
    MailMessage message = new MailMessage();
    message.setTo(Arrays.asList("user@example.com (Username)"));
    String mime = new MailEncoder(message).encode();
    log.info("\""+mime+"\"");
    assertThat(mime, containsString("To: user@example.com (Username)\n"));
  }

  @Test
  public void testTo2() {
    MailMessage message = new MailMessage();
    message.setTo(Arrays.asList("user@example.com (Username)","user2@example.com"));
    String mime = new MailEncoder(message).encode();
    log.info("\""+mime+"\"");
    assertThat(mime, containsString("To: user@example.com (Username),user2@example.com\n"));
  }

  @Test
  public void testToMany() {
    MailMessage message = new MailMessage();
    List<String> to=new ArrayList<String>();
    for(int i=0;i<50;i++) {
      to.add("user"+i+"@example.com");
    }
    message.setTo(to);
    String mime = new MailEncoder(message).encode();
    log.info("\""+mime+"\"");
    assertThat(mime, containsString("To: user0@example.com,user1@example.com,user2@example.com,user3@example.com,user4@example.com,user5@example.com,user6@example.com,user7@example.com,user8@example.com,user9@example.com,user10@example.com,user11@example.com,user12@example.com,user13@example.com,user14@example.com,user15@example.com,user16@example.com,user17@example.com,user18@example.com,user19@example.com,user20@example.com,user21@example.com,user22@example.com,user23@example.com,user24@example.com,user25@example.com,user26@example.com,user27@example.com,user28@example.com,user29@example.com,user30@example.com,user31@example.com,user32@example.com,user33@example.com,user34@example.com,user35@example.com,user36@example.com,user37@example.com,user38@example.com,user39@example.com,user40@example.com,user41@example.com,user42@example.com,user43@example.com,user44@example.com,user45@example.com,user46@example.com,user47@example.com,user48@example.com,user49@example.com\n"));
  }

  @Test
  public void testToLong() {
    MailMessage message = new MailMessage();
    message.setTo("user@example.com (this user has an insanely long username just to check that the text is correctly wrapped into multiple lines)");
    String mime = new MailEncoder(message).encode();
    log.info("\""+mime+"\"");
    assertThat(mime, containsString("To: user@example.com (this user has an insanely long username just to check that the text is correctly wrapped into multiple lines)\n"));
  }

  @Test
  public void testToLongEncoded() {
    MailMessage message = new MailMessage();
    message.setTo("user@example.com (ä this user has an insanely long username just to check that the text is correctly wrapped into multiple lines)");
    String mime = new MailEncoder(message).encode();
    log.info("\""+mime+"\"");
    assertThat(mime, containsString("To: user@example.com (=?UTF-8?Q?=C3=A4_this_user_has_an_insanely_long_username_just_to_check_that_the_text_is_correctly_wrapped_into_multiple_lines?=)\n"));
  }

  @Test
  public void testTextPlain() {
    MailMessage message = new MailMessage();
    final String text = "the quick brown fox jumps over the lazy dog";
    message.setText(text);
    String mime = new MailEncoder(message).encode();
    log.info("\""+mime+"\"");
    assertThat(mime, containsString("Content-Type: text/plain"));
    assertThat(mime, containsString(text));
  }

  @Test
  public void testTextHtml() {
    MailMessage message = new MailMessage();
    final String text = "the <b>quick brown fox</b> jumps over the lazy dog";
    message.setHtml(text);
    String mime = new MailEncoder(message).encode();
    log.info("\""+mime+"\"");
    assertThat(mime, containsString("Content-Type: text/html"));
    assertThat(mime, containsString(text));
  }

  // TODO would be better to check the decoded text?
  @Test
  public void testTextPlainEncoded() {
    MailMessage message = new MailMessage();
    final String text = "Zwölf Boxkämpfer jagen Viktor quer über den großen Sylter Deich";
    final String encodedtext = "Zw=C3=B6lf Boxk=C3=A4mpfer jagen Viktor quer =C3=BCber den gro=C3=9Fen Sylt=\n" + 
        "er Deich";
    message.setHtml(text);
    String mime = new MailEncoder(message).encode();
    log.info("\""+mime+"\"");
    assertThat(mime, containsString("Content-Type: text/html"));
    assertThat(mime, containsString(encodedtext));
  }

  @Test
  public void testTextHtmlEncoded() {
    MailMessage message = new MailMessage();
    final String text = "<a href=\"http://vertx.io/\">go\u00a0to\u00a0vertx.io</a>";
    final String encodedtext = "<a href=3D\"http://vertx.io/\">go=C2=A0to=C2=A0vertx.io</a>";
    message.setHtml(text);
    String mime = new MailEncoder(message).encode();
    log.info("\""+mime+"\"");
    assertThat(mime, containsString("Content-Type: text/html"));
    assertThat(mime, containsString(encodedtext));
  }

  @Test
  public void testSubjectEncoded() {
    MailMessage message = new MailMessage();
    final String subject = "subject with äöü_=??=";
    final String encodedSubject= "=?UTF-8?Q?subject_with_=C3=A4=C3=B6=C3=BC=5F=3D=3F=3F=3D?=";
    message.setSubject(subject);
    String mime = new MailEncoder(message).encode();
    log.info("\""+mime+"\"");
    assertThat(mime, containsString(encodedSubject));
  }

  @Test
  public void testSubjectEncodedLong() {
    MailMessage message = new MailMessage();
    final String subject = "ä=======================================================================================";
    final String encodedSubject= "Subject: =?UTF-8?Q?=C3=A4=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D=3D?=";
    message.setSubject(subject);
    String mime = new MailEncoder(message).encode();
    log.info("\""+mime+"\"");
    assertThat(mime, containsString(encodedSubject));
  }

  @Test
  public void testSubjectEncodedNul() {
    MailMessage message = new MailMessage();
    final String subject = "\0";
    final String encodedSubject= "=00";
    message.setSubject(subject);
    String mime = new MailEncoder(message).encode();
    log.info("\""+mime+"\"");
    assertThat(mime, containsString(encodedSubject));
  }

  @Test
  public void testTextPlain76Chars() {
    MailMessage message = new MailMessage();
    final String text = "ä**********************************************************************";
    final String encodedSubject= "=C3=A4**********************************************************************";
    message.setText(text);
    String mime = new MailEncoder(message).encode();
    log.info("\""+mime+"\"");
    assertThat(mime, containsString(encodedSubject));
  }

  @Test
  public void testTextPlainEOLSpace() {
    MailMessage message = new MailMessage();
    final String text = "ä ";
    final String encodedSubject= "=C3=A4=20";
    message.setText(text);
    String mime = new MailEncoder(message).encode();
    log.info("\""+mime+"\"");
    assertThat(mime, containsString(encodedSubject));
  }

}

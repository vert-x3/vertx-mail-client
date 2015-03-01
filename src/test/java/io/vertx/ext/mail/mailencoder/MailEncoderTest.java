package io.vertx.ext.mail.mailencoder;

import io.vertx.ext.mail.MailAttachment;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.mailencoder.MailEncoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class MailEncoderTest {

  @Test
  public final void test() {
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

    attachments.add(new MailAttachment()
    .setData("\u0417\u043d\u0430\u043c\u0435\u043d\u0438\u0442\u043e\u0441\u0442\u0438"));

    attachments.add(new MailAttachment()
    .setData("\u00D0\u0097\u00D0\u00BD\u00D0\u00B0\u00D0\u00BC\u00D0\u00B5\u00D0\u00BD\u00D0\u00B8\u00D1\u0082\u00D0\u00BE\u00D1\u0081\u00D1\u0082\u00D0\u00B8"));

    message.setAttachment(attachments);

    MailEncoder encoder=new MailEncoder(message);
    System.out.println(encoder.encode());
  }

}

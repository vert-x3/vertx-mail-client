package io.vertx.ext.mail.mailencoder;

import io.vertx.ext.mail.MailAttachment;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.mailencoder.MailEncoder;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class MailEncoderTest {

  @Test
  public final void test() {
    MailMessage message = new MailMessage();
    message.setSubject("this is the subject äöü");
    message.setTo("user@example.com");
    message.setFrom("from@example.com");
    message.setSubject("this is the subject äöü");
    message.setText("asdf=\n\näöüÄÖÜ\u00ff\n\t=======================================================================================\n");
    message.setHtml("<a href=\"http://vertx.io\">vertx.io</a>\n");

    List<MailAttachment> attachments = new ArrayList<MailAttachment>();

    attachments.add(new MailAttachment()
      .setData("****************************************************************************************"));

    attachments.add(new MailAttachment()
      .setData("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"));

    attachments.add(new MailAttachment()
    .setData("\u0417\u043d\u0430\u043c\u0435\u043d\u0438\u0442\u043e\u0441\u0442\u0438"));

    attachments.add(new MailAttachment()
    .setData("\u00d0\u2014\u00d0\u00bd\u00d0\u00b0\u00d0\u00bc\u00d0\u00b5\u00d0\u00bd\u00d0\u00b8\u00d1\u201a\u00d0\u00be\u00d1\u0081\u00d1\u201a\u00d0\u00b8"));

    message.setAttachment(attachments);

    MailEncoder encoder=new MailEncoder(message);
    System.out.println(encoder.encode());
  }

}

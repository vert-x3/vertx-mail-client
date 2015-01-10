package io.vertx.ext.mail;

import io.vertx.core.json.JsonObject;

import org.junit.Test;

public class MailAttachmentTest {

  @Test
  public void testToJson() {
    MailAttachment attachment=new MailAttachment();
    
    attachment.setData("hello\"\0\u0001\t\r\n\u00ffx\u00a0\u00a1<>");
    JsonObject json=attachment.toJson();
    System.out.println(json.encodePrettily());
  }

}

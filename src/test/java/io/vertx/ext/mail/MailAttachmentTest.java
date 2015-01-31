package io.vertx.ext.mail;

import static org.junit.Assert.assertEquals;
import io.vertx.core.json.JsonObject;

import org.junit.Test;

public class MailAttachmentTest {

  @Test
  public void testConstructor() {
    new MailAttachment();
  }

  @Test
  public void testToJson() {
    assertEquals("{}", new MailAttachment().toJson().encode());
    assertEquals("{\"data\":\"data\",\"content-type\":\"text/plain\",\"disposition\":\"inline\",\"description\":\"description\"}",
        new MailAttachment()
        .setData("data")
        .setContentType("text/plain")
        .setDescription("description")
        .setDisposition("inline")
        .toJson().encode());

    JsonObject json=new MailAttachment()
      .setData("hello\"\0\u0001\t\r\n\u00ffx\u00a0\u00a1<>").toJson();
    assertEquals("{\"data\":\"hello\\\"\\u0000\\u0001\\t\\r\\n\u00ffx\u00a0\u00a1<>\"}", json.encode());
  }

  @Test
  public void testConstructorFromClass() {
    MailAttachment message=new MailAttachment();

    assertEquals(message, new MailAttachment());
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorFromJsonNull() {
    new MailAttachment((JsonObject) null);
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorFromMailAttachmentNull() {
    new MailAttachment((MailAttachment) null);
  }

  @Test
  public void testConstructorFromJsonEmpty() {
    assertEquals(new MailAttachment(), new MailAttachment(new JsonObject()));
  }

  @Test
  public void testConstructorFromJson() {
    final String jsonString = "{\"data\":\"asdfg\",\"name\":\"filename.jpg\"}";
    JsonObject json=new JsonObject(jsonString);

    MailAttachment message = new MailAttachment(json);

    assertEquals(jsonString, message.toJson().encode());
  }

}

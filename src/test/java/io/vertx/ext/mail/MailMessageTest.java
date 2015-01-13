package io.vertx.ext.mail;

import static org.junit.Assert.assertEquals;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class MailMessageTest {

  @Test
  public void testConstructor() {
    new MailMessage();
    new MailMessage("user@example.com", "user@example.net", "hello", "message");
  }

  @Test
  public void testToJson() {
    assertEquals("{}", new MailMessage().toJson().encode());
    assertEquals("{\"from\":\"a\",\"tos\":[\"b\"],\"subject\":\"c\",\"text\":\"d\"}",
        new MailMessage("a","b","c","d").toJson().encode());
  }

  @Test
  public void testAttachment() {
    MailAttachment attachment = new MailAttachment();
    attachment.setData("asdfasdf");
    attachment.setName("file.txt");
    MailMessage message = new MailMessage("a","b","c","d");
    message.setAttachment(attachment);
    assertEquals("{\"from\":\"a\",\"tos\":[\"b\"],\"subject\":\"c\",\"text\":\"d\",\"attachment\":[{\"data\":\"asdfasdf\",\"name\":\"file.txt\"}]}", message.toJson().encode());
  }

  @Test
  public void testAttachment2() {
    List<MailAttachment> list = new ArrayList<MailAttachment>();
    list.add(new MailAttachment()
      .setData("asdfasdf")
      .setName("file.txt"));
    list.add(new MailAttachment()
      .setData("xxxxx")
      .setName("file2.txt"));
    MailMessage message = new MailMessage();
    message.setAttachment(list);
    assertEquals("{\"attachment\":[{\"data\":\"asdfasdf\",\"name\":\"file.txt\"},{\"data\":\"xxxxx\",\"name\":\"file2.txt\"}]}", message.toJson().encode());
  }

  @Test
  public void testConstructorFromClass() {
    MailMessage message=new MailMessage();

    assertEquals(message, new MailMessage(message));
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorFromJsonNull() {
    new MailMessage((JsonObject)null);
  }

  @Test
  public void testConstructorFromJsonEmpty() {
    assertEquals(new MailMessage(), new MailMessage(new JsonObject()));
  }

  @Test
  public void testConstructorFromJson() {
    String jsonString = "{\"from\":\"a\",\"tos\":[\"b\"],\"subject\":\"c\",\"text\":\"d\"}";
    assertEquals(jsonString, new MailMessage(new JsonObject(jsonString)).toJson().encode());
    assertEquals("{\"from\":\"a\",\"tos\":[\"b\"],\"subject\":\"c\",\"text\":\"d\"}",
        new MailMessage(new JsonObject("{\"from\":\"a\",\"to\":\"b\",\"subject\":\"c\",\"text\":\"d\"}")).toJson().encode());
  }

}

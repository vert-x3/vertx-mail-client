package io.vertx.ext.mail;

import static org.junit.Assert.assertEquals;
import io.vertx.core.json.JsonObject;

import org.junit.Ignore;
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
    assertEquals("{\"from\":\"a\",\"recipients\":[\"b\"],\"subject\":\"c\",\"text\":\"d\"}", new MailMessage("a","b","c","d").toJson().encode());
  }

  @Ignore
  @Test
  public void testConstructorFromClass() {
    MailMessage message=new MailMessage();

    assertEquals(message, new MailMessage());
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
    final String jsonString = "{\"from\":\"a\",\"recipients\":[\"b\"],\"subject\":\"c\",\"text\":\"d\"}";
    JsonObject json=new JsonObject(jsonString);

    MailMessage message = new MailMessage(json);

    assertEquals(jsonString, message.toJson().encode());
  }

}

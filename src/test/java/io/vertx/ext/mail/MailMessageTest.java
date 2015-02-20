package io.vertx.ext.mail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
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
    assertEquals("{\"from\":\"a\",\"to\":[\"b\"],\"subject\":\"c\",\"text\":\"d\"}",
        new MailMessage("a", "b", "c", "d").toJson().encode());
  }

  @Test
  public void testAttachment() {
    MailAttachment attachment = new MailAttachment();
    attachment.setData("asdfasdf");
    attachment.setName("file.txt");
    MailMessage message = new MailMessage("a", "b", "c", "d");
    message.setAttachment(attachment);
    assertEquals("{\"from\":\"a\",\"to\":[\"b\"],\"subject\":\"c\",\"text\":\"d\",\"attachment\":[{\"data\":\"asdfasdf\",\"name\":\"file.txt\"}]}",
        message.toJson().encode());
  }

  @Test
  public void testAttachment2() {
    List<MailAttachment> list = new ArrayList<MailAttachment>();
    list.add(new MailAttachment().setData("asdfasdf").setName("file.txt"));
    list.add(new MailAttachment().setData("xxxxx").setName("file2.txt"));
    MailMessage message = new MailMessage();
    message.setAttachment(list);
    assertEquals("{\"attachment\":[{\"data\":\"asdfasdf\",\"name\":\"file.txt\"},{\"data\":\"xxxxx\",\"name\":\"file2.txt\"}]}",
        message.toJson().encode());
  }

  @Test
  public void testConstructorFromClass() {
    MailMessage message = new MailMessage();

    assertEquals(message, new MailMessage(message));
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorFromClassNull() {
    new MailMessage((MailMessage) null);
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorFromJsonNull() {
    new MailMessage((JsonObject) null);
  }

  @Test
  public void testConstructorFromJsonEmpty() {
    assertEquals(new MailMessage(), new MailMessage(new JsonObject()));
  }

  @Test
  public void testConstructorFromJson() {
    String jsonString = "{\"from\":\"a\",\"to\":[\"b\"],\"subject\":\"c\",\"text\":\"d\"}";
    assertEquals(jsonString, new MailMessage(new JsonObject(jsonString)).toJson().encode());
    assertEquals("{\"from\":\"a\",\"to\":[\"b\"],\"subject\":\"c\",\"text\":\"d\"}", new MailMessage(new JsonObject(
        "{\"from\":\"a\",\"to\":\"b\",\"subject\":\"c\",\"text\":\"d\"}")).toJson().encode());
  }

  @Test
  public void testConstructorFromJsonAttachment() {
    final String jsonString = "{\"attachment\":[{\"data\":\"asdfasdf\",\"name\":\"file.txt\"},{\"data\":\"xxxxx\",\"name\":\"file2.txt\"}]}";
    assertEquals(jsonString, new MailMessage(new JsonObject(jsonString)).toJson().encode());
    final String jsonString2 = "{\"attachment\":{\"data\":\"asdfasdf\",\"name\":\"file.txt\"}}";
    final String jsonString3 = "{\"attachment\":[{\"data\":\"asdfasdf\",\"name\":\"file.txt\"}]}";
    assertEquals(jsonString3, new MailMessage(new JsonObject(jsonString2)).toJson().encode());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorFromJsonAttachmentError() {
    final String jsonString = "{\"attachment\":true}";
    new MailMessage(new JsonObject(jsonString));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorFromJsonToError() {
    final String jsonString = "{\"to\":true}";
    new MailMessage(new JsonObject(jsonString));
  }

  @Test
  public void testConstructorFromMailConfigCopy() {
    MailMessage message = new MailMessage();

    MailAttachment attachment = new MailAttachment();
    attachment.setData("message");
    message.setAttachment(attachment);
    MailMessage message2 = new MailMessage(message);

    // change message to make sure it is really copied

    message2.getAttachment().get(0).setData("message2");

    assertEquals("{\"attachment\":[{\"data\":\"message\"}]}", message.toJson().encode());
    assertEquals("{\"attachment\":[{\"data\":\"message2\"}]}", message2.toJson().encode());
  }

  @Test
  public void testHash() {
    assertEquals(new MailMessage().hashCode(), new MailMessage().hashCode());
    assertEquals(593098263, new MailMessage().setFrom("user@example.com").hashCode());
  }

  @Test
  public void testEquals() {
    MailMessage mailMessage = new MailMessage();
    assertEquals(mailMessage, mailMessage);
    assertFalse(new MailMessage().setFrom("user@example.com").equals(new MailMessage().setFrom("user2@example.com")));
    assertFalse(mailMessage.equals(null));
    assertFalse(mailMessage.equals(""));
  }

  @Test
  public void testBounceAddress() {
    MailMessage mailMessage = new MailMessage();
    mailMessage.setBounceAddress("user@example.com");
    assertEquals("user@example.com", mailMessage.getBounceAddress());
  }

  @Test
  public void testFrom() {
    MailMessage mailMessage = new MailMessage();
    mailMessage.setFrom("user@example.com");
    assertEquals("user@example.com", mailMessage.getFrom());
  }

  @Test
  public void testTo() {
    MailMessage mailMessage = new MailMessage();
    mailMessage.setTo("user@example.com");
    assertEquals(Arrays.asList("user@example.com"), mailMessage.getTo());
    mailMessage.setTo(Arrays.asList("user@example.com", "user@example.org"));
    assertEquals(Arrays.asList("user@example.com", "user@example.org"), mailMessage.getTo());
  }

  @Test
  public void testCc() {
    MailMessage mailMessage = new MailMessage();
    mailMessage.setCc("user@example.com");
    assertEquals(Arrays.asList("user@example.com"), mailMessage.getCc());
    mailMessage.setCc(Arrays.asList("user@example.com", "user@example.org"));
    assertEquals(Arrays.asList("user@example.com", "user@example.org"), mailMessage.getCc());
  }

  @Test
  public void testBcc() {
    MailMessage mailMessage = new MailMessage();
    mailMessage.setBcc("user@example.com");
    assertEquals(Arrays.asList("user@example.com"), mailMessage.getBcc());
    mailMessage.setBcc(Arrays.asList("user@example.com", "user@example.org"));
    assertEquals(Arrays.asList("user@example.com", "user@example.org"), mailMessage.getBcc());
  }

  @Test
  public void testSubject() {
    MailMessage mailMessage = new MailMessage();
    mailMessage.setSubject("this is a subject");
    assertEquals("this is a subject", mailMessage.getSubject());
  }

  @Test
  public void testText() {
    MailMessage mailMessage = new MailMessage();
    mailMessage.setText("mail text");
    assertEquals("mail text", mailMessage.getText());
  }

  @Test
  public void testHtml() {
    MailMessage mailMessage = new MailMessage();
    mailMessage.setHtml("<a href=\"http://vertx.io/\">link</a>");
    assertEquals("<a href=\"http://vertx.io/\">link</a>", mailMessage.getHtml());
  }

}

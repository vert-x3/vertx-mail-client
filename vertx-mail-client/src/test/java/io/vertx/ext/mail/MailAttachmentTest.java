package io.vertx.ext.mail;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class MailAttachmentTest {

  @Test
  public void testConstructor() {
    final MailAttachment attachment = new MailAttachment();
    assertNotNull(attachment);
  }

  @Test
  public void testToJson() {
    assertEquals("{}", new MailAttachment().toJson().encode());
    assertEquals("{\"data\":\"ZGF0YQ==\",\"content-type\":\"text/plain\",\"disposition\":\"inline\",\"description\":\"description\"}",
      new MailAttachment()
        .setData(Buffer.buffer("data"))
        .setContentType("text/plain")
        .setDescription("description")
        .setDisposition("inline")
        .toJson().encode());

    JsonObject json = new MailAttachment()
      .setData(Buffer.buffer("hello\"\0\u0001\t\r\n\u00ffx\u00a0\u00a1<>")).toJson();
    assertEquals("{\"data\":\"aGVsbG8iAAEJDQrDv3jCoMKhPD4=\"}", json.encode());
  }

  @Test
  public void testConstructorFromClass() {
    MailAttachment message = new MailAttachment();
    assertEquals(message, new MailAttachment(message));
  }

  @Test
  public void testConstructorFromClassDoesCopy() {
    MailAttachment message = new MailAttachment();

    Buffer data = Buffer.buffer("asdf");
    message.setData(data);

    final MailAttachment copy = new MailAttachment(message);
    assertEquals(message.getData(), copy.getData());
    assertFalse("Buffer not copied", message.getData()==copy.getData());
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorFromJsonNull() {
    final MailAttachment attachment = new MailAttachment((JsonObject) null);
    assertNotNull(attachment);
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorFromMailAttachmentNull() {
    final MailAttachment attachment = new MailAttachment((MailAttachment) null);
    assertNotNull(attachment);
  }

  @Test
  public void testConstructorFromJsonEmpty() {
    assertEquals(new MailAttachment(), new MailAttachment(new JsonObject()));
  }

  @Test
  public void testConstructorFromJson() {
    final String jsonString = "{\"data\":\"YXNkZmc=\",\"name\":\"filename.jpg\"}";
    JsonObject json = new JsonObject(jsonString);

    MailAttachment message = new MailAttachment(json);

    assertEquals(jsonString, message.toJson().encode());
  }

  @Test
  public void testEquals() {
    MailAttachment mailAttachment = new MailAttachment();
    assertEquals(mailAttachment, mailAttachment);
    assertEquals(mailAttachment, new MailAttachment());
  }

  @Test
  public void testHashcode() {
    MailAttachment mailAttachment = new MailAttachment();
    assertEquals(mailAttachment.hashCode(), new MailAttachment().hashCode());
  }

  @Test
  public void testName() {
    MailAttachment mailMessage = new MailAttachment();
    mailMessage.setName("file.jpg");
    assertEquals("file.jpg", mailMessage.getName());
  }

  @Test
  public void testData() {
    MailAttachment mailMessage = new MailAttachment();
    mailMessage.setData(Buffer.buffer("xxxx"));
    assertEquals("xxxx", mailMessage.getData().toString());
  }

  @Test
  public void testContentType() {
    MailAttachment mailMessage = new MailAttachment();
    mailMessage.setContentType("text/plain");
    assertEquals("text/plain", mailMessage.getContentType());
  }

  @Test
  public void testDescription() {
    MailAttachment mailMessage = new MailAttachment();
    mailMessage.setDescription("attachment");
    assertEquals("attachment", mailMessage.getDescription());
  }

  @Test
  public void testDispostion() {
    MailAttachment mailMessage = new MailAttachment();
    mailMessage.setDisposition("inline");
    assertEquals("inline", mailMessage.getDisposition());
  }

}

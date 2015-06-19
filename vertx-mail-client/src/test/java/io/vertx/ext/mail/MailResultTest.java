/**
 * 
 */
package io.vertx.ext.mail;

import io.vertx.core.json.JsonObject;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
public class MailResultTest {

  /**
   * Test method for {@link io.vertx.ext.mail.MailResult#MailResult()}.
   */
  @Test
  public final void testMailResult() {
    MailResult result = new MailResult();
    assertNotNull(result);
  }

  /**
   * Test method for {@link io.vertx.ext.mail.MailResult#MailResult(io.vertx.ext.mail.MailResult)}.
   */
  @Test
  public final void testMailResultMailResult() {
    MailResult result = new MailResult();
    MailResult result2 = new MailResult(result);
    assertNotNull(result2);
  }

  /**
   * Test method for {@link io.vertx.ext.mail.MailResult#MailResult(io.vertx.core.json.JsonObject)}.
   */
  @Test
  public final void testMailResultJsonObject() {
    MailResult result = new MailResult(new JsonObject());
    assertNotNull(result);
  }

  /**
   * Test method for {@link io.vertx.ext.mail.MailResult#toJson()}.
   */
  @Test
  public final void testToJson() {
    MailResult result = new MailResult();
    assertEquals("{\"recipients\":[]}", result.toJson().encode());
  }

  @Test
  public final void testToJson2() {
    MailResult result = new MailResult();
    result.setMessageID("12345");
    result.setRecipients(Arrays.asList("user1","user2"));
    assertEquals("{\"messageId\":\"12345\",\"recipients\":[\"user1\",\"user2\"]}", result.toJson().encode());
  }

  /**
   * Test method for {@link io.vertx.ext.mail.MailResult#getMessageID()}.
   */
  @Test
  public final void testGetMessageID() {
    MailResult result = new MailResult();
    assertEquals(null, result.getMessageID());
  }

  /**
   * Test method for {@link io.vertx.ext.mail.MailResult#setMessageID(java.lang.String)}.
   */
  @Test
  public final void testSetMessageID() {
    MailResult result = new MailResult();
    assertEquals("asdf", result.setMessageID("asdf").getMessageID());
  }

  /**
   * Test method for {@link io.vertx.ext.mail.MailResult#getRecipients()}.
   */
  @Test
  public final void testGetRecipients() {
    MailResult result = new MailResult();
    assertEquals("[]", result.getRecipients().toString());
  }

  @Test
  public final void testGetRecipients2() {
    MailResult result = new MailResult();
    result.getRecipients().add("user");
    result.getRecipients().add("user2");
    assertEquals("[user, user2]", result.getRecipients().toString());
  }

  /**
   * Test method for {@link io.vertx.ext.mail.MailResult#setRecipients(java.util.List)}.
   */
  @Test
  public final void testSetRecipients() {
    MailResult result = new MailResult();
    List<String> recipients = Arrays.asList("user1", "user2");
    assertEquals(recipients, result.setRecipients(recipients).getRecipients());
  }

  /**
   * Test method for {@link io.vertx.ext.mail.MailResult#toString()}.
   */
  @Test
  public final void testToString() {
    MailResult result = new MailResult();
    assertEquals("{\"recipients\":[]}", result.toString());
  }

  @Test
  public final void testToString2() {
    MailResult result = new MailResult();
    result.setMessageID("12345");
    result.setRecipients(Arrays.asList("user1","user2"));
    assertEquals("{\"messageId\":\"12345\",\"recipients\":[\"user1\",\"user2\"]}", result.toString());
  }

}

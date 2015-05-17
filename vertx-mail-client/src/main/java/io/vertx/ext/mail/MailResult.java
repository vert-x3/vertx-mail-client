/**
 * 
 */
package io.vertx.ext.mail;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Represent the result of the sendMail operation
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
@DataObject
public class MailResult {

  private String messageID;
  private List<String> recipients;

  public MailResult() {
    messageID = null;
    recipients = new ArrayList<String>();
  }

  public MailResult(MailResult other) {
    messageID = other.messageID;
    recipients = new ArrayList<String>(other.recipients);
  }

  @SuppressWarnings("unchecked")
  public MailResult(JsonObject json) {
    messageID = json.getString("message-id");
    JsonArray jsonArray = json.getJsonArray("recipients");
    recipients = jsonArray == null ? new ArrayList<String>() : (List<String>) jsonArray.getList();
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    if(messageID != null) {
      json.put("message-id", messageID);
    }
    if(recipients != null) {
      json.put("recipients", recipients);
    }
    return json;
  }

  /**
   * @return the messageId
   */
  public String getMessageID() {
    return messageID;
  }

  /**
   * @param messageId the messageId to set
   */
  public MailResult setMessageID(String messageId) {
    this.messageID = messageId;
    return this;
  }

  /**
   * @return the recipients
   */
  public List<String> getRecipients() {
    return recipients;
  }

  /**
   * @param recipients the recipients to set
   */
  public MailResult setRecipients(List<String> recipients) {
    this.recipients = recipients;
    return this;
  }

  public String toString() {
    return toJson().encode();
  }

}

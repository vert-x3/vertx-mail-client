/**
 * 
 */
package io.vertx.ext.mail;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

/**
 * Represent the result of the sendMail operation This class is currently a placeholder, it will get a field message-id
 * and recipient list (for the list of addresses that the mail was actually sent to)
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
@DataObject
public class MailResult {

  public MailResult() {
    
  }

  public MailResult(MailResult other) {

  }

  public MailResult(JsonObject json) {

  }

  public JsonObject toJson() {
    return new JsonObject();
  }

  public String toString() {
    return toJson().encode();
  }

}

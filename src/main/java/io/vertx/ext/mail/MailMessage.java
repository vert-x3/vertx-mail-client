package io.vertx.ext.mail;

import io.vertx.codegen.annotations.Options;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Options
public class MailMessage {

  private String bounceAddress;
  private String from;
  private List<String> recipients;
  private String subject;
  private String text;
  private String html;
  private List<MailAttachment> attachment;

  public MailMessage() {
  }

  public MailMessage(MailMessage other) {
    this.bounceAddress=other.bounceAddress;
    this.from=other.from;
    this.recipients=other.recipients;
    this.subject=other.subject;
    this.text=other.text;
    this.html=other.html;
    // TODO: create new list with new instances?
    this.attachment=other.attachment;
  }

  @SuppressWarnings("unchecked")
  public MailMessage(JsonObject json) {
    Objects.requireNonNull(json);
    this.bounceAddress=json.getString("bounceAddress");
    this.from=json.getString("from");
    // TODO: handle single recipient without array
    if(json.containsKey("recipients")) {
      final List<String> recipients = (List<String>) json.getJsonArray("recipients").getList();
      this.recipients=recipients;
    }
    this.subject=json.getString("subject");
    this.text=json.getString("text");
    this.html=json.getString("html");
    if(json.containsKey("attachment")) {
      List<MailAttachment> list;
      Object object=json.getValue("attachment");
      if(object instanceof JsonObject) {
        list=Arrays.asList(new MailAttachment((JsonObject) object));
      }
      else if(object instanceof JsonArray) {
        list=(List<MailAttachment>) ((JsonArray)object).getList();
      }
      else {
        throw new IllegalArgumentException("invalid attachment type");
      }
      this.attachment=list;
    }
  }

  // construct a simple message with text/plain
  public MailMessage(String from, String to, String subject, String text) {
    this.from=from;
    this.recipients=Arrays.asList(to);
    this.subject=subject;
    this.text=text;
  }

  public String getBounceAddress() {
    return bounceAddress;
  }

  public MailMessage setBounceAddress(String bounceAddress) {
    this.bounceAddress = bounceAddress;
    return this;
  }

  public String getFrom() {
    return from;
  }

  public MailMessage setFrom(String from) {
    this.from = from;
    return this;
  }

  public List<String> getRecipients() {
    return recipients;
  }

  public MailMessage setRecipients(List<String> recipients) {
    this.recipients = recipients;
    return this;
  }

  // helper method for single recipient
  public MailMessage setRecipient(String recipient) {
    this.recipients = Arrays.asList(recipient);
    return this;
  }

  public String getSubject() {
    return subject;
  }

  public MailMessage setSubject(String subject) {
    this.subject = subject;
    return this;
  }

  public String getText() {
    return text;
  }

  public MailMessage setText(String text) {
    this.text = text;
    return this;
  }

  public String getHtml() {
    return html;
  }

  public MailMessage setHtml(String html) {
    this.html = html;
    return this;
  }

  public List<MailAttachment> getAttachment() {
    return attachment;
  }

  public MailMessage setAttachment(List<MailAttachment> attachment) {
    this.attachment = attachment;
    return this;
  }

  public MailMessage setAttachment(MailAttachment attachment) {
    this.attachment = Arrays.asList(attachment);
    return this;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    if(bounceAddress!=null) {
      json.put("bounceAddress", bounceAddress);
    }
    if(from!=null) {
      json.put("from", from);
    }
    if(recipients!=null) {
      json.put("recipients", recipients);
    }
    if(subject!=null) {
      json.put("subject", subject);
    }
    if(text!=null) {
      json.put("text", text);
    }
    if(html!=null) {
      json.put("html", html);
    }
    if(attachment!=null) {
      JsonArray array=new JsonArray();
      for(MailAttachment a:attachment) {
        array.add(a.toJson());
      }
      json.put("attachment", array);
    }
    return json;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    MailMessage other = (MailMessage) o;

    if (!equalsNull(bounceAddress, other.bounceAddress)) {
      return false;
    }
    if (!equalsNull(from, other.from)) {
      return false;
    }
    if (!equalsNull(recipients, other.recipients)) {
      return false;
    }
    if (!equalsNull(subject, other.subject)) {
      return false;
    }
    if (!equalsNull(text, other.text)) {
      return false;
    }
    if (!equalsNull(html, other.html)) {
      return false;
    }
    return equalsNull(attachment, other.attachment);
  }

  @Override
  public int hashCode() {
    int result = hashCodeNull(bounceAddress);
    result = 31 * result + hashCodeNull(from);
    result = 31 * result + hashCodeNull(recipients);
    result = 31 * result + hashCodeNull(subject);
    result = 31 * result + hashCodeNull(text);
    result = 31 * result + hashCodeNull(html);
    result = 31 * result + hashCodeNull(attachment);
    return result;
  }

  private boolean equalsNull(Object o1, Object o2) {
    if(o1 == null && o2 == null) {
      return true;
    }
    if(o1 == null || o2 == null) {
      return false;
    }
    return o1.equals(o2);
  }

  private int hashCodeNull(Object o) {
    return o == null ? 0 : o.hashCode();
  }

}

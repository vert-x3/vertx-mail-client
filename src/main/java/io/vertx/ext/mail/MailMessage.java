package io.vertx.ext.mail;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@DataObject
public class MailMessage {

  private String bounceAddress;
  private String from;
  private List<String> to;
  private List<String> cc;
  private List<String> bcc;
  private String subject;
  private String text;
  private String html;
  private List<MailAttachment> attachment;

  public MailMessage() {
  }

  public MailMessage(MailMessage other) {
    Objects.requireNonNull(other);
    this.bounceAddress = other.bounceAddress;
    this.from = other.from;
    this.to = other.to;
    this.cc = other.cc;
    this.bcc = other.bcc;
    this.subject = other.subject;
    this.text = other.text;
    this.html = other.html;
    // TODO: create new list with new instances?
    this.attachment = other.attachment;
  }

  public MailMessage(JsonObject json) {
    Objects.requireNonNull(json);
    this.bounceAddress = json.getString("bounceAddress");
    this.from = json.getString("from");
    this.to = getKeyAsStringOrList(json, "to");
    this.cc = getKeyAsStringOrList(json, "cc");
    this.bcc = getKeyAsStringOrList(json, "bcc");
    this.subject = json.getString("subject");
    this.text = json.getString("text");
    this.html = json.getString("html");
    if (json.containsKey("attachment")) {
      List<MailAttachment> list;
      Object object = json.getValue("attachment");
      if (object instanceof JsonObject) {
        list = Arrays.asList(new MailAttachment((JsonObject) object));
      } else if (object instanceof JsonArray) {
        list = new ArrayList<MailAttachment>();
        for (Object attach : (JsonArray) object) {
          list.add(new MailAttachment((JsonObject) attach));
        }
      } else {
        throw new IllegalArgumentException("invalid attachment type");
      }
      this.attachment = list;
    }
  }

  @SuppressWarnings("unchecked")
  private List<String> getKeyAsStringOrList(JsonObject json, String key) {
    Object value = json.getValue(key);
    if (value == null) {
      return null;
    } else {
      if (value instanceof String) {
        return Arrays.asList((String) value);
      } else if (value instanceof JsonArray) {
        return (List<String>) ((JsonArray) value).getList();
      } else {
        throw new IllegalArgumentException("invalid attachment type");
      }
    }
  }

  // construct a simple message with text/plain
  public MailMessage(String from, String to, String subject, String text) {
    this.from = from;
    this.to = Arrays.asList(to);
    this.subject = subject;
    this.text = text;
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

  public List<String> getTo() {
    return to;
  }

  public MailMessage setTo(List<String> to) {
    this.to = to;
    return this;
  }

  // helper method for single recipient
  public MailMessage setTo(String to) {
    this.to = Arrays.asList(to);
    return this;
  }

  public List<String> getCc() {
    return cc;
  }

  public MailMessage setCc(List<String> cc) {
    this.cc = cc;
    return this;
  }

  // helper method for single recipient
  public MailMessage setCc(String cc) {
    this.cc = Arrays.asList(cc);
    return this;
  }

  public List<String> getBcc() {
    return bcc;
  }

  public MailMessage setBcc(List<String> bcc) {
    this.bcc = bcc;
    return this;
  }

  // helper method for single recipient
  public MailMessage setBcc(String bcc) {
    this.bcc = Arrays.asList(bcc);
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
    if (bounceAddress != null) {
      json.put("bounceAddress", bounceAddress);
    }
    if (from != null) {
      json.put("from", from);
    }
    if (to != null) {
      json.put("to", to);
    }
    if (cc != null) {
      json.put("cc", cc);
    }
    if (bcc != null) {
      json.put("bcc", bcc);
    }
    if (subject != null) {
      json.put("subject", subject);
    }
    if (text != null) {
      json.put("text", text);
    }
    if (html != null) {
      json.put("html", html);
    }
    if (attachment != null) {
      JsonArray array = new JsonArray();
      for (MailAttachment a : attachment) {
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
    if (!equalsNull(to, other.to)) {
      return false;
    }
    if (!equalsNull(cc, other.cc)) {
      return false;
    }
    if (!equalsNull(bcc, other.bcc)) {
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
    result = 31 * result + hashCodeNull(to);
    result = 31 * result + hashCodeNull(cc);
    result = 31 * result + hashCodeNull(bcc);
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

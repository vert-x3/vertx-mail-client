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
  private List<String> to = null;
  private List<String> cc = null;
  private List<String> bcc = null;
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
    this.to = copyList(other.to);
    this.cc = copyList(other.cc);
    this.bcc = copyList(other.bcc);
    this.subject = other.subject;
    this.text = other.text;
    this.html = other.html;
    if (other.attachment != null) {
      List<MailAttachment> newList = new ArrayList<MailAttachment>(other.attachment.size());
      for (MailAttachment a : other.attachment) {
        newList.add(new MailAttachment(a));
      }
      this.attachment = newList;
    }
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
        return asList((String) value);
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
    this.to = asList(to);
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
    this.to = asList(to);
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
    this.cc = asList(cc);
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
    this.bcc = asList(bcc);
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
    putIfNotNull(json, "bounceAddress", bounceAddress);
    putIfNotNull(json, "from", from);
    putIfNotNull(json, "to", to);
    putIfNotNull(json, "cc", cc);
    putIfNotNull(json, "bcc", bcc);
    putIfNotNull(json, "subject", subject);
    putIfNotNull(json, "text", text);
    putIfNotNull(json, "html", html);
    if (attachment != null) {
      JsonArray array = new JsonArray();
      for (MailAttachment a : attachment) {
        array.add(a.toJson());
      }
      json.put("attachment", array);
    }
    return json;
  }

  private List<Object> getList() {
    final List<Object> objects = Arrays.asList(bounceAddress, from, to, cc, bcc, subject, text, html, attachment);
    return objects;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || !(o instanceof MailMessage)) {
      return false;
    }
    final MailMessage message = (MailMessage) o;
    return getList().equals(message.getList());
  }

  @Override
  public int hashCode() {
    return getList().hashCode();
  }

  private void putIfNotNull(JsonObject json, String key, Object value) {
    if (value != null) {
      json.put(key, value);
    }
  }

  private List<String> copyList(List<String> list) {
    if(list == null) {
      return null;
    } else {
      return new ArrayList<String>(list);
    }
  }

  private List<String> asList(String to) {
    List<String> list = new ArrayList<String>(1);
    list.add(to);
    return list;
  }
}

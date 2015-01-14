package io.vertx.ext.mail;

import io.vertx.codegen.annotations.Options;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Options
public class MailMessage {

  private String bounceAddress;
  private String from;
  private List<String> tos;
  private List<String> ccs;
  private List<String> bccs;
  private String subject;
  private String text;
  private String html;
  private List<MailAttachment> attachment;

  public MailMessage() {
  }

  public MailMessage(MailMessage other) {
    this.bounceAddress=other.bounceAddress;
    this.from=other.from;
    this.tos=other.tos;
    this.ccs=other.ccs;
    this.bccs=other.bccs;
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
    if(json.containsKey("tos")) {
      final List<String> recipients = (List<String>) json.getJsonArray("tos").getList();
      this.tos=recipients;
    }
    else if(json.containsKey("to")) {
      final List<String> recipients = Arrays.asList(json.getString("to"));
      this.tos=recipients;
    }
    if(json.containsKey("ccs")) {
      final List<String> recipients = (List<String>) json.getJsonArray("ccs").getList();
      this.ccs=recipients;
    }
    else if(json.containsKey("cc")) {
      final List<String> recipients = Arrays.asList(json.getString("cc"));
      this.ccs=recipients;
    }
    if(json.containsKey("bccs")) {
      final List<String> recipients = (List<String>) json.getJsonArray("bccs").getList();
      this.bccs=recipients;
    }
    else if(json.containsKey("bcc")) {
      final List<String> recipients = Arrays.asList(json.getString("bcc"));
      this.bccs=recipients;
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
        list=new ArrayList<MailAttachment>();
        for(Object attach:(JsonArray)object) {
          list.add(new MailAttachment((JsonObject)attach));
        }
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
    this.tos=Arrays.asList(to);
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

  public List<String> getTos() {
    return tos;
  }

  public MailMessage setTos(List<String> tos) {
    this.tos = tos;
    return this;
  }

  // helper method for single recipient
  public MailMessage setTo(String to) {
    this.tos = Arrays.asList(to);
    return this;
  }

  public List<String> getCcs() {
    return ccs;
  }

  public MailMessage setCcs(List<String> ccs) {
    this.ccs = ccs;
    return this;
  }

  // helper method for single recipient
  public MailMessage setCc(String cc) {
    this.ccs = Arrays.asList(cc);
    return this;
  }

  public List<String> getBccs() {
    return bccs;
  }

  public MailMessage setBccs(List<String> bccs) {
    this.bccs = bccs;
    return this;
  }

  // helper method for single recipient
  public MailMessage setBcc(String bcc) {
    this.bccs = Arrays.asList(bcc);
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
    if(tos!=null) {
      json.put("tos", tos);
    }
    if(ccs!=null) {
      json.put("ccs", ccs);
    }
    if(bccs!=null) {
      json.put("bccs", bccs);
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
    if (!equalsNull(tos, other.tos)) {
      return false;
    }
    if (!equalsNull(ccs, other.ccs)) {
      return false;
    }
    if (!equalsNull(bccs, other.bccs)) {
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
    result = 31 * result + hashCodeNull(tos);
    result = 31 * result + hashCodeNull(ccs);
    result = 31 * result + hashCodeNull(bccs);
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

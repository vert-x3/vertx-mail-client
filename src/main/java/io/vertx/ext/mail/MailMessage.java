package io.vertx.ext.mail;

import io.vertx.codegen.annotations.Options;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

@Options
public class MailMessage {

  private String bounceAddress;
  private String from;
  private List<String> recipients;
  private String subject;
  private String text;
  private String html;
  private MailAttachment attachment;

  public MailMessage() {
  }

  public MailMessage(MailMessage other) {
    // TODO
    }

  public MailMessage(JsonObject config) {
    // TODO
  }

  public MailMessage(String from, String to, String subject, String text) {
    this.from=from;
    this.recipients=new ArrayList<String>();
    recipients.add(to);
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
    List<String> rList=new ArrayList<String>();
    rList.add(recipient);
    this.recipients = rList;
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

  public MailAttachment getAttachment() {
    return attachment;
  }

  public MailMessage setAttachment(MailAttachment attachment) {
    this.attachment = attachment;
    return this;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    // TODO
    return json;
  }

  @Override
  public boolean equals(Object o) {
    // TODO
    return false;
  }

  @Override
  public int hashCode() {
    // TODO
    return 0;  
  }

//  private int hashCodeNull(Object o) {
//    return o == null ? 0 : o.hashCode();
//  }

}

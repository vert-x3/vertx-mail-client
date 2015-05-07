package io.vertx.ext.mail;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.MultiMap;
import io.vertx.core.http.CaseInsensitiveHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * represent a mail message that can be sent via the MailService
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
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
  private MultiMap headers = null;
  private boolean fixedHeaders = false;

  /**
   * construct empty mail message that can be used with the setters
   */
  public MailMessage() {
  }

  /**
   * copy object to another @link MailMessage object
   *
   * @param other object to copy
   */
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
    if (other.headers != null) {
      headers = new CaseInsensitiveHeaders().addAll(headers);
    }
  }

  /**
   * construct object from a JsonObject representation
   *
   * @param json json object to copy
   */
  public MailMessage(JsonObject json) {
    Objects.requireNonNull(json);
    bounceAddress = json.getString("bounceAddress");
    from = json.getString("from");
    to = getKeyAsStringOrList(json, "to");
    cc = getKeyAsStringOrList(json, "cc");
    bcc = getKeyAsStringOrList(json, "bcc");
    subject = json.getString("subject");
    text = json.getString("text");
    html = json.getString("html");
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
      attachment = list;
    }
    if (json.containsKey("headers")) {
      headers = jsonToMultiMap(json);
    }
  }

  /**
   * @param json
   * @return
   */
  private MultiMap jsonToMultiMap(JsonObject json) {
    JsonObject jsonHeaders = json.getJsonObject("headers");
    MultiMap headers = new CaseInsensitiveHeaders();
    for (String key : jsonHeaders.getMap().keySet()) {
      headers.add(key, getKeyAsStringOrList(jsonHeaders, key));
    }
    return headers;
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

  /**
   * construct a simple message with text/plain
   *
   * @param from    from email address
   * @param to      string to email address
   * @param subject subject of the mail
   * @param text    plain text of the message body
   */
  public MailMessage(String from, String to, String subject, String text) {
    this.from = from;
    this.to = asList(to);
    this.subject = subject;
    this.text = text;
  }

  /**
   * get bounce address of this mail
   *
   * @return bounce address
   */
  public String getBounceAddress() {
    return bounceAddress;
  }

  /**
   * set bounce address of this mail
   *
   * @param bounceAddress bounce address
   * @return this to be able to use it fluently
   */
  public MailMessage setBounceAddress(String bounceAddress) {
    this.bounceAddress = bounceAddress;
    return this;
  }

  /**
   * get from address of this mail
   *
   * @return from address
   */
  public String getFrom() {
    return from;
  }

  /**
   * set from address of this mail
   *
   * @param from from addrss
   * @return this to be able to use it fluently
   */
  public MailMessage setFrom(String from) {
    this.from = from;
    return this;
  }

  /**
   * get list of to addresses
   *
   * @return List of to addresses
   */
  public List<String> getTo() {
    return to;
  }

  /**
   * set list of to addresses
   *
   * @param to List of to addresses
   * @return this to be able to use it fluently
   */
  public MailMessage setTo(List<String> to) {
    this.to = to;
    return this;
  }

  /**
   * helper method for single recipient
   *
   * @param to to address
   * @return this to be able to use it fluently
   */
  public MailMessage setTo(String to) {
    this.to = asList(to);
    return this;
  }

  /**
   * get list of cc addresses
   *
   * @return List of cc addresses
   */
  public List<String> getCc() {
    return cc;
  }

  /**
   * set list of cc addresses
   *
   * @param cc List of cc addresses
   * @return this to be able to use it fluently
   */
  public MailMessage setCc(List<String> cc) {
    this.cc = cc;
    return this;
  }

  /**
   * helper method for single recipient
   *
   * @param cc cc address
   * @return this to be able to use it fluently
   */
  public MailMessage setCc(String cc) {
    this.cc = asList(cc);
    return this;
  }

  /**
   * get list of bcc addresses
   *
   * @return List of bcc addresses
   */
  public List<String> getBcc() {
    return bcc;
  }

  /**
   * set list of bcc addresses
   *
   * @param bcc List of bcc addresses
   * @return this to be able to use it fluently
   */
  public MailMessage setBcc(List<String> bcc) {
    this.bcc = bcc;
    return this;
  }

  /**
   * helper method for single recipient
   *
   * @param bcc bcc address
   * @return this to be able to use it fluently
   */
  public MailMessage setBcc(String bcc) {
    this.bcc = asList(bcc);
    return this;
  }

  /**
   * get the subject of this mail
   *
   * @return the subject
   */
  public String getSubject() {
    return subject;
  }

  /**
   * set the subject of this mail
   *
   * @param subject the subject
   * @return this to be able to use it fluently
   */
  public MailMessage setSubject(String subject) {
    this.subject = subject;
    return this;
  }

  /**
   * get the plain text of this mail
   *
   * @return the text
   */
  public String getText() {
    return text;
  }

  /**
   * set the plain text of this mail
   *
   * @param text the text
   * @return this to be able to use it fluently
   */
  public MailMessage setText(String text) {
    this.text = text;
    return this;
  }

  /**
   * get the html text of this mail
   *
   * @return the text
   */
  public String getHtml() {
    return html;
  }

  /**
   * set the html text of this mail
   *
   * @param html the text
   * @return this to be able to use it fluently
   */
  public MailMessage setHtml(String html) {
    this.html = html;
    return this;
  }

  /**
   * get the list of attachments of this mail
   *
   * @return List of attachment
   */
  public List<MailAttachment> getAttachment() {
    return attachment;
  }

  /**
   * set the list of attachments of this mail
   *
   * @param attachment List of attachment
   * @return this to be able to use it fluently
   */
  public MailMessage setAttachment(List<MailAttachment> attachment) {
    this.attachment = attachment;
    return this;
  }

  /**
   * set a single attachment of this mail the result of getAttachment when using
   * this method returns an unmodifiable list, if you want to be able to add
   * attachments later, please use
   * {@code setAttachment(new ArrayList<MailAttachment>())} instead
   *
   * @param attachment the attachment to add
   * @return this to be able to use it fluently
   */
  public MailMessage setAttachment(MailAttachment attachment) {
    this.attachment = Arrays.asList(attachment);
    return this;
  }

  /**
   * get the headers to be set before filling our headers
   *
   * @return the headers
   */
  public MultiMap getHeaders() {
    return headers;
  }

  /**
   * set the headers to be set before filling our headers
   *
   * @param headers the headers to set
   * @return this to be able to use it fluently
   */
  public MailMessage setHeaders(MultiMap headers) {
    this.headers = headers;
    return this;
  }

  /**
   * get whether our own headers should be the only headers added to the message
   *
   * @return the fixedHeaders
   */
  public boolean isFixedHeaders() {
    return fixedHeaders;
  }

  /**
   * set whether our own headers should be the only headers added to the message
   *
   * @param fixedHeaders the fixedHeaders to set
   * @return this to be able to use it fluently
   */
  public MailMessage setFixedHeaders(boolean fixedHeaders) {
    this.fixedHeaders = fixedHeaders;
    return this;
  }

  /**
   * convert the mail message to Json representation
   *
   * @return the json object
   */
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
    if (headers != null) {
      json.put("headers", multiMapJson(headers));
    }
    if (fixedHeaders) {
      json.put("fixedheaders", true);
    }
    return json;
  }

  /**
   * @param headers
   * @return
   */
  private JsonObject multiMapJson(MultiMap headers) {
    JsonObject json = new JsonObject();
    for (String key : headers.names()) {
      json.put(key, headers.getAll(key));
    }
    return json;
  }

  private List<Object> getList() {
    return Arrays.asList(bounceAddress, from, to, cc, bcc, subject, text, html, attachment, headers, fixedHeaders);
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
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

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#hashCode()
   */
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
    if (list == null) {
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

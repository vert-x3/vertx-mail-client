package io.vertx.ext.mail;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * represent a mail attachment that can be used in a MailMessage
 * 
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */

@DataObject
public class MailAttachment {

  private Buffer data;
  // name is the descriptive filename that will be put into the mail
  // i.e. usually a local filename without path
  // this can be set to "" to omit the filename attribute
  private String name;
  private String contentType;
  private String disposition;
  private String description;

  /**
   * get the name
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * set the name
   * 
   * @param name name of the attachment file
   * @return this to be able to use it fluently
   */
  public MailAttachment setName(String name) {
    this.name = name;
    return this;
  }

  /**
   * construct an empty MailAttachment object that can be filled with the setters
   */
  public MailAttachment() {
  }

  /**
   * create a copy of a MailAttachment object
   * @param other object to be copied
   */
  public MailAttachment(MailAttachment other) {
    Objects.requireNonNull(other);
    this.data = other.data;
    this.name = other.name;
    this.contentType = other.contentType;
    this.disposition = other.disposition;
    this.description = other.description;
  }

  /**
   * create a MailAttachment object from a JsonObject representation
   * @param json object to be copied
   */
  public MailAttachment(JsonObject json) {
    Objects.requireNonNull(json);
    this.data = json.getBinary("data") == null ? null : Buffer.buffer(json.getBinary("data"));
    this.name = json.getString("name");
    this.contentType = json.getString("content-type");
    this.disposition = json.getString("disposition");
    this.description = json.getString("description");
  }

  /**
   * get the data
   * @return the data
   */
  public Buffer getData() {
    return data;
  }

  /**
   * set the data
   * @param data Buffer of bytes to be used at attachmnet
   * @return this to be able to use it fluently
   */
  public MailAttachment setData(Buffer data) {
    this.data = data;
    return this;
  }

  /**
   * get the Content-Type
   * @return the contentType
   */
  public String getContentType() {
    return contentType;
  }

  /**
   * set the Content-Type
   * @param the contentType
   * @return this to be able to use it fluently
   */
  public MailAttachment setContentType(String contentType) {
    this.contentType = contentType;
    return this;
  }

  /**
   * get the disposition field
   * @return the disposition
   */
  public String getDisposition() {
    return disposition;
  }

  /**
   * set the disposition field to be used in the attachment
   * @param disposition the disposition
   * @return this to be able to use it fluently
   */
  public MailAttachment setDisposition(String disposition) {
    this.disposition = disposition;
    return this;
  }

  /**
   * get the description field
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * set the description field to be used in the attachment
   * @param description the description
   * @return this to be able to use it fluently
   */
  public MailAttachment setDescription(String description) {
    this.description = description;
    return this;
  }

  /**
   * convert this object to Json representation
   * @return the json object
   */
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    if(data != null) {
      putIfNotNull(json, "data", data.getBytes());
    }
    putIfNotNull(json, "name", name);
    putIfNotNull(json, "content-type", contentType);
    putIfNotNull(json, "disposition", disposition);
    putIfNotNull(json, "description", description);
    return json;
  }

  private List<Object> getList() {
    final List<Object> objects = Arrays.asList(data, name, disposition, description);
    return objects;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || !(o instanceof MailAttachment)) {
      return false;
    }
    final MailAttachment attachment = (MailAttachment) o;
    return getList().equals(attachment.getList());
  }

  /* (non-Javadoc)
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

}

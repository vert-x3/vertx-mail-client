package io.vertx.ext.mail;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */

@DataObject
public class MailAttachment {

  // note that this must be a String only containing chars 0-255
  // to represent the binary file data
  private String data;
  // if filename is set, data will be read from the filesystem
  // TODO: this is not yet implemented
  // private String filename;
  // name is the descriptive filename that will be put into the mail
  // i.e. usually a local filename without path
  // will be filled based on filename if not set
  // this can be set to "" to omit the filename attribute
  private String name;
  private String contentType;
  private String disposition;
  private String description;

  /**
   * @return the filename
   */
  // public String getFilename() {
  // return filename;
  // }

  /**
   * @param filename
   *          the filename to set
   */
  // public MailAttachment setFilename(String filename) {
  // this.filename = filename;
  // return this;
  // }

  public String getName() {
    return name;
  }

  public MailAttachment setName(String name) {
    this.name = name;
    return this;
  }

  public MailAttachment() {
  }

  public MailAttachment(MailAttachment other) {
    Objects.requireNonNull(other);
    this.data = other.data;
    this.name = other.name;
    this.contentType = other.contentType;
    this.disposition = other.disposition;
    this.description = other.description;
  }

  public MailAttachment(JsonObject json) {
    Objects.requireNonNull(json);
    this.data = json.getString("data");
    this.name = json.getString("name");
    this.contentType = json.getString("content-type");
    this.disposition = json.getString("disposition");
    this.description = json.getString("description");
  }

  public String getData() {
    return data;
  }

  public MailAttachment setData(String data) {
    this.data = data;
    return this;
  }

  public String getContentType() {
    return contentType;
  }

  public MailAttachment setContentType(String contentType) {
    this.contentType = contentType;
    return this;
  }

  public String getDisposition() {
    return disposition;
  }

  public MailAttachment setDisposition(String disposition) {
    this.disposition = disposition;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public MailAttachment setDescription(String description) {
    this.description = description;
    return this;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    putIfNotNull(json, "data", data);
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final MailAttachment attachment = (MailAttachment) o;
    return getList().equals(attachment.getList());
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

}

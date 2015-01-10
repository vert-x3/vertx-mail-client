package io.vertx.ext.mail;

import io.vertx.codegen.annotations.Options;
import io.vertx.core.json.JsonObject;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */

@Options
public class MailAttachment {

  // note that this must be a String only containing chars 0-255
  // to represent the binary file data
  private String data;
  // if filename is set, data will be read from the filesystem
  // TODO: this is not yet implemented
  private String filename;
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
  public String getFilename() {
    return filename;
  }

  /**
   * @param filename the filename to set
   */
  public MailAttachment setFilename(String filename) {
    this.filename = filename;
    return this;
  }

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
    this.data=other.data;
    this.name=other.name;
    this.contentType=other.contentType;
    this.disposition=other.disposition;
    this.description=other.description;
  }

  public MailAttachment(JsonObject config) {
    this.data=config.getString("data");
    this.name=config.getString("name");
    this.contentType=config.getString("content-type");
    this.disposition=config.getString("disposition");
    this.description=config.getString("description");
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
    json.put("data", data);
    json.put("name", name);
    json.put("content-type", contentType);
    json.put("disposition", disposition);
    json.put("description", description);
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

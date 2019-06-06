package io.vertx.ext.mail;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import io.vertx.core.spi.json.JsonCodec;

/**
 * Converter and Codec for {@link io.vertx.ext.mail.MailAttachment}.
 * NOTE: This class has been automatically generated from the {@link io.vertx.ext.mail.MailAttachment} original class using Vert.x codegen.
 */
public class MailAttachmentConverter implements JsonCodec<MailAttachment, JsonObject> {

  public static final MailAttachmentConverter INSTANCE = new MailAttachmentConverter();

  @Override public JsonObject encode(MailAttachment value) { return (value != null) ? value.toJson() : null; }

  @Override public MailAttachment decode(JsonObject value) { return (value != null) ? new MailAttachment(value) : null; }

  @Override public Class<MailAttachment> getTargetClass() { return MailAttachment.class; }
}

package io.vertx.ext.mail;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import io.vertx.core.spi.json.JsonCodec;

/**
 * Converter and Codec for {@link io.vertx.ext.mail.MailMessage}.
 * NOTE: This class has been automatically generated from the {@link io.vertx.ext.mail.MailMessage} original class using Vert.x codegen.
 */
public class MailMessageConverter implements JsonCodec<MailMessage, JsonObject> {

  public static final MailMessageConverter INSTANCE = new MailMessageConverter();

  @Override public JsonObject encode(MailMessage value) { return (value != null) ? value.toJson() : null; }

  @Override public MailMessage decode(JsonObject value) { return (value != null) ? new MailMessage(value) : null; }

  @Override public Class<MailMessage> getTargetClass() { return MailMessage.class; }
}

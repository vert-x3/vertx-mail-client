package io.vertx.ext.mail;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import io.vertx.core.spi.json.JsonCodec;

/**
 * Converter and Codec for {@link io.vertx.ext.mail.MailConfig}.
 * NOTE: This class has been automatically generated from the {@link io.vertx.ext.mail.MailConfig} original class using Vert.x codegen.
 */
public class MailConfigConverter implements JsonCodec<MailConfig, JsonObject> {

  public static final MailConfigConverter INSTANCE = new MailConfigConverter();

  @Override public JsonObject encode(MailConfig value) { return (value != null) ? value.toJson() : null; }

  @Override public MailConfig decode(JsonObject value) { return (value != null) ? new MailConfig(value) : null; }

  @Override public Class<MailConfig> getTargetClass() { return MailConfig.class; }
}

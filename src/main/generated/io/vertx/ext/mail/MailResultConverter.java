package io.vertx.ext.mail;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import io.vertx.core.spi.json.JsonCodec;

/**
 * Converter and Codec for {@link io.vertx.ext.mail.MailResult}.
 * NOTE: This class has been automatically generated from the {@link io.vertx.ext.mail.MailResult} original class using Vert.x codegen.
 */
public class MailResultConverter implements JsonCodec<MailResult, JsonObject> {

  public static final MailResultConverter INSTANCE = new MailResultConverter();

  @Override public JsonObject encode(MailResult value) { return (value != null) ? value.toJson() : null; }

  @Override public MailResult decode(JsonObject value) { return (value != null) ? new MailResult(value) : null; }

  @Override public Class<MailResult> getTargetClass() { return MailResult.class; }
}

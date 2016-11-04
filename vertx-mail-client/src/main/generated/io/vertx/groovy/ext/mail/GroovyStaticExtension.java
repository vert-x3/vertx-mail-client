package io.vertx.groovy.ext.mail;
public class GroovyStaticExtension {
  public static io.vertx.ext.mail.MailClient createNonShared(io.vertx.ext.mail.MailClient j_receiver, io.vertx.core.Vertx vertx, java.util.Map<String, Object> config) {
    return io.vertx.lang.groovy.ConversionHelper.wrap(io.vertx.ext.mail.MailClient.createNonShared(vertx,
      config != null ? new io.vertx.ext.mail.MailConfig(io.vertx.lang.groovy.ConversionHelper.toJsonObject(config)) : null));
  }
  public static io.vertx.ext.mail.MailClient createShared(io.vertx.ext.mail.MailClient j_receiver, io.vertx.core.Vertx vertx, java.util.Map<String, Object> config, java.lang.String poolName) {
    return io.vertx.lang.groovy.ConversionHelper.wrap(io.vertx.ext.mail.MailClient.createShared(vertx,
      config != null ? new io.vertx.ext.mail.MailConfig(io.vertx.lang.groovy.ConversionHelper.toJsonObject(config)) : null,
      poolName));
  }
  public static io.vertx.ext.mail.MailClient createShared(io.vertx.ext.mail.MailClient j_receiver, io.vertx.core.Vertx vertx, java.util.Map<String, Object> config) {
    return io.vertx.lang.groovy.ConversionHelper.wrap(io.vertx.ext.mail.MailClient.createShared(vertx,
      config != null ? new io.vertx.ext.mail.MailConfig(io.vertx.lang.groovy.ConversionHelper.toJsonObject(config)) : null));
  }
}

package io.vertx.groovy.ext.mail;
public class GroovyExtension {
  public static io.vertx.ext.mail.MailClient sendMail(io.vertx.ext.mail.MailClient j_receiver, java.util.Map<String, Object> email, io.vertx.core.Handler<io.vertx.core.AsyncResult<java.util.Map<String, Object>>> resultHandler) {
    io.vertx.lang.groovy.ConversionHelper.wrap(j_receiver.sendMail(email != null ? new io.vertx.ext.mail.MailMessage(io.vertx.lang.groovy.ConversionHelper.toJsonObject(email)) : null,
      resultHandler != null ? new io.vertx.core.Handler<io.vertx.core.AsyncResult<io.vertx.ext.mail.MailResult>>() {
      public void handle(io.vertx.core.AsyncResult<io.vertx.ext.mail.MailResult> ar) {
        resultHandler.handle(ar.map(event -> io.vertx.lang.groovy.ConversionHelper.applyIfNotNull(event, a -> io.vertx.lang.groovy.ConversionHelper.fromJsonObject(a.toJson()))));
      }
    } : null));
    return j_receiver;
  }
}

package io.vertx.ext.mail;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mail.impl.MailClientImpl;

/**
 * SMTP mail client for Vert.x
 * <p>
 * A simple asynchronous API for sending mails from Vert.x applications
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
@VertxGen
public interface MailClient {

  /**
   * create an instance of MailService that is running in the local JVM
   *
   * @param vertx  the Vertx instance the operation will be run in
   * @param config MailConfig configuration to be used for sending mails
   * @return MailService instance that can then be used to send multiple mails
   */
  static MailClient create(Vertx vertx, MailConfig config) {
    return new MailClientImpl(vertx, config);
  }

  /**
   * send a single mail via MailService
   *
   * @param email         MailMessage object containing the mail text, from/to, attachments etc
   * @param resultHandler will be called when the operation is finished or it fails
   *                      (may be null to ignore the result)
   *                      the result JsonObject currently only contains {@code {"result":"success"}}
   * @return this MailService instance so the method can be used fluently
   */
  @Fluent
  MailClient sendMail(MailMessage email, Handler<AsyncResult<JsonObject>> resultHandler);

  /**
   * close the MailClient
   */
  void close();

}

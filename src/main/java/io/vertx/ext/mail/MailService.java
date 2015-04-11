package io.vertx.ext.mail;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.ProxyIgnore;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mail.impl.MailServiceImpl;
import io.vertx.serviceproxy.ProxyHelper;

/**
 * smtp mail service for vert.x
 * 
 * this Interface provides the methods to be used by the application program and is used to
 * generate the service in other languages
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
@VertxGen
@ProxyGen
public interface MailService {

  /**
   * create an instance of MailService that is running in the local JVM
   *
   * @param vertx the Vertx instance the operation will be run in
   * @param config MailConfig configuration to be used for sending mails
   * @return MailService instance that can then be used to send multiple mails
   */
  static MailService create(Vertx vertx, MailConfig config) {
    return new MailServiceImpl(vertx, config);
  }

  /**
   * create an instance of  MailService that calls the mail service via the event bus running somewhere else
   *
   * @param vertx the Vertx instance the operation will be run in
   * @param address the eb address of the mail service running somewhere, default is "vertx.mail"
   * @return MailService instance that can then be used to send multiple mails
   */
  static MailService createEventBusProxy(Vertx vertx, String address) {
    return ProxyHelper.createProxy(MailService.class, vertx, address);
  }

  /**
   * send a single mail via MailService
   * @param email MailMessage object containing the mail text, from/to, attachments etc
   * @param resultHandler will be called when the operation is finished or it fails
   * (may be null to ignore the result)
   * the result JsonObject currently only contains {@code {"result":"success"}}
   * @return this MailService instance so the method can be used fluently
   */
  @Fluent
  MailService sendMail(MailMessage email, Handler<AsyncResult<JsonObject>> resultHandler);

  /**
   * start the MailServer instance if it is running locally (this operation is currently a no-op)
   */
  @ProxyIgnore
  void start();

  /**
   * stop the MailServer instance if it is running locally
   * <p>
   * this operation shuts down the connection pool, doesn't wait for completion of the close operations
   * when the mail service is running on the event bus, this operation has no effect
   */
  @ProxyIgnore
  void stop();

}

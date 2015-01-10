package io.vertx.ext.mail;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.ProxyIgnore;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ProxyHelper;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
// this is not yet working
@VertxGen
@ProxyGen
public interface MailService {

  static MailService create(Vertx vertx, MailConfig config) {
    return new MailServiceImpl(vertx, config);
  }

  static MailService createEventBusProxy(Vertx vertx, String address) {
    return ProxyHelper.createProxy(MailService.class, vertx, address);
  }

  // send an email previously constructed
  // TODO: this is not implemented
  void sendMailString(String email, Handler<AsyncResult<JsonObject>> resultHandler);

  void sendMail(MailMessage email, Handler<AsyncResult<JsonObject>> resultHandler);

  // send an email with options based on a apache.commons.mail
  // Email object, this will not go through the event bus but will
  // be constructed as JsonObject locally and sent via the previous method
  // TODO: not quite sure if this method makes much sense to be available in the interface
//  @GenIgnore
//  void sendMail(Email email, Handler<AsyncResult<JsonObject>> resultHandler);

  @ProxyIgnore
  void start();

  @ProxyIgnore
  void stop();

}

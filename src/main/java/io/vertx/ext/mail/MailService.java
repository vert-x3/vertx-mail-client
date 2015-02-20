package io.vertx.ext.mail;

import io.vertx.codegen.annotations.Fluent;
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
@VertxGen
@ProxyGen
public interface MailService {

  static MailService create(Vertx vertx, MailConfig config) {
    return new MailServiceImpl(vertx, config);
  }

  static MailService createEventBusProxy(Vertx vertx, String address) {
    return ProxyHelper.createProxy(MailService.class, vertx, address);
  }

  @Fluent
  MailService sendMail(MailMessage email, Handler<AsyncResult<JsonObject>> resultHandler);

  @ProxyIgnore
  void start();

  @ProxyIgnore
  void stop();

}

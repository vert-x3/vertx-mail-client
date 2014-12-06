package io.vertx.ext.mail;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.ProxyIgnore;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
// this is not yet working
@VertxGen
@ProxyGen
public interface MailService {

  static MailService create(Vertx vertx, JsonObject config) {
    return new MailServiceImpl(vertx, config);
  }

  // static MailService createEventBusProxy(Vertx vertx, String address) {
  // return ProxyHelper.createProxy(MailService.class, vertx, address);
  // }

  void sendMail(String email, String username, String pw,
      Handler<AsyncResult<String>> resultHandler);

  @ProxyIgnore
  void start();

  @ProxyIgnore
  void stop();

}

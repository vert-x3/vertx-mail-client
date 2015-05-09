package io.vertx.ext.mail;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.ProxyIgnore;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
@ProxyGen
@VertxGen
public interface MailService extends MailClient {

  @Override
  @Fluent
  MailService sendMail(MailMessage email, Handler<AsyncResult<JsonObject>> resultHandler);

  @Override
  @ProxyIgnore
  void close();
}

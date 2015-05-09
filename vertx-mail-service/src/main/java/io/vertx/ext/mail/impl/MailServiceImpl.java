package io.vertx.ext.mail.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.MailService;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class MailServiceImpl implements MailService {

  private final MailClient client;

  public MailServiceImpl(MailClient client) {
    this.client = client;
  }

  @Override
  public MailService sendMail(MailMessage email, Handler<AsyncResult<JsonObject>> resultHandler) {
    client.sendMail(email, resultHandler);
    return this;
  }

  @Override
  public void close() {
    client.close();
  }
}

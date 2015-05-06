package io.vertx.ext.mail.impl;

import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;

/**
 * MailService implementation for sending mails inside the local JVM
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
public class MailClientImpl implements MailClient {

  private static final Logger log = LoggerFactory.getLogger(MailClientImpl.class);

  private final Vertx vertx;
  private final SMTPConnectionPool connectionPool;
  private boolean closed = false;

  public MailClientImpl(Vertx vertx, MailConfig config) {
    this.vertx = vertx;
    this.connectionPool = new SMTPConnectionPool(vertx, config);
  }

  @Override
  public void close() {
    connectionPool.close();
  }

  @Override
  public MailClient sendMail(MailMessage message, Handler<AsyncResult<JsonObject>> resultHandler) {
    Context context = vertx.getOrCreateContext();
    if (!closed) {
      MailMain mailMain = new MailMain(connectionPool,
        resultHandler == null ? null : res -> context.runOnContext(v -> resultHandler.handle(res)));
      mailMain.sendMail(message);
    } else {
      resultHandler.handle(Future.failedFuture("mail service has been closed"));
    }
    return this;
  }

}

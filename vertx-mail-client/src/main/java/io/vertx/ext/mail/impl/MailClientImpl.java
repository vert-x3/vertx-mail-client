package io.vertx.ext.mail.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.MailClient;
import io.vertx.core.Future;

/**
 * MailService implementation for sending mails inside the local JVM
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
public class MailClientImpl implements MailClient {

  private static final Logger log = LoggerFactory.getLogger(MailClientImpl.class);

  private final Context context;
  private final SMTPConnectionPool connectionPool;
  private boolean closed = false;

  /**
   * construct a MailService object with the vertx and config configuration
   * <p>
   * this is used by MailService.create
   * @param vertx the Vertx instance the mails will be sent in
   * @param config the configuration of the mailserver
   */
  public MailClientImpl(Vertx vertx, MailConfig config) {
    context = vertx.getOrCreateContext();
    connectionPool = new SMTPConnectionPool(vertx, config, context);
  }

  @Override
  public void close() {
    if (!closed) {
      log.debug("closing mail service");
      connectionPool.stop();
      closed = true;
    }
  }

  @Override
  public MailClient sendMail(MailMessage message, Handler<AsyncResult<JsonObject>> resultHandler) {
    if(!closed) {
      context.runOnContext(v -> {
        MailMain mailMain = new MailMain(connectionPool, resultHandler);
        mailMain.sendMail(message);
      });
    } else {
      resultHandler.handle(Future.failedFuture("mail service has been closed"));
    }
    return this;
  }

}

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
 */
public class MailClientImpl implements MailClient {

  private static final Logger log = LoggerFactory.getLogger(MailClientImpl.class);

  private final Vertx vertx;
  private final SMTPConnectionPool connectionPool;
  private volatile boolean closed = false;

  public MailClientImpl(Vertx vertx, MailConfig config) {
    this.vertx = vertx;
    this.connectionPool = new SMTPConnectionPool(vertx, config);
  }

  @Override
  public void close() {
    connectionPool.close();
    closed = true;
  }

  @Override
  public MailClient sendMail(MailMessage message, Handler<AsyncResult<JsonObject>> resultHandler) {
    Context context = vertx.getOrCreateContext();
    if (!closed) {
      if (validateHeaders(message, resultHandler, context)) {
        connectionPool.getConnection(conn -> sendMessage(message, conn, resultHandler, context),
                                     t ->  handleError(t, resultHandler, context));
      }
    } else {
      handleError("mail service has been closed", resultHandler, context);
    }
    return this;
  }

  private void sendMessage(MailMessage email, SMTPConnection conn, Handler<AsyncResult<JsonObject>> resultHandler,
                           Context context) {
    new SMTPSendMail(conn, email, v -> {
      conn.returnToPool();
      /// FIXME Why return a JSON object? What's the point?
      JsonObject result = new JsonObject();
      result.put("result", "success");
      returnResult(Future.succeededFuture(result), resultHandler, context);
    }, t -> {
      conn.setBroken();
      handleError(t, resultHandler, context);
    }).startMail();
  }

  // do some validation before we open the connection
  // return true on successful validation so we can stop processing above
  private boolean validateHeaders(MailMessage email, Handler<AsyncResult<JsonObject>> resultHandler, Context context) {
    if (email.getBounceAddress() == null && email.getFrom() == null) {
      handleError("sender address is not present", resultHandler, context);
      return false;
    } else if ((email.getTo() == null || email.getTo().size() == 0)
      && (email.getCc() == null || email.getCc().size() == 0)
      && (email.getBcc() == null || email.getBcc().size() == 0)) {
      log.warn("no recipient addresses are present");
      handleError("no recipient addresses are present", resultHandler, context);
      return false;
    } else {
      return true;
    }
  }

  private void handleError(String message, Handler<AsyncResult<JsonObject>> resultHandler, Context context) {
    log.debug("handleError:" + message);
    returnResult(Future.failedFuture(message), resultHandler, context);
  }

  private void handleError(Throwable t, Handler<AsyncResult<JsonObject>> resultHandler, Context context) {
    log.debug("handleError:" + t);
    returnResult(Future.failedFuture(t), resultHandler, context);
  }

  private void returnResult(Future<JsonObject> result, Handler<AsyncResult<JsonObject>> resultHandler, Context context) {
    // Note - results must always be executed on the right context, asynchronously, not directly!
    context.runOnContext(v -> {
      if (resultHandler != null) {
        resultHandler.handle(result);
      } else {
        if (result.succeeded()) {
          log.debug("dropping sendMail result");
        } else {
          log.info("dropping sendMail failure", result.cause());
        }
      }
    });
  }

}

package io.vertx.ext.mail;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
public class MailServiceImpl implements MailService {

  private static final Logger log = LoggerFactory.getLogger(MailServiceImpl.class);

  private Vertx vertx;
  private MailConfig config;
  private boolean stopped = false;

  private ConnectionPool connectionPool;

  public MailServiceImpl(Vertx vertx, MailConfig config) {
    if (connectionPool == null) {
      connectionPool = new ConnectionPool(vertx);
    }
    this.vertx = vertx;
    this.config = config;
  }

  @Override
  public void start() {
    // may take care of validating the options
    // and configure a queue if we implement one
    log.debug("mail service started");
  }

  @Override
  public void stop() {
    log.debug("mail service stopped");
    if (!stopped) {
      stopped = true;
      connectionPool.stop();
    }
  }

  @Override
  public MailService sendMail(MailMessage message, Handler<AsyncResult<JsonObject>> resultHandler) {
    vertx.runOnContext(v -> {
      MailMain mailMain = new MailMain(config, connectionPool, resultHandler);
      mailMain.sendMail(message);
    });
    return this;
  }

  @Override
  public MailService sendMailString(MailMessage message, String messageText,
      Handler<AsyncResult<JsonObject>> resultHandler) {
    vertx.runOnContext(v -> {
      MailMain mailMain = new MailMain(config, connectionPool, resultHandler);
      mailMain.sendMail(message, messageText);
    });
    return this;
  }

}

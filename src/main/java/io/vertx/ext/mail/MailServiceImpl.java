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

  public MailServiceImpl(Vertx vertx, MailConfig config) {
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
    // may shut down the queue, if we implement one
    log.debug("mail service stopped");
  }

  @Override
  public MailService sendMail(MailMessage message, Handler<AsyncResult<JsonObject>> resultHandler) {
    MailMain mailVerticle = new MailMain(vertx, config, resultHandler);
    mailVerticle.sendMail(message);
    return this;
  }

  @Override
  public MailService sendMailString(String email, Handler<AsyncResult<JsonObject>> resultHandler) {
    // TODO Auto-generated method stub
    return null;
  }

}

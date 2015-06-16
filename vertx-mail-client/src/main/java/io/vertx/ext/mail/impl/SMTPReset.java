package io.vertx.ext.mail.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.Future;

/**
 * Handle the reset command, this is mostly used to check if the connection is
 * still active
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
class SMTPReset {

  SMTPConnection connection;
  Handler<AsyncResult<Void>> handler;

  private static final Logger log = LoggerFactory.getLogger(SMTPReset.class);

  public SMTPReset(SMTPConnection connection, Handler<AsyncResult<Void>> finishedHandler) {
    this.connection = connection;
    this.handler = finishedHandler;
  }

  public void start() {
    connection.setErrorHandler(th -> {
      log.info("exception on RSET " + th);
      connection.resetErrorHandler();
      connection.setBroken();
      connection.shutdown();
      handleError("exception on RSET " + th);
    });
    connection.write("RSET", message -> {
      log.debug("RSET result: " + message);
      connection.resetErrorHandler();
      if (!StatusCode.isStatusOk(message)) {
        log.warn("RSET failed: " + message);
        handleError("reset command failed: " + message);
      } else {
        finished();
      }
    });
  }

  /**
   *
   */
  private void finished() {
    handler.handle(Future.succeededFuture(null));
  }

  private void handleError(String message) {
    handler.handle(Future.failedFuture(message));
  }

}

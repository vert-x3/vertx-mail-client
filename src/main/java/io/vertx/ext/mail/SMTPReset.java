package io.vertx.ext.mail;

import io.vertx.core.Handler;
import io.vertx.core.impl.NoStackTraceThrowable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;

/**
 * Handle the reset command, this is mostly used to check if the connection is still active 
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
class SMTPReset {

  SMTPConnection connection;
  MailConfig config;
  Handler<Void> finishedHandler;
  Handler<Throwable> errorHandler;

  private static final Logger log = LoggerFactory.getLogger(SMTPReset.class);

  public SMTPReset(SMTPConnection connection, MailConfig config,
      Handler<Void> finishedHandler, Handler<Throwable> errorHandler) {
    this.connection = connection;
    this.config = config;
    this.finishedHandler = finishedHandler;
    this.errorHandler = errorHandler;
  }

  public void rsetCmd() {
    connection.setErrorHandler(th -> {
      log.info("exception on RSET "+th);
      connection.resetErrorHandler();
      connection.setInactive();
      connection.shutdown();
      throwError("exception on RSET "+th);
    });
    connection.write("RSET", message -> {
      log.debug("RSET result: " + message);
      connection.resetErrorHandler();
      if (!StatusCode.isStatusOk(message)) {
        log.warn("RSET failed: " + message);
        throwError("reset command failed: " + message);
      } else {
        finished();
      }
    });
  }

  /**
   * 
   */
  private void finished() {
    finishedHandler.handle(null);
  }

  private void throwError(String message) {
    errorHandler.handle(new NoStackTraceThrowable(message));
  }

//  private void throwError(Throwable throwable) {
//    errorHandler.handle(throwable);
//  }

}

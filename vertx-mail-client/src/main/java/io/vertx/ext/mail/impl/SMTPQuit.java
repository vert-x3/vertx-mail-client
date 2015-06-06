package io.vertx.ext.mail.impl;

import io.vertx.core.Handler;
import io.vertx.core.impl.NoStackTraceThrowable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * handle connection quit
 * <p>
 * There is not much point in encapsulating this but its useful for the
 * connection pool
 * <p>
 * this operation does not throw any error, it just closes the connection in the
 * end
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
class SMTPQuit {

  private static final Logger log = LoggerFactory.getLogger(SMTPQuit.class);

  private final SMTPConnection connection;

  private final Handler<Void> finishedHandler;

  private final Handler<Throwable> exceptionHandler;

  SMTPQuit(SMTPConnection connection, Handler<Void> finishedHandler, Handler<Throwable> exceptionHandler) {
    this.connection = connection;
    this.finishedHandler = finishedHandler;
    this.exceptionHandler = exceptionHandler;
  }

  void start() {
    connection.setErrorHandler(exceptionHandler);
    connection.write("QUIT", message -> {
      connection.resetErrorHandler();
      log.debug("QUIT result: " + message);
      if (StatusCode.isStatusOk(message)) {
        finishedHandler.handle(null);
      } else {
        log.warn("quit failed: " + message);
        exceptionHandler.handle(new NoStackTraceThrowable("QUIT result " + message));
      }
    });
  }

}

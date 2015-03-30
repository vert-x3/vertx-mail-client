package io.vertx.ext.mail;

import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;


/**
 * handle connection quit
 *
 * There is not much point in encapsulating this
 * but its useful for the connection pool
 *
 * this operation does not throw any error, it just closes the connection
 * in the end
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
class SMTPQuit {

  private static final Logger log = LoggerFactory.getLogger(SMTPQuit.class);

  private SMTPConnection connection;

  private Handler<Void> finishedHandler;

  SMTPQuit(SMTPConnection connection, Handler<Void> finishedHandler) {
    this.connection = connection;
    this.finishedHandler = finishedHandler;
  }

  void quitCmd() {
    connection.write("QUIT", message -> {
      log.debug("QUIT result: " + message);
      if (!StatusCode.isStatusOk(message)) {
        log.warn("quit failed: " + message);
      }
      finishedHandler.handle(null);
    });
  }

}

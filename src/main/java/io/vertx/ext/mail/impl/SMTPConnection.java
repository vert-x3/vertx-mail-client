package io.vertx.ext.mail.impl;

import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.impl.NoStackTraceThrowable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetSocket;
import io.vertx.ext.mail.MailConfig;

/**
 * SMTP connection to a server.
 *
 * Encapsulate the NetClient connection and the data writing/reading, but not
 * the protocol itself
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
class SMTPConnection {

  private NetSocket ns;
  private boolean socketClosed;
  private boolean socketShutDown;
  private Handler<String> commandReplyHandler;
  private Handler<Throwable> errorHandler;
  private boolean broken;
  private boolean idle;
  private final NetClient client;
  private final Context context;

  private static final Logger log = LoggerFactory.getLogger(SMTPConnection.class);

  SMTPConnection(NetClient client, Context context) {
    broken = true;
    idle = false;
    this.client = client;
    this.context = context;
  }

  private Capabilities capa = new Capabilities();

  /**
   * @return the capabilities object
   */
  Capabilities getCapa() {
    return capa;
  }

  /**
   * parse capabilities from the ehlo reply string
   *
   * @param message
   *          the capabilities to set
   */
  void parseCapabilities(String message) {
    capa = new Capabilities();
    capa.parseCapabilities(message);
  }

  void shutdown() {
    commandReplyHandler = null;
    socketShutDown = true;
    if (ns != null) {
      ns.close();
      ns = null;
    }
  }

  /*
   * write command without masking anything
   */
  void write(String str, Handler<String> commandResultHandler) {
    write(str, -1, commandResultHandler);
  }

  /*
   * write command masking everything after position blank
   */
  void write(String str, int blank, Handler<String> commandResultHandler) {
    this.commandReplyHandler = commandResultHandler;
    if (socketClosed) {
      log.debug("connection was closed by server");
      handleError("connection was closed by server");
    }
    if (log.isDebugEnabled()) {
      String logStr;
      if (blank >= 0) {
        StringBuilder sb = new StringBuilder();
        for (int i = blank; i < str.length(); i++) {
          sb.append('*');
        }
        logStr = str.substring(0, blank) + sb;
      } else {
        logStr = str;
      }
      // avoid logging large mail body
      if (logStr.length() < 1000) {
        log.debug("write on STMPConnection " + this.toString() + " command: " + logStr);
      } else {
        log.debug("write on STMPConnection " + this.toString() + " command: " + logStr.substring(0, 1000) + "...");
      }
    }
    ns.write(str + "\r\n");
  }

  private void handleError(String message) {
    handleError(new NoStackTraceThrowable(message));
  }

  private void handleError(Throwable throwable) {
    errorHandler.handle(throwable);
  }

  public void openConnection(Vertx vertx, MailConfig config, Handler<String> initialReplyHandler,
      Handler<Throwable> errorHandler) {
    this.errorHandler = errorHandler;
    broken = false;
    idle = false;

    context.runOnContext(v1 -> {
      client.connect(config.getPort(), config.getHostname(), asyncResult -> {
        if (asyncResult.succeeded()) {
          ns = asyncResult.result();
          socketClosed = false;
          ns.exceptionHandler(e -> {
            // avoid returning two exceptions
            log.debug("exceptionHandler called");
            if (!socketClosed && !socketShutDown && !idle && !broken) {
              broken = true;
              log.debug("got an exception on the netsocket", e);
              handleError(e);
            }
          });
          ns.closeHandler(v -> {
            log.debug("closeHandler called");
            log.debug("socket has been closed");
            socketClosed = true;
            // avoid exception if we regularly shut down the socket on our side
            if (!socketShutDown && !idle && !broken) {
              broken = true;
              log.debug("throwing: connection has been closed by the server");
              handleError("connection has been closed by the server");
            }
          });
          commandReplyHandler = initialReplyHandler;
          final Handler<Buffer> mlp = new MultilineParser(buffer -> {
            if (commandReplyHandler == null) {
              log.debug("dropping reply arriving after we stopped processing \"" + buffer.toString() + "\"");
            } else {
              // make sure we only call the handler once
              Handler<String> currentHandler = commandReplyHandler;
              commandReplyHandler = null;
              currentHandler.handle(buffer.toString());
            }
          });
          ns.handler(mlp);
        } else {
          log.error("exception on connect", asyncResult.cause());
          handleError(asyncResult.cause());
        }
      });
    });
  }

  boolean isSsl() {
    return ns.isSsl();
  }

  void upgradeToSsl(Handler<Void> handler) {
    ns.upgradeToSsl(handler);
  }

  public boolean isBroken() {
    return broken;
  }

  public boolean isIdle() {
    return idle;
  }

  public void returnToPool() {
    log.debug("returning connection to pool");
    idle = true;
    commandReplyHandler = null;
  }

  /**
   * mark a connection as being used again
   */
  public void useConnection() {
    idle = false;
  }

  /*
   * set error handler to a "local" handler to be reset later
   */
  private Handler<Throwable> prevErrorHandler = null;

  public void setErrorHandler(Handler<Throwable> newHandler) {
    if (prevErrorHandler == null) {
      prevErrorHandler = errorHandler;
    }

    errorHandler = newHandler;
  }

  /*
   * reset error handler to default
   */
  public void resetErrorHandler() {
    errorHandler = prevErrorHandler;
  }

  /**
   * set connection to broken, it will not be used again
   * TODO: (and shut down and closed async)
   */
  public void setBroken() {
    log.debug("setting connection to broken");
    broken = true;
    commandReplyHandler = null;
  }
}

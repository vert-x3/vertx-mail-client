package io.vertx.ext.mail;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.impl.NoStackTraceThrowable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;

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

  private static final Logger log = LoggerFactory.getLogger(SMTPConnection.class);

  public SMTPConnection() {
    // TODO Auto-generated constructor stub
  }

  private NetSocket ns;
  private boolean socketClosed;
  private boolean socketShutDown;
  private NetClient client;
  private Handler<String> commandReplyHandler;
  private Handler<Throwable> errorHandler;

  void shutdown() {
    commandReplyHandler = null;
    socketShutDown = true;
    if (ns != null) {
      ns.close();
      ns = null;
    }
    if (client != null) {
      client.close();
      client = null;
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
      throwError("connection was closed by server");
    }
    if (log.isDebugEnabled()) {
      String logStr;
      if (blank >= 0) {
        StringBuilder sb = new StringBuilder();
        for (int i = blank; i < str.length(); i++) {
          sb.append('*');
        }
        logStr = str.substring(0, blank + 1) + sb;
      } else {
        logStr = str;
      }
      // avoid logging large mail body
      if (logStr.length() < 1000) {
        log.debug("command: " + logStr);
      } else {
        log.debug("command: " + logStr.substring(0, 1000) + "...");
      }
    }
    ns.write(str + "\r\n");
  }

  private void throwError(String message) {
    errorHandler.handle(new NoStackTraceThrowable(message));
  }

  private void throwError(Throwable throwable) {
    errorHandler.handle(throwable);
  }

  public void initializeConnection(Vertx vertx, MailConfig config, Handler<String> initialReplyHandler,
      Handler<Throwable> errorHandler) {
    this.errorHandler = errorHandler;
    NetClientOptions netClientOptions = new NetClientOptions().setSsl(config.isSsl());
    client = vertx.createNetClient(netClientOptions);

    client.connect(config.getPort(), config.getHostname(), asyncResult -> {
      if (asyncResult.succeeded()) {
        ns = asyncResult.result();
        socketClosed = false;
        ns.exceptionHandler(e -> {
          // avoid returning two exceptions
          if (!socketClosed && !socketShutDown) {
            log.debug("got an exception on the netsocket", e);
            throwError(e);
          }
        });
        ns.closeHandler(v -> {
          // avoid exception if we regularly shut down the socket on our side
          if (!socketShutDown) {
            log.debug("socket has been closed");
            socketClosed = true;
            throwError("connection has been closed by the server");
          }
        });
        commandReplyHandler = initialReplyHandler;
        final Handler<Buffer> mlp = new MultilineParser(buffer -> {
          if (commandReplyHandler == null) {
            log.debug("dropping reply arriving after we stopped processing \"" + buffer.toString() + "\"");
          } else {
            commandReplyHandler.handle(buffer.toString());
          }
        });
        ns.handler(mlp);
      } else {
        log.error("exception on connect", asyncResult.cause());
        throwError(asyncResult.cause());
      }
    });
  }

  boolean isSsl() {
    return ns.isSsl();
  }

  void upgradeToSsl(Handler<Void> handler) {
    ns.upgradeToSsl(handler);
  }

}

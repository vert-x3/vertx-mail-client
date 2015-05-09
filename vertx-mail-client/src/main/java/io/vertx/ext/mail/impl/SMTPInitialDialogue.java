package io.vertx.ext.mail.impl;

import io.vertx.core.Handler;
import io.vertx.core.impl.NoStackTraceThrowable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.StartTLSOptions;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Handle welcome line, EHLO/HELO, capabilities
 * and STARTTLS if necessary
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
class SMTPInitialDialogue {

  private static final Logger log = LoggerFactory.getLogger(SMTPInitialDialogue.class);

  private SMTPConnection connection;

  private Handler<Throwable> errorHandler;
  private Handler<Void> finishedHandler;

  private MailConfig config;

  public SMTPInitialDialogue(SMTPConnection connection, MailConfig config, Handler<Void> finishedHandler,
                             Handler<Throwable> errorHandler) {
    this.connection = connection;
    this.config = config;
    this.finishedHandler = finishedHandler;
    this.errorHandler = errorHandler;
  }

  public void start(final String message) {
    log.debug("server greeting: " + message);
    if (StatusCode.isStatusOk(message)) {
      if (isEsmtpSupported(message)) {
        ehloCmd();
      } else {
        heloCmd();
      }
    } else {
      handleError("got error response " + message);
    }
  }

  private boolean isEsmtpSupported(String message) {
    // note that message in this case is the inital reply from the server
    // e.g. 220 example.com ESMTP Postfix
    return message.contains("ESMTP");
  }

  private void ehloCmd() {
    connection
      .write(
        "EHLO " + getMyHostname(),
        message -> {
          log.debug("EHLO result: " + message);
          if (StatusCode.isStatusOk(message)) {
            connection.parseCapabilities(message);
            if (connection.getCapa().isStartTLS()
              && !connection.isSsl()
              && (config.getStarttls() == StartTLSOptions.REQUIRED || config.getStarttls() == StartTLSOptions.OPTIONAL)) {
              // do not start TLS if we are connected with SSL
              // or are already in TLS
              startTLSCmd();
            } else {
              finished();
            }
          } else {
            // if EHLO fails, assume we have to do HELO
            if (StatusCode.isStatusTemporary(message)) {
              heloCmd();
            } else {
              handleError("EHLO failed with " + message);
            }
          }
        });
  }

  private void heloCmd() {
    connection.write("HELO " + getMyHostname(), message -> {
      log.debug("HELO result: " + message);
      if (StatusCode.isStatusOk(message)) {
        finished();
      } else {
        handleError("HELO failed with " + message);
      }
    });
  }

  private void handleError(String message) {
    log.debug("handleError:" + message);
    errorHandler.handle(new NoStackTraceThrowable(message));
  }

//  private void handleError(Throwable throwable) {
//    errorHandler.handle(throwable);
//  }

  /**
   * get the hostname either from config or by resolving our own address
   *
   * @return the hostname
   */
  private String getMyHostname() {
    if (config.getEhloHostname() != null) {
      return config.getEhloHostname();
    } else {
      try {
        InetAddress ip = InetAddress.getLocalHost();
        return ip.getCanonicalHostName();
      } catch (UnknownHostException e) {
        // as a last resort, use localhost
        return "localhost";
      }
    }
  }

  /**
   *
   */
  private void startTLSCmd() {
    connection.write("STARTTLS", message -> {
      log.debug("STARTTLS result: " + message);
      connection.upgradeToSsl(v -> {
        log.debug("ssl started");
        // capabilities may have changed, e.g.
        // if a service only announces PLAIN/LOGIN
        // on secure channel (e.g. googlemail)
        ehloCmd();
      });
    });
  }

  private void finished() {
    if (connection.isSsl() || config.getStarttls() != StartTLSOptions.REQUIRED) {
      finishedHandler.handle(null);
    } else {
      log.warn("STARTTLS required but not supported by server");
      handleError("STARTTLS required but not supported by server");
    }
  }

}

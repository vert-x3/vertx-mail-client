package io.vertx.ext.mail.impl;

import io.vertx.core.Handler;
import io.vertx.core.impl.NoStackTraceThrowable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.mail.LoginOption;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.impl.sasl.AuthOperation;
import io.vertx.ext.mail.impl.sasl.AuthOperationFactory;
import io.vertx.ext.mail.impl.sasl.CryptUtils;

import java.util.Set;

/**
 * Handle the authentication flow
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
class SMTPAuthentication {

  SMTPConnection connection;
  MailConfig config;
  Handler<Void> finishedHandler;
  Handler<Throwable> errorHandler;

  private static final Logger log = LoggerFactory.getLogger(SMTPAuthentication.class);

  public SMTPAuthentication(SMTPConnection connection, MailConfig config, Handler<Void> finishedHandler,
      Handler<Throwable> errorHandler) {
    this.connection = connection;
    this.config = config;
    this.finishedHandler = finishedHandler;
    this.errorHandler = errorHandler;
  }

  public void start() {
    final boolean foundAllowedMethods = !intersectAllowedMethods().isEmpty();
    if (config.getLogin() != LoginOption.DISABLED && config.getUsername() != null && config.getPassword() != null
        && foundAllowedMethods) {
      authCmd();
    } else {
      if (config.getLogin() == LoginOption.REQUIRED) {
        if (!foundAllowedMethods) {
          handleError("login is required, but no allowed AUTH methods available. You may need to do STARTTLS");
        } else {
          handleError("login is required, but no credentials supplied");
        }
      } else {
        finished();
      }
    }
  }

  /**
   * find the auth methods we can use
   * 
   * @return
   */
  private Set<String> intersectAllowedMethods() {
    final String authMethods = config.getAuthMethods();
    Set<String> allowed;
    if (authMethods == null || authMethods.isEmpty()) {
      allowed = connection.getCapa().getCapaAuth();
    } else {
      allowed = Utils.parseCapaAuth(authMethods);
      allowed.retainAll(connection.getCapa().getCapaAuth());
    }
    return allowed;
  }

  public void authCmd() {
    // if we have defined a choice of methods, only use these
    // this works for example to avoid plain text pw methods with
    // "CRAM-SHA1 CRAM-MD5"
    AuthOperation authOperation = AuthOperationFactory.createAuth(config.getUsername(), config.getPassword(),
        intersectAllowedMethods());

    if (authOperation != null) {
      authCmdStep(authOperation, null);
    } else {
      log.warn("cannot find supported auth method");
      handleError("cannot find supported auth method");
    }
  }

  private void authCmdStep(AuthOperation authMethod, String message) {
    String nextLine;
    int blank;
    if (message == null) {
      String authParameter = authMethod.nextStep(null);
      if (!authParameter.isEmpty()) {
        nextLine = "AUTH " + authMethod.getName() + " " + CryptUtils.base64(authParameter);
        blank = authMethod.getName().length() + 6;
      } else {
        nextLine = "AUTH " + authMethod.getName();
        blank = -1;
      }
    } else {
      nextLine = CryptUtils.base64(authMethod.nextStep(CryptUtils.decodeb64(message.substring(4))));
      blank = 0;
    }
    connection.write(nextLine, blank, message2 -> {
      log.debug("AUTH command result: " + message2);
      if (StatusCode.isStatusOk(message2)) {
        if (StatusCode.isStatusContinue(message2)) {
          authCmdStep(authMethod, message2);
        } else {
          finished();
        }
      } else {
        handleError("AUTH " + authMethod.getName() + " failed " + message2);
      }
    });
  }

  /**
   * 
   */
  private void finished() {
    finishedHandler.handle(null);
  }

  private void handleError(String message) {
    errorHandler.handle(new NoStackTraceThrowable(message));
  }

}

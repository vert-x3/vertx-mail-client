/*
 *  Copyright (c) 2011-2015 The original author or authors
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *       The Eclipse Public License is available at
 *       http://www.eclipse.org/legal/epl-v10.html
 *
 *       The Apache License v2.0 is available at
 *       http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 */

package io.vertx.ext.mail.impl;

import io.vertx.core.Handler;
import io.vertx.core.impl.NoStackTraceThrowable;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.ext.mail.LoginOption;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.impl.sasl.AuthOperation;
import io.vertx.ext.mail.impl.sasl.AuthOperationFactory;
import io.vertx.ext.mail.impl.sasl.CryptUtils;

import java.util.List;

/**
 * Handle the authentication flow
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
class SMTPAuthentication {

  private final SMTPConnection connection;
  private final MailConfig config;
  private final Handler<Void> finishedHandler;
  private final Handler<Throwable> errorHandler;

  private static final Logger log = LoggerFactory.getLogger(SMTPAuthentication.class);

  private final AuthOperationFactory authOperationFactory;

  SMTPAuthentication(SMTPConnection connection, MailConfig config, AuthOperationFactory authOperationFactory, Handler<Void> finishedHandler,
                     Handler<Throwable> errorHandler) {
    this.connection = connection;
    this.config = config;
    this.finishedHandler = finishedHandler;
    this.errorHandler = errorHandler;
    this.authOperationFactory = authOperationFactory;
  }

  public void start() {
    List<String> auths = intersectAllowedMethods();
    final boolean foundAllowedMethods = !auths.isEmpty();
    if (config.getLogin() != LoginOption.DISABLED && config.getUsername() != null && config.getPassword() != null
      && foundAllowedMethods) {
      authCmd(auths);
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
   * @return intersection between supported and allowed auth methods
   */
  private List<String> intersectAllowedMethods() {
    List<String> supported = this.authOperationFactory.supportedAuths(config);
    supported.retainAll(connection.getCapa().getCapaAuth());
    return supported;
  }

  private void authCmd(List<String> auths) {
    // if we have defined a choice of methods, only use these
    // this works for example to avoid plain text pw methods with
    // "CRAM-SHA1 CRAM-MD5"
    String defaultAuth = this.authOperationFactory.getAuthMethod();
    if (defaultAuth != null) {
      if (log.isDebugEnabled()) {
        log.debug("Using default auth method: " + defaultAuth);
      }
      authMethod(defaultAuth, error -> authChain(auths, 0, null));
    } else {
      authChain(auths, 0, null);
    }
  }

  private void authChain(List<String> auths, int i, Throwable e) {
    if (i < auths.size()) {
      if (e != null) {
        log.warn(e);
      }
      authMethod(auths.get(i), error -> authChain(auths, i + 1, error));
    } else {
      handleError(e);
    }
  }

  private void authMethod(String auth, Handler<Throwable> onError) {
    AuthOperation authMethod;
    try {
      authMethod = authOperationFactory.createAuth(config, auth);
    } catch (IllegalArgumentException | SecurityException ex) {
      log.warn("authentication factory threw exception", ex);
      handleError(ex);
      return;
    }
    authCmdStep(authMethod, null, onError);
  }

  private void authCmdStep(AuthOperation authMethod, String message, Handler<Throwable> onError) {
    String nextLine;
    int blank;
    try {
      if (message == null) {
        String authParameter = authMethod.nextStep(null);
        if (!authParameter.isEmpty()) {
          if (!authMethod.handleCoding()) {
            authParameter = CryptUtils.base64(authParameter);
          }
          nextLine = "AUTH " + authMethod.getName() + " " + authParameter;
          blank = authMethod.getName().length() + 6;
        } else {
          nextLine = "AUTH " + authMethod.getName();
          blank = -1;
        }
      } else {
        if (!authMethod.handleCoding()) {
          nextLine = CryptUtils.base64(authMethod.nextStep(CryptUtils.decodeb64(message.substring(4))));
        } else {
          nextLine = authMethod.nextStep(message.substring(4));
        }
        blank = 0;
      }
    } catch (Exception e) {
      log.warn("Failed to handle server auth message: " + message, e);
      onError.handle(e);
      return;
    }
    connection.write(nextLine, blank, message2 -> {
      SMTPResponse response = new SMTPResponse(message2);
      if (response.isStatusOk()) {
        if (response.isStatusContinue()) {
          log.debug("Auth Continue with response: " + message2);
          authCmdStep(authMethod, message2, onError);
        } else {
          authOperationFactory.setAuthMethod(authMethod.getName());
          finished();
        }
      } else {
        onError.handle(response.toException("AUTH " + authMethod.getName() + " failed", connection.getCapa().isCapaEnhancedStatusCodes()));
      }
    });
  }

  private void finished() {
    finishedHandler.handle(null);
  }

  private void handleError(String message) {
    errorHandler.handle(new NoStackTraceThrowable(message));
  }

  private void handleError(Throwable th) {
    errorHandler.handle(th);
  }

}

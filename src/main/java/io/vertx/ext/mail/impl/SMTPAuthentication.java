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

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
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
  private static final Logger log = LoggerFactory.getLogger(SMTPAuthentication.class);
  private final AuthOperationFactory authOperationFactory;

  SMTPAuthentication(SMTPConnection connection, MailConfig config, AuthOperationFactory authOperationFactory) {
    this.connection = connection;
    this.config = config;
    this.authOperationFactory = authOperationFactory;
  }

  public Future<SMTPConnection> start() {
    List<String> auths = intersectAllowedMethods();
    final boolean foundAllowedMethods = !auths.isEmpty();
    if (config.getLogin() != LoginOption.DISABLED && config.getUsername() != null && config.getPassword() != null
      && foundAllowedMethods) {
      return authCmd(auths);
    } else {
      if (config.getLogin() == LoginOption.REQUIRED) {
        if (!foundAllowedMethods) {
          return Future.failedFuture("login is required, but no allowed AUTH methods available. You may need to do STARTTLS");
        } else {
          return Future.failedFuture("login is required, but no credentials supplied");
        }
      } else {
        return Future.succeededFuture(connection);
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

  private Future<SMTPConnection> authCmd(List<String> auths) {
    // if we have defined a choice of methods, only use these
    // this works for example to avoid plain text pw methods with
    // "CRAM-SHA1 CRAM-MD5"
    String defaultAuth = this.authOperationFactory.getAuthMethod();
    Promise<SMTPConnection> promise = Promise.promise();
    if (defaultAuth != null) {
      if (log.isDebugEnabled()) {
        log.debug("Using default auth method: " + defaultAuth);
      }
      Promise<SMTPConnection> innerPromise = Promise.promise();
      innerPromise.future().onComplete(result -> {
        if (result.succeeded()) {
          promise.handle(Future.succeededFuture(connection));
        } else {
          authChain(auths, 0, promise, null);
        }
      });
      authMethod(defaultAuth, innerPromise);
    } else {
      authChain(auths, 0, promise, null);
    }
    return promise.future();
  }

  private void authChain(List<String> auths, int i, Handler<AsyncResult<SMTPConnection>> handler, Throwable e) {
    if (i < auths.size()) {
      Promise<SMTPConnection> promise = Promise.promise();
      promise.future().onComplete(result -> {
        if (result.succeeded()) {
          handler.handle(Future.succeededFuture(connection));
        } else {
          log.warn(result.cause());
          authChain(auths, i + 1, handler, result.cause());
        }
      });
      authMethod(auths.get(i), promise);
    } else {
      handler.handle(Future.failedFuture(e));
    }
  }

  private void authMethod(String auth, Handler<AsyncResult<SMTPConnection>> handler) {
    AuthOperation authMethod;
    try {
      authMethod = authOperationFactory.createAuth(config, auth);
    } catch (IllegalArgumentException | SecurityException ex) {
      log.warn("authentication factory threw exception", ex);
      handler.handle(Future.failedFuture(ex));
      return;
    }
    authCmdStep(authMethod, null, handler);
  }

  private void authCmdStep(AuthOperation authMethod, String message, Handler<AsyncResult<SMTPConnection>> handler) {
    Promise<String> promise = Promise.promise();
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
      handler.handle(Future.failedFuture(e));
      return;
    }
    connection.write(nextLine, blank, promise);
    promise.future().onComplete(result -> {
      if (result.succeeded()) {
        String message2 = result.result();
        SMTPResponse response = new SMTPResponse(message2);
        if (response.isStatusOk()) {
          if (response.isStatusContinue()) {
            log.debug("Auth Continue with response: " + message2);
            authCmdStep(authMethod, message2, handler);
          } else {
            authOperationFactory.setAuthMethod(authMethod.getName());
            handler.handle(Future.succeededFuture(connection));
          }
        } else {
          handler.handle(Future.failedFuture(response.toException("AUTH " + authMethod.getName() + " failed", connection.getCapa().isCapaEnhancedStatusCodes())));
        }
      } else {
        handler.handle(Future.failedFuture(result.cause()));
      }
    });
  }

}

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

import io.vertx.core.Future;
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
 * @author <a href="mailto: aoingl@gmail.com">Lin Gao</a>
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

  Future<String> start() {
    Promise<String> promise = Promise.promise();
    try {
      List<String> auths = intersectAllowedMethods();
      final boolean foundAllowedMethods = !auths.isEmpty();
      if (config.getLogin() != LoginOption.DISABLED && config.getUsername() != null && config.getPassword() != null
        && foundAllowedMethods) {
        String defaultAuth = this.authOperationFactory.getAuthMethod();
        if (defaultAuth != null) {
          if (log.isDebugEnabled()) {
            log.debug("Using default auth method: " + defaultAuth);
          }
          final Future<String> authFut = authMethod(defaultAuth);
          return authFut.compose(s -> authFut, e -> authCmdChains(auths, 0));
        } else {
          return authCmdChains(auths, 0);
        }
      } else {
        if (config.getLogin() == LoginOption.REQUIRED) {
          if (!foundAllowedMethods) {
            promise.fail("login is required, but no allowed AUTH methods available. You may need to do STARTTLS");
          } else {
            promise.fail("login is required, but no credentials supplied");
          }
        } else {
          promise.complete("Done");
        }
      }
    } catch (Exception e) {
      promise.fail(e);
    }
    return promise.future();
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

  private Future<String> authCmdChains(List<String> auths, final int i) {
    Promise<String> promise = Promise.promise();
    Future<String> future = promise.future();
    try {
      if (i == auths.size()) {
        promise.fail("Failed to authenticate");
      } else {
        String auth = auths.get(i);
        Future<String> authFuture = authMethod(auth);
        return authFuture.compose(s -> authFuture, e -> {
          log.debug("Failed to auth with method: " + auth, e);
          if (i < auths.size() - 1) {
            log.debug("Try next auth: " + auths.get(i + 1));
            return authCmdChains(auths, i + 1);
          } else {
            return Future.failedFuture(e);
          }
        });
      }
    } catch (Exception e) {
      promise.fail(e);
    }
    return future;
  }

  private Future<String> authMethod(String auth) {
    Promise<String> promise = Promise.promise();
    AuthOperation authMethod;
    try {
      authMethod = authOperationFactory.createAuth(config.getUsername(), config.getPassword(), auth);
      return authCmdSteps(authMethod, null);
    } catch (IllegalArgumentException | SecurityException ex) {
      log.warn("authentication factory threw exception", ex);
      promise.fail(ex);
    }
    return promise.future();
  }

  private Future<String> authCmdSteps(AuthOperation authMethod, String message) {
    Promise<String> promise = Promise.promise();
    try {
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
      return connection.writeWithReply(nextLine, blank).flatMap(msg -> {
        if (StatusCode.isStatusOk(msg)) {
          if (StatusCode.isStatusContinue(msg)) {
            return authCmdSteps(authMethod, msg);
          } else {
            authOperationFactory.setAuthMethod(authMethod.getName());
            return Future.succeededFuture(msg);
          }
        } else {
          return Future.failedFuture("AUTH " + authMethod.getName() + " failed " + msg);
        }
      });
    } catch (Exception e) {
      promise.fail(e);
    }
    return promise.future();
  }

}

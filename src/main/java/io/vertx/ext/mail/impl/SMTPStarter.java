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

import io.vertx.core.*;
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.impl.sasl.AuthOperationFactory;

import java.util.function.Supplier;

/**
 * this encapsulates open connection, initial dialogue and authentication
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
class SMTPStarter {

  private static final Expectation<UsernamePasswordCredentials> CREDENTIALS_EXPECTATION = new Expectation<UsernamePasswordCredentials>() {
    @Override
    public boolean test(UsernamePasswordCredentials credentials) {
      return credentials != null && credentials.getUsername() != null && credentials.getPassword() != null;
    }

    @Override
    public Throwable describe(UsernamePasswordCredentials credentials) {
      if (credentials == null) {
        return new VertxException("Credentials must not be null", true);
      }
      return new VertxException("Username or password is null", true);
    }
  };


  private final SMTPConnection connection;
  private final String hostname;
  private final MailConfig config;
  private final AuthOperationFactory authOperationFactory;
  private final Supplier<Future<UsernamePasswordCredentials>> credentialsSupplier;
  private final Handler<AsyncResult<SMTPConnection>> handler;

  SMTPStarter(SMTPConnection connection, MailConfig config, String hostname, AuthOperationFactory authOperationFactory, Supplier<Future<UsernamePasswordCredentials>> credentialsSupplier, Handler<AsyncResult<SMTPConnection>> handler) {
    this.connection = connection;
    this.hostname = hostname;
    this.config = config;
    this.authOperationFactory = authOperationFactory;
    this.credentialsSupplier = credentialsSupplier;
    this.handler = handler;
  }

  void serverGreeting(String message) {
    Future<UsernamePasswordCredentials> creds;
    if (credentialsSupplier != null) {
      creds = credentialsSupplier.get().expecting(CREDENTIALS_EXPECTATION);
    } else {
      creds = Future.succeededFuture();
    }
    creds.onComplete(credentials -> {
      new SMTPInitialDialogue(connection, config, hostname, v -> doAuthentication(credentials), this::handleError).start(message);
    }, this::handleError);
  }

  private void doAuthentication(UsernamePasswordCredentials credentials) {
    new SMTPAuthentication(connection, config, this.authOperationFactory, v -> handler.handle(Future.succeededFuture(connection)), this::handleError, credentials).start();
  }

  private void handleError(Throwable throwable) {
    handler.handle(Future.failedFuture(throwable));
  }

}

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
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.impl.sasl.AuthOperationFactory;

/**
 * this encapsulates open connection, initial dialogue and authentication
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
class SMTPStarter {

  private static final Logger log = LoggerFactory.getLogger(SMTPStarter.class);

  private final SMTPConnection connection;
  private final String hostname;
  private final MailConfig config;
  private final AuthOperationFactory authOperationFactory;
  private final Handler<AsyncResult<Void>> handler;

  SMTPStarter(SMTPConnection connection, MailConfig config, String hostname, AuthOperationFactory authOperationFactory, Handler<AsyncResult<Void>> handler) {
    this.connection = connection;
    this.hostname = hostname;
    this.config = config;
    this.authOperationFactory = authOperationFactory;
    this.handler = handler;
  }

  void start() {
    log.debug("connection.openConnection");
    connection.openConnection(this::serverGreeting, this::handleError);
  }

  private void serverGreeting(String message) {
    log.debug("SMTPInitialDialogue");
    new SMTPInitialDialogue(connection, config, hostname, v -> doAuthentication(), this::handleError).start(message);
  }

  private void doAuthentication() {
    log.debug("SMTPAuthentication");
    new SMTPAuthentication(connection, config, this.authOperationFactory, v -> handler.handle(Future.succeededFuture()), this::handleError).start();
  }

  private void handleError(Throwable throwable) {
    log.debug("handleError:" + throwable);
    if (connection != null) {
      connection.setBroken();
    }
    handler.handle(Future.failedFuture(throwable));
  }

}

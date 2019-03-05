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
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;

/**
 * handle connection quit
 * <p>
 * There is not much point in encapsulating this but its useful for the connection pool
 * <p>
 * this operation does not throw any error, it just closes the connection in the end
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
class SMTPQuit {

  private static final Logger log = LoggerFactory.getLogger(SMTPQuit.class);

  private final SMTPConnection connection;

  private final Handler<Void> resultHandler;

  SMTPQuit(SMTPConnection connection, Handler<Void> resultHandler) {
    this.connection = connection;
    this.resultHandler = resultHandler;
  }

  void start() {
    connection.setErrorHandler(th -> {
      log.debug("QUIT failed, ignoring exception", th);
      resultHandler.handle(null);
    });
    connection.write("QUIT", message -> {
      log.debug("QUIT result: " + message);
      if (!StatusCode.isStatusOk(message)) {
        log.warn("quit failed: " + message);
      }
      resultHandler.handle(null);
    });
  }

}

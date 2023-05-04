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
import io.vertx.core.Handler;
import io.vertx.core.Future;

/**
 * Handle the reset command, this is mostly used to check if the connection is
 * still active
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
class SMTPReset {

  private final SMTPConnection connection;
  private final Handler<AsyncResult<SMTPConnection>> handler;

  SMTPReset(SMTPConnection connection, Handler<AsyncResult<SMTPConnection>> finishedHandler) {
    this.connection = connection;
    this.handler = finishedHandler;
  }

  void start() {
    connection.setExceptionHandler(th -> handler.handle(Future.failedFuture(th)));
    connection.write("RSET", ar -> {
      if (ar.failed()) {
        handler.handle(Future.failedFuture(ar.cause()));
        return;
      }
      String message = ar.result();
      SMTPResponse response = new SMTPResponse(message);
      if (!response.isStatusOk()) {
        handler.handle(Future.failedFuture(response.toException("reset command failed", connection.getCapa().isCapaEnhancedStatusCodes())));
      } else {
        handler.handle(Future.succeededFuture(connection));
      }
    });
  }

}

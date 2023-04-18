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
import io.vertx.core.impl.ContextInternal;

/**
 * Handle the reset command, this is mostly used to check if the connection is
 * still active
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
class SMTPReset {

  private final SMTPConnection connection;

  SMTPReset(SMTPConnection connection) {
    this.connection = connection;
  }

  Future<SMTPConnection> start(ContextInternal contextInternal) {
    Promise<SMTPConnection> promise = contextInternal.promise();
    connection.setErrorHandler(promise::fail);
    connection.write("RSET", message -> {
      SMTPResponse response = new SMTPResponse(message);
      if (!response.isStatusOk()) {
        promise.fail(response.toException("reset command failed", connection.getCapa().isCapaEnhancedStatusCodes()));
      } else {
        promise.complete(connection);
      }
    });
    return promise.future();
  }

}

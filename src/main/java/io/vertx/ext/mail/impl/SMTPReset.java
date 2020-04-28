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

/**
 * Handle the reset command, this is mostly used to check if the connection is
 * still active
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 * @author <a href="mailto: aoingl@gmail.com">Lin Gao</a>
 */
class SMTPReset {

  private final SMTPConnection connection;

  SMTPReset(SMTPConnection connection) {
    this.connection = connection;
  }

  Future<String> start() {
    return connection.writeWithReply("RSET").flatMap(message -> {
      if (!StatusCode.isStatusOk(message)) {
        return Future.failedFuture("reset command failed: " + message);
      } else {
        return Future.succeededFuture(message);
      }
    });
  }
}

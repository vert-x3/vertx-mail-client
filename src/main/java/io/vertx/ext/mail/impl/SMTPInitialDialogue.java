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
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.StartTLSOptions;

/**
 * Handle welcome line, EHLO/HELO, capabilities
 * and STARTTLS if necessary
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 * @author <a href="mailto: aoingl@gmail.com">Lin Gao</a>
 */
class SMTPInitialDialogue {

  private static final Logger log = LoggerFactory.getLogger(SMTPInitialDialogue.class);

  private final SMTPConnection connection;
  private final MailConfig config;
  private final String hostname;

  SMTPInitialDialogue(SMTPConnection connection, MailConfig config, String hostname) {
    this.connection = connection;
    this.config = config;
    this.hostname = hostname;
  }

  Future<String> start(final String message) {
    Promise<String> promise = Promise.promise();
    try {
      if (StatusCode.isStatusOk(message)) {
        if (!config.isDisableEsmtp()) {
          return ehloCmd();
        } else {
          return heloCmd();
        }
      } else {
        promise.fail("got error response " + message);
      }
    } catch (Exception e) {
      promise.fail(e);
    }
    return promise.future();
  }

  private Future<String> ehloCmd() {
    return connection.writeWithReply("EHLO " + hostname).flatMap(message -> {
      try {
        if (StatusCode.isStatusOk(message)) {
          connection.parseCapabilities(message);
          if (connection.getCapa().isStartTLS()
            && !connection.isSsl()
            && (config.getStarttls() == StartTLSOptions.REQUIRED || config.getStarttls() == StartTLSOptions.OPTIONAL)) {
            // do not start TLS if we are connected with SSL or are already in TLS
            return startTLSCmd();
          } else {
            if (connection.isSsl() || config.getStarttls() != StartTLSOptions.REQUIRED) {
              return Future.succeededFuture(message);
            } else {
              log.warn("STARTTLS required but not supported by server");
              return Future.failedFuture("STARTTLS required but not supported by server");
            }
          }
        } else {
          // if EHLO fails, assume we have to do HELO
          // if the command is not supported, the response is probably
          // a 5xx error code and we should be able to continue, if not
          // the options disableEsmtp has to be set
          return heloCmd();
        }
      } catch (Exception e) {
        return Future.failedFuture(e);
      }
    });
  }

  private Future<String> heloCmd() {
    return connection.writeWithReply("HELO " + hostname).flatMap(message -> {
      try {
        if (StatusCode.isStatusOk(message)) {
          return Future.succeededFuture(message);
        } else {
          return Future.failedFuture("HELO failed with " + message);
        }
      } catch (Exception e) {
        return Future.failedFuture(e);
      }
    });
  }

  /**
   * run STARTTLS command and redo EHLO
   */
  private Future<String> startTLSCmd() {
    return connection.writeWithReply("STARTTLS")
      .flatMap(message -> connection.upgradeToSsl())
      .flatMap(ssl -> {
      log.trace("tls started");
      return ehloCmd();
    });
  }

}

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
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.StartTLSOptions;

/**
 * Handle welcome line, EHLO/HELO, capabilities
 * and STARTTLS if necessary
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
class SMTPInitialDialogue {

  private static final Logger log = LoggerFactory.getLogger(SMTPInitialDialogue.class);

  private final SMTPConnection connection;

  private final Handler<Throwable> errorHandler;
  private final Handler<Void> finishedHandler;

  private final MailConfig config;

  private final String hostname;

  public SMTPInitialDialogue(SMTPConnection connection, MailConfig config, String hostname, Handler<Void> finishedHandler,
                             Handler<Throwable> errorHandler) {
    this.connection = connection;
    this.config = config;
    this.hostname = hostname;
    this.finishedHandler = finishedHandler;
    this.errorHandler = errorHandler;
    this.connection.setErrorHandler(errorHandler);
  }

  public void start(final String message) {
    if (StatusCode.isStatusOk(message)) {
      if (!config.isDisableEsmtp()) {
        ehloCmd();
      } else {
        heloCmd();
      }
    } else {
      handleError("got error response " + message);
    }
  }

  private void ehloCmd() {
    connection
      .write(
        "EHLO " + hostname,
        message -> {
          if (StatusCode.isStatusOk(message)) {
            connection.parseCapabilities(message);
            if (connection.getCapa().isStartTLS()
              && !connection.isSsl()
              && (config.getStarttls() == StartTLSOptions.REQUIRED || config.getStarttls() == StartTLSOptions.OPTIONAL)) {
              // do not start TLS if we are connected with SSL or are already in TLS
              startTLSCmd();
            } else {
              finished();
            }
          } else {
            // if EHLO fails, assume we have to do HELO
            // if the command is not supported, the response is probably
            // a 5xx error code and we should be able to continue, if not
            // the options disableEsmtp has to be set
            heloCmd();
          }
        });
  }

  private void heloCmd() {
    connection.write("HELO " + hostname, message -> {
      if (StatusCode.isStatusOk(message)) {
        finished();
      } else {
        handleError("HELO failed with " + message);
      }
    });
  }

  private void handleError(String message) {
    errorHandler.handle(new NoStackTraceThrowable(message));
  }

  /**
   * run STARTTLS command and redo EHLO
   */
  private void startTLSCmd() {
    connection.write("STARTTLS", message -> {
      connection.upgradeToSsl(ar -> {
        if (ar.succeeded()) {
          log.trace("tls started");
          // capabilities may have changed, e.g.
          // if a service only announces PLAIN/LOGIN
          // on secure channel (e.g. googlemail)
          ehloCmd();
        } else {
          errorHandler.handle(ar.cause());
        }
      });
    });
  }

  private void finished() {
    if (connection.isSsl() || config.getStarttls() != StartTLSOptions.REQUIRED) {
      finishedHandler.handle(null);
    } else {
      log.warn("STARTTLS required but not supported by server");
      handleError("STARTTLS required but not supported by server");
    }
  }

}

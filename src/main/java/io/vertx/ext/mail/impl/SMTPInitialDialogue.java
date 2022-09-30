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
  private final MailConfig config;
  private final String hostname;

  public SMTPInitialDialogue(SMTPConnection connection, MailConfig config, String hostname) {
    this.connection = connection;
    this.config = config;
    this.hostname = hostname;
  }

  public Future<SMTPConnection> start(final String message) {
    Promise<SMTPConnection> promise = Promise.promise();
    try {
      SMTPResponse response = new SMTPResponse(message);
      if (response.isStatusOk()) {
        if (!config.isDisableEsmtp()) {
          ehloCmd().onComplete(promise);
        } else {
          heloCmd().onComplete(promise);
        }
      } else {
        promise.fail(response.toException("got error response"));
      }
    } catch (Exception e) {
      promise.fail(e);
    }
    return promise.future();
  }

  private Future<SMTPConnection> ehloCmd() {
    Promise<String> promise = Promise.promise();
    connection.write("EHLO " + hostname, promise);
    return promise.future().flatMap(message -> {
      try {
        SMTPResponse response = new SMTPResponse(message);
        if (response.isStatusOk()) {
          connection.parseCapabilities(message);
          if (connection.getCapa().isStartTLS()
            && !connection.isSsl()
            && (config.getStarttls() == StartTLSOptions.REQUIRED || config.getStarttls() == StartTLSOptions.OPTIONAL)) {
            // do not start TLS if we are connected with SSL or are already in TLS
            return startTLSCmd();
          } else {
            return finished();
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

  private Future<SMTPConnection> heloCmd() {
    Promise<String> promise = Promise.promise();
    connection.write("HELO " + hostname, promise);
    return promise.future().flatMap(message -> {
      SMTPResponse response = new SMTPResponse(message);
      if (response.isStatusOk()) {
        return finished();
      } else {
        return Future.failedFuture(response.toException("HELO failed."));
      }
    });
  }

  /**
   * run STARTTLS command and redo EHLO
   */
  private Future<SMTPConnection> startTLSCmd() {
    Promise<String> promise = Promise.promise();
    connection.write("STARTTLS", promise);
    return promise.future().flatMap(message -> {
      Promise<Void> startTLS = Promise.promise();
      connection.upgradeToSsl(startTLS);
      return startTLS.future().flatMap(ar -> {
        log.trace("tls started");
        // capabilities may have changed, e.g.
        // if a service only announces PLAIN/LOGIN
        // on secure channel (e.g. googlemail)
        return ehloCmd();
      });
    });
  }

  private Future<SMTPConnection> finished() {
    if (connection.isSsl() || config.getStarttls() != StartTLSOptions.REQUIRED) {
      return Future.succeededFuture(connection);
    } else {
      log.warn("STARTTLS required but not supported by server");
      return Future.failedFuture(new NoStackTraceThrowable("STARTTLS required but not supported by server"));
    }
  }

}

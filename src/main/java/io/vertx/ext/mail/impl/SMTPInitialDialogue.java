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

  private final Promise<Void> promise;

  public SMTPInitialDialogue(ContextInternal context, SMTPConnection connection, MailConfig config, String hostname) {
    this.connection = connection;
    this.config = config;
    this.hostname = hostname;
    this.promise = context.promise();
    this.connection.setErrorHandler(promise::fail);
  }

  public Future<Void> start(final String serverGreeting) {
    SMTPResponse response = new SMTPResponse(serverGreeting);
    if (response.isStatusOk()) {
      if (config.isDisableEsmtp()) {
        helo();
      } else {
        ehlo();
      }
    } else {
      promise.fail(response.toException("got error response"));
    }
    return promise.future();
  }

  private void ehlo() {
    connection.write("EHLO " + hostname).onSuccess(response -> {
      if (response.isStatusOk()) {
        connection.parseCapabilities(response.getValue());
        if (connection.getCapa().isStartTLS()
          && !connection.isSsl()
          && (config.getStarttls() == StartTLSOptions.REQUIRED || config.getStarttls() == StartTLSOptions.OPTIONAL)) {
          // do not start TLS if we are connected with SSL or are already in TLS
          startTLS();
        } else {
          finished();
        }
      } else {
        // if EHLO fails, assume we have to do HELO
        // if the command is not supported, the response is probably
        // a 5xx error code and we should be able to continue, if not
        // the options disableEsmtp has to be set
        helo();
      }
    });
  }

  private void helo() {
    connection.write("HELO " + hostname).onSuccess(response -> {
      if (response.isStatusOk()) {
        finished();
      } else {
        promise.fail(response.toException("HELO failed."));
      }
    });
  }

  /**
   * run STARTTLS command and redo EHLO
   */
  private void startTLS() {
    connection.write("STARTTLS")
      .flatMap(ignored -> connection.upgradeToSsl())
      .onComplete(ar -> {
        if (ar.succeeded()) {
          log.trace("tls started");
          // capabilities may have changed, e.g.
          // if a service only announces PLAIN/LOGIN
          // on secure channel (e.g. googlemail)
          ehlo();
        } else {
          promise.fail(ar.cause());
        }
      });
  }

  private void finished() {
    if (connection.isSsl() || config.getStarttls() != StartTLSOptions.REQUIRED) {
      promise.complete();
    } else {
      log.warn("STARTTLS required but not supported by server");
      promise.fail("STARTTLS required but not supported by server");
    }
  }

}

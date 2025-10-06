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

package io.vertx.ext.mail;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.mail.impl.MailClientBuilderImpl;

import java.util.function.Supplier;

/**
 * SMTP mail client for Vert.x
 * <p>
 * A simple asynchronous API for sending mails from Vert.x applications
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
@VertxGen
public interface MailClient {

  /**
   * The name of the default pool
   */
  String DEFAULT_POOL_NAME = "DEFAULT_POOL";

  /**
   * Provide a builder for {@link MailClient}.
   * <p>
   * It can be used to configure advanced settings like changing credentials with {@link MailClientBuilder#withCredentialsSupplier(Supplier)}.
   */
  static MailClientBuilder builder(Vertx vertx) {
    return new MailClientBuilderImpl(vertx);
  }

  /**
   * Create a non shared instance of the mail client.
   *
   * @param vertx  the Vertx instance the operation will be run in
   * @param config MailConfig configuration to be used for sending mails
   * @return MailClient instance that can then be used to send multiple mails
   */
  static MailClient create(Vertx vertx, MailConfig config) {
    return builder(vertx).with(config).build();
  }

  /**
   * Create a Mail client which shares its connection pool with any other Mail clients created with the same
   * pool name
   *
   * @param vertx  the Vert.x instance
   * @param config  the configuration
   * @param poolName  the pool name
   * @return the client
   */
  static MailClient createShared(Vertx vertx, MailConfig config, String poolName) {
    return builder(vertx).with(config).shared(poolName).build();
  }

  /**
   * Like {@link #createShared(io.vertx.core.Vertx, MailConfig, String)} but with the default pool name
   * @param vertx  the Vert.x instance
   * @param config  the configuration
   * @return the client
   */
  static MailClient createShared(Vertx vertx, MailConfig config) {
    return builder(vertx).with(config).shared(DEFAULT_POOL_NAME).build();
  }

  /**
   * send a single mail via MailClient
   *
   * @param email         MailMessage object containing the mail text, from/to, attachments etc
   * @return a future notified when the operation is finished or it fails
   *                      (may be null to ignore the result)
   */
  Future<MailResult> sendMail(MailMessage email);

  /**
   * Close the MailClient
   */
  Future<Void> close();

}

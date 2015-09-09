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
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.MailResult;
import io.vertx.ext.mail.MailService;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class MailServiceImpl implements MailService {

  private final MailClient client;

  public MailServiceImpl(MailClient client) {
    this.client = client;
  }

  @Override
  public MailService sendMail(MailMessage email, Handler<AsyncResult<MailResult>> resultHandler) {
    client.sendMail(email, resultHandler);
    return this;
  }

  @Override
  public void close() {
    client.close();
  }
}

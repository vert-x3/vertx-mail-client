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
import io.vertx.core.Vertx;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.MailResult;

/**
 * MailClient providing a few internal getters for unit tests
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
public class TestMailClient implements MailClient {

  private final MailClientImpl mailClient;

  /**
   * @param vertx
   * @param config
   */
  public TestMailClient(Vertx vertx, MailConfig config) {
    mailClient = new MailClientImpl(vertx, config, "foo");
  }

  /* (non-Javadoc)
   * @see io.vertx.ext.mail.MailClient#sendMail(io.vertx.ext.mail.MailMessage, io.vertx.core.Handler)
   */
  @Override
  public MailClient sendMail(MailMessage email, Handler<AsyncResult<MailResult>> resultHandler) {
    return mailClient.sendMail(email, resultHandler);
  }

  /* (non-Javadoc)
   * @see io.vertx.ext.mail.MailClient#close()
   */
  @Override
  public void close(Handler<AsyncResult<Void>> closedHandler) {
    mailClient.close(closedHandler);
  }

  /**
   * get the connection pool to be able to assert things about the connections
   * @return SMTPConnectionPool
   */
  public SMTPConnectionPool getConnectionPool() {
    return mailClient.getConnectionPool();
  }

  /**
   * get the connection count of the pool
   * @return SMTPConnectionPool
   */
  public int connCount() {
    return mailClient.getConnectionPool().connCount();
  }
}

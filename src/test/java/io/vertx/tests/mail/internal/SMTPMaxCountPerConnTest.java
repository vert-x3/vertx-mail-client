/*
 *  Copyright (c) 2011-2022 The original author or authors
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

package io.vertx.tests.mail.internal;

import io.vertx.core.internal.ContextInternal;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.impl.SMTPConnectionPool;
import io.vertx.ext.mail.impl.SMTPSendMail;
import io.vertx.tests.mail.client.SMTPTestWiser;
import io.vertx.ext.mail.mailencoder.MailEncoder;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests on the emails count limitation per connection.
 *
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
@RunWith(VertxUnitRunner.class)
public class SMTPMaxCountPerConnTest extends SMTPTestWiser {

  private static final int MAX_MAILS_COUNTS_PER_CONN = 10;
  // we use maxPoolSize = 1 to have only 1 connection for the test, connection count in the pool will be 0 after 10 emails.
  private final MailConfig config = configNoSSL().setMaxPoolSize(1).setKeepAlive(true).setMaxMailsPerConnection(MAX_MAILS_COUNTS_PER_CONN);

  @Test
  public final void testConnectionPool(TestContext testContext) {
    Async async = testContext.async();
    final SMTPConnectionPool pool = new SMTPConnectionPool(vertx, config);
    MailMessage mail = exampleMessage();
    sendMail(testContext, pool, mail, async, 0);
  }

  private void sendMail(TestContext testContext, SMTPConnectionPool pool, MailMessage mail, Async async, int idx) {
    ContextInternal context = (ContextInternal) vertx.getOrCreateContext();
    final MailEncoder encoder = new MailEncoder(mail, "host", config);
    pool.getConnection("localhost").onComplete(testContext.asyncAssertSuccess(conn -> {
      new SMTPSendMail(context, conn, mail, config, encoder.encodeMail(), encoder.getMessageID()).startMailTransaction().onComplete(testContext.asyncAssertSuccess(mr -> {
        conn.returnToPool().onComplete(testContext.asyncAssertSuccess(c -> {
          if (idx < MAX_MAILS_COUNTS_PER_CONN - 1) {
            Assert.assertEquals(1, pool.connCount());
            sendMail(testContext, pool, mail, async, idx + 1);
          } else {
            Assert.assertEquals(0, pool.connCount());
            pool.getConnection("localhost").onComplete(testContext.asyncAssertSuccess(conn2 -> {
              MailEncoder encoder2 = new MailEncoder(mail, "host", config);
              new SMTPSendMail(context, conn2, mail, config, encoder2.encodeMail(), encoder2.getMessageID()).startMailTransaction().onComplete(testContext.asyncAssertSuccess(mrr -> {
                conn2.returnToPool().onComplete(testContext.asyncAssertSuccess(vv -> {
                  // connect will be created again.
                  Assert.assertEquals(1, pool.connCount());
                  pool.doClose().onComplete(vvv -> async.complete());
                }));
              }));
            }));
          }
        }));
      }));
    }));
  }


}

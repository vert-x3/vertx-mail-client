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

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.subethamail.wiser.Wiser;

/*
 first implementation of a SMTP client
 */

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
@RunWith(VertxUnitRunner.class)
public class MailClosedConnectionTest {

  private static final Logger log = LoggerFactory.getLogger(MailClosedConnectionTest.class);

  Vertx vertx = Vertx.vertx();

  @Test
  public void mailTest(TestContext context) {
    log.info("starting");

    Async async = context.async();

    MailClient mailClient = MailClient.createNonShared(vertx, mailConfig());

    MailMessage email = new MailMessage()
      .setFrom("user@example.com")
      .setTo("user@example.com")
      .setSubject("Test email")
      .setText("this is a message");

    mailClient.sendMail(email, result -> {
      log.info("mail finished");
      if (result.succeeded()) {
        log.info(result.result().toString());
        async.complete();
      } else {
        log.warn("got exception", result.cause());
        context.fail(result.cause());
      }
    });
  }

  @Test
  public void mailTest2(TestContext context) {
    log.info("starting");

    Async async = context.async();

    MailClient mailClient = MailClient.createNonShared(vertx, mailConfig());

    MailMessage email = new MailMessage()
      .setFrom("user@example.com")
      .setTo("user@example.com")
      .setSubject("Test email")
      .setText("this is a message");

    mailClient.sendMail(email, result -> {
      log.info("mail finished");
      if (result.succeeded()) {
        log.info(result.result().toString());
        async.complete();
      } else {
        log.warn("got exception", result.cause());
        context.fail(result.cause());
      }
    });
  }

  /**
   * @return
   */
  private MailConfig mailConfig() {
    return new MailConfig("localhost", 1587, StartTLSOptions.DISABLED, LoginOption.DISABLED);
  }

  Wiser wiser;

  @Before
  public void startSMTP() {
    wiser = new Wiser();
    wiser.setPort(1587);
    wiser.start();
  }

  @After
  public void stopSMTP() {
    if (wiser != null) {
      wiser.stop();
    }
  }

}

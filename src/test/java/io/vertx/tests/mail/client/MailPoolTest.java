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

package io.vertx.tests.mail.client;

import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
@RunWith(VertxUnitRunner.class)
public class MailPoolTest extends SMTPTestWiser {

  private static final Logger log = LoggerFactory.getLogger(MailPoolTest.class);

  @Test
  public void mailTest(TestContext context) {
    MailClient mailClient = MailClient.create(vertx, configNoSSL());

    MailMessage email = exampleMessage();

    PassOnce pass1 = new PassOnce(context::fail);
    PassOnce pass2 = new PassOnce(context::fail);

    mailClient.sendMail(email).onComplete(context.asyncAssertSuccess(result -> {
      log.info("mail finished");
      pass1.passOnce();
      log.info(result.toString());
      mailClient.sendMail(email).onComplete(context.asyncAssertSuccess(result2 -> {
        log.info("mail finished");
        pass2.passOnce();
        log.info(result2.toString());
        mailClient.close().onComplete(context.asyncAssertSuccess());
      }));
    }));
  }

  @Test
  public void mailConcurrentTest(TestContext context) {
    log.info("starting");

    Async mail1 = context.async();
    Async mail2 = context.async();

    MailClient mailClient = MailClient.create(vertx, configNoSSL());

    MailMessage email = exampleMessage();

    PassOnce pass1 = new PassOnce(context::fail);
    PassOnce pass2 = new PassOnce(context::fail);

    mailClient.sendMail(email).onComplete(context.asyncAssertSuccess(result -> {
      log.info("mail1 finished");
      pass1.passOnce();
      log.info(result.toString());
      if (mail2.isCompleted()) {
        mailClient.close().onComplete(context.asyncAssertSuccess());
      }
      mail1.complete();
    }));

    mailClient.sendMail(email).onComplete(context.asyncAssertSuccess(result -> {
      log.info("mail2 finished");
      pass2.passOnce();
      log.info(result.toString());
      if (mail1.isCompleted()) {
        mailClient.close().onComplete(context.asyncAssertSuccess());
      }
      mail2.complete();
    }));
  }

  @Test
  public void mailConcurrent2Test(TestContext context) {
    Async mail1 = context.async();
    Async mail2 = context.async();

    vertx.getOrCreateContext().runOnContext(v -> {

      log.info("starting");

      MailClient mailClient = MailClient.create(vertx, configNoSSL());

      MailMessage email = exampleMessage();

      PassOnce pass1 = new PassOnce(context::fail);
      PassOnce pass2 = new PassOnce(context::fail);
      PassOnce pass3 = new PassOnce(context::fail);
      PassOnce pass4 = new PassOnce(context::fail);

      log.info("starting mail 1");
      mailClient.sendMail(email).onComplete(context.asyncAssertSuccess(result -> {
        log.info("mail finished");
        pass1.passOnce();
        log.info(result.toString());
        mailClient.sendMail(email).onComplete(context.asyncAssertSuccess(result2 -> {
          log.info("mail finished");
          pass2.passOnce();
          log.info(result2.toString());
          if (mail2.isCompleted()) {
            mailClient.close().onComplete(context.asyncAssertSuccess());
          }
          mail1.complete();
        }));
      }));

      log.info("starting mail 2");
      mailClient.sendMail(email).onComplete(context.asyncAssertSuccess(result -> {
        log.info("mail finished");
        pass3.passOnce();
        log.info(result.toString());
        mailClient.sendMail(email).onComplete(context.asyncAssertSuccess(result2 -> {
          log.info("mail finished");
          pass4.passOnce();
          log.info(result2.toString());
          if (mail1.isCompleted()) {
            mailClient.close().onComplete(context.asyncAssertSuccess());
          }
          mail2.complete();
        }));
      }));
    });
  }

}

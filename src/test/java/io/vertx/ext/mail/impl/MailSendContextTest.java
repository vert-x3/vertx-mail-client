/*
 *  Copyright (c) 2011-2019 The original author or authors
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

import io.vertx.core.*;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.SMTPTestWiser;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test that context on which the MailClient executes.
 *
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
@RunWith(VertxUnitRunner.class)
public class MailSendContextTest extends SMTPTestWiser {

  private static final Logger log = LoggerFactory.getLogger(MailSendContextTest.class);

  private class VerticleA extends AbstractVerticle {
    private MailClientImpl mailClientA;

    @Override
    public void start() {
      mailClientA = (MailClientImpl)MailClient.createShared(vertx, configLogin());
    }

  }

  @Test
  public void sendMailContextCheck(TestContext testContext) {
    Async async = testContext.async();
    vertx.runOnContext(h -> {
      Context ctx = vertx.getOrCreateContext();
      MailClient mailClient = mailClientLogin();
      mailClient.sendMail(exampleMessage()).onComplete(mr -> {
        testContext.assertTrue(mr.succeeded());
        testContext.assertEquals(ctx, Vertx.currentContext());
        async.complete();
      });
    });
  }

  @Test
  public void sendMailContextCheckWithinCallback(TestContext testContext) {
    Async async = testContext.async();
    vertx.runOnContext(v -> {
      Context ctxOut = vertx.getOrCreateContext();
      MailClient mailClient = mailClientLogin();
      mailClient.sendMail(exampleMessage()).onComplete(mr1 -> {
        testContext.assertTrue(mr1.succeeded());
        Context ctxCallback1 = Vertx.currentContext();
        testContext.assertEquals(ctxOut, ctxCallback1);
        mailClient.sendMail(exampleMessage()).onComplete(mr2 -> {
          testContext.assertTrue(mr2.succeeded());
          Context ctxCallback2 = Vertx.currentContext();
          testContext.assertEquals(ctxCallback1, ctxCallback2);
          async.complete();
        });
      });
    });
  }

  @Test
  public void sendMailDifferentContext(TestContext testContext) {
    Async async = testContext.async();
    VerticleA verticleA = new VerticleA();
    log.debug("Before deploying");
    vertx.deployVerticle(verticleA).onComplete(did -> {
      testContext.assertTrue(did.succeeded());
      log.debug("Deployed VerticleA as: " + did.result());
      Context ctx = vertx.getOrCreateContext();
      assertNotNull(ctx);
      verticleA.mailClientA.sendMail(exampleMessage(), r -> {
        testContext.assertTrue(r.succeeded());
        log.debug("example messages sent successfully!");
        Context callBackContxt = Vertx.currentContext();
        assertNotNull(callBackContxt);
        assertEquals(ctx, callBackContxt);
        verticleA.mailClientA.close();
        async.complete();
      });
    });
  }

  private class VerticleB extends AbstractVerticle {
    private MailClient mailClientB;

    @Override
    public void start(Promise<Void> startPromise) {
      mailClientB = MailClient.createShared(vertx, configLogin());
      log.debug("Create a MailClient B instance.");
      assertNotNull(context);
      mailClientB.sendMail(exampleMessage(), r -> {
        if (r.succeeded()) {
          log.debug("Example message sent by mailClientB.");
          Context ctx = Vertx.currentContext();
          assertNotNull(ctx);
          assertEquals(context, ctx);
          startPromise.complete();
        } else {
          log.warn("Failed to send mail in Verticle B");
          startPromise.fail(r.cause());
        }
      });
    }
  }

  @Test
  public void sendMailSameContext(TestContext testContext) {
    Async async = testContext.async();
    VerticleB verticleB = new VerticleB();
    vertx.deployVerticle(verticleB).onComplete(testContext.asyncAssertSuccess(did -> {
      log.debug("Deployed VerticleB as: " + did + ", and mail got sent already!");
      verticleB.mailClientB.close();
      async.complete();
    }));
  }

}

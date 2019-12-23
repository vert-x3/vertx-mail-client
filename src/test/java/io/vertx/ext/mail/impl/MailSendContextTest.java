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
    public void start() throws Exception {
      mailClientA = (MailClientImpl)MailClient.createShared(vertx, configLogin());
    }

  }

  @Test
  public void sendMailDifferentContext(TestContext testContext) {
    Async async = testContext.async();
    VerticleA verticleA = new VerticleA();
    vertx.deployVerticle(verticleA).onSuccess(did -> {
      log.debug("Deployed VerticleA as: " + did);
      Context ctx = Vertx.currentContext();
      assertNotNull(ctx);
      verticleA.mailClientA.sendMail(exampleMessage(), r -> {
        if (r.succeeded()) {
          log.debug("example messages sent successfully!");
          Context callBackContxt = Vertx.currentContext();
          assertNotNull(callBackContxt);
          assertEquals(ctx, callBackContxt);
          verticleA.mailClientA.close();
          async.complete();
        } else {
          log.warn("Failed to send email in VerticleA", r.cause());
          testContext.fail(r.cause());
        }
      });
    }).onFailure(t -> {
      log.warn("Failed to deploy VerticleA", t);
      testContext.fail(t);
    });

  }

  private class VerticleB extends AbstractVerticle {
    private MailClient mailClientB;

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
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
    vertx.deployVerticle(verticleB).onSuccess(did -> {
      log.debug("Deployed VerticleB as: " + did + ", and mail got sent already!");
      verticleB.mailClientB.close();
      async.complete();
    }).onFailure(t -> {
      log.warn("Failed to deploy VerticleB", t);
      testContext.fail(t);
    });
  }

}

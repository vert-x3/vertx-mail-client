/*
 *  Copyright (c) 2011-2020 The original author or authors
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

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.SMTPTestWiser;
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
    MailClient mailClientA;
    @Override
    public void start(Promise<Void> startPromise) {
      mailClientA = MailClient.create(vertx, configLogin());
      mailClientA.sendMail(exampleMessage()).onComplete(r -> {
        assertTrue(r.succeeded());
        assertEquals(Vertx.currentContext(), context);
        // deploy Verticle B
        VerticleB verticleB = new VerticleB();
        vertx.deployVerticle(verticleB).onComplete(dr -> {
          assertTrue(dr.succeeded());
          assertEquals(Vertx.currentContext(), context);
          assertNotNull(verticleB.mailClientB);
          verticleB.mailClientB.sendMail(exampleMessage()).onComplete(sr -> {
            assertTrue(sr.succeeded());
            assertEquals(Vertx.currentContext(), context);
            startPromise.complete();
          });
        });
      });
    }
    @Override
    public void stop(Promise<Void> stopPromise) throws Exception {
      mailClientA.close().onComplete(stopPromise);
    }
  }

  private class VerticleB extends AbstractVerticle {
    MailClient mailClientB;
    @Override
    public void start(Promise<Void> startPromise) {
      mailClientB = MailClient.create(vertx, configLogin());
      mailClientB.sendMail(exampleMessage()).onComplete(sr -> {
        assertTrue(sr.succeeded());
        assertEquals(Vertx.currentContext(), context);
        startPromise.complete();
      });
    }
    @Override
    public void stop(Promise<Void> stopPromise) throws Exception {
      mailClientB.close().onComplete(stopPromise);
    }
  }

  @Test
  public void sendMailDifferentContext(TestContext testContext) {
    VerticleA verticleA = new VerticleA();
    log.debug("Deploy VerticleA");
    vertx.deployVerticle(verticleA).onComplete(testContext.asyncAssertSuccess(va -> vertx.undeploy(va).onComplete(testContext.asyncAssertSuccess())));
  }

}

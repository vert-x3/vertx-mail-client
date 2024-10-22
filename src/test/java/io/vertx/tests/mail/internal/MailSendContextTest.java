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

package io.vertx.tests.mail.internal;

import io.vertx.core.*;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.ext.mail.MailClient;
import io.vertx.tests.mail.client.SMTPTestWiser;
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

  private class VerticleA extends VerticleBase {
    MailClient mailClientA;
    @Override
    public Future<?> start() throws Exception {
      mailClientA = MailClient.create(vertx, configLogin());
      return mailClientA.sendMail(exampleMessage()).compose(r -> {
        assertEquals(Vertx.currentContext(), context);
        // deploy Verticle B
        VerticleB verticleB = new VerticleB();
        return vertx.deployVerticle(verticleB).compose(dr -> {
          assertEquals(Vertx.currentContext(), context);
          assertNotNull(verticleB.mailClientB);
          return verticleB.mailClientB.sendMail(exampleMessage());
        });
      });
    }
    @Override
    public Future<?> stop() throws Exception {
      return mailClientA.close();
    }
  }

  private class VerticleB extends VerticleBase {
    MailClient mailClientB;

    @Override
    public Future<?> start() throws Exception {
      mailClientB = MailClient.create(vertx, configLogin());
      return mailClientB.sendMail(exampleMessage());
    }
    @Override
    public Future<?> stop() throws Exception {
      return mailClientB.close();
    }
  }

  @Test
  public void sendMailDifferentContext(TestContext testContext) {
    VerticleA verticleA = new VerticleA();
    log.debug("Deploy VerticleA");
    vertx.deployVerticle(verticleA).onComplete(testContext.asyncAssertSuccess(va -> vertx.undeploy(va).onComplete(testContext.asyncAssertSuccess())));
  }

}

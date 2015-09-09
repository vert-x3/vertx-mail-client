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

import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
@RunWith(VertxUnitRunner.class)
public class MailEBTest extends SMTPTestDummy {

  private static final Logger log = LoggerFactory.getLogger(MailEBTest.class);

  @Test
  public void mailTest(TestContext testContext) {
    this.testContext=testContext;
    testSuccess(MailService.createEventBusProxy(vertx, "vertx.mail"));
  }

  @Before
  public void startVerticle(TestContext testContext) {
    Async async = testContext.async();

    JsonObject config = new JsonObject("{\"config\":{\"address\":\"vertx.mail\",\"hostname\":\"localhost\",\"port\":1587}}");
    DeploymentOptions deploymentOptions = new DeploymentOptions(config);
    vertx.deployVerticle("io.vertx.ext.mail.MailServiceVerticle", deploymentOptions ,r -> {
      if(r.succeeded()) {
        log.info(r.result());
        async.complete();
      } else {
        log.info("exception", r.cause());
        testContext.fail(r.cause());
      }
    });
  }

}

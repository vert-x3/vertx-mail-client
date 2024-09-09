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

package io.vertx.tests.mail.internal;

import io.vertx.ext.mail.impl.Capabilities;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests SMTP Server's capabilities from message
 *
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
@RunWith(VertxUnitRunner.class)
public class CapabilitiesTest {

  @Test
  public void testCapaFromMessage(TestContext testContext) {
    String message = "250-localhost\n" +
      "250-8BITMIME\n" +
      "250-STARTTLS\n" +
      "250-PIPELINING\n" +
      "250-AUTH PLAIN\n" +
      "250 Ok";
    Capabilities capa = new Capabilities();
    capa.parseCapabilities(message);
    testContext.assertTrue(capa.isCapaPipelining());
    testContext.assertTrue(capa.isStartTLS());
    testContext.assertEquals(1, capa.getCapaAuth().size());
    testContext.assertTrue(capa.getCapaAuth().iterator().next().equals("PLAIN"));
  }

}

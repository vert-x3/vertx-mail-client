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

package io.vertx.ext.mail.impl.sasl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AuthLoginTest {
  @Test
  public void testAuthLogin() {
    final AuthOperation result = new AuthLogin().init("xxx", "yyy");
    assertNotNull(result);
    assertEquals("LOGIN", result.getName());
  }

  @Test
  public void testGetName() {
    assertEquals("LOGIN", new AuthLogin().init("xxx", "yyy").getName());
  }

  @Test
  public void testNextStep() {
    final AuthOperation auth = new AuthLogin().init("xxx", "yyy");
    assertEquals("", auth.nextStep(null));
    assertEquals("xxx", auth.nextStep("Username"));
    assertEquals("yyy", auth.nextStep("Password"));
    assertEquals(null, auth.nextStep("250 auth ok"));
  }

}

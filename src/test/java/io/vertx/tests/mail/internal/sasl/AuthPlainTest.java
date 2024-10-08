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

package io.vertx.tests.mail.internal.sasl;

import io.vertx.ext.mail.impl.sasl.AuthOperation;
import io.vertx.ext.mail.impl.sasl.AuthPlain;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AuthPlainTest {

  @Test
  public void testAuthPlain() {
    final AuthOperation auth = new AuthPlain("xxx", "yyy");
    assertNotNull(auth);
    assertEquals("PLAIN", auth.getName());
  }

  @Test
  public void testNextStep() {
    final AuthOperation auth = new AuthPlain("xxx", "yyy");
    assertEquals("\0xxx\0yyy", auth.nextStep(null));
    assertEquals(null, auth.nextStep("250 auth successful"));
  }

}

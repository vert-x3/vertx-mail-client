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

public class AuthCramMD5Test {

  @Test
  public void testAuthCramMD5() {
    final AuthCramMD5 auth = new AuthCramMD5("xxx", "yyy");

    assertNotNull(auth);
    assertEquals("CRAM-MD5", auth.getName());
  }

  @Test
  public void testGetName() {
    assertEquals("CRAM-MD5", new AuthCramMD5("xxx", "yyy").getName());
  }

  @Test
  public void testNextStep() {
    final AuthCramMD5 auth = new AuthCramMD5("xxx", "yyy");
    assertEquals("", auth.nextStep(null));
    assertEquals("xxx d23f0dea640c99059a0517ad16e7abfb", auth.nextStep("<12345@example.com>"));
    assertEquals(null, auth.nextStep("250 ok"));
  }

  /**
   * hmac example from Wikipedia
   */
  @Test
  public void testNextStep2() {
    final AuthCramMD5 auth = new AuthCramMD5("user", "key");
    assertEquals("", auth.nextStep(null));
    assertEquals("user 80070713463e7749b90c2dc24911e275", auth.nextStep("The quick brown fox jumps over the lazy dog"));
    assertEquals(null, auth.nextStep("250 ok"));
  }

}

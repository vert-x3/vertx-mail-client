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

public class AuthCramSHA1Test {

  @Test
  public void testAuthCramSHA1() {
    final AuthCramSHA1 auth = new AuthCramSHA1("xxx", "yyy");

    assertNotNull(auth);
    assertEquals("CRAM-SHA1", auth.getName());
  }

  @Test
  public void testGetName() {
    assertEquals("CRAM-SHA1", new AuthCramSHA1("xxx", "yyy").getName());
  }

  @Test
  public void testNextStep() {
    final AuthCramSHA1 auth = new AuthCramSHA1("xxx", "yyy");
    assertEquals("", auth.nextStep(null));
    assertEquals("xxx 76c4faeae224fce51c3d372bb4bfdbdfbc563134", auth.nextStep("<12345@example.com>"));
    assertEquals(null, auth.nextStep("250 ok"));
  }

  /**
   * hmac example from Wikipedia
   */
  @Test
  public void testNextStep2() {
    final AuthCramSHA1 auth = new AuthCramSHA1("user", "key");
    assertEquals("", auth.nextStep(null));
    assertEquals("user de7c9b85b8b78aa6bc8a7a36f70a90701c9db4d9", auth.nextStep("The quick brown fox jumps over the lazy dog"));
    assertEquals(null, auth.nextStep("250 ok"));
  }

}

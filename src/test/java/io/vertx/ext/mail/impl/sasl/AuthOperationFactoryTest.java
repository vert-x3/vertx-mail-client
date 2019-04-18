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

/**
 *
 */
package io.vertx.ext.mail.impl.sasl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
public class AuthOperationFactoryTest {

  /**
   * Test method for
   * {@link io.vertx.ext.mail.impl.sasl.AuthOperationFactory#createAuth(java.lang.String, java.lang.String, java.util.Set)}
   * make sure that the default auth method works and is PLAIN 
   * @throws Exception
   */
  @Test
  public final void testCreateAuth() throws Exception {
    Set<String> allowedAuth = new HashSet<String>();
    allowedAuth.add("PLAIN");
    assertEquals(AuthPlain.class, new AuthOperationFactory(null).createAuth("user", "pw", allowedAuth).getClass());
  }

  @Test
  public final void testAuthNotFound() throws Exception {
    Set<String> allowedAuth = new HashSet<String>();
    allowedAuth.add("ASDF");
    assertNull(new AuthOperationFactory(null).createAuth("user", "pw", allowedAuth));
  }

}

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

import io.vertx.ext.mail.MailConfig;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
public class AuthOperationFactoryTest {

  /**
   * Test method for
   * {@link io.vertx.ext.mail.impl.sasl.AuthOperationFactory#createAuth(java.lang.String, java.lang.String, java.lang.String)}
   * make sure that the default auth method works and is PLAIN
   */
  @Test
  public final void testCreateAuth() {
    assertEquals(AuthPlain.class, new AuthOperationFactory(null).createAuth("user", "pw", "PLAIN").getClass());
  }

  @Test(expected = IllegalArgumentException.class)
  public final void testAuthNotFound() {
    assertNull(new AuthOperationFactory(null).createAuth("user", "pw", "ASDF"));
  }

  @Test
  public final void testCreateXOAUTH2Auth() {
    assertEquals(AuthXOAUTH2.class, new AuthOperationFactory(null).createAuth("user", "token", "XOAUTH2").getClass());
  }

  @Test
  public void testSupportedAuths() {
    //TODO intersection between supported and specified
    AuthOperationFactory authOperationFactory = new AuthOperationFactory(null);
    MailConfig mailConfig = new MailConfig();
    assertThat(authOperationFactory.supportedAuths(mailConfig), contains("XOAUTH2", "DIGEST-MD5", "CRAM-SHA256", "CRAM-SHA1", "CRAM-MD5", "LOGIN", "PLAIN"));

    mailConfig.setAuthMethods("PLAIN LOGIN");
    // pay attention on the order
    assertThat(authOperationFactory.supportedAuths(mailConfig), contains("LOGIN", "PLAIN"));
  }

}

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

package io.vertx.ext.mail.impl;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
public class UtilsTest {

  /**
   * Test method for {@link io.vertx.ext.mail.impl.SMTPInitialDialogue#isEsmtpSupported(java.lang.String)}. check if we
   * can detect ESMTP support with upper/lower case chars and not if the string contains only SMTP
   */
  @Test
  public void testIsEsmtpSupported() {
    assertTrue(Utils.isEsmtpSupported("220 example.com ESMTP server"));
    assertTrue(Utils.isEsmtpSupported("220 example.com Esmtp server"));
    assertTrue(Utils.isEsmtpSupported("220 example.com esmtp server"));
    assertFalse(Utils.isEsmtpSupported("220 example.com SMTP server"));
  }

}

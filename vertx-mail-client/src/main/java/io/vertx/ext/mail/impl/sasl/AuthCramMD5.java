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

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
class AuthCramMD5 extends AuthCram {

  /**
   *
   */
  static final String AUTH_NAME = "CRAM-MD5";

  /**
   * @param username
   * @param password
   */
  public AuthCramMD5(String username, String password) {
    super(username, password, "HmacMD5");
  }

  /*
   * (non-Javadoc)
   * 
   * @see io.vertx.ext.mail.impl.AuthBaseClass#getName()
   */
  @Override
  public String getName() {
    return AUTH_NAME;
  }

}

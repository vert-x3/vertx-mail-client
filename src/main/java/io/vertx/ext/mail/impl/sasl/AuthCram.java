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
abstract class AuthCram extends AuthBaseClass {

  protected boolean firstStep;
  protected boolean finished;
  private final String hmac;

  /**
   * @param username
   * @param password
   */
  protected AuthCram(String username, String password, String hmac) {
    super(username, password);
    firstStep = true;
    finished = false;
    this.hmac = hmac;

  }

  /*
   * (non-Javadoc)
   * 
   * @see io.vertx.ext.mail.impl.AuthBaseClass#nextStep(java.lang.String)
   */
  @Override
  public String nextStep(String data) {
    if (finished) {
      return null;
    }
    if (firstStep) {
      firstStep = false;
      return "";
    } else {
      finished = true;
      String reply = CryptUtils.hmacHex(password, data, hmac);
      return username + " " + reply;
    }
  }

}

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

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
abstract class AuthBaseClass implements AuthOperation {

  private final String name;

  final String username;
  final String password;

  AuthBaseClass(String name, String username, String password) {
    this.name = name;
    this.username = username;
    this.password = password;
  }

  @Override
  public abstract String nextStep(String data);

  @Override
  public String getName() {
    return name;
  }
}

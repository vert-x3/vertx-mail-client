/*
 *  Copyright (c) 2011-2019 The original author or authors
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

package io.vertx.ext.mail;

import io.vertx.codegen.annotations.VertxGen;

/**
 *
 * Message canonicalization for DKIM.
 *
 * @author <a href="mailto: aoingl@gmail.com">Lin Gao</a>
 */
@VertxGen
public enum MessageCanonic {
  SIMPLE, // simple
  RELAXED; // relaxed

  /**
   * Gets the message canonicalization representation.
   *
   * @return the message canonicalization representation
   */
  public String canonic() {
    return this.name().toLowerCase();
  }

}

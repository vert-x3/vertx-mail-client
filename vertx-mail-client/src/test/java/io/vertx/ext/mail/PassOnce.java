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

package io.vertx.ext.mail;

import io.vertx.core.Handler;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Assert that a point in the code is passed only once.
 * <p>
 * this is necessary since there may be bugs where an error handler is called
 * twice (there were some ...)
 * <p>
 * PassOnce pass = new PassOnce(s -> fail(s));
 * ...
 * pass.passOnce(); // will call fail handler if the statement is passed more than once
 * <p>
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */

public class PassOnce {

  private final AtomicBoolean passed = new AtomicBoolean(false);
  private final Handler<String> fail;

  public PassOnce(Handler<String> fail) {
    this.fail = fail;
  }

  public void passOnce() {
    if (passed.getAndSet(true)) {
      fail.handle("should only pass this point once");
    }
  }

}

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
 * run a few additional asserts (synchronous) after an async operation was succeeded
 */
package io.vertx.ext.mail;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
@FunctionalInterface
public interface AdditionalAsserts {
  void doAsserts() throws Exception;
}

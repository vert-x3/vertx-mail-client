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

import io.vertx.codegen.annotations.VertxGen;

/**
 * possible options for a secure connection using TLS
 * <br>
 * either DISABLED, OPTIONAL or REQUIRED
 * <p>
 * DISABLED means STARTTLS will not be used in any case
 * <p>
 * OPTIONAL means STARTTLS will be used if the server supports it and a plain connection will be used otherwise;
 * please note that this option is not as secure as it seems since a MITM attacker can remove the STARTTLS line
 * from the capabilities reply.
 * <p>
 * REQUIRED means that STARTTLS will be used if the server supports it and the send operation will fail otherwise
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
@VertxGen
public enum StartTLSOptions {
  DISABLED,
  OPTIONAL,
  REQUIRED
}

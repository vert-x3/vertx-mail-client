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

/**
 * possible options for a login into a SMTP server
 * <br>
 * either DISABLED, OPTIONAL, REQUIRED or XOAUTH2
 * <p>
 * DISABLED means no login will be attempted
 * <p>
 * NONE means a login will be attempted if the server supports in and login credentials are set
 * <p>
 * REQUIRED means that a login will be attempted if the server supports it and the send operation will fail otherwise
 * <p>
 * XOAUTH2 means that a login will be attempted using Google Gmail Oauth2 tokens
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
public enum LoginOption {
  DISABLED,
  NONE,
  REQUIRED,
  XOAUTH2
}

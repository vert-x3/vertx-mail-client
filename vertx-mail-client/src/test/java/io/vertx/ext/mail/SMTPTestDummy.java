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
 * Start/stop a dummy test server for each test
 * <p>
 * the server is currently a rather simple fake server that writes lines to the socket and checks the commands by
 * substring or regexp
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
public class SMTPTestDummy extends SMTPTestBase {

  protected TestSmtpServer smtpServer;

  protected void startSMTP() {
    smtpServer = new TestSmtpServer(vertx, false, null);
  }

  protected void stopSMTP() {
    if (smtpServer!=null) {
      smtpServer.stop();
      smtpServer=null;
    }
  }

}

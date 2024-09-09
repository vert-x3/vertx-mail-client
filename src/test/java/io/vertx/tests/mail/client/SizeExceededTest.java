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

package io.vertx.tests.mail.client;

import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * this test uses a message that exceeds the SIZE limit of the smtp server (uses the mockup server that just plays a
 * file)
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
@RunWith(VertxUnitRunner.class)
public class SizeExceededTest extends SMTPTestDummy {

  private static final Logger log = LoggerFactory.getLogger(SizeExceededTest.class);

  @Test
  public void mailTest(TestContext testContext) {
    this.testContext=testContext;

    // message to exceed SIZE limit (1000000 for our server)
    // 32 Bytes
    StringBuilder sb = new StringBuilder("*******************************\n");
    // multiply by 2**15
    for (int i = 0; i < 15; i++) {
      sb.append(sb);
    }
    String message = sb.toString();

    log.info("message size is " + message.length());

    testException(new MailMessage("user@example.com", "user@example.com", "Subject", message));
  }

}

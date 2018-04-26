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

import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * test sending message with either missing from or to
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
@RunWith(VertxUnitRunner.class)
public class MissingToTest extends SMTPTestDummy {

  @Test
  public void mailMissingToTest(TestContext testContext) {
    this.testContext=testContext;
    testException(new MailMessage().setFrom("user@example.com"));
  }

  @Test
  public void mailMissingFromTest(TestContext testContext) {
    this.testContext=testContext;
    testException(new MailMessage().setTo("user@example.com"));
  }

  @Test
  public void mailBounceAddrOnlyTest(TestContext testContext) {
    this.testContext=testContext;
    testSuccess(new MailMessage().setBounceAddress("from@example.com").setBcc("user@example.com"));
  }

}

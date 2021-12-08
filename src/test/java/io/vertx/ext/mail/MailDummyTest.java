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

import io.vertx.core.buffer.Buffer;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
@RunWith(VertxUnitRunner.class)
public class MailDummyTest extends SMTPTestDummy {

  @Test
  public void mailTest(TestContext testContext) {
    this.testContext=testContext;
    testSuccess();
  }

  @Test
  public void mailHtml(TestContext testContext) throws UnsupportedEncodingException {
    this.testContext=testContext;
    Buffer image = vertx.fileSystem().readFileBlocking("logo-white-big.png");

    MailMessage email = new MailMessage()
      .setFrom("user@example.com")
      .setTo("user@example.com")
      .setBounceAddress("bounce@example.com")
      .setSubject("Test email with HTML")
      .setText("this is a message")
      .setHtml("<a href=\"http://vertx.io\">vertx.io</a>");

    List<MailAttachment> list = new ArrayList<>();

    list.add(MailAttachment.create()
      .setData(Buffer.buffer(image.getBytes()))
      .setName("logo-white-big.png")
      .setContentType("image/png")
      .setDisposition("inline")
      .setDescription("logo of vert.x web page"));

    list.add(MailAttachment.create()
      .setData(Buffer.buffer("this is a text attachment"))
      .setName("file.txt")
      .setContentType("text/plain")
      .setDisposition("attachment")
      .setDescription("some text"));

    email.setAttachment(list);

    testSuccess(email);
  }

  @Test
  public void mailTestNoBody(TestContext testContext) {
    this.testContext=testContext;
    MailMessage email = new MailMessage()
      .setFrom("user@example.com")
      .setTo("user@example.com")
      .setAttachment(MailAttachment.create()
        .setData(TestUtils.asBuffer(0xff, 0xff, 0xff, 0xff, 0xff, 0xff)));

    testSuccess(email);
  }

  /**
   * pass null to ignore result
   * this is not properly async since we do not have a result lambda
   *
   * @throws InterruptedException
   */
  @Test
  public void mailTestNoResult(TestContext testContext) throws InterruptedException {
    final MailClient mailClient = mailClientDefault();
    mailClient.sendMail(exampleMessage(), null);
    Thread.sleep(1000);
    mailClient.close(testContext.asyncAssertSuccess());
  }

}

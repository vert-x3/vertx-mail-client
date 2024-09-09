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

package io.vertx.tests.mail.internal;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.ext.mail.impl.Capabilities;
import io.vertx.ext.mail.impl.MultilineParser;
import io.vertx.ext.mail.impl.SMTPResponse;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tests MultilineParser.
 *
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
@RunWith(VertxUnitRunner.class)
public class MultilineParserTest {

  private static final Logger logger = LoggerFactory.getLogger(MultilineParserTest.class);

  /**
   * Tests one response with one line
   */
  @Test
  public void testOneResponseOneLine(TestContext testContext) {
    Async async = testContext.async(2);
    final String ehloBanner = "220 hello from smtp server";
    final String oneLineResp = "250 2.1.0 OK";
    final AtomicBoolean init = new AtomicBoolean(false);
    Handler<Buffer> dataHandler = b -> {
      if (!init.get()) {
        testContext.assertEquals(ehloBanner, b.toString());
        async.countDown();
      } else {
        String message = b.toString();
        SMTPResponse response = new SMTPResponse(message);
        testContext.assertTrue(response.isStatusOk());
        testContext.assertEquals(oneLineResp, message);
        async.countDown();
      }
    };
    MultilineParser multilineParser = new MultilineParser(dataHandler);
    multilineParser.setExpected(1);
    // simulate the ehlo banner on connection
    multilineParser.handle(Buffer.buffer(ehloBanner + "\r\n"));
    init.set(true);
    multilineParser.handle(Buffer.buffer(oneLineResp + "\r\n"));
  }

  /**
   * Tests one response with multiple lines.
   *
   * Capabilities declared by SMTP server are good example here.
   */
  @Test
  public void testOneResponseMultiLines(TestContext testContext) {
    Async async = testContext.async(2);
    final String ehloBanner = "220 hello from smtp server";
    final AtomicBoolean init = new AtomicBoolean(false);
    final String capaMessage = "250-smtp.gmail.com at your service, [209.132.188.80]\n" +
      "250-AUTH LOGIN PLAIN\n" +
      "250 PIPELINING";
    Handler<Buffer> dataHandler = b -> {
      if (!init.get()) {
        testContext.assertEquals(ehloBanner, b.toString());
        async.countDown();
      } else {
        String message = b.toString();
        SMTPResponse response = new SMTPResponse(message);
        testContext.assertTrue(response.isStatusOk());
        testContext.assertEquals(capaMessage, message);
        Capabilities capa = new Capabilities();
        capa.parseCapabilities(message);
        testContext.assertTrue(capa.isCapaPipelining());
        testContext.assertFalse(capa.isStartTLS());
        testContext.assertEquals(2, capa.getCapaAuth().size());
        testContext.assertTrue(capa.getCapaAuth().contains("LOGIN"));
        testContext.assertTrue(capa.getCapaAuth().contains("PLAIN"));
        async.countDown();
      }
    };
    MultilineParser multilineParser = new MultilineParser(dataHandler);
    multilineParser.setExpected(1);
    // simulate the ehlo banner on connection
    multilineParser.handle(Buffer.buffer(ehloBanner + "\r\n"));
    init.set(true);
    multilineParser.handle(Buffer.buffer(capaMessage + "\r\n"));
  }

  /**
   * Tests one response with multiple lines with crlf ends for each line
   *
   * Capabilities declared by SMTP server are good example here.
   */
  @Test
  public void testOneResponseMultiLinesEndwithCRLF(TestContext testContext) {
    Async async = testContext.async(2);
    final String ehloBanner = "220 hello from smtp server";
    final AtomicBoolean init = new AtomicBoolean(false);
    final String capaMessage = "250-smtp.gmail.com at your service, [209.132.188.80]\r\n" +
      "250-AUTH LOGIN PLAIN\r\n" +
      "250 PIPELINING";
    final String expected = "250-smtp.gmail.com at your service, [209.132.188.80]\n" +
      "250-AUTH LOGIN PLAIN\n" +
      "250 PIPELINING";
    Handler<Buffer> dataHandler = b -> {
      if (!init.get()) {
        testContext.assertEquals(ehloBanner, b.toString());
        async.countDown();
      } else {
        String message = b.toString();
        SMTPResponse response = new SMTPResponse(message);
        testContext.assertTrue(response.isStatusOk());
        testContext.assertEquals(expected, message);
        Capabilities capa = new Capabilities();
        capa.parseCapabilities(message);
        testContext.assertTrue(capa.isCapaPipelining());
        testContext.assertFalse(capa.isStartTLS());
        testContext.assertEquals(2, capa.getCapaAuth().size());
        testContext.assertTrue(capa.getCapaAuth().contains("LOGIN"));
        testContext.assertTrue(capa.getCapaAuth().contains("PLAIN"));
        async.countDown();
      }
    };
    MultilineParser multilineParser = new MultilineParser(dataHandler);
    multilineParser.setExpected(1);
    // simulate the ehlo banner on connection
    multilineParser.handle(Buffer.buffer(ehloBanner + "\r\n"));
    init.set(true);
    multilineParser.handle(Buffer.buffer(capaMessage + "\r\n"));
  }

  /**
   * Tests multi responses, each response has one line
   */
  @Test
  public void testMultiResponseMultiLines(TestContext testContext) {
    Async async = testContext.async(2);
    final String ehloBanner = "220 hello from smtp server";
    final AtomicBoolean init = new AtomicBoolean(false);
    final String multilines = "250 2.1.0 OK1\r\n" +
      "250 2.1.1 OK2\r\n" +
      "250 2.1.2 OK3";
    Handler<Buffer> dataHandler = b -> {
      if (!init.get()) {
        testContext.assertEquals(ehloBanner, b.toString());
        async.countDown();
      } else {
        String message = b.toString();
        SMTPResponse response = new SMTPResponse(message);
        testContext.assertTrue(response.isStatusOk());
        testContext.assertEquals(multilines, message);
        String[] lines = message.split("\r\n");
        for (String l: lines) {
          System.out.println("Line:" + l + ":-");
        }
        testContext.assertEquals(3, lines.length);
        testContext.assertEquals("250 2.1.0 OK1", lines[0]);
        testContext.assertEquals("250 2.1.1 OK2", lines[1]);
        testContext.assertEquals("250 2.1.2 OK3", lines[2]);
        async.countDown();
      }
    };
    MultilineParser multilineParser = new MultilineParser(dataHandler);
    multilineParser.setExpected(1);
    // simulate the ehlo banner on connection
    multilineParser.handle(Buffer.buffer(ehloBanner + "\r\n"));
    init.set(true);
    multilineParser.setExpected(3);
    multilineParser.handle(Buffer.buffer(multilines + "\r\n"));
  }

  /**
   * Tests multi responses, each response has multiple lines
   */
  @Test
  public void testMultiResponseMultiLinesMore(TestContext testContext) {
    Async async = testContext.async(2);
    final String ehloBanner = "220 hello from smtp server";
    final AtomicBoolean init = new AtomicBoolean(false);
    final String multilinesWithLF = "250-2.1.0 OK1\n250 2.1.0.1 OK1.1\r\n" +
      "250 2.1.1 OK2\r\n" +
      "250 2.1.2 OK3";
    Handler<Buffer> dataHandler = b -> {
      if (!init.get()) {
        testContext.assertEquals(ehloBanner, b.toString());
        async.countDown();
      } else {
        String message = b.toString();
        SMTPResponse response = new SMTPResponse(message);
        testContext.assertTrue(response.isStatusOk());
        testContext.assertEquals(multilinesWithLF, message);
        String[] lines = message.split("\r\n");
        testContext.assertEquals(3, lines.length);
        testContext.assertEquals("250-2.1.0 OK1\n250 2.1.0.1 OK1.1", lines[0]);
        testContext.assertEquals("250 2.1.1 OK2", lines[1]);
        testContext.assertEquals("250 2.1.2 OK3", lines[2]);
        async.countDown();
      }
    };
    MultilineParser multilineParser = new MultilineParser(dataHandler);
    multilineParser.setExpected(1);
    // simulate the ehlo banner on connection
    multilineParser.handle(Buffer.buffer(ehloBanner + "\r\n"));
    init.set(true);
    multilineParser.setExpected(3);
    multilineParser.handle(Buffer.buffer(multilinesWithLF + "\r\n"));
  }


  /**
   * Tests multi responses, each response has multiple lines with crlf ended for each line
   */
  @Test
  public void testMultiResponseMultiLinesEndswithCRLFMore(TestContext testContext) {
    Async async = testContext.async(2);
    final String ehloBanner = "220 hello from smtp server";
    final AtomicBoolean init = new AtomicBoolean(false);
    final String multilinesWithLF = "250-2.1.0 OK1\r\n250 2.1.0.1 OK1.1\r\n" +
      "250 2.1.1 OK2\r\n" +
      "250 2.1.2 OK3";
    final String expected = "250-2.1.0 OK1\n250 2.1.0.1 OK1.1\r\n" +
      "250 2.1.1 OK2\r\n" +
      "250 2.1.2 OK3";
    Handler<Buffer> dataHandler = b -> {
      if (!init.get()) {
        testContext.assertEquals(ehloBanner, b.toString());
        async.countDown();
      } else {
        String message = b.toString();
        SMTPResponse response = new SMTPResponse(message);
        testContext.assertTrue(response.isStatusOk());
        testContext.assertEquals(expected, message);
        String[] lines = message.split("\r\n");
        testContext.assertEquals(3, lines.length);
        testContext.assertEquals("250-2.1.0 OK1\n250 2.1.0.1 OK1.1", lines[0]);
        testContext.assertEquals("250 2.1.1 OK2", lines[1]);
        testContext.assertEquals("250 2.1.2 OK3", lines[2]);
        async.countDown();
      }
    };
    MultilineParser multilineParser = new MultilineParser(dataHandler);
    multilineParser.setExpected(1);
    // simulate the ehlo banner on connection
    multilineParser.handle(Buffer.buffer(ehloBanner + "\r\n"));
    init.set(true);
    multilineParser.setExpected(3);
    multilineParser.handle(Buffer.buffer(multilinesWithLF + "\r\n"));
  }

  @Test
  public void testLastLine(TestContext testContext) {
    MultilineParser multilineParser = new MultilineParser(b -> logger.debug(b.toString()));
    testContext.assertTrue(multilineParser.isFinalLine(Buffer.buffer("250 welcome OK")));
    testContext.assertFalse(multilineParser.isFinalLine(Buffer.buffer("250-welcome OK")));

    testContext.assertTrue(multilineParser.isFinalLine(Buffer.buffer("250-welcome OK\r\n250 2.1.0 OK")));
    testContext.assertTrue(multilineParser.isFinalLine(Buffer.buffer("250 welcome OK\n250 2.1.0 OK")));

    testContext.assertFalse(multilineParser.isFinalLine(Buffer.buffer("250-welcome OK\n250-2.1.0 OK")));
    testContext.assertFalse(multilineParser.isFinalLine(Buffer.buffer("250-welcome OK\r\n250-2.1.0 OK")));

    testContext.assertTrue(multilineParser.isFinalLine(Buffer.buffer("250-welcome OK\n250-2.1.0 OK\n250 2.1.1 OK")));
    testContext.assertTrue(multilineParser.isFinalLine(Buffer.buffer("250-welcome OK\r\n250-2.1.0 OK\r\n250 2.1.1 OK")));

  }
}

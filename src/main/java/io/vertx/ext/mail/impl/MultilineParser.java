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

package io.vertx.ext.mail.impl;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.parsetools.RecordParser;

import java.util.regex.Pattern;

/**
 * Handler to handle the possible multi-lines responses.
 *
 * Multi-lines responses for one command split with '\n'.
 * Responses(one line response and multi-lines responses) split with '\r\n'.
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
class MultilineParser implements Handler<Buffer> {
  private static final Pattern STATUS_LINE_CONTINUE = Pattern.compile("^\\d{3}-.*");
  private static final Logger log = LoggerFactory.getLogger(MultilineParser.class);
  private boolean initialized = false;
  private Buffer result;
  private RecordParser rp;
  private int expected = 1;
  private int actual = 0;

  public MultilineParser(Handler<Buffer> output) {
    Handler<Buffer> mlp = new Handler<Buffer>() {

      @Override
      public void handle(final Buffer buffer) {
        if (!initialized) {
          initialized = true;
          // process the first line to determine CRLF mode
          final String line = buffer.toString();
          if (line.endsWith("\r")) {
            log.debug("setting crlf line mode");
            rp.delimitedMode("\r\n");
            appendOrHandle(Buffer.buffer(line.substring(0, line.length() - 1)));
          } else {
            appendOrHandle(buffer);
          }
        } else {
          appendOrHandle(buffer);
        }
      }

      private void appendOrHandle(final Buffer buffer) {
        if (result == null) {
          result = Buffer.buffer();
        }
        result.appendBuffer(buffer);
        if (isFinalLine(buffer)) {
          actual ++;
          if (actual < expected) {
            result.appendString("\r\n");
          } else if (actual == expected) {
            try {
              output.handle(result);
            } finally {
              result = null;
              actual = 0;
            }
          }
        } else {
          // append \n for all non-last line, there are more buffers to handle
          result.appendString("\n");
        }
      }
    };

    rp = RecordParser.newDelimited("\n", mlp);
  }

  boolean isFinalLine(final Buffer buffer) {
    String line = buffer.toString();
    if (line.contains("\n")) {
      String[] lines = line.split("\n");
      line = lines[lines.length - 1];
    }
    return !STATUS_LINE_CONTINUE.matcher(line).matches();
  }

  @Override
  public void handle(final Buffer event) {
    rp.handle(event);
  }

  MultilineParser setExpected(int expected) {
    this.expected = expected;
    return this;
  }

}

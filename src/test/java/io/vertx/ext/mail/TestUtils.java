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
 *
 */
package io.vertx.ext.mail;

import java.io.*;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import io.vertx.core.buffer.Buffer;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
public class TestUtils {

  private TestUtils() {
  }

  /**
   * @param values
   * @return
   */
  public static Buffer asBuffer(final int... values) {
    byte[] bytes = new byte[values.length];
    for (int i = 0; i < values.length; i++) {
      bytes[i] = (byte) values[i];
    }
    return Buffer.buffer(bytes);
  }

  public static String conv2nl(String string) {
    return string.replace("\r\n", "\n");
  }

  public static MimeMessage getMessage(String mime) throws UnsupportedEncodingException, MessagingException {
    return new MimeMessage(Session.getInstance(new Properties(), null),
        new ByteArrayInputStream(mime.getBytes("ASCII")));
  }

  public static String inputStreamToString(final InputStream inputStream) throws IOException {
    final StringBuilder string = new StringBuilder();
    int ch;
    while ((ch = inputStream.read()) != -1) {
      string.append((char) ch);
    }
    return string.toString();
  }

  // this method can be deprecated after JDK9+ which has InputStream.readAllBytes()
  public static byte[] inputStreamToBytes(final InputStream inputStream) throws IOException {
    ByteArrayOutputStream bufferedArray = new ByteArrayOutputStream();
    int nRead;
    byte[] buffer = new byte[512];
    while ((nRead = inputStream.read(buffer, 0, buffer.length)) != -1) {
      bufferedArray.write(buffer, 0, nRead);
    }
    return bufferedArray.toByteArray();
  }

}

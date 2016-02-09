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

package io.vertx.ext.mail.mailencoder;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * this are tests of the Utils class (as opposed to utils for our tests, that
 * class is called TestUtils)
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
public class UtilsTest {

  @Test
  public void testMustEncodeChar() {
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < 256; i++) {
      if (!Utils.mustEncode((char) i)) {
        sb.append((char) i);
      }
    }
    assertEquals(
      "\n !\"#$%&'()*+,-./0123456789:;<>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\u007f",
      sb.toString());
  }

  @Test
  public void testDate() {
    System.out.println(Utils.generateDate());
  }

  @Test
  public void testBase64() throws Exception {
    assertEquals("", Utils.base64("".getBytes("ISO-8859-1")));
    assertEquals("Kg==", Utils.base64("*".getBytes("ISO-8859-1")));
    assertEquals("KioqKioqKioqKioqKioqKioqKioqKioqKioqKioqKioqKioqKioqKioqKioqKioqKioqKioqKioq\n"
        + "KioqKioqKioqKioqKioqKioqKioqKioqKioqKioqKioqKioqKg==",
      Utils.base64("**********************************************************************************************".getBytes("ISO-8859-1")));
  }

}

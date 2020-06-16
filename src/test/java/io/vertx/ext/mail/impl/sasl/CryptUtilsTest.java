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
package io.vertx.ext.mail.impl.sasl;

import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
public class CryptUtilsTest {

  /**
   * Test method for
   * {@link io.vertx.ext.mail.impl.SMTPAuthentication#encodeHex(byte[])}.
   */
  @Test
  public final void testEncodeHex() {
    assertEquals("", CryptUtils.encodeHex(new byte[]{}));
    assertEquals("00", CryptUtils.encodeHex(new byte[]{0}));
    assertEquals("01", CryptUtils.encodeHex(new byte[]{1}));
    assertEquals("7f", CryptUtils.encodeHex(new byte[]{127}));
    assertEquals("80", CryptUtils.encodeHex(new byte[]{(byte) 128}));
    assertEquals("ff", CryptUtils.encodeHex(new byte[]{(byte) 255}));
    assertEquals("ffffffff", CryptUtils.encodeHex(new byte[]{(byte) 255, (byte) 255, (byte) 255, (byte) 255}));

    byte[] bytes = new byte[256];
    for (int i = 0; i < 256; i++) {
      bytes[i] = (byte) i;
    }
    assertEquals(
      "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f303132333435363738393a3b3c3d3e3f404142434445464748494a4b4c4d4e4f505152535455565758595a5b5c5d5e5f606162636465666768696a6b6c6d6e6f707172737475767778797a7b7c7d7e7f808182838485868788898a8b8c8d8e8f909192939495969798999a9b9c9d9e9fa0a1a2a3a4a5a6a7a8a9aaabacadaeafb0b1b2b3b4b5b6b7b8b9babbbcbdbebfc0c1c2c3c4c5c6c7c8c9cacbcccdcecfd0d1d2d3d4d5d6d7d8d9dadbdcdddedfe0e1e2e3e4e5e6e7e8e9eaebecedeeeff0f1f2f3f4f5f6f7f8f9fafbfcfdfeff",
      CryptUtils.encodeHex(bytes));
  }

  @Test
  public final void testHMacMD5() {
    assertEquals("80070713463e7749b90c2dc24911e275",
      CryptUtils.hmacHex("key", "The quick brown fox jumps over the lazy dog", "HmacMD5"));
    assertEquals("63530468a04e386459855da0063b6596", CryptUtils.hmacHex("key", "", "HmacMD5"));
  }

  @Test
  public final void testHMacSHA1() {
    assertEquals("de7c9b85b8b78aa6bc8a7a36f70a90701c9db4d9",
      CryptUtils.hmacHex("key", "The quick brown fox jumps over the lazy dog", "HmacSHA1"));
    assertEquals("f42bb0eeb018ebbd4597ae7213711ec60760843f", CryptUtils.hmacHex("key", "", "HmacSHA1"));
  }

  @Test
  public final void testHMacSHA256() {
    assertEquals("f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8",
      CryptUtils.hmacHex("key", "The quick brown fox jumps over the lazy dog", "HmacSHA256"));
    assertEquals("5d5d139563c95b5967b9bd9a8c9b233a9dedb45072794cd232dc1b74832607d0",
      CryptUtils.hmacHex("key", "", "HmacSHA256"));
  }

  @Test
  public final void testBase64() {
    assertEquals("", CryptUtils.base64(""));
    assertEquals("Kg==", CryptUtils.base64("*"));
    assertEquals("Kio=", CryptUtils.base64("**"));
    assertEquals("Kioq", CryptUtils.base64("***"));
    assertEquals("KioqKg==", CryptUtils.base64("****"));

    StringBuilder sb = new StringBuilder(256);
    for (char ch = 0; ch < 256; ch++) {
      sb.append(ch);
    }
    // note that this is encoded as UTF-8
    assertEquals(
      "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8gISIjJCUmJygpKissLS4vMDEyMzQ1Njc4OTo7PD0+P0BBQkNERUZHSElKS0xNTk9QUVJTVFVWV1hZWltcXV5fYGFiY2RlZmdoaWprbG1ub3BxcnN0dXZ3eHl6e3x9fn/CgMKBwoLCg8KEwoXChsKHwojCicKKwovCjMKNwo7Cj8KQwpHCksKTwpTClcKWwpfCmMKZwprCm8Kcwp3CnsKfwqDCocKiwqPCpMKlwqbCp8KowqnCqsKrwqzCrcKuwq/CsMKxwrLCs8K0wrXCtsK3wrjCucK6wrvCvMK9wr7Cv8OAw4HDgsODw4TDhcOGw4fDiMOJw4rDi8OMw43DjsOPw5DDkcOSw5PDlMOVw5bDl8OYw5nDmsObw5zDncOew5/DoMOhw6LDo8Okw6XDpsOnw6jDqcOqw6vDrMOtw67Dr8Oww7HDssOzw7TDtcO2w7fDuMO5w7rDu8O8w73DvsO/",
      CryptUtils.base64(sb.toString()));
  }

  @Test
  public final void testDecodeb64() throws UnsupportedEncodingException {
    assertEquals("", CryptUtils.decodeb64(""));
    assertEquals("*", CryptUtils.decodeb64("Kg=="));
    assertEquals("**", CryptUtils.decodeb64("Kio="));
    assertEquals("***", CryptUtils.decodeb64("Kioq"));
    assertEquals("****", CryptUtils.decodeb64("KioqKg=="));

    StringBuilder sbSource = new StringBuilder(256);
    StringBuilder sbExpected = new StringBuilder(512);
    for (char ch = 0; ch < 256; ch++) {
      sbSource.append(ch);
      sbExpected.append(CryptUtils.encodeHex(new byte[]{(byte) ch}));
    }
    String source = CryptUtils.base64(sbSource.toString());
    String expected = sbExpected.toString();
    assertEquals(expected, CryptUtils.encodeHex(CryptUtils.decodeb64(source).getBytes(StandardCharsets.ISO_8859_1)));
  }

}

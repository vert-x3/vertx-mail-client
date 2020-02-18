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

package io.vertx.ext.mail.impl.dkim;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.mail.DKIMSignAlgorithm;
import io.vertx.ext.mail.DKIMSignOptions;
import io.vertx.ext.mail.CanonicalizationAlgorithm;
import org.junit.Test;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

/**
 * Tests against DKIMSigner and it's configurations.
 *
 * @author <a href="mailto: aoingl@gmail.com">Lin Gao</a>
 */
public class DKIMSignerTest {

  // a PKCS#8 format private key for testing
  private final static String PRIVATE_KEY =
    "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAKqSazYC8pj/JQmo\n" +
      "2ep0m3Shs6WGyHarknUzRJxiHWIVl2CvvOz2aCo4QCFk7nHjJbSQigA/xRrQ+Mzg\n" +
      "uNv4n/c+0MjMQscpyhrMYhza89jP3yMRjIEPJxiQzeMgGHTQifiBfB+2a8959YkB\n" +
      "oOJZuoY0TOEyB+Lm3j000B4evsRdAgMBAAECgYAdSw38dZ8iJVdABG6ANExqSEbo\n" +
      "22/b6XU6iXZ0AOmY6apYoXWpoFudPJHO6l2E04SrMNNyXYFFLLQ9wy4cIOOfs5yB\n" +
      "bdZ17tvOqSWT7nsCcuHpUvF89JNXnQvV2xwS6npp/tIuehMfxOxPLdN87Nge7BEy\n" +
      "6DCSW7U72pX9zjl1BQJBANv56R9X+XLWjW6n4s0tZ271XVYI4DlRxQHYHP3B7eLm\n" +
      "4DJtoHk65WU3kfHUeBNy/9TmpC25Gw6WTDco+mOS8wsCQQDGgVPCqhNDUcZYMeOH\n" +
      "X6hm+l8zBeTMF2udQbkl0dRdLFpbMtw3cg+WUjHg3AYv38P2ikSJZzgzdDyZzcxF\n" +
      "Hcc3AkBXoBNm8upg/mpUW/gSdzWuk3rcnKiE7LenZmkWBDw4mHNSYyz7XaSnTx2J\n" +
      "0XMLfFHAgyd/Ny85/lDZ4C7tn0nFAkEAkS2mz9lJa1PUZ05dZPWuGVqF47AszKNY\n" +
      "XlPiEGntEhPNJaQF8TsncT4+IoFouPzDun0XcRKfxOn/JFGiUu5bcwJAGbai+kPl\n" +
      "AoyfGLxOLu40IMNOHKhHOq8cm3dOC+HpQYpx96JGaQPY4kl3fos6e43DGp9vyOxv\n" +
      "VMj5fan+wzHLcw==";

  @Test
  public void testDefaultConstructor() {
    final DKIMSignOptions dkimOps = new DKIMSignOptions();
    assertNotNull(dkimOps);

    assertNull(dkimOps.getAuid());
    assertNull(dkimOps.getCopiedHeaders());
    assertNull(dkimOps.getSdid());
    assertNull(dkimOps.getSelector());
    assertFalse(dkimOps.isSignatureTimestamp());

    assertTrue(dkimOps.getSignedHeaders().stream().anyMatch(h -> h.equalsIgnoreCase("from")));
    assertEquals(DKIMSignAlgorithm.RSA_SHA256, dkimOps.getSignAlgo());
    assertEquals(CanonicalizationAlgorithm.SIMPLE, dkimOps.getHeaderCanonAlgo());
    assertEquals(CanonicalizationAlgorithm.SIMPLE, dkimOps.getBodyCanonAlgo());
    assertEquals(-1, dkimOps.getBodyLimit());
    assertEquals(-1, dkimOps.getExpireTime());

    JsonObject json = dkimOps.toJson();
    assertEquals(DKIMSignAlgorithm.RSA_SHA256, DKIMSignAlgorithm.valueOf(json.getString("signAlgo")));
    assertEquals(CanonicalizationAlgorithm.SIMPLE.name(), json.getString("headerCanonAlgo"));
    assertEquals(CanonicalizationAlgorithm.SIMPLE.name(), json.getString("bodyCanonAlgo"));
  }

  @Test
  public void testConfigFull() {
    final DKIMSignOptions dkimOps = new DKIMSignOptions();
    assertNotNull(dkimOps);

    dkimOps.setAuid("local-part@example.com");
    dkimOps.setSdid("example.com");
    dkimOps.setBodyCanonAlgo(CanonicalizationAlgorithm.RELAXED);
    dkimOps.setBodyLimit(5000);
    dkimOps.setCopiedHeaders(Stream.of("From", "To").collect(Collectors.toList()));
    dkimOps.setSelector("exampleUser");
    dkimOps.setHeaderCanonAlgo(CanonicalizationAlgorithm.SIMPLE);
    dkimOps.setPrivateKey(PRIVATE_KEY);

    assertEquals("local-part@example.com", dkimOps.getAuid());
    assertEquals("example.com", dkimOps.getSdid());
    assertEquals(CanonicalizationAlgorithm.RELAXED, dkimOps.getBodyCanonAlgo());
    assertEquals(5000, dkimOps.getBodyLimit());
    assertArrayEquals(new String[]{"From", "To"}, dkimOps.getCopiedHeaders().toArray());
    assertEquals("exampleUser", dkimOps.getSelector());
    assertEquals(CanonicalizationAlgorithm.SIMPLE, dkimOps.getHeaderCanonAlgo());

    DKIMSigner dkimSigner = new DKIMSigner(dkimOps, null);
    assertNotNull(dkimSigner);

  }

  @Test
  public void testInvalidConfig() {
    final DKIMSignOptions dkimOps = new DKIMSignOptions();

    try {
      new DKIMSigner(dkimOps, null);
      fail("not here");
    } catch (IllegalStateException e) {
      assertEquals("Either private key or private key file path must be specified to sign", e.getMessage());
    }

    dkimOps.setPrivateKey(PRIVATE_KEY);
    dkimOps.setSdid("example.com");
    try {
      new DKIMSigner(dkimOps, null);
      fail("not here");
    } catch (IllegalStateException e) {
      assertEquals("The selector must be specified to be able to verify", e.getMessage());
    }
    dkimOps.setSelector("examUser");

    dkimOps.setSdid("example.com");
    dkimOps.setAuid("local-part@another.domain.com");
    try {
      new DKIMSigner(dkimOps, null);
      fail("not here");
    } catch (IllegalStateException e) {
      assertEquals("Identity domain mismatch, expected is: [xx]@[xx.]sdid", e.getMessage());
    }
  }

  private DKIMSignOptions dkimOps() {
    final DKIMSignOptions dkimOps = new DKIMSignOptions();
    dkimOps.setPrivateKey(PRIVATE_KEY);
    dkimOps.setSelector("examUser");
    dkimOps.setSdid("example.com");
    dkimOps.setAuid("local-part@example.com");
    return dkimOps;
  }

  @Test
  public void testDKIMHeaderTemplate() {
    DKIMSignOptions dkimOps = dkimOps();
    DKIMSigner signer = new DKIMSigner(dkimOps, null);
    String dkimTagsListTemplate = signer.dkimSignatureTemplate();
    String expected = "v=1; a=rsa-sha256; c=simple/simple; d=example.com; " +
      "i=local-part@example.com; s=examUser; h=From:Reply-to:Subject:Date:To:Cc:Content-Type:Message-ID; ";
    assertEquals(expected, dkimTagsListTemplate);

    dkimOps.setSelector("with space");
    signer = new DKIMSigner(dkimOps, null);
    dkimTagsListTemplate = signer.dkimSignatureTemplate();
    expected = "v=1; a=rsa-sha256; c=simple/simple; d=example.com; " +
      "i=local-part@example.com; s=with=20space; h=From:Reply-to:Subject:Date:To:Cc:Content-Type:Message-ID; ";
    assertEquals(expected, dkimTagsListTemplate);

    dkimOps.setAuid(null);
    signer = new DKIMSigner(dkimOps, null);
    dkimTagsListTemplate = signer.dkimSignatureTemplate();
    expected = "v=1; a=rsa-sha256; c=simple/simple; d=example.com; " +
      "i=@example.com; s=with=20space; h=From:Reply-to:Subject:Date:To:Cc:Content-Type:Message-ID; ";
    assertEquals(expected, dkimTagsListTemplate);

  }

  @Test
  public void testSimpleHeader() {
    DKIMSignOptions dkimOps = dkimOps().setHeaderCanonAlgo(CanonicalizationAlgorithm.SIMPLE);
    DKIMSigner signer = new DKIMSigner(dkimOps, null);
    // there will be possible to have \n in the header value, keep it as it is.
    String name = "from";
    String value = "user@example.com";
    String canonicHeaderLine = signer.canonicHeader(name, value);
    assertEquals("from: user@example.com", canonicHeaderLine);

    name = "from ";
    value = "user@example.com";
    canonicHeaderLine = signer.canonicHeader(name, value);
    assertEquals("from : user@example.com", canonicHeaderLine);

    name = " from ";
    value = " user@example.com ";
    canonicHeaderLine = signer.canonicHeader(name, value);
    assertEquals(" from :  user@example.com ", canonicHeaderLine);

    name = " from ";
    value = " user@example.com \n ";
    canonicHeaderLine = signer.canonicHeader(name, value);
    assertEquals(" from :  user@example.com \n ", canonicHeaderLine);
  }

  @Test
  public void testRelaxedHeader() {
    DKIMSignOptions dkimOps = dkimOps().setHeaderCanonAlgo(CanonicalizationAlgorithm.RELAXED);
    DKIMSigner signer = new DKIMSigner(dkimOps, null);
    // there will be possible to have \n in the header value
    String name = "From";
    String value = "user@example.com";
    String canonicHeaderLine = signer.canonicHeader(name, value);
    assertEquals("from:user@example.com", canonicHeaderLine);

    name = "From ";
    value = "user@example.com";
    canonicHeaderLine = signer.canonicHeader(name, value);
    assertEquals("from:user@example.com", canonicHeaderLine);

    name = " From";
    value = "user@example.com\t ";
    canonicHeaderLine = signer.canonicHeader(name, value);
    assertEquals("from:user@example.com", canonicHeaderLine);

    name = "From";
    value = " user@example.com \n \t ";
    canonicHeaderLine = signer.canonicHeader(name, value);
    assertEquals("from: user@example.com", canonicHeaderLine);

    name = "dummyHeader";
    value = " dummyValue \r \r\n \t  test  ";
    canonicHeaderLine = signer.canonicHeader(name, value);
    assertEquals("dummyheader: dummyValue test", canonicHeaderLine);
  }

  @Test
  public void testRelaxedBodyLine() {
    DKIMSignOptions dkimOps = dkimOps().setBodyCanonAlgo(CanonicalizationAlgorithm.RELAXED);
    DKIMSigner signer = new DKIMSigner(dkimOps, null);
    // there will be no \n in each line, so just test whitespaces
    String line = "Line with trailing HTAB\t";
    String cannonicLine = signer.canonicBodyLine(line);
    assertEquals("Line with trailing HTAB", cannonicLine);

    line = "Line with trailing HTAB and space \t ";
    cannonicLine = signer.canonicBodyLine(line);
    assertEquals("Line with trailing HTAB and space", cannonicLine);

    line = "\tLine with leading HTAB";
    cannonicLine = signer.canonicBodyLine(line);
    assertEquals(" Line with leading HTAB", cannonicLine);

    line = "\t Line with leading HTAB and space";
    cannonicLine = signer.canonicBodyLine(line);
    assertEquals(" Line with leading HTAB and space", cannonicLine);

    line = "\t Line with leading and trailing HTAB and space \t ";
    cannonicLine = signer.canonicBodyLine(line);
    assertEquals(" Line with leading and trailing HTAB and space", cannonicLine);

  }

  @Test
  public void testSimpleBodyCannonic() {
    DKIMSignOptions dkimOps = dkimOps().setBodyCanonAlgo(CanonicalizationAlgorithm.SIMPLE);
    DKIMSigner signer = new DKIMSigner(dkimOps, null);

    String body = "This is a Multiple Lines Text\n.Some lines start with one dot\n..Some" +
      "lines start with 2 dots.\n";
    String cannonicBody = signer.dkimMailBody(body);
    assertEquals("This is a Multiple Lines Text\r\n.Some lines start with one dot\r\n..Some" +
      "lines start with 2 dots.\r\n", cannonicBody);

    body = "This is a Multiple Lines Text\n.Some lines start with one dot\n..Some" +
      "lines start with 2 dots. \r\n\r\n\r\n\r\n\r\n";
    cannonicBody = signer.dkimMailBody(body);
    assertEquals("This is a Multiple Lines Text\r\n.Some lines start with one dot\r\n..Some" +
      "lines start with 2 dots. \r\n", cannonicBody);

    body = "This is a Multiple Lines Text\n.Some lines start with one dot\n..Some" +
      "lines start with 2 dots.\n \r\n \t \r\n";
    cannonicBody = signer.dkimMailBody(body);
    assertEquals("This is a Multiple Lines Text\r\n.Some lines start with one dot\r\n..Some" +
      "lines start with 2 dots.\r\n \r\n \t \r\n", cannonicBody);
  }

  @Test
  public void testRelaxedBodyCannonic() {
    DKIMSignOptions dkimOps = dkimOps().setBodyCanonAlgo(CanonicalizationAlgorithm.RELAXED);
    DKIMSigner signer = new DKIMSigner(dkimOps, null);

    String body = "simple line";
    String cannonicBody = signer.dkimMailBody(body);
    assertEquals("simple line\r\n", cannonicBody);

    body = "simple line ";
    cannonicBody = signer.dkimMailBody(body);
    assertEquals("simple line\r\n", cannonicBody);

    body = " \tsimple line \t ";
    cannonicBody = signer.dkimMailBody(body);
    assertEquals(" simple line\r\n", cannonicBody);

    body = " line \r\nnext \t line\r\n\r\n\r\n";
    cannonicBody = signer.dkimMailBody(body);
    assertEquals(" line\r\nnext line\r\n", cannonicBody);

    body = " line \t\r\nnext \t line \t \r\n\r\n\r\n";
    cannonicBody = signer.dkimMailBody(body);
    assertEquals(" line\r\nnext line\r\n", cannonicBody);

  }

}

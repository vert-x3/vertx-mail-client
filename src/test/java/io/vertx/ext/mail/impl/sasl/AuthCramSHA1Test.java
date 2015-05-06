package io.vertx.ext.mail.impl.sasl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class AuthCramSHA1Test {

  @Test
  public void testAuthCramSHA1() {
    final AuthCramSHA1 auth = new AuthCramSHA1("xxx", "yyy");

    assertNotNull(auth);
    assertEquals("CRAM-SHA1", auth.getName());
  }

  @Test
  public void testGetName() {
    assertEquals("CRAM-SHA1", new AuthCramSHA1("xxx", "yyy").getName());
  }

  @Test
  public void testNextStep() {
    final AuthCramSHA1 auth = new AuthCramSHA1("xxx", "yyy");
    assertEquals("", auth.nextStep(null));
    assertEquals("xxx 76c4faeae224fce51c3d372bb4bfdbdfbc563134", auth.nextStep("<12345@example.com>"));
    assertEquals(null, auth.nextStep("250 ok"));
  }

  /**
   * hmac example from Wikipedia
   */
  @Test
  public void testNextStep2() {
    final AuthCramSHA1 auth = new AuthCramSHA1("user", "key");
    assertEquals("", auth.nextStep(null));
    assertEquals("user de7c9b85b8b78aa6bc8a7a36f70a90701c9db4d9", auth.nextStep("The quick brown fox jumps over the lazy dog"));
    assertEquals(null, auth.nextStep("250 ok"));
  }

}

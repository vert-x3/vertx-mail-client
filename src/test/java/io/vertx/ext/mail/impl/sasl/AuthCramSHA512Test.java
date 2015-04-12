package io.vertx.ext.mail.impl.sasl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class AuthCramSHA512Test {

  @Test
  public void testAuthCramSHA512() {
    final AuthCramSHA512 auth = new AuthCramSHA512("xxx", "yyy");

    assertNotNull(auth);
    assertEquals("CRAM-SHA512", auth.getName());
  }

  @Test
  public void testGetName() {
    assertEquals("CRAM-SHA512", new AuthCramSHA512("xxx", "yyy").getName());
  }

  @Test
  public void testNextStep() {
    final AuthCramSHA512 auth = new AuthCramSHA512("xxx", "yyy");
    assertEquals("", auth.nextStep(null));
    assertEquals("xxx 9f4bac24604033236d36b48b44cfda448e0aca65a2be0b5b4bac31706445c8804c5108ead5cd0771b308fa3e0aa878afc3f4792d0dbb73fa89fe172259fffdba", auth.nextStep("<12345@example.com>"));
    assertEquals(null, auth.nextStep("250 ok"));
  }

  /**
   * hmac example from Wikipedia
   */
  @Test
  public void testNextStep2() {
    final AuthCramSHA512 auth = new AuthCramSHA512("user", "key");
    assertEquals("", auth.nextStep(null));
    assertEquals("user b42af09057bac1e2d41708e48a902e09b5ff7f12ab428a4fe86653c73dd248fb82f948a549f7b791a5b41915ee4d1ec3935357e4e2317250d0372afa2ebeeb3a", auth.nextStep("The quick brown fox jumps over the lazy dog"));
    assertEquals(null, auth.nextStep("250 ok"));
  }

}

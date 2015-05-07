package io.vertx.ext.mail.impl.sasl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AuthPlainTest {

  @Test
  public void testAuthPlain() {
    final AuthPlain auth = new AuthPlain("xxx", "yyy");
    assertNotNull(auth);
    assertEquals("PLAIN", auth.getName());
  }

  @Test
  public void testNextStep() {
    final AuthPlain auth = new AuthPlain("xxx", "yyy");
    assertEquals("\0xxx\0yyy", auth.nextStep(null));
    assertEquals(null, auth.nextStep("250 auth successful"));
  }

}

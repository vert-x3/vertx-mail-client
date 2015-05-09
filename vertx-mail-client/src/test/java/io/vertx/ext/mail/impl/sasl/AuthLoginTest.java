package io.vertx.ext.mail.impl.sasl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AuthLoginTest {
  @Test
  public void testAuthLogin() {
    AuthLogin result = new AuthLogin("xxx", "yyy");
    assertNotNull(result);
    assertEquals("LOGIN", result.getName());
  }

  @Test
  public void testGetName() {
    assertEquals("LOGIN", new AuthLogin("xxx", "yyy").getName());
  }

  @Test
  public void testNextStep() {
    final AuthLogin auth = new AuthLogin("xxx", "yyy");
    assertEquals("", auth.nextStep(null));
    assertEquals("xxx", auth.nextStep("Username"));
    assertEquals("yyy", auth.nextStep("Password"));
    assertEquals(null, auth.nextStep("250 auth ok"));
  }

}

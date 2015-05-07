/**
 *
 */
package io.vertx.ext.mail.impl.sasl;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
public class AuthOperationFactoryTest {

  /**
   * Test method for
   * {@link io.vertx.ext.mail.impl.sasl.AuthOperationFactory#createAuth(java.lang.String, java.lang.String, java.util.Set)}
   * .
   */
  @Test
  public final void testCreateAuth() {
    Set<String> allowedAuth = new HashSet<String>();
    allowedAuth.add("PLAIN");
    assertEquals(AuthPlain.class, AuthOperationFactory.createAuth("", "", allowedAuth).getClass());
  }

}

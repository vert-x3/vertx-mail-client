/**
 *
 */
package io.vertx.ext.mail.impl.sasl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
public class AuthOperationFactoryTest {

  /**
   * Test method for
   * {@link io.vertx.ext.mail.impl.sasl.AuthOperationFactory#createAuth(java.lang.String, java.lang.String, java.util.Set)}
   * make sure that the default auth method works and is PLAIN 
   * @throws Exception
   */
  @Test
  public final void testCreateAuth() throws Exception {
    Set<String> allowedAuth = new HashSet<String>();
    allowedAuth.add("PLAIN");
    assertEquals(AuthPlain.class, AuthOperationFactory.createAuth("user", "pw", allowedAuth).getClass());
  }

  @Test
  public final void testAuthNotFound() throws Exception {
    Set<String> allowedAuth = new HashSet<String>();
    allowedAuth.add("ASDF");
    assertNull(AuthOperationFactory.createAuth("user", "pw", allowedAuth));
  }

}

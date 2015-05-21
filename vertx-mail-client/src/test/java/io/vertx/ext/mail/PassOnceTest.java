/**
 * 
 */
package io.vertx.ext.mail;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
public class PassOnceTest {

  @Test
  public final void testOnce() {
    PassOnce pass = new PassOnce(s -> fail(s));
    pass.passOnce();
  }

  @Test(expected = AssertionError.class)
  public final void testTwice() {
    PassOnce pass = new PassOnce(s -> fail(s));
    pass.passOnce();
    pass.passOnce();
  }

}

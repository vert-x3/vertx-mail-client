/**
 * run a few additional asserts (synchronous) after an async operation was succeeded
 */
package io.vertx.ext.mail;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
@FunctionalInterface
public interface AdditionalAsserts {
  public void doAsserts() throws Exception;
}

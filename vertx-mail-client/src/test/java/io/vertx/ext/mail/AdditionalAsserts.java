/**
 * run a few additional asserts (synchronous) after an async operation was succeeded
 */
package io.vertx.ext.mail;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
@FunctionalInterface
interface AdditionalAsserts {
  void doAsserts() throws Exception;
}

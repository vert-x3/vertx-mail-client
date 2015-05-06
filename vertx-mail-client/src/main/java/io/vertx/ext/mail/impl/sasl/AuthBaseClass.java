/**
 * 
 */
package io.vertx.ext.mail.impl.sasl;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
abstract class AuthBaseClass implements AuthOperation {

  protected final String username;
  protected final String password;

  /**
   * 
   */
  protected AuthBaseClass(String username, String password) {
    this.username = username;
    this.password = password;
  }

  public abstract String getName();

  public abstract String nextStep(String data);

}

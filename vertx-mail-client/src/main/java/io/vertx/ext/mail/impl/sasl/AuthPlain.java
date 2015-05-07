/**
 *
 */
package io.vertx.ext.mail.impl.sasl;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
class AuthPlain extends AuthBaseClass {

  /**
   *
   */
  static final String AUTH_NAME = "PLAIN";
  private boolean first;

  /**
   * @param username
   * @param password
   */
  public AuthPlain(String username, String password) {
    super(username, password);
    first = true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see io.vertx.ext.mail.impl.AuthBaseClass#getName()
   */
  @Override
  public String getName() {
    return AUTH_NAME;
  }

  /*
   * (non-Javadoc)
   * 
   * @see io.vertx.ext.mail.impl.AuthBaseClass#nextStep(java.lang.String)
   */
  @Override
  public String nextStep(String data) {
    if (first) {
      first = false;
      return "\0" + username + "\0" + password;
    } else {
      return null;
    }
  }
}

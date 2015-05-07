/**
 *
 */
package io.vertx.ext.mail.impl.sasl;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
class AuthCramMD5 extends AuthCram {

  /**
   *
   */
  static final String AUTH_NAME = "CRAM-MD5";

  /**
   * @param username
   * @param password
   */
  public AuthCramMD5(String username, String password) {
    super(username, password, "HmacMD5");
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

}

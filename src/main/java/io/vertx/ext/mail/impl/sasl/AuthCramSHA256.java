/**
 * 
 */
package io.vertx.ext.mail.impl.sasl;


/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
class AuthCramSHA256 extends AuthCram {

  /**
   * 
   */
  static final String AUTH_NAME = "CRAM-SHA256";

  /**
   * @param username
   * @param password
   */
  public AuthCramSHA256(String username, String password) {
    super(username, password, "HmacSHA256");
  }

  /* (non-Javadoc)
   * @see io.vertx.ext.mail.impl.AuthBaseClass#getName()
   */
  @Override
  public String getName() {
    return AUTH_NAME;
  }

}

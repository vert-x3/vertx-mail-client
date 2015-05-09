/**
 * 
 */
package io.vertx.ext.mail.impl.sasl;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
class AuthDigestMD5 extends AuthDigest {

  /**
   * 
   */
  static final String AUTH_NAME = "DIGEST-MD5";

  /**
   * @param username
   * @param password
   */
  public AuthDigestMD5(String username, String password) {
    super(username, password, "MD5");
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

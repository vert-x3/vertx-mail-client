/**
 *
 */
package io.vertx.ext.mail.impl.sasl;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
abstract class AuthCram extends AuthBaseClass {

  protected boolean firstStep;
  protected boolean finished;
  final private String hmac;

  /**
   * @param username
   * @param password
   */
  protected AuthCram(String username, String password, String hmac) {
    super(username, password);
    firstStep = true;
    finished = false;
    this.hmac = hmac;

  }

  /*
   * (non-Javadoc)
   * 
   * @see io.vertx.ext.mail.impl.AuthBaseClass#nextStep(java.lang.String)
   */
  @Override
  public String nextStep(String data) {
    if (finished) {
      return null;
    }
    if (firstStep) {
      firstStep = false;
      return "";
    } else {
      finished = true;
      String reply = CryptUtils.hmacHex(password, data, hmac);
      return username + " " + reply;
    }
  }

}

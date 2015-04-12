/**
 * 
 */
package io.vertx.ext.mail.impl.sasl;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
class AuthLogin extends AuthBaseClass {

  /**
   * 
   */
  static final String AUTH_NAME = "LOGIN";
  private boolean firstStep;
  private boolean secondStep;
  private boolean finished;

  /**
   * @param username
   * @param password
   */
  public AuthLogin(String username, String password) {
    super(username, password);
    firstStep=true;
    secondStep=true;
    finished=false;
  }

  /* (non-Javadoc)
   * @see io.vertx.ext.mail.impl.AuthBaseClass#getName()
   */
  @Override
  public String getName() {
    return AUTH_NAME;
  }

  /* (non-Javadoc)
   * @see io.vertx.ext.mail.impl.AuthBaseClass#nextStep(java.lang.String)
   */
  @Override
  public String nextStep(String data) {
    if(finished) {
      return null;
    }
    if(firstStep) {
      firstStep = false;
      return "";
    } else if(secondStep) {
      secondStep=false;
      return username;
    } else {
      finished = true;
      return password;
    }
  }

}

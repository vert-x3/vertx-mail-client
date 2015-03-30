package io.vertx.ext.mail;

import io.vertx.core.Handler;

/**
 * Assert that a point in the code is passed only once.
 * <p>
 * this is necessary since there may be bugs where an error handler is called
 * twice (there were some ...)
 * <p>
 * PassOnce pass = new PassOnce(s -> fail(s));
 * ...
 * pass.passOnce(); // will call fail handler if the statement is passed more than once
 * <p>
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */

class PassOnce {

  private boolean passed;
  private final Handler<String> fail;

  public PassOnce(Handler<String> fail) {
    this.fail = fail;
    passed = false;
  }

  public void passOnce() {
    if (passed) {
      fail.handle("should only pass this point once");
    } else {
      passed = true;
    }
  }

}

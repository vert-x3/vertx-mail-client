package io.vertx.ext.mail;

import org.junit.Test;

/**
 * this test requires login but connects to a server that doesn't support AUTH
 * this tests the behaviour of some services which announce AUTH only when
 * connected with TLS e.g. gmail.com
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
public class MissingAuthTest extends SMTPTestDummy {

  @Test
  public void mailTest() {
    runTestException(mailServiceLogin());
  }

}

package io.vertx.ext.mail;

import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * this test requires login but connects to a server that doesn't support AUTH
 * this tests the behaviour of some services which announce AUTH only when
 * connected with TLS e.g. gmail.com
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
@RunWith(VertxUnitRunner.class)
public class MissingAuthTest extends SMTPTestDummy {

  @Test
  public void mailTest(TestContext testContext) {
    this.testContext=testContext;
    testException(mailClientLogin());
  }

}

package io.vertx.ext.mail;

import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * this tests uses more than 57 bytes as AUTH PLAIN string which would break the authentication if the base64 data were
 * chunked
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
@RunWith(VertxUnitRunner.class)
public class LongAuthTest extends SMTPTestWiser {

  @Test
  public void mailTest(TestContext testContext) {
    this.testContext = testContext;
    testSuccess(
        mailClientLogin("*************************************************",
            "*************************************************"), exampleMessage(), assertExampleMessage());
  }

}

package io.vertx.ext.mail;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
@RunWith(VertxUnitRunner.class)
public class MailEBTest extends SMTPTestDummy {

  private static final Logger log = LoggerFactory.getLogger(MailEBTest.class);

  @Test
  public void mailTest(TestContext testContext) {
    testSuccess(MailService.createEventBusProxy(vertx, "vertx.mail"));
  }

  @Before
  public void startVerticle(TestContext testContext) {
    Async async = testContext.async();

    JsonObject config = new JsonObject("{\"config\":{\"address\":\"vertx.mail\",\"hostname\":\"localhost\",\"port\":1587}}");
    DeploymentOptions deploymentOptions = new DeploymentOptions(config);
    vertx.deployVerticle("io.vertx.ext.mail.MailServiceVerticle", deploymentOptions ,r -> {
      if(r.succeeded()) {
        log.info(r.result());
        async.complete();
      } else {
        log.info("exception", r.cause());
        testContext.fail(r.cause());
      }
    });
  }

}

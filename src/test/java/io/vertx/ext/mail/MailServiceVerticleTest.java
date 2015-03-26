package io.vertx.ext.mail;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.test.core.VertxTestBase;

import org.junit.Test;

/*
 * Test setup of MailServiceVerticle
 *
 * the actual functionality will be tested in MailEBTest
 */
/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
public class MailServiceVerticleTest extends VertxTestBase {

  private static final Logger log = LoggerFactory.getLogger(MailServiceVerticleTest.class);

  @Test
  public void testService() throws InterruptedException {
    JsonObject config = new JsonObject(
        "{\"config\":{\"address\":\"vertx.mail\",\"hostname\":\"localhost\",\"port\":1587}}");
    DeploymentOptions deploymentOptions = new DeploymentOptions(config);
    vertx.deployVerticle("service:io.vertx.mail-service", deploymentOptions, r -> {
      if (r.succeeded()) {
        log.info(r.result());
        testComplete();
      } else {
        log.info("exception", r.cause());
        throw new RuntimeException(r.cause());
      }
    });

    await();
  }

  @Test
  public void testServiceError() throws InterruptedException {
    JsonObject config = new JsonObject("{}");
    DeploymentOptions deploymentOptions = new DeploymentOptions(config);
    vertx.deployVerticle("service:io.vertx.mail-service", deploymentOptions, r -> {
      if (r.succeeded()) {
        log.info(r.result());
        fail("operation should fail");
      } else {
        log.info("exception", r.cause());
        testComplete();
      }
    });

    await();
  }

}

package io.vertx.ext.mail;

import io.vertx.test.core.VertxTestBase;

/*
 first implementation of a SMTP client
 */
/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
public class MailEBTest extends VertxTestBase {

//  private static final Logger log = LoggerFactory.getLogger(MailEBTest.class);
//
//  @Test
//  public void mailTest() throws InterruptedException {
//    testSuccess(MailService.createEventBusProxy(vertx, "vertx.mail"));
//  }
//
//  @Before
//  public void startSMTP() {
//    smtpServer = new TestSmtpServer(vertx);
//    CountDownLatch latch = new CountDownLatch(1);
//    JsonObject config = new JsonObject("{\"config\":{\"address\":\"vertx.mail\",\"hostname\":\"localhost\",\"port\":1587}}");
//    DeploymentOptions deploymentOptions = new DeploymentOptions(config);
//    vertx.deployVerticle("MailServiceVerticle", deploymentOptions ,r -> {
//      if(r.succeeded()) {
//        log.info(r.result());
//      } else {
//        log.info("exception", r.cause());
//      }
//      latch.countDown();
//    });
//    try {
//      latch.await();
//    } catch (InterruptedException e) {
//      // TODO Auto-generated catch block
//      e.printStackTrace();
//    }
//  }
//
//  @After
//  public void stopSMTP() {
//    smtpServer.stop();
//  }

}

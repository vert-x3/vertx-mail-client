package io.vertx.ext.mail;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.test.core.VertxTestBase;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import org.junit.Ignore;
import org.junit.Test;

/*
 first implementation of a SMTP client
 */
/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
public class MailTest extends VertxTestBase {

  Vertx vertx = Vertx.vertx();
  private static final Logger log = LoggerFactory.getLogger(MailTest.class);

  CountDownLatch latch;

  @Ignore
  @Test
  public void mailTest() throws IOException, InterruptedException {
    log.info("starting");

    latch = new CountDownLatch(1);

    // this is a hack to avoid putting an actual account into the test
    // script, you will have to put your own account into the file
    // or write the account data directly into the java code
    // or a vertx conf file

    String username = null;
    String password = null;

    if (new File("account.properties").exists()) {
      Properties account = new Properties();
      try (InputStream inputstream = new FileInputStream("account.properties")) {
        account.load(inputstream);
        username = account.getProperty("username");
        password = account.getProperty("password");
      }
    } else if ("true".equals(System.getenv("DRONE")) || System.getenv("JENKINS_URL ") != null) {
      // assume we are running inside CI (drone.io or jenkins)
      // and can get the credentials from environment
      username = System.getenv("SMTP_USERNAME");
      password = System.getenv("SMTP_PASSWORD");
    }

    // if username is null, auth will fail in the smtp dialog since we set
    // LoginOption.REQUIRED
    if (username == null) {
      log.warn("auth account unavailable");
    }

    // MailConfig mailConfig = ServerConfigs.configSendgrid();
    MailConfig mailConfig = new MailConfig("smtp.aol.com", 587, StarttlsOption.REQUIRED, LoginOption.REQUIRED);
    mailConfig.setUsername(username);
    mailConfig.setPassword(password);

    MailService mailService = MailService.create(vertx, mailConfig);

    Buffer image=vertx.fileSystem().readFileBlocking("logo-white-big.png");

    JsonObject email = new JsonObject()
      .put("from", "alexlehm1969@aol.com")
      .put("recipient", "alexlehm@gmail.com")
      .put("bounceAddress", "alexlehm1969@aol.com")
      .put("subject", "Test email with HTML")
      .put("text", "this is a message")
      .put("html", "<a href=\"http://vertx.io\">vertx.io</a>");

    JsonObject attachment=new JsonObject()
      .put("data", image.getBytes())
      .put("name", "logo-white-big.png")
      .put("content-type", "image/png")
      .put("disposition", "inline")
      .put("description", "logo of vert.x web page");

    email.put("attachment", attachment);

    mailService.sendMail(email, result -> {
      log.info("mail finished");
      if (result.succeeded()) {
        log.info(result.result().toString());
        latch.countDown();
      } else {
        log.warn("got exception", result.cause());
        throw new RuntimeException(result.cause());
      }
    });
    awaitLatch(latch);
  }
}

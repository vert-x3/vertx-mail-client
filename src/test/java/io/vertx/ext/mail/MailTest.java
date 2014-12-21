package io.vertx.ext.mail;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;

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
public class MailTest {

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
    }
    else if ("true".equals(System.getenv("DRONE")) || System.getenv("JENKINS_URL ") != null) {
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

    MailConfig mailConfig = ServerConfigs.configSendgrid();
    mailConfig.setUsername(username);
    mailConfig.setPassword(password);

    MailService mailService = MailService.create(vertx, mailConfig);

    JsonObject email = new JsonObject();
    email.put("from", "lehmann333@arcor.de");
    email.put("recipient", "lehmann333@arcor.de");
    email.put("bounceAddress", "nobody@lehmann.cx");
    email.put("subject", "Test email with HTML");
    email.put("text", "this is a message");

    mailService.sendMail(email, result -> {
      log.info("mail finished");
      if (result.succeeded()) {
        log.info(result.result().toString());
      } else {
        log.warn("got exception", result.cause());
      }
      latch.countDown();
    });

    latch.await();
  }
}

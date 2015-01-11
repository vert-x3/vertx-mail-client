package io.vertx.ext.mail;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.test.core.VertxTestBase;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
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

    MailMessage email = new MailMessage()
      .setFrom("alexlehm1969@aol.com")
      .setRecipient("alexlehm@gmail.com")
      .setBounceAddress("alexlehm1969@aol.com")
      .setSubject("Test email with HTML")
      .setText("this is a message")
      .setHtml("<a href=\"http://vertx.io\">vertx.io</a>");

    List<MailAttachment> list=new ArrayList<MailAttachment>();

    list.add(new MailAttachment()
      .setData(new String(image.getBytes(), "ISO-8859-1"))
      .setName("logo-white-big.png")
      .setContentType("image/png")
      .setDisposition("inline")
      .setDescription("logo of vert.x web page"));

    list.add(new MailAttachment()
      .setData("this is a text attachment")
      .setName("file.txt")
      .setContentType("text/plain")
      .setDisposition("attachment")
      .setDescription("some text"));

    email.setAttachment(list);

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

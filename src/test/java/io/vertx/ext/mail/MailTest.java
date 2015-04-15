package io.vertx.ext.mail;

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

  private static final Logger log = LoggerFactory.getLogger(MailTest.class);

//  @Ignore
  @Test
  public void mailTest() throws IOException {
    log.info("starting");

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

    MailConfig mailConfig = new MailConfig("smtp.aol.com", 587, StarttlsOption.REQUIRED, LoginOption.REQUIRED);
//    MailConfig mailConfig = new MailConfig();
    mailConfig.setUsername(username);
    mailConfig.setPassword(password);
//    mailConfig.setTrustAll(true);
//    mailConfig.setAuthMethods("DIGEST-MD5");

    MailService mailService = MailService.create(vertx, mailConfig);

    Buffer image=vertx.fileSystem().readFileBlocking("logo-white-big.png");

    MailMessage email = new MailMessage()
      .setFrom("alexlehm1969@aol.com")
      .setTo("alexlehm1969@aol.com")
      .setCc("lehmann333@arcor.de")
      .setBcc("alexlehm@gmail.com")
      .setBounceAddress("alexlehm1969@aol.com")
      .setSubject("Test email with HTML")
      .setText("this is a message")
      .setHtml("<a href=\"http://vertx.io\">vertx.io</a>");

    List<MailAttachment> list=new ArrayList<MailAttachment>();

    list.add(new MailAttachment()
      .setData(image)
      .setName("logo-white-big.png")
      .setContentType("image/png")
      .setDisposition("inline")
      .setDescription("logo of vert.x web page"));

    list.add(new MailAttachment()
      .setData(Buffer.buffer("this is a text attachment"))
      .setName("file.txt")
      .setContentType("text/plain")
      .setDisposition("attachment")
      .setDescription("some text"));

    list.add(new MailAttachment()
      .setData(TestUtils.asBuffer(0xD0, 0x97, 0xD0, 0xBD, 0xD0, 0xB0, 0xD0, 0xBC, 0xD0, 0xB5, 0xD0, 0xBD, 0xD0, 0xB8, 0xD1, 0x82, 0xD0, 0xBE, 0xD1, 0x81, 0xD1, 0x82, 0xD0, 0xB8))
      .setName("file2.txt")
      .setContentType("text/plain; charset=utf-8")
      .setDisposition("attachment")
      .setDescription("russian text"));

    email.setAttachment(list);

    mailService.sendMail(email, result -> {
      log.info("mail finished");
      if (result.succeeded()) {
        log.info(result.result().toString());
        testComplete();
      } else {
        log.warn("got exception", result.cause());
        fail("got exception "+result.cause());
      }
    });

    await();
  }
}

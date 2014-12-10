package io.vertx.ext.mail;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;

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
  Logger log = LoggerFactory.getLogger(this.getClass());

  CountDownLatch latch;

//  @Ignore
  @Test
  public void mailTest() {
    log.info("starting");

    latch = new CountDownLatch(1);

    try {
      // this is a hack to avoid putting an actual account into the test
      // script, you will have to put your own account into the file
      // or write the account data directly into the java code
      // or a vertx conf file
      Properties account = new Properties();
      InputStream inputstream = new FileInputStream("account.properties");
      account.load(inputstream);

      JsonObject mailConfig = new JsonObject();

      mailConfig.put("hostname", "mail.arcor.de");
      mailConfig.put("port", 587);
      mailConfig.put("starttls", "required");
      mailConfig.put("login", "required");
      mailConfig.put("username", account.getProperty("username"));
      mailConfig.put("password", account.getProperty("pw"));

      MailService mailService = MailService.create(vertx, mailConfig);

      JsonObject email = new JsonObject();
      email.put("from", "lehmann333@arcor.de");
      email.put("recipient", "lehmann333@arcor.de");
      email.put("bounceAddress", "user@example.com");
      email.put("subject", "Test email with HTML");
      // message to exceed SIZE limit (48000000 for our server)
      // 46 Bytes
      StringBuilder sb=new StringBuilder("*********************************************\n");
      // multiply by 2**20
      for(int i=0;i<20;i++) {
        sb.append(sb);
      }
      String message=sb.toString();
      log.info("message size "+message.length());
      email.put("text", message);

      mailService.sendMail(email, v -> {
        log.info("mail finished");
        if(v!=null) {
          if(v.succeeded()) {
            log.info(v.result().toString());
          } else {
            log.warn("got exception", v.cause());
          }
        }
        latch.countDown();
      });

      latch.await();
    } catch (IOException | InterruptedException ioe) {
      log.error("IOException", ioe);
    }
  }

}

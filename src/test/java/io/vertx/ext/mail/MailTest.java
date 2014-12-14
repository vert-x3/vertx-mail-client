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

      String username=null;
      String password=null;

      if(new File("account.properties").exists()) {
        Properties account = new Properties();
        try(InputStream inputstream= new FileInputStream("account.properties")) {
          account.load(inputstream);
          username = account.getProperty("username");
          password = account.getProperty("password");
        };
      }
      else if("true".equals(System.getenv("DRONE"))) {
        // assume we are running inside drone.io
        // and get the credentials from environment
        username=System.getenv("SMTP_USERNAME");
        password=System.getenv("SMTP_PASSWORD");
      }

      // if username is null, auth will fail in the smtp dialog since we set
      // LoginOption.REQUIRED
      if(username==null) {
        log.warn("auth account unavailable");
      }

      MailConfig mailConfig = ServerConfigs.configMailgun();
      mailConfig.setUsername(username);
      mailConfig.setPassword(password);

      MailService mailService = MailService.create(vertx, mailConfig);

      JsonObject email = new JsonObject();
      email.put("from", "lehmann333@arcor.de");
      email.put("recipient", "lehmann333@arcor.de");
      email.put("bounceAddress", "postmaster@mailgun.lehmann.cx");
      email.put("subject", "Test email with HTML");
//      // message to exceed SIZE limit (48000000 for our server)
//      // 46 Bytes
//      StringBuilder sb=new StringBuilder("*********************************************\n");
//      // multiply by 2**20
//      for(int i=0;i<20;i++) {
//        sb.append(sb);
//      }
//      String message=sb.toString();
      String message="this is a message";
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
    } catch (InterruptedException | IOException ioe) {
      log.error("IOException", ioe);
    }
  }
}

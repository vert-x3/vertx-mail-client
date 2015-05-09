package io.vertx.ext.mail;

import io.vertx.core.net.JksOptions;
import io.vertx.core.net.NetClientOptions;

import java.io.IOException;

import javax.mail.MessagingException;

import org.junit.Test;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 * this test uses a local SMTP server (wiser from subethasmtp)
 * since this server supports SSL/TLS, the tests relating to that are here
 */
public class MailLocalTest extends SMTPTestWiser {

  @Test
  public void mailTest() {

    testSuccess(mailServiceLogin(), exampleMessage(), assertExampleMessage());
  }

  @Test
  public void mailTestTLSTrustAll() {

    MailService mailService = MailService.create(vertx,
        configLogin().setStarttls(StarttlsOption.REQUIRED).setTrustAll(true));

    testSuccess(mailService, exampleMessage(), assertExampleMessage());
  }

  @Test
  public void mailTestTLSNoTrust() throws MessagingException, IOException {

    MailService mailService = MailService.create(vertx,
        configLogin().setStarttls(StarttlsOption.REQUIRED));

    testException(mailService, exampleMessage());
  }

  @Test
  public void mailTestTLSCorrectCert() {
    NetClientOptions netClientOptions = new NetClientOptions().setTrustStoreOptions(new JksOptions().setPath(
        "src/test/resources/certs/keystore.jks").setPassword("password"));

    MailService mailService = MailService.create(vertx,
        configLogin()
          .setStarttls(StarttlsOption.REQUIRED)
          .setNetClientOptions(netClientOptions));

    testSuccess(mailService, exampleMessage(), assertExampleMessage());
  }

}

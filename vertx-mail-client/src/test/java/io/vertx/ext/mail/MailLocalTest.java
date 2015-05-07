package io.vertx.ext.mail;

import io.vertx.core.net.JksOptions;
import io.vertx.core.net.NetClientOptions;
import org.junit.Test;

import javax.mail.MessagingException;
import java.io.IOException;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *         <p>
 *         this test uses a local SMTP server (wiser from subethasmtp)
 *         since this server supports SSL/TLS, the tests relating to that are here
 */
public class MailLocalTest extends SMTPTestWiser {

  @Test
  public void mailTest() {

    testSuccess(mailServiceLogin(), exampleMessage(), assertExampleMessage());
  }

  @Test
  public void mailTestTLSTrustAll() {

    MailClient mailService = MailClient.create(vertx,
      configLogin().setStarttls(StartTLSOptions.REQUIRED).setTrustAll(true));

    testSuccess(mailService, exampleMessage(), assertExampleMessage());
  }

  @Test
  public void mailTestTLSNoTrust() throws MessagingException, IOException {

    MailClient mailService = MailClient.create(vertx,
      configLogin().setStarttls(StartTLSOptions.REQUIRED));

    testException(mailService, exampleMessage());
  }

  @Test
  public void mailTestTLSCorrectCert() {
    NetClientOptions netClientOptions = new NetClientOptions().setTrustStoreOptions(new JksOptions().setPath(
      "src/test/resources/certs/keystore.jks").setPassword("password"));

    MailClient mailService = MailClient.create(vertx,
      configLogin()
        .setStarttls(StartTLSOptions.REQUIRED)
        .setNetClientOptions(netClientOptions));

    testSuccess(mailService, exampleMessage(), assertExampleMessage());
  }

}

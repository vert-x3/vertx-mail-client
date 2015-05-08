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

    testSuccess(mailClientLogin(), exampleMessage(), assertExampleMessage());
  }

  @Test
  public void mailTestTLSTrustAll() {

    MailClient mailClient = MailClient.create(vertx,
      configLogin().setStarttls(StartTLSOptions.REQUIRED).setTrustAll(true));

    testSuccess(mailClient, exampleMessage(), assertExampleMessage());
  }

  @Test
  public void mailTestTLSNoTrust() throws MessagingException, IOException {

    MailClient mailClient = MailClient.create(vertx,
      configLogin().setStarttls(StartTLSOptions.REQUIRED));

    testException(mailClient, exampleMessage());
  }

  @Test
  public void mailTestTLSCorrectCert() {
    NetClientOptions netClientOptions = new NetClientOptions().setTrustStoreOptions(new JksOptions().setPath(
      "src/test/resources/certs/keystore.jks").setPassword("password"));

    MailClient mailClient = MailClient.create(vertx,
      configLogin()
        .setStarttls(StartTLSOptions.REQUIRED)
        .setNetClientOptions(netClientOptions));

    testSuccess(mailClient, exampleMessage(), assertExampleMessage());
  }

}

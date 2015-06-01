package io.vertx.ext.mail;

import org.junit.Test;

/**
 * this test uses a local SMTP server (wiser from subethasmtp) since this server supports SSL/TLS, the tests relating to
 * that are here
 * 
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
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
  public void mailTestTLSNoTrust() {
    MailClient mailClient = MailClient.create(vertx, configLogin().setStarttls(StartTLSOptions.REQUIRED));
    testException(mailClient, exampleMessage());
  }

  @Test
  public void mailTestTLSCorrectCert() {
    MailClient mailClient = MailClient.create(vertx,
        configLogin().setStarttls(StartTLSOptions.REQUIRED).setKeyStore("src/test/resources/certs/keystore.jks")
            .setKeyStorePassword("password"));
    testSuccess(mailClient, exampleMessage(), assertExampleMessage());
  }

}

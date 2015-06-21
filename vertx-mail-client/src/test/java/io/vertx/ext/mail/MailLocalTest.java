package io.vertx.ext.mail;

import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * this test uses a local SMTP server (wiser from subethasmtp) since this server supports SSL/TLS, the tests relating to
 * that are here
 * 
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
@RunWith(VertxUnitRunner.class)
public class MailLocalTest extends SMTPTestWiser {

  @Test
  public void mailTest(TestContext testContext) {
    this.testContext=testContext;
    testSuccess(mailClientLogin(), exampleMessage(), assertExampleMessage());
  }

  @Test
  public void mailTestTLSTrustAll(TestContext testContext) {
    this.testContext=testContext;
    MailClient mailClient = MailClient.createShared(vertx,
        configLogin().setStarttls(StartTLSOptions.REQUIRED).setTrustAll(true));
    testSuccess(mailClient, exampleMessage(), assertExampleMessage());
  }

  @Test
  public void mailTestTLSNoTrust(TestContext testContext) {
    this.testContext=testContext;
    MailClient mailClient = MailClient.createShared(vertx, configLogin().setStarttls(StartTLSOptions.REQUIRED));
    testException(mailClient, exampleMessage());
  }

  @Test
  public void mailTestTLSCorrectCert(TestContext testContext) {
    this.testContext=testContext;
    MailClient mailClient = MailClient.createShared(vertx,
        configLogin().setStarttls(StartTLSOptions.REQUIRED).setKeyStore("src/test/resources/certs/keystore.jks")
            .setKeyStorePassword("password"));
    testSuccess(mailClient, exampleMessage(), assertExampleMessage());
  }

}

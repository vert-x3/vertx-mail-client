package io.vertx.ext.mail;

import org.junit.After;
import org.junit.Before;

/**
 * Start/stop a dummy test server for each test
 *
 * the server is currently a rather simple fake server that
 * just writes the replies to the socket and exits after 10 seconds
 *
 * TODO: the class currently isn't really abstract, maybe it would be better to make is regular class
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
public abstract class SMTPTestDummy extends SMTPTestBase {

  protected TestSmtpServer smtpServer;

  @Before
  public void startSMTP() {
    smtpServer = new TestSmtpServer(vertx);
  }

  @After
  public void stopSMTP() {
    smtpServer.stop();
  }

}

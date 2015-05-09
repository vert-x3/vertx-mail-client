package io.vertx.ext.mail;

import org.junit.After;
import org.junit.Before;

/**
 * Start/stop a dummy test server for each test
 *
 * the server is currently a rather simple fake server that
 * just writes the replies to the socket and exits after 10 seconds
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
public class SMTPTestDummy extends SMTPTestBase {

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

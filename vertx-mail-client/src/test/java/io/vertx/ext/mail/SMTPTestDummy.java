package io.vertx.ext.mail;

/**
 * Start/stop a dummy test server for each test
 * <p>
 * the server is currently a rather simple fake server that writes lines to the socket and checks the commands by
 * substring or regexp
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
public class SMTPTestDummy extends SMTPTestBase {

  protected TestSmtpServer smtpServer;

  protected void startSMTP() {
    smtpServer = new TestSmtpServer(vertx);
  }

  protected void stopSMTP() {
    smtpServer.stop();
  }

}

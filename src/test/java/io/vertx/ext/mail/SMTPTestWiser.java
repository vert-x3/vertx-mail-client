package io.vertx.ext.mail;

import java.io.IOException;
import java.io.InputStream;

import org.junit.After;
import org.junit.Before;
import org.subethamail.wiser.Wiser;

/**
 * Start/stop a dummy test server for each test
 *
 * the server is Wiser, this supports inspecting the messages that were received in the test code
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
public class SMTPTestWiser extends SMTPTestBase {

  protected Wiser wiser;

  @Before
  public void startSMTP() {
    wiser = new Wiser();
    wiser.setPort(1587);
    wiser.start();
  }

  @After
  public void stopSMTP() {
    if (wiser != null) {
      wiser.stop();
    }
  }

  protected String inputStreamToString(InputStream inputStream) throws IOException {
    StringBuilder sb = new StringBuilder();
    int ch;
    while((ch=inputStream.read())!=-1) {
      sb.append((char) ch);
    }
    return sb.toString();
  }

}

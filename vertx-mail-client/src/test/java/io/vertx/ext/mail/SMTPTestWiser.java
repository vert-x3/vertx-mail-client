package io.vertx.ext.mail;

import static org.hamcrest.core.StringContains.containsString;

import java.security.Security;
import java.util.Arrays;
import java.util.List;

import javax.mail.internet.MimeMessage;

import org.slf4j.LoggerFactory;
import org.subethamail.smtp.AuthenticationHandler;
import org.subethamail.smtp.AuthenticationHandlerFactory;
import org.subethamail.smtp.RejectException;
import org.subethamail.wiser.Wiser;
import org.subethamail.wiser.WiserMessage;

/**
 * Start/stop a dummy test server for each test
 * <p>
 * the server is Wiser, this supports inspecting the messages that were received in the test code
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
public class SMTPTestWiser extends SMTPTestBase {

  protected Wiser wiser;

  protected void startSMTP() {
    wiser = new Wiser();

    wiser.setPort(1587);
    wiser.getServer().setAuthenticationHandlerFactory(new AuthenticationHandlerFactory() {
      /*
       * AUTH PLAIN handler which returns success on any string
       */
      @Override
      public List<String> getAuthenticationMechanisms() {
        return Arrays.asList("PLAIN");
      }

      @Override
      public AuthenticationHandler create() {
        return new AuthenticationHandler() {

          @Override
          public String auth(final String clientInput) throws RejectException {
            LoggerFactory.getLogger(this.getClass()).info(clientInput);
            return null;
          }

          @Override
          public Object getIdentity() {
            return "username";
          }
        };
      }
    });

    Security.setProperty("ssl.SocketFactory.provider", KeyStoreSSLSocketFactory.class.getName());
    wiser.getServer().setEnableTLS(true);

    wiser.start();
  }

  protected void stopSMTP() {
    if (wiser != null) {
      wiser.stop();
    }
  }

  protected AdditionalAsserts assertExampleMessage() {
    return () -> {
      final WiserMessage message = wiser.getMessages().get(0);
      testContext.assertEquals("from@example.com", message.getEnvelopeSender());
      final MimeMessage mimeMessage = message.getMimeMessage();
      assertThat(mimeMessage.getContentType(), containsString("text/plain"));
      testContext.assertEquals("Subject", mimeMessage.getSubject());
      testContext.assertEquals("Message\n", TestUtils.conv2nl(TestUtils.inputStreamToString(mimeMessage.getInputStream())));
    };
  }

}

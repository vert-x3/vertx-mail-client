/**
 *
 */
package io.vertx.ext.mail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import io.vertx.core.buffer.Buffer;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
public class TestUtils {

  private TestUtils() {
  }

  /**
   * @param values
   * @return
   */
  public static Buffer asBuffer(final int... values) {
    byte[] bytes = new byte[values.length];
    for (int i = 0; i < values.length; i++) {
      bytes[i] = (byte) values[i];
    }
    return Buffer.buffer(bytes);
  }

  public static String conv2nl(String string) {
    return string.replace("\r\n", "\n");
  }

  public static MimeMessage getMessage(String mime) throws UnsupportedEncodingException, MessagingException {
    return new MimeMessage(Session.getInstance(new Properties(), null),
        new ByteArrayInputStream(mime.getBytes("ASCII")));
  }

  public static String inputStreamToString(final InputStream inputStream) throws IOException {
    final StringBuilder string = new StringBuilder();
    int ch;
    while ((ch = inputStream.read()) != -1) {
      string.append((char) ch);
    }
    return string.toString();
  }

}

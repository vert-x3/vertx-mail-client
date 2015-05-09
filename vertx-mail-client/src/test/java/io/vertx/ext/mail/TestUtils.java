/**
 *
 */
package io.vertx.ext.mail;

import io.vertx.core.buffer.Buffer;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
public class TestUtils {

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

  private TestUtils() {
  }

  public static String conv2nl(String string) {
    return string.replace("\r\n", "\n");
  }

}

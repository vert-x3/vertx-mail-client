/**
 *
 */
package io.vertx.ext.mail.impl.sasl;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
public class CryptUtils {

  private CryptUtils() {
  }

  protected static String hmacHex(String keyString, String message, String hmac) {
    try {
      SecretKey key = new SecretKeySpec(keyString.getBytes("UTF-8"), hmac);
      Mac mac = Mac.getInstance(key.getAlgorithm());
      mac.init(key);
      return encodeHex(mac.doFinal(message.getBytes("UTF-8")));
    } catch (UnsupportedEncodingException | NoSuchAlgorithmException | InvalidKeyException e) {
      // doesn't happen, auth will fail in that case
      return "";
    }
  }

  /**
   * @param outBytes
   * @return
   */
  protected static String encodeHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      final int v = ((int) b) & 0xff;
      if (v < 16) {
        sb.append('0');
      }
      sb.append(Integer.toHexString(v));
    }
    return sb.toString();
  }

  public static String base64(String string) {
    try {
      // this call does not create multi-line base64 data
      // (if someone uses a password longer than 57 chars or
      // one of the other SASL replies is longer than 76 chars)
      return Base64.getEncoder().encodeToString(string.getBytes("UTF-8"));
    } catch (UnsupportedEncodingException e) {
      // doesn't happen
      return "";
    }
  }

  public static String base64(byte[] data) {
    return Base64.getEncoder().encodeToString(data);
  }

  public static String decodeb64(String string) {
    try {
      return new String(Base64.getDecoder().decode(string), "UTF-8");
    } catch (UnsupportedEncodingException e) {
      // doesn't happen
      return "";
    }
  }

}

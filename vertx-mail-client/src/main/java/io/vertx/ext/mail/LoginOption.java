package io.vertx.ext.mail;

/**
 * possible options for a login into a SMTP server
 * <br>
 * either DISABLED, OPTIONAL or REQUIRED
 * <p>
 * DISABLED means no login will be attempted
 * <p>
 * NONE means a login will be attempted if the server supports in and login credentials are set
 * <p>
 * REQUIRED means that a login will be attempted if the server supports it and the send operation will fail otherwise
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
public enum LoginOption {
  DISABLED,
  NONE,
  REQUIRED;
}

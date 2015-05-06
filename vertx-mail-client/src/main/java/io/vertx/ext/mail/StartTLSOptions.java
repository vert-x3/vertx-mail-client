package io.vertx.ext.mail;

/**
 * possible options for a secure connection using TLS
 * <br>
 * either DISABLED, OPTIONAL or REQUIRED
 *<p>
 * DISABLED means STARTTLS will not be used in any case
 *<p>
 * OPTIONS means STARTTLS will be used if the server supports it and a plain connection will be used otherwise
 * please note that this option is not a secure as it seems since a MITM attacker can remove the STARTTLS line
 * from the capabilities reply.
 *<p>
 * REQUIRED means that STARTTLS will be used if the server supports it and the send operation will fail otherwise
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
public enum StartTLSOptions {
  DISABLED,
  OPTIONAL,
  REQUIRED;
}

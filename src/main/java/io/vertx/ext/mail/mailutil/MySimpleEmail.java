package io.vertx.ext.mail.mailutil;

import org.apache.commons.mail.SimpleEmail;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
public class MySimpleEmail extends SimpleEmail implements BounceGetter {

  public String getBounceAddress() {
    return bounceAddress;
  }

}

package io.vertx.ext.mail.mailutil;

import org.apache.commons.mail.HtmlEmail;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
public class MyHtmlEmail extends HtmlEmail implements BounceGetter {

  public String getBounceAddress() {
    return bounceAddress;
  }
}

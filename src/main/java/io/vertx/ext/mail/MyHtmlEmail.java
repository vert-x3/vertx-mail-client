package io.vertx.ext.mail;

import org.apache.commons.mail.HtmlEmail;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
class MyHtmlEmail extends HtmlEmail implements BounceGetter {

  public String getBounceAddress() {
    return bounceAddress;
  }
}

/*
 * interface to add a getter for bounceAddres for the Email object
 * we can get rid of this as soon as commons-email supports this directly.
 * my issue was fixed and this will probably be in version 1.4
 * https://issues.apache.org/jira/browse/EMAIL-146
 */
package io.vertx.ext.mail.mailutil;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
public interface BounceGetter {
  public String getBounceAddress();
}

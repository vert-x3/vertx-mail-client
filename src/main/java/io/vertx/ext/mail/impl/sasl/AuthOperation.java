/**
 * 
 */
package io.vertx.ext.mail.impl.sasl;


/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
public interface AuthOperation {

  String getName();

  abstract String nextStep(String line);

}

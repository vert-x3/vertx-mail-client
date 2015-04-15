/**
 * 
 */
package io.vertx.ext.mail.impl.sasl;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
public class AuthOperationFactory {

  private final static Logger log = LoggerFactory.getLogger(AuthOperationFactory.class);

  final static Class<?>[] authList = new Class<?>[] {
    AuthDigestMD5.class,
    AuthCramSHA256.class,
    AuthCramSHA1.class,
    AuthCramMD5.class,
    AuthPlain.class,
    AuthLogin.class
  };

  public static AuthOperation createAuth(String username, String password, Set<String> allowedMethods) {
    Class<?> classToUse = null;
    for (Class<?> authClass : authList) {
      Field[] fields = authClass.getDeclaredFields();
      for (Field f : fields) {
        try {
          f.setAccessible(true);
          String fieldName = f.getName();
          if (fieldName.equals("AUTH_NAME")) {
            String authName = (String) f.get(null);
            if (allowedMethods.contains(authName)) {
              classToUse = authClass;
              break;
            }
          }
        } catch (IllegalArgumentException | IllegalAccessException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }
    if (classToUse != null) {
      try {
        log.info(classToUse.getName());
        return (AuthOperation) classToUse.getConstructor(String.class, String.class).newInstance(username, password);
      } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
          | NoSuchMethodException | SecurityException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    return null;
  }

}

package io.vertx.ext.mail.impl.sasl;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
public class AuthOperationFactory {

  private AuthOperationFactory() {
    // Avoid direct instantiation.
  }

  private static final Class<?>[] authList = new Class<?>[] {
      AuthDigestMD5.class,
      AuthCramSHA256.class,
      AuthCramSHA1.class,
      AuthCramMD5.class,
      AuthPlain.class,
      AuthLogin.class
  };

  public static AuthOperation createAuth(String username, String password, Set<String> allowedMethods)
      throws IllegalArgumentException, IllegalAccessException, InstantiationException, InvocationTargetException,
      NoSuchMethodException, SecurityException {
    Class<?> classToUse = null;
    for (Class<?> authClass : authList) {
      Field[] fields = authClass.getDeclaredFields();
      for (Field f : fields) {
        f.setAccessible(true);
        String fieldName = f.getName();
        if ("AUTH_NAME".equals(fieldName)) {
          String authName = (String) f.get(null);
          if (allowedMethods.contains(authName)) {
            classToUse = authClass;
            break;
          }
        }
      }
    }
    if (classToUse != null) {
      return (AuthOperation) classToUse.getConstructor(String.class, String.class).newInstance(username, password);
    }
    return null;
  }

}

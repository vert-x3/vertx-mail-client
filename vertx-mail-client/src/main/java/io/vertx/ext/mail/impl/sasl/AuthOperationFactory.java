/*
 *  Copyright (c) 2011-2015 The original author or authors
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *       The Eclipse Public License is available at
 *       http://www.eclipse.org/legal/epl-v10.html
 *
 *       The Apache License v2.0 is available at
 *       http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 */

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

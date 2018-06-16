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

/**
 *
 */
package io.vertx.ext.mail.impl.sasl;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @author <a href="mailto:plopes@redhat.com">Paulo Lopes</a>
 */
class AuthXOAUTH2 extends AuthBaseClass {

  private static final Logger LOG = LoggerFactory.getLogger(AuthXOAUTH2.class);


  private static final String AUTH_NAME = "XOAUTH2";
  private boolean first;

  /**
   * @param username
   * @param accessToken
   */
  public AuthXOAUTH2(String username, String accessToken) {
    super(username, accessToken);
    first = true;
  }

  /*
   * (non-Javadoc)
   *
   * @see io.vertx.ext.mail.impl.AuthBaseClass#getName()
   */
  @Override
  public String getName() {
    return AUTH_NAME;
  }

  /*
   * (non-Javadoc)
   *
   * @see io.vertx.ext.mail.impl.AuthBaseClass#nextStep(java.lang.String)
   */
  @Override
  public String nextStep(String data) {
    if (first) {
      first = false;
      return "user=" + username + "\1auth=Bearer " + password + "\1\1";
    } else {
      // quick escape
      if (data == null) {
        return null;
      }

      try {
        // expect a JSON message on error
        JsonObject response = new JsonObject(data);
        // the response must contain 3 values
        if (
          response.containsKey("status") &&
          response.containsKey("schemes") &&
          response.containsKey("scope")) {

          LOG.debug("XOAUTH2 Error Response: " + data);
          // if there is a next step we're receiving an error
          // protocol expects a empty response
          return "";
        } else {
          // this is something totally different (return null)
          return null;
        }
      } catch (RuntimeException e) {
        return null;
      }
    }
  }
}

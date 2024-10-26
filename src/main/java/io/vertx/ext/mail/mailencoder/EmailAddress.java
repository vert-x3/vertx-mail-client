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

package io.vertx.ext.mail.mailencoder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * represent a mail address with an email address part and an optional full name e.g. <br>
 * {@code user@example.com} <br>
 * {@code user@example.com (This User)} <br>
 * {@code Another User <other@example.net>} <br>
 * {@code "display(name)" <sample@email.com>}
 * <p>
 * the constructor will validate the address catching format errors like excess spaces, newlines the test is not very
 * strict, for example an IDN address will be considered valid, even though SMTP doesn't work with that yet
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
public class EmailAddress {

  private static final Pattern PATTERN_EMAIL = Pattern.compile("([^(\\s]+) *\\((.*)\\)");
  private static final Pattern PATTERN_EMAIL_ANGLE = Pattern.compile("([^<]*[^< ])? *\\<([^>]*)\\>");
  private static final Pattern PATTERN_EMAIL_INVALID = Pattern.compile("([^\\s,<>]+@[^\\s,<>]+)|");

  public static final String POSTMASTER = "postmaster";

  private final String email;
  private String name;

  /**
   * parse and create an email address
   *
   * @param fullAddress full address string
   * @throws IllegalArgumentException if an address is not valid
   */
  public EmailAddress(String fullAddress) {

    if (fullAddress.contains("(")) {
      Matcher matcher = PATTERN_EMAIL.matcher(fullAddress);
      if (matcher.matches()) {
        email = matcher.group(1);
        name = matcher.group(2);
        // TODO check if the parentheses were in the display name
      } else {
        throw new IllegalArgumentException("invalid email address [" + fullAddress + "]");
      }
    } else if (fullAddress.contains("<")) {
      Matcher matcher = PATTERN_EMAIL_ANGLE.matcher(fullAddress);
      if (matcher.matches()) {
        name = matcher.group(1);
        if (name == null) {
          name = "";
        }
        email = matcher.group(2);
      } else {
        throw new IllegalArgumentException("invalid email address [" + fullAddress + "]");
      }
    } else {
      email = fullAddress;
      name = "";
    }

    // this only catches very simple errors
    // mostly to avoid protocol errors due to spaces and newlines
    if (!PATTERN_EMAIL_INVALID.matcher(email).matches()) {
      throw new IllegalArgumentException("invalid email address [" + fullAddress + "]");
    }

  }

  /**
   * get the email part of the address
   *
   * @return the email
   */
  public String getEmail() {
    return email;
  }

  /**
   * get the name part of the address
   *
   * @return the full name
   */
  public String getName() {
    return name;
  }

  /**
   * get a representation of the address (this is mostly for testing)
   *
   * @return representation of the address
   */
  public String toString() {
    if (name.isEmpty()) {
      return "[" + email + "]";
    } else {
      return "[" + email + "," + name + "]";
    }
  }
}

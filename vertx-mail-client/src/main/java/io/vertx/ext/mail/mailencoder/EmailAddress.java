package io.vertx.ext.mail.mailencoder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * represent a mail address with an email address part and an optional full name
 * e.g. <br>
 * {@code user@example.com} <br>
 * {@code user@example.com (This User)} <br>
 * {@code Another User <other@example.net>}
 * <p>
 * the constructor will validate the address catching format errors like excess
 * spaces, newlines the test is not very strict, for example an IDN address will
 * be considered valid, even though SMTP doesn't work with that yet
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
public class EmailAddress {

  private static final Pattern PATTERN_EMAIL = Pattern.compile("([^(\\s]+) *\\((.*)\\)");
  private static final Pattern PATTERN_EMAIL_ANGLE = Pattern.compile("([^<]*[^< ])? *\\<([^>]*)\\>");
  private static final Pattern PATTERN_EMAIL_INVALID = Pattern.compile("[^\\s,<>]+@[^\\s,<>]+");

  private String email;
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
      } else {
        throw new IllegalArgumentException("invalid email address");
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
        throw new IllegalArgumentException("invalid email address");
      }
    } else {
      email = fullAddress;
      name = "";
    }

    // this only catches very simple errors
    // mostly to avoid protocol errors due to spaces and newlines
    if (!PATTERN_EMAIL_INVALID.matcher(email).matches()) {
      throw new IllegalArgumentException("invalid email address");
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
    return "[" + email + "," + name + "]";
  }
}

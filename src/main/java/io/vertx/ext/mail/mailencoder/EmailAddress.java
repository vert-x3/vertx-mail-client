package io.vertx.ext.mail.mailencoder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmailAddress {

  private static final Pattern PATTERN_EMAIL = Pattern.compile("([^(\\s]+) *\\((.*)\\)");
  private static final Pattern PATTERN_EMAIL_ANGLE = Pattern.compile("([^<]*[^< ])? *\\<([^>]*)\\>");
  private static final Pattern PATTERN_EMAIL_INVALID = Pattern.compile("[^\\s,<>]+@[^\\s,<>]+");
  private String email;
  private String name;

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

  public String getEmail() {
    return email;
  }

  public String getName() {
    return name;
  }

  public String toString() {
    return "[" + email + "," + name + "]";
  }
}

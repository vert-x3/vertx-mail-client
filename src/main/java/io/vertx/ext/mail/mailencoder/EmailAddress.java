package io.vertx.ext.mail.mailencoder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmailAddress {

  private String address; 
  private String name; 

  public EmailAddress(String fullAddress) {

    if(fullAddress.contains("(")) {
      Pattern RE=Pattern.compile("([^(\\s]+) *\\((.*)\\)");
      Matcher matcher=RE.matcher(fullAddress);
      if(matcher.matches()) {
        address=matcher.group(1);
        name=matcher.group(2);
      } else {
        throw new IllegalArgumentException("invalid email address");
      }
    } else {
      address=fullAddress;
      name="";
    }

    // this only catches very simple errors
    // mostly to avoid protocol errors due to spaces and newlines
    if(!address.matches("[^\\s,<>]+@[^\\s,<>]+")) {
      throw new IllegalArgumentException("invalid email address");
    }

  }

  public String getAddress() {
    return address;
  }

  public String getName() {
    return name;
  }

}

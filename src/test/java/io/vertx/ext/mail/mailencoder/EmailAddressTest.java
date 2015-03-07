package io.vertx.ext.mail.mailencoder;

import org.junit.Test;

public class EmailAddressTest {

  @Test
  public void testEmail() {
    new EmailAddress("user@example.com");
    new EmailAddress("user@example.com (a user)");
    new EmailAddress("user@example.com (\u00e4\u00f6\u00fc)");
    // emails with angle brackets are valid, TODO
//    new EmailAddress("User Name <user@example.com>");
    // a few quirky examples, all valid though :-)
    new EmailAddress("user@example.com (\\\"User Name\\\")");
    new EmailAddress("user@example.com (Lastname, Firstname)");
    new EmailAddress("user@example.com (\"User Name\")");
    new EmailAddress("\"Username\" <user@example.com>");
    new EmailAddress("Mailer <>");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmailInvalid() {
    new EmailAddress("");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmailInvalid2() {
    new EmailAddress("localpart");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmailInvalid3() {
    new EmailAddress("localpart@");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmailInvalid4() {
    new EmailAddress("@domain");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmailInvalid5() {
    new EmailAddress("user @domain");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmailInvalid6() {
    new EmailAddress("user\n@domain");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmailInvalid7() {
    new EmailAddress("user1@domain,user2@domain");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmailInvalid8() {
    new EmailAddress("<user1@domain>");
  }

}

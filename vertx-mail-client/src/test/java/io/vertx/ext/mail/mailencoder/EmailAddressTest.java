package io.vertx.ext.mail.mailencoder;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EmailAddressTest {

  @Test
  public void testEmail() {
    checkAddress("user@example.com", "[user@example.com]");
    checkAddress("user@example.com (a user)", "[user@example.com,a user]");
    checkAddress("user@example.com (\u00e4\u00f6\u00fc)", "[user@example.com,\u00e4\u00f6\u00fc]");
    checkAddress("<user@example.com>", "[user@example.com]");
    checkAddress("User Name <user@example.com>", "[user@example.com,User Name]");
    // a few quirky examples, all valid though :-)
    checkAddress("user@example.com (\\\"User Name\\\")", "[user@example.com,\\\"User Name\\\"]");
    checkAddress("user@example.com (Lastname, Firstname)", "[user@example.com,Lastname, Firstname]");
    checkAddress("user@example.com (\"User Name\")", "[user@example.com,\"User Name\"]");
    checkAddress("\"Username\" <user@example.com>", "[user@example.com,\"Username\"]");
    checkAddress("\"Last, First\" <user@example.com>", "[user@example.com,\"Last, First\"]");
    checkAddress("Last, First <user@example.com>", "[user@example.com,Last, First]");
    checkAddress("user@example.com (Last, First)", "[user@example.com,Last, First]");
    // <> can be used as MAIL FROM address
    checkAddress("", "[]");
    checkAddress("<>", "[]");
    checkAddress("Mailer <>", "[,Mailer]");
  }

  private void checkAddress(String input, String string) {
    EmailAddress emailAddress = new EmailAddress(input);
    assertEquals(string, emailAddress.toString());
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
    new EmailAddress("user@example.com (");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmailInvalid9() {
    new EmailAddress("<user@example.com");
  }

}

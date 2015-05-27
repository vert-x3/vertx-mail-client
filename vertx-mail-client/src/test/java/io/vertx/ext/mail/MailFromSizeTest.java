package io.vertx.ext.mail;

import org.junit.Before;
import org.junit.Test;

/**
 * test that the SIZE option is added to the MAIL FROM command when ESMTP SIZE is supported
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
public class MailFromSizeTest extends SMTPTestDummy {

  @Test
  public void mailTest() {

    testSuccess(mailClientDefault(), exampleMessage());
  }

  @Before
  public void startSMTP() {
    super.startSMTP();
    smtpServer.setDialogue(
        "220 example.com ESMTP",
        "EHLO",
        "250-example.com\n" +
            "250-SIZE 1000000\n" +
            "250 PIPELINING",
        "^MAIL FROM:<[^>]+@[^>]+> SIZE=[0-9]+$",
        "250 2.1.0 Ok",
        "RCPT TO:",
        "250 2.1.5 Ok",
        "DATA",
        "354 End data with <CR><LF>.<CR><LF>",
        "250 2.0.0 Ok: queued as ABCDDEF0123456789",
        "QUIT",
        "221 2.0.0 Bye");

    smtpServer.setCloseImmediately(true);
  }

}

package io.vertx.ext.mail;

import org.junit.Test;

/*
 * Test a server that doesn't support EHLO
 */
/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
public class HeloTest extends SMTPTestDummy {

  @Test
  public void mailEhloMissingTest() {
    smtpServer.setAnswers("220 example.com ESMTP",
        "402 4.5.2 Error: command not recognized", 
        "250 example.com",
        "250 2.1.0 Ok", 
        "250 2.1.5 Ok",
        "354 End data with <CR><LF>.<CR><LF>",
        "250 2.0.0 Ok: queued as ABCDDEF0123456789",
        "221 2.0.0 Bye");
    smtpServer.setCloseImmediately(false);

    testSuccess();
  }

  @Test
  public void mailNoEsmtpTest() {
    smtpServer.setAnswers("220 example.com",
        "250 example.com",
        "250 2.1.0 Ok", 
        "250 2.1.5 Ok",
        "354 End data with <CR><LF>.<CR><LF>",
        "250 2.0.0 Ok: queued as ABCDDEF0123456789",
        "221 2.0.0 Bye");
    smtpServer.setCloseImmediately(false);

    testSuccess();
  }

  /*
   * Test what happens when a reply is sent after the QUIT reply
   */
  @Test
  public void replyAfterQuitTest() {
    smtpServer.setAnswers("220 example.com ESMTP",
        // EHLO
        "250-example.com\n" + 
        "250-SIZE 48000000\n" + 
        "250 PIPELINING",
        // MAIL FROM
        "250 2.1.0 Ok",
        // RCPT TO
        "250 2.1.5 Ok",
        // DATA
        "354 End data with <CR><LF>.<CR><LF>",
        // message data
        "250 2.0.0 Ok: queued as ABCDDEF0123456789",
        // QUIT
        "221 2.0.0 Bye",
        // this should not happen:
        "this is unexpected"
        );
    smtpServer.setCloseImmediately(false);

    testSuccess();
  }

  @Test
  public void serverUnavailableTest() {
    smtpServer.setAnswers("400 cannot talk to you right now\r\n");
    smtpServer.setCloseImmediately(true);

    testException();
  }

  @Test
  public void connectionRefusedTest() {
    runTestException(MailService.create(vertx, new MailConfig("localhost", 1588)));
  }

  @Test
  public void stlsMissingTest() {
    smtpServer.setAnswers("220 example.com ESMTP multiline",
        "250-example.com\n" + 
        "250-SIZE 48000000\n" + 
        "250 PIPELINING",
        "250 2.1.0 Ok", 
        "250 2.1.5 Ok",
        "354 End data with <CR><LF>.<CR><LF>",
        "250 2.0.0 Ok: queued as ABCDDEF0123456789",
        "221 2.0.0 Bye");
    smtpServer.setCloseImmediately(false);

    runTestException(mailServiceTLS());
  }

  @Test
  public void closeOnConnectTest() {
    smtpServer.setAnswers("");
    smtpServer.setCloseImmediately(true);

    testException();
  }

  /*
   * test a multiline reply as welcome message. this is done
   * e.g. by the servers from AOL. the service will deny access
   * if the client tries to do PIPELINING before checking the EHLO
   * capabilities
   */
  @Test
  public void mailMultilineWelcomeTest() {
    smtpServer.setAnswers("220-example.com ESMTP multiline\n" +
        "220-this server uses a multi-line welcome message\n" +
        "220 this is supposed to confuse spammers",
        "250-example.com\n" + 
        "250-SIZE 48000000\n" + 
        "250 PIPELINING",
        "250 2.1.0 Ok", 
        "250 2.1.5 Ok",
        "354 End data with <CR><LF>.<CR><LF>",
        "250 2.0.0 Ok: queued as ABCDDEF0123456789",
        "221 2.0.0 Bye");
    smtpServer.setCloseImmediately(false);

    testSuccess();
  }

  /*
   * simulate the server closes the connection immediately after the
   * banner message
   */
  @Test
  public void closeAfterBannerTest() {
    smtpServer.setAnswers("220 example.com ESMTP\r\n");
    smtpServer.setCloseImmediately(true);

    testException();
  }

}

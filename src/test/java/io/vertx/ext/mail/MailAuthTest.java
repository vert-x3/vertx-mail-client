package io.vertx.ext.mail;

import org.junit.Test;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 * auth examples with failures mostly 
 */
public class MailAuthTest extends SMTPTestDummy {

  @Test
  public void authLoginTest() {
    smtpServer.setAnswers("220 example.com ESMTP",
        "250-example.com\n" +
        "250 AUTH LOGIN",
        "334 VXNlcm5hbWU6",
        "334 UGFzc3dvcmQ6",
        "250 2.1.0 Ok",
        "250 2.1.0 Ok",
        "250 2.1.5 Ok",
        "354 End data with <CR><LF>.<CR><LF>",
        "250 2.0.0 Ok: queued as ABCD",
        "221 2.0.0 Bye");

    testSuccess(mailServiceLogin());
  }

  @Test
  public void authLoginFailTest() {
    smtpServer.setAnswers("220 example.com ESMTP",
        "250-example.com\n" +
        "250 AUTH LOGIN",
        "334 VXNlcm5hbWU6",
        "334 UGFzc3dvcmQ6",
        "435 4.7.8 Error: authentication failed: authentication failure");

    testException(mailServiceLogin());
  }

  @Test
  public void authPlainTest() {
    smtpServer.setAnswers("220 example.com ESMTP",
        "250-example.com\n" +
        "250 AUTH PLAIN",
        "250 2.1.0 Ok",
        "250 2.1.0 Ok",
        "250 2.1.5 Ok",
        "354 End data with <CR><LF>.<CR><LF>",
        "250 2.0.0 Ok: queued as ABCD",
        "221 2.0.0 Bye");

    testSuccess(mailServiceLogin());
  }

  @Test
  public void authPlainFailTest() {
    smtpServer.setAnswers("220 example.com ESMTP",
        "250-example.com\n" +
        "250 AUTH PLAIN",
        "435 4.7.8 Error: authentication failed: bad protocol / cancel");

    testException(mailServiceLogin());
  }

  @Test
  public void authCramMD5Test() {
    smtpServer.setAnswers("220 example.com ESMTP",
        "250-example.com\n" +
        "250 AUTH CRAM-MD5",
        "334 PDEyMzQuYWJjZEBleGFtcGxlLmNvbT4=",
        "250 2.1.0 Ok",
        "250 2.1.0 Ok",
        "250 2.1.5 Ok",
        "354 End data with <CR><LF>.<CR><LF>",
        "250 2.0.0 Ok: queued as ABCD",
        "221 2.0.0 Bye");

    testSuccess(mailServiceLogin());
  }

  @Test
  public void authCramMD5FailTest() {
    smtpServer.setAnswers("220 example.com ESMTP",
        "250-example.com\n" +
        "250 AUTH CRAM-MD5",
        "334 PDEyMzQuYWJjZEBleGFtcGxlLmNvbT4=",
        "435 4.7.8 Error: authentication failed: bad protocol / cancel", 
        "250 2.1.0 Ok",
        "250 2.1.5 Ok",
        "354 End data with <CR><LF>.<CR><LF>",
        "250 2.0.0 Ok: queued as ABCD",
        "221 2.0.0 Bye");

    testException(mailServiceLogin());
  }

  @Test
  public void authJunkTest() {
    smtpServer.setAnswers("220 example.com ESMTP",
        "250-example.com\n" +
        "250 AUTH JUNK");

    testException(mailServiceLogin());
  }

  @Test
  public void authLoginMissingTest() {
    smtpServer.setAnswers("220 example.com ESMTP",
        "250-example.com\n" +
        "250 AUTH PLAIN");

    testException(MailService.create(vertx, defaultConfig().setLogin(LoginOption.REQUIRED)));
  }

}

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
    smtpServer.setDialogue("220 example.com ESMTP",
        "EHLO",
        "250-example.com\n" +
        "250 AUTH LOGIN",
        "AUTH LOGIN",
        "334 VXNlcm5hbWU6",
        "eHh4",
        "334 UGFzc3dvcmQ6",
        "eXl5",
        "250 2.1.0 Ok",
        "MAIL FROM",
        "250 2.1.0 Ok",
        "RCPT TO",
        "250 2.1.5 Ok",
        "DATA",
        "354 End data with <CR><LF>.<CR><LF>",
        "250 2.0.0 Ok: queued as ABCD",
        "QUIT",
        "221 2.0.0 Bye");

    testSuccess(mailServiceLogin());
  }

  @Test
  public void authLoginFailTest() {
    smtpServer.setDialogue("220 example.com ESMTP",
        "EHLO",
        "250-example.com\n" +
        "250 AUTH LOGIN",
        "AUTH LOGIN",
        "334 VXNlcm5hbWU6",
        "eHh4",
        "334 UGFzc3dvcmQ6",
        "eXl5",
        "435 4.7.8 Error: authentication failed: authentication failure");

    testException(mailServiceLogin());
  }

  @Test
  public void authLoginStartFailTest() {
    smtpServer.setDialogue("220 example.com ESMTP",
        "EHLO",
        "250-example.com\n" +
        "250 AUTH LOGIN",
        "AUTH LOGIN",
        "555 login is not possible due to some error");

    testException(mailServiceLogin());
  }

  @Test
  public void authLoginUsernameFailTest() {
    smtpServer.setDialogue("220 example.com ESMTP",
        "EHLO",
        "250-example.com\n" +
        "250 AUTH LOGIN",
        "AUTH LOGIN",
        "334 VXNlcm5hbWU6",
        "eHh4",
        "555 login is not possible due to some error");

    testException(mailServiceLogin());
  }

  @Test
  public void authPlainTest() {
    smtpServer.setDialogue("220 example.com ESMTP",
        "EHLO",
        "250-example.com\n" +
        "250 AUTH PLAIN",
        "AUTH PLAIN AHh4eAB5eXk=",
        "250 2.1.0 Ok",
        "MAIL FROM",
        "250 2.1.0 Ok",
        "RCPT TO",
        "250 2.1.5 Ok",
        "DATA",
        "354 End data with <CR><LF>.<CR><LF>",
        "250 2.0.0 Ok: queued as ABCD",
        "QUIT",
        "221 2.0.0 Bye");

    testSuccess(mailServiceLogin());
  }

  @Test
  public void authPlainFailTest() {
    smtpServer.setDialogue("220 example.com ESMTP",
        "EHLO",
        "250-example.com\n" +
        "250 AUTH PLAIN",
        "AUTH PLAIN AHh4eAB5eXk=",
        "435 4.7.8 Error: authentication failed: bad protocol / cancel");

    testException(mailServiceLogin());
  }

  @Test
  public void authCramMD5Test() {
    smtpServer.setDialogue("220 example.com ESMTP",
        "EHLO",
        "250-example.com\n" +
        "250 AUTH CRAM-MD5",
        "AUTH CRAM-MD5",
        "334 PDEyMzQuYWJjZEBleGFtcGxlLmNvbT4=",
        "eHh4IDE2ZGEzMGQ5NmEwNTY4NWQ0MmQ4YzM5ZDlkMDgxOGIx",
        "250 2.1.0 Ok",
        "MAIL FROM",
        "250 2.1.0 Ok",
        "RCPT TO",
        "250 2.1.5 Ok",
        "DATA",
        "354 End data with <CR><LF>.<CR><LF>",
        "250 2.0.0 Ok: queued as ABCD",
        "QUIT",
        "221 2.0.0 Bye");

    testSuccess(mailServiceLogin());
  }

  @Test
  public void authCramMD5StartFailTest() {
    smtpServer.setDialogue("220 example.com ESMTP",
        "EHLO",
        "250-example.com\n" +
        "250 AUTH CRAM-MD5",
        "AUTH CRAM-MD5",
        "555 login is not possible due to some error");

    testException(mailServiceLogin());
  }

  @Test
  public void authCramMD5FailTest() {
    smtpServer.setDialogue("220 example.com ESMTP",
        "EHLO",
        "250-example.com\n" +
        "250 AUTH CRAM-MD5",
        "AUTH CRAM-MD5",
        "334 PDEyMzQuYWJjZEBleGFtcGxlLmNvbT4=",
        "eHh4IDE2ZGEzMGQ5NmEwNTY4NWQ0MmQ4YzM5ZDlkMDgxOGIx",
        "435 4.7.8 Error: authentication failed: bad protocol / cancel");

    testException(mailServiceLogin());
  }

  @Test
  public void authJunkTest() {
    smtpServer.setDialogue("220 example.com ESMTP",
        "EHLO",
        "250-example.com\n" +
        "250 AUTH JUNK");

    testException(mailServiceLogin());
  }

  /**
   * test we have Login REQUIRED but no login data in the config
   */
  @Test
  public void authAuthDataMissingTest() {
    smtpServer.setDialogue("220 example.com ESMTP",
        "EHLO",
        "250-example.com\n" +
        "250 AUTH PLAIN");

    testException(MailService.create(vertx, defaultConfig().setLogin(LoginOption.REQUIRED)));
  }

}

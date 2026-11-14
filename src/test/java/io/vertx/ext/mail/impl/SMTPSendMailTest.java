package io.vertx.ext.mail.impl;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.ext.mail.*;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.mail.Address;
import javax.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.List;

@RunWith(VertxUnitRunner.class)
public class SMTPSendMailTest extends SMTPTestWiser {

  private static final Logger log = LoggerFactory.getLogger(SMTPSendMailTest.class);

  private static final String BARE_LF_REJECTION = "554 bare <LF> received after DATA";

  private final MailConfig config = configNoSSL();
  private SMTPConnectionPool pool;

  @Override
  protected void startSMTP(String factory) {
    startSMTP(factory, true);
  }

  @Override
  protected void stopSMTP() {
    if (pool != null) {
      pool.close();
      pool = null;
    }
    super.stopSMTP();
  }

  private String strRepeat(String s, int length) {
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < length; i++) {
      b.append(s);
    }
    return b.toString();
  }

  /*
   * The 4.x SMTPConnectionPool and SMTPConnection are callback based, so each step is wrapped
   * into a Future to keep the dialogue readable.
   */
  private Future<SMTPConnection> connect() {
    pool = new SMTPConnectionPool(vertx, config);
    Promise<SMTPConnection> promise = Promise.promise();
    pool.getConnection("hostname", promise);
    return promise.future();
  }

  /*
   * Writes a command and hands back the raw reply, which is what SMTPResponse is built from.
   */
  private static Future<String> write(SMTPConnection connection, String command) {
    Promise<String> promise = Promise.promise();
    connection.write(command, promise::complete);
    return promise.future();
  }

  private static Future<Void> writeLine(SMTPConnection connection, String line) {
    Promise<Void> promise = Promise.promise();
    connection.writeLineWithDrainPromise(line, true, promise);
    return promise.future();
  }

  private static Future<String> expectStatusOk(String command, String reply) {
    log.info(command + " response: " + reply);
    if (!new SMTPResponse(reply).isStatusOk()) {
      return Future.failedFuture(command + " was not accepted: " + reply);
    }
    return Future.succeededFuture(reply);
  }

  private static Future<String> expectStatusContinue(String command, String reply) {
    log.info(command + " response: " + reply);
    if (!new SMTPResponse(reply).isStatusContinue()) {
      return Future.failedFuture(command + " did not ask for the data: " + reply);
    }
    return Future.succeededFuture(reply);
  }

  private static Future<String> expectBareLfRejected(String reply) {
    log.info("DATA end response: " + reply);
    if (new SMTPResponse(reply).isStatusOk()) {
      return Future.failedFuture("the bare <LF> was accepted: " + reply);
    }
    if (!reply.startsWith(BARE_LF_REJECTION)) {
      return Future.failedFuture("the message was rejected for another reason: " + reply);
    }
    return Future.succeededFuture(reply);
  }

  private static Future<String> expectAccepted(String reply) {
    log.info("DATA end response: " + reply);
    if (!new SMTPResponse(reply).isStatusOk()) {
      return Future.failedFuture("the message was rejected: " + reply);
    }
    return Future.succeededFuture(reply);
  }

  /*
   * Opens a session up to the point where the server waits for the data of the message.
   */
  private Future<SMTPConnection> dataDialogue(SMTPConnection connection, String messageId) {
    return write(connection, "AUTH PLAIN AHh4eAB5eXk=")
      .compose(reply -> expectStatusOk("AUTH PLAIN", reply))
      .compose(ignore -> write(connection, "MAIL FROM: <from@example.com>"))
      .compose(reply -> expectStatusOk("MAIL FROM", reply))
      .compose(ignore -> write(connection, "RCPT TO: <user@xample.com>"))
      .compose(reply -> expectStatusOk("RCPT TO", reply))
      .compose(ignore -> write(connection, "DATA"))
      .compose(reply -> expectStatusContinue("DATA", reply))
      .compose(ignore -> writeLine(connection, "MIME-Version: 1.0\r\nMessage-ID: <" + messageId + ">"))
      .compose(ignore -> writeLine(connection, "Subject: BareLFDetection"))
      .compose(ignore -> writeLine(connection, "From: from@example.com\r\nTo: user@example.com"))
      .compose(ignore -> writeLine(connection, "Content-Type: text/plain; charset=utf-8\r\nContent-Transfer-Encoding: quoted-printable"))
      .compose(ignore -> writeLine(connection, ""))
      .map(connection);
  }

  @Test
  public void testBareLfDetectionFailing(TestContext testContext) {
    this.testContext = testContext;

    connect()
      .compose(connection -> dataDialogue(connection, "msg.0815@bareLF")
        .compose(ignore -> writeLine(connection, "will send some bare `\\n` as LineEnding here \n"))
        .compose(ignore -> writeLine(connection, "This should be invalid"))
        .compose(ignore -> write(connection, "."))
        .compose(SMTPSendMailTest::expectBareLfRejected)
        .compose(ignore -> write(connection, "QUIT")))
      .mapEmpty()
      .onComplete(testContext.asyncAssertSuccess());
  }

  @Test
  public void testBareLfDetectionEdgeCases(TestContext testContext) {
    this.testContext = testContext;

    connect()
      .compose(connection -> dataDialogue(connection, "msg.0815@bareLF")
        .compose(ignore -> writeLine(connection, "will send some bare `\\n`"))
        // the bare <LF> is the last character of the body
        .compose(ignore -> writeLine(connection, "This should be invalid\n"))
        .compose(ignore -> write(connection, "."))
        .compose(SMTPSendMailTest::expectBareLfRejected)
        .compose(ignore -> write(connection, "QUIT")))
      .mapEmpty()
      .onComplete(testContext.asyncAssertSuccess());
  }

  @Test
  public void testBareLfDetectionSucceed(TestContext testContext) {
    this.testContext = testContext;

    connect()
      .compose(connection -> dataDialogue(connection, "msg.0815.1@bareLF")
        .compose(ignore -> writeLine(connection, "will send some `\\r\\n` as LineEnding here \r\n"))
        .compose(ignore -> writeLine(connection, "This should be ok for us"))
        .compose(ignore -> write(connection, "."))
        .compose(SMTPSendMailTest::expectAccepted)
        .compose(ignore -> write(connection, "QUIT")))
      .mapEmpty()
      .onComplete(testContext.asyncAssertSuccess());
  }

  @Test
  public void testLongRecepientList(TestContext testContext) {
    int recipients = 32;
    String domain = "example.com";
    final String subject = "testLongRecepientList";
    final String text = "Hello testLongRecepientList!";

    this.testContext = testContext;
    MailClient mailClient = mailClientLogin();

    List<String> to = new ArrayList<>();
    for (int i = 0; i < recipients; i++) {
      to.add("user" + i + "@" + domain);
    }

    MailMessage message = exampleMessage()
      .setSubject(subject)
      .setText(text)
      .setTo(to);

    testSuccess(mailClient, message, () -> {
      final MimeMessage mimeMessage = wiser.getMessages().get(0).getMimeMessage();
      String rawSubject = mimeMessage.getHeader("Subject", null);
      assertEquals("raw Subject is not Wrapped", subject, rawSubject);

      String rawTo = mimeMessage.getHeader("To", null);
      assertTrue("raw To contains \\r\\n within the AutoWrapped text", rawTo.contains("\r\n") && !rawTo.endsWith("\r\n"));
    });
  }

  @Test
  public void testLongRecepientName(TestContext testContext) {
    String domain = "example.com";
    final String subject = "testLongRecepientList";
    final String text = "Hello testLongRecepientList!";
    final String toName = strRepeat("U", 1024);
    final String to = "\""+toName+"\" <user@"+domain+">";

    this.testContext = testContext;
    MailClient mailClient = mailClientLogin();

    MailMessage message = exampleMessage()
      .setSubject(subject)
      .setText(text)
      .setTo(to);

    testSuccess(mailClient, message, () -> {
      final MimeMessage mimeMessage = wiser.getMessages().get(0).getMimeMessage();
      String rawSubject = mimeMessage.getHeader("Subject", null);
      assertEquals("raw Subject is not Wrapped", subject, rawSubject);

      String rawTo = mimeMessage.getHeader("To", null);
      assertTrue("raw To contains \\r\\n within the AutoWrapped text", rawTo.contains("\r\n") && !rawTo.endsWith("\r\n"));
    });
  }

  @Test
  public void testLongFromName(TestContext testContext) {
    String domain = "example.com";
    final String subject = "testLongRecepientList";
    final String text = "Hello testLongRecepientList!";
    final String toName = strRepeat("U", 1024);
    final String from = "\""+toName+"\" <user@"+domain+">";

    this.testContext = testContext;
    MailClient mailClient = mailClientLogin();

    MailMessage message = exampleMessage()
      .setSubject(subject)
      .setText(text)
      .setFrom(from);

    testSuccess(mailClient, message, () -> {
      final MimeMessage mimeMessage = wiser.getMessages().get(0).getMimeMessage();
      String rawSubject = mimeMessage.getHeader("Subject", null);
      assertEquals("raw Subject is not Wrapped", subject, rawSubject);

      String rawFrom = mimeMessage.getHeader("From", null);
      assertTrue("raw To contains \\r\\n within the AutoWrapped text", rawFrom.contains("\r\n") && !rawFrom.endsWith("\r\n"));
    });
  }

  @Test
  public void testUtfLongMsg(TestContext testContext) {
    int recipients = 1;
    String domain = "example.com";

    this.testContext = testContext;
    MailClient mailClient = mailClientLogin();

    final String subject = "testUtfLongMsg";
    final String text = strRepeat("ä", 512) + "\n" +
      strRepeat("ö", 1024) + "\n" +
      strRepeat("ü", 2048) + "\n";

    List<String> to = new ArrayList<>();
    for (int i = 0; i < recipients; i++) {
      to.add("user" + i + "@" + domain);
    }


    MailMessage message = exampleMessage()
      .setSubject(subject)
      .setText(text)
      .setTo(to);

    testSuccess(mailClient, message, () -> {
      final MimeMessage mimeMessage = wiser.getMessages().get(0).getMimeMessage();
      String rawSubject = mimeMessage.getHeader("Subject", null);
      assertEquals("raw Subject is not Wrapped", subject, rawSubject);

      String messageText = mimeMessage.getContent().toString();
      assertTrue(messageText.endsWith("\r\n"));
      assertEquals("message text contains \\r\\n 3 Times in AutoWrapped text", 3, messageText.split("\r\n").length);
      assertEquals(text, messageText.replace("\r\n", "\n"));
    });
  }

  @Test
  public void testHugeEmail(TestContext testContext) {
    int recipients = 32;
    String domain = "example.com";

    this.testContext = testContext;
    MailClient mailClient = mailClientLogin();

    final String subject = strRepeat("S", 1024);
    final String text = strRepeat("1", 512) + "\n" +
      strRepeat("2", 1024) + "\n" +
      strRepeat("3", 2048) + "\n";

    List<String> to = new ArrayList<>();
    for (int i = 0; i < recipients; i++) {
      to.add("user" + i + "@" + domain);
    }

    MailMessage message = exampleMessage()
      .setSubject(subject)
      .setText(text)
      .setTo(to);

    testSuccess(mailClient, message, () -> {
      final MimeMessage mimeMessage = wiser.getMessages().get(0).getMimeMessage();
      String rawSubject = mimeMessage.getHeader("Subject", null);
      assertTrue("raw subject contains \\r\\n within the AutoWrapped text", rawSubject.contains("\r\n") && !rawSubject.endsWith("\r\n"));
      assertEquals(subject, mimeMessage.getSubject());

      String messageText = mimeMessage.getContent().toString();
      assertTrue(messageText.endsWith("\r\n"));
      assertEquals("message text contains \\r\\n 3 Times in AutoWrapped text", 3, messageText.split("\r\n").length);
      assertEquals(text, messageText.replace("\r\n", "\n"));

      String rawTo = mimeMessage.getHeader("To", null);
      assertTrue("raw To contains \\r\\n within the AutoWrapped text", rawTo.contains("\r\n") && !rawTo.endsWith("\r\n"));
      assertEquals(recipients, mimeMessage.getAllRecipients().length);
      for (Address recipient : mimeMessage.getAllRecipients()) {
        assertTrue(recipient.toString() + "is in original recipient list", to.contains(recipient.toString()));
      }
    });
  }
}

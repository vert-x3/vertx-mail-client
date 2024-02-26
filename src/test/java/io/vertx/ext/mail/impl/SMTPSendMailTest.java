package io.vertx.ext.mail.impl;

import io.vertx.core.Future;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.ext.mail.*;
import io.vertx.ext.unit.Async;
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

  private final MailConfig config = configNoSSL();

  @Override
  protected void startSMTP(String factory) {
    startSMTP(factory, true);
  }

  private String strRepeat(String s, int length) {
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < length; i++) {
      b.append(s);
    }
    return b.toString();
  }

  @Test
  public void testBareLfDetectionFailing(TestContext testContext) {
    this.testContext = testContext;

    Async async = testContext.async();

    SMTPConnectionPool pool = new SMTPConnectionPool(vertx, config);
    pool.getConnection("hostname").onComplete(connectionResult -> {
      SMTPConnection smtpConnection = connectionResult.result();

      //smtpConnection.setExceptionHandler(log::info);
      smtpConnection.write("AUTH PLAIN AHh4eAB5eXk=")
        .flatMap(result -> {
          log.info("Auth Response: " + result.getValue());
          assertTrue(result.isStatusOk());
          return Future.succeededFuture(result);
        })
        .flatMap(ignore -> smtpConnection.write("MAIL FROM: <from@example.com>"))
        .flatMap(result -> {
          log.info("MAIL FROM Response: " + result.getValue());
          assertTrue(result.isStatusOk());
          return Future.succeededFuture(result);
        })
        .flatMap(ignore -> smtpConnection.write("RCPT TO: <user@xample.com>"))
        .flatMap(result -> {
          log.info("RCPT TO Response: " + result.getValue());
          assertTrue(result.isStatusOk());
          return Future.succeededFuture(result);
        })
        .flatMap((ignore) -> smtpConnection.write("DATA"))
        .flatMap(result -> {
          log.info("DATA Response: " + result.getValue());
          assertTrue(result.isStatusContinue());
          return Future.succeededFuture(result);
        })
        .flatMap((ignore) -> smtpConnection.writeLineWithDrain("MIME-Version: 1.0\r\nMessage-ID: <msg.0815@bareLF>", true))
        .flatMap((ignore) -> smtpConnection.writeLineWithDrain("Subject: BareLFDetection", true))
        .flatMap((ignore) -> smtpConnection.writeLineWithDrain("From: from@example.com\r\nTo: user@example.com", true))
        .flatMap((ignore) -> smtpConnection.writeLineWithDrain("Content-Type: text/plain; charset=utf-8\r\nContent-Transfer-Encoding: quoted-printable", true))
        .flatMap((ignore) -> smtpConnection.writeLineWithDrain("", true))
        .flatMap((ignore) -> smtpConnection.writeLineWithDrain("will send some bare `\\n` as LineEnding here \n", true))
        .flatMap((ignore) -> smtpConnection.writeLineWithDrain("This should be invalid", true))
        .flatMap((ignore) -> smtpConnection.write("."))
        .flatMap(result -> {
          log.info("DATA end Response: " + result.getValue());
          assertFalse(result.isStatusOk());
          return Future.succeededFuture(result);
        })
        .andThen((ignore) -> async.complete());
    });
  }

  @Test
  public void testBareLfDetectionSucceed(TestContext testContext) {
    this.testContext = testContext;

    Async async = testContext.async();

    SMTPConnectionPool pool = new SMTPConnectionPool(vertx, config);
    pool.getConnection("hostname").onComplete(connectionResult -> {
      SMTPConnection smtpConnection = connectionResult.result();

      //smtpConnection.setExceptionHandler(log::info);
      smtpConnection.write("AUTH PLAIN AHh4eAB5eXk=")
        .flatMap(result -> {
          log.info("Auth Response: " + result.getValue());
          assertTrue(result.isStatusOk());
          return Future.succeededFuture(result);
        })
        .flatMap(ignore -> smtpConnection.write("MAIL FROM: <from@example.com>"))
        .flatMap(result -> {
          log.info("MAIL FROM Response: " + result.getValue());
          assertTrue(result.isStatusOk());
          return Future.succeededFuture(result);
        })
        .flatMap(ignore -> smtpConnection.write("RCPT TO: <user@xample.com>"))
        .flatMap(result -> {
          log.info("RCPT TO Response: " + result.getValue());
          assertTrue(result.isStatusOk());
          return Future.succeededFuture(result);
        })
        .flatMap((ignore) -> smtpConnection.write("DATA"))
        .flatMap(result -> {
          log.info("DATA Response: " + result.getValue());
          assertTrue(result.isStatusContinue());
          return Future.succeededFuture(result);
        })
        .flatMap((ignore) -> smtpConnection.writeLineWithDrain("MIME-Version: 1.0\r\nMessage-ID: <msg.0815.1@bareLF>", true))
        .flatMap((ignore) -> smtpConnection.writeLineWithDrain("Subject: BareLFDetection", true))
        .flatMap((ignore) -> smtpConnection.writeLineWithDrain("From: from@example.com\r\nTo: user@example.com", true))
        .flatMap((ignore) -> smtpConnection.writeLineWithDrain("Content-Type: text/plain; charset=utf-8\r\nContent-Transfer-Encoding: quoted-printable", true))
        .flatMap((ignore) -> smtpConnection.writeLineWithDrain("", true))
        .flatMap((ignore) -> smtpConnection.writeLineWithDrain("will send some `\\r\\n` as LineEnding here \r\n", true))
        .flatMap((ignore) -> smtpConnection.writeLineWithDrain("This should be ok for us", true))
        .flatMap((ignore) -> smtpConnection.write("."))
        .flatMap(result -> {
          log.info("DATA end Response: " + result.getValue());
          assertTrue(result.isStatusOk());
          return Future.succeededFuture(result);
        })
        .andThen((ignore) -> async.complete());
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

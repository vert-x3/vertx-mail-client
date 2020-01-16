/*
 *  Copyright (c) 2011-2019 The original author or authors
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *       The Eclipse Public License is available at
 *       http://www.eclipse.org/legal/epl-v10.html
 *
 *       The Apache License v2.0 is available at
 *       http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 */

package io.vertx.ext.mail;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.impl.InboundBuffer;
import io.vertx.ext.mail.impl.dkim.DKIMSigner;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.james.jdkim.DKIMVerifier;
import org.apache.james.jdkim.MockPublicKeyRecordRetriever;
import org.apache.james.jdkim.api.SignatureRecord;
import org.apache.james.jdkim.impl.Message;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.mail.internet.MimeMultipart;
import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.vertx.ext.mail.TestUtils.*;

/**
 * Test sending mails with DKIM enabled.
 *
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
@RunWith(VertxUnitRunner.class)
@org.junit.FixMethodOrder()
public class MailWithDKIMSignTest extends SMTPTestWiser {

  private static final String privateKey = "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAKqSazYC8pj/JQmo\n" +
    "2ep0m3Shs6WGyHarknUzRJxiHWIVl2CvvOz2aCo4QCFk7nHjJbSQigA/xRrQ+Mzg\n" +
    "uNv4n/c+0MjMQscpyhrMYhza89jP3yMRjIEPJxiQzeMgGHTQifiBfB+2a8959YkB\n" +
    "oOJZuoY0TOEyB+Lm3j000B4evsRdAgMBAAECgYAdSw38dZ8iJVdABG6ANExqSEbo\n" +
    "22/b6XU6iXZ0AOmY6apYoXWpoFudPJHO6l2E04SrMNNyXYFFLLQ9wy4cIOOfs5yB\n" +
    "bdZ17tvOqSWT7nsCcuHpUvF89JNXnQvV2xwS6npp/tIuehMfxOxPLdN87Nge7BEy\n" +
    "6DCSW7U72pX9zjl1BQJBANv56R9X+XLWjW6n4s0tZ271XVYI4DlRxQHYHP3B7eLm\n" +
    "4DJtoHk65WU3kfHUeBNy/9TmpC25Gw6WTDco+mOS8wsCQQDGgVPCqhNDUcZYMeOH\n" +
    "X6hm+l8zBeTMF2udQbkl0dRdLFpbMtw3cg+WUjHg3AYv38P2ikSJZzgzdDyZzcxF\n" +
    "Hcc3AkBXoBNm8upg/mpUW/gSdzWuk3rcnKiE7LenZmkWBDw4mHNSYyz7XaSnTx2J\n" +
    "0XMLfFHAgyd/Ny85/lDZ4C7tn0nFAkEAkS2mz9lJa1PUZ05dZPWuGVqF47AszKNY\n" +
    "XlPiEGntEhPNJaQF8TsncT4+IoFouPzDun0XcRKfxOn/JFGiUu5bcwJAGbai+kPl\n" +
    "AoyfGLxOLu40IMNOHKhHOq8cm3dOC+HpQYpx96JGaQPY4kl3fos6e43DGp9vyOxv\n" +
    "VMj5fan+wzHLcw==";

  // the corresponding public key for the private key above.
  private static final String pubKeyStr =
    "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCqkms2AvKY/yUJqNnqdJt0obOl" +
    "hsh2q5J1M0ScYh1iFZdgr7zs9mgqOEAhZO5x4yW0kIoAP8Ua0PjM4Ljb+J/3PtDI" +
    "zELHKcoazGIc2vPYz98jEYyBDycYkM3jIBh00In4gXwftmvPefWJAaDiWbqGNEzh" +
    "Mgfi5t49NNAeHr7EXQIDAQAB";

  private static final String TEXT_BODY = "This is a Multiple Lines Text\r\n.Some lines start with one dot\n..Some" +
    "lines start with 2 dots.\n Some line starts with \n \r\n\t\nspace and ends with space ";

  private static final String HTML_BODY = "this is html text, \r\n <a href=\"http://vertx.io\">vertx.io</a>";

  private final String IDENTITY = "f r;om@example.com";
  private final DKIMSignOptions dkimOptionsBase = new DKIMSignOptions().setPrivateKey(privateKey)
    .setAuid(IDENTITY).setSdid("example.com").setSelector("lgao").setSignAlgo(DKIMSignAlgorithm.RSA_SHA256);

  private MailClient dkimMailClient(DKIMSignOptions dkimOps) {
    return MailClient.create(vertx, configLogin().setEnableDKIM(true).addDKIMSignOption(dkimOps));
  }

  @Test
  public void testMailSimpleSimplePlain(TestContext testContext) {
    this.testContext = testContext;
    MailMessage message = exampleMessage().setText(TEXT_BODY);
    DKIMSignOptions dkimOps = new DKIMSignOptions(dkimOptionsBase)
      .setHeaderCanonic(MessageCanonic.SIMPLE).setBodyCanonic(MessageCanonic.SIMPLE);
    testSuccess(dkimMailClient(dkimOps), message, () -> {
      testContext.assertEquals(TEXT_BODY + "\n", TestUtils.conv2nl(TestUtils.inputStreamToString(wiser.getMessages().get(0).getMimeMessage().getInputStream())));
      testDKIMSign(dkimOps, testContext);
    });
  }

  @Test
  public void testMailSimpleRelaxedPlain(TestContext testContext) {
    this.testContext = testContext;
    MailMessage message = exampleMessage().setText(TEXT_BODY);
    DKIMSignOptions dkimOps = new DKIMSignOptions(dkimOptionsBase)
      .setHeaderCanonic(MessageCanonic.SIMPLE).setBodyCanonic(MessageCanonic.RELAXED);
    testSuccess(dkimMailClient(dkimOps), message, () -> {
      testContext.assertEquals(TEXT_BODY + "\n", TestUtils.conv2nl(TestUtils.inputStreamToString(wiser.getMessages().get(0).getMimeMessage().getInputStream())));
      testDKIMSign(dkimOps, testContext);
    });
  }

  @Test
  public void testMailRelaxedRelaxedPlain(TestContext testContext) {
    this.testContext = testContext;
    MailMessage message = exampleMessage().setText(TEXT_BODY);
    DKIMSignOptions dkimOps = new DKIMSignOptions(dkimOptionsBase)
      .setHeaderCanonic(MessageCanonic.RELAXED).setBodyCanonic(MessageCanonic.RELAXED);
    testSuccess(dkimMailClient(dkimOps), message, () -> {
      testContext.assertEquals(TEXT_BODY + "\n", TestUtils.conv2nl(TestUtils.inputStreamToString(wiser.getMessages().get(0).getMimeMessage().getInputStream())));
      testDKIMSign(dkimOps, testContext);
    });
  }

  @Test
  public void testMailSimpleSimpleMultiHeaderInstances(TestContext testContext) {
    this.testContext = testContext;
    List<String> signedHeaders = Stream.of("From", "Reply-to", "Subject", "To", "Received", "Received").collect(Collectors.toList());
    MailMessage message = exampleMessage().setText(TEXT_BODY).addHeader("Received", "by 2002:ab3:7755:0:0:0:0:0 with SMTP id z21csp2085702lti")
      .addHeader("Received", "by 2002:a05:620a:147c:: with SMTP id j28mr519391qkl.13.1575424987504");
    DKIMSignOptions dkimOps = new DKIMSignOptions(dkimOptionsBase).setSignedHeaders(signedHeaders)
      .setHeaderCanonic(MessageCanonic.SIMPLE).setBodyCanonic(MessageCanonic.SIMPLE);
    testSuccess(dkimMailClient(dkimOps), message, () -> {
      testContext.assertEquals(TEXT_BODY + "\n", TestUtils.conv2nl(TestUtils.inputStreamToString(wiser.getMessages().get(0).getMimeMessage().getInputStream())));
      testDKIMSign(dkimOps, signedHeaders, testContext);
    });
  }

  @Test
  public void testMailSimpleRelaxedMultiHeaderInstances(TestContext testContext) {
    this.testContext = testContext;
    List<String> signedHeaders = Stream.of("From", "Reply-to", "Subject", "To", "Received", "Received").collect(Collectors.toList());
    MailMessage message = exampleMessage().setText(TEXT_BODY).addHeader("Received", "by 2002:ab3:7755:0:0:0:0:0 with SMTP id z21csp2085702lti")
      .addHeader("Received", "by 2002:a05:620a:147c:: with SMTP id j28mr519391qkl.13.1575424987504")
      .addHeader("Received", "by 2005:a15:725a:579c:: with SMTP id j28mR876591qkl.13.1575473987504");
    DKIMSignOptions dkimOps = new DKIMSignOptions(dkimOptionsBase).setSignedHeaders(signedHeaders)
      .setHeaderCanonic(MessageCanonic.SIMPLE).setBodyCanonic(MessageCanonic.RELAXED);
    testSuccess(dkimMailClient(dkimOps), message, () -> {
      testContext.assertEquals(TEXT_BODY + "\n", TestUtils.conv2nl(TestUtils.inputStreamToString(wiser.getMessages().get(0).getMimeMessage().getInputStream())));
      testDKIMSign(dkimOps, signedHeaders, testContext);
    });
  }

  @Test
  public void testMailRelaxedSimpleMultiHeaderInstances(TestContext testContext) {
    this.testContext = testContext;
    List<String> signedHeaders = Stream.of("From", "Reply-to", "Subject", "To", "Received", "Received").collect(Collectors.toList());
    MailMessage message = exampleMessage().setText(TEXT_BODY).addHeader("Received", "by 2002:ab3:7755:0:0:0:0:0 with SMTP id z21csp2085702lti")
      .addHeader("Received", "by 2002:a05:620a:147c:: with SMTP id j28mr519391qkl.13.1575424987504");
    DKIMSignOptions dkimOps = new DKIMSignOptions(dkimOptionsBase).setSignedHeaders(signedHeaders)
      .setHeaderCanonic(MessageCanonic.RELAXED).setBodyCanonic(MessageCanonic.SIMPLE);
    testSuccess(dkimMailClient(dkimOps), message, () -> {
      testContext.assertEquals(TEXT_BODY + "\n", TestUtils.conv2nl(TestUtils.inputStreamToString(wiser.getMessages().get(0).getMimeMessage().getInputStream())));
      testDKIMSign(dkimOps, signedHeaders, testContext);
    });
  }

  @Test
  public void testMailRelaxedRelaxedMultiHeaderInstances(TestContext testContext) {
    this.testContext = testContext;
    List<String> signedHeaders = Stream.of("From", "Reply-to", "Subject", "To", "Received", "Received").collect(Collectors.toList());
    MailMessage message = exampleMessage().setText(TEXT_BODY)
      .addHeader("Received", "by 2002:ab3:7755:0:0:0:0:0 with SMTP id z21csp2085702lti")
      .addHeader("Received", "by 2002:a05:620a:147c:: with SMTP id j28mr519391qkl.13.1575424987504");
    DKIMSignOptions dkimOps = new DKIMSignOptions(dkimOptionsBase).setSignedHeaders(signedHeaders)
      .setHeaderCanonic(MessageCanonic.RELAXED).setBodyCanonic(MessageCanonic.RELAXED);
    testSuccess(dkimMailClient(dkimOps), message, () -> {
      testContext.assertEquals(TEXT_BODY + "\n", TestUtils.conv2nl(TestUtils.inputStreamToString(wiser.getMessages().get(0).getMimeMessage().getInputStream())));
      testDKIMSign(dkimOps, signedHeaders, testContext);
    });
  }

  @Test
  public void testMailRelaxedRelaxedPlainWithLimit(TestContext testContext) {
    this.testContext = testContext;
    MailMessage message = exampleMessage().setText(TEXT_BODY);
    DKIMSignOptions dkimOps = new DKIMSignOptions(dkimOptionsBase).setBodyLimit(100)
      .setHeaderCanonic(MessageCanonic.RELAXED).setBodyCanonic(MessageCanonic.RELAXED);
    testSuccess(dkimMailClient(dkimOps), message, () -> {
      testContext.assertEquals(TEXT_BODY + "\n", TestUtils.conv2nl(TestUtils.inputStreamToString(wiser.getMessages().get(0).getMimeMessage().getInputStream())));
      testDKIMSign(dkimOps, testContext);
    });
  }

  @Test
  public void testMailRelaxedSimplePlainWithLimit(TestContext testContext) {
    this.testContext = testContext;
    MailMessage message = exampleMessage().setText(TEXT_BODY);
    DKIMSignOptions dkimOps = new DKIMSignOptions(dkimOptionsBase).setBodyLimit(20)
      .setHeaderCanonic(MessageCanonic.RELAXED).setBodyCanonic(MessageCanonic.SIMPLE);
    testSuccess(dkimMailClient(dkimOps), message, () -> {
      testContext.assertEquals(TEXT_BODY + "\n", TestUtils.conv2nl(TestUtils.inputStreamToString(wiser.getMessages().get(0).getMimeMessage().getInputStream())));
      testDKIMSign(dkimOps, testContext);
    });
  }

  @Test
  public void testMailRelaxedSimplePlainWithLargeLimit(TestContext testContext) {
    this.testContext = testContext;
    MailMessage message = exampleMessage().setText(TEXT_BODY);
    DKIMSignOptions dkimOps = new DKIMSignOptions(dkimOptionsBase).setBodyLimit(Integer.MAX_VALUE)
      .setHeaderCanonic(MessageCanonic.RELAXED).setBodyCanonic(MessageCanonic.SIMPLE);
    testSuccess(dkimMailClient(dkimOps), message, () -> {
      testContext.assertEquals(TEXT_BODY + "\n", TestUtils.conv2nl(TestUtils.inputStreamToString(wiser.getMessages().get(0).getMimeMessage().getInputStream())));
      testDKIMSign(dkimOps, testContext);
    });
  }

  @Test
  public void testMailRelaxedSimplePlain(TestContext testContext) {
    this.testContext = testContext;
    MailMessage message = exampleMessage().setText(TEXT_BODY);
    DKIMSignOptions dkimOps = new DKIMSignOptions(dkimOptionsBase)
      .setHeaderCanonic(MessageCanonic.RELAXED).setBodyCanonic(MessageCanonic.SIMPLE);
    testSuccess(dkimMailClient(dkimOps), message, () -> {
      testContext.assertEquals(TEXT_BODY + "\n", TestUtils.conv2nl(TestUtils.inputStreamToString(wiser.getMessages().get(0).getMimeMessage().getInputStream())));
      testDKIMSign(dkimOps, testContext);
    });
  }

  @Test
  public void testMailSimpleSimplePlainFullOptions(TestContext testContext) {
    this.testContext = testContext;
    MailMessage message = exampleMessage().setText(TEXT_BODY);
    DKIMSignOptions dkimOps = new DKIMSignOptions(dkimOptionsBase).setExpireTime(2000)
      .setCopiedHeaders(Stream.of("From", "To").collect(Collectors.toList()))
      .setHeaderCanonic(MessageCanonic.SIMPLE).setBodyCanonic(MessageCanonic.SIMPLE);
    testSuccess(dkimMailClient(dkimOps), message, () -> {
      testContext.assertEquals(TEXT_BODY + "\n", TestUtils.conv2nl(TestUtils.inputStreamToString(wiser.getMessages().get(0).getMimeMessage().getInputStream())));
      testDKIMSign(dkimOps, testContext);
    });
  }

  @Test
  public void testMailSPlainNonExistedCopiedHeaders(TestContext testContext) {
    this.testContext = testContext;
    MailMessage message = exampleMessage().setText(TEXT_BODY);
    DKIMSignOptions dkimOps = new DKIMSignOptions(dkimOptionsBase).setExpireTime(2000)
      .setCopiedHeaders(Stream.of("From", "Not-Existed-Header").collect(Collectors.toList()))
      .setHeaderCanonic(MessageCanonic.SIMPLE).setBodyCanonic(MessageCanonic.SIMPLE);
    testException(dkimMailClient(dkimOps), message, RuntimeException.class);
  }

  @Test
  public void testMailSimpleSimpleAttachment(TestContext testContext) {
    this.testContext = testContext;
    Buffer img = vertx.fileSystem().readFileBlocking("logo-white-big.png");
    testContext.assertTrue(img.length() > 0);
    MailAttachment attachment = MailAttachment.create().setName("logo-white-big.png").setData(img);
    MailMessage message = exampleMessage().setText(TEXT_BODY).setAttachment(attachment);

    DKIMSignOptions dkimOps = new DKIMSignOptions(dkimOptionsBase)
      .setHeaderCanonic(MessageCanonic.SIMPLE).setBodyCanonic(MessageCanonic.SIMPLE);
    testSuccess(dkimMailClient(dkimOps), message, () -> {
      final MimeMultipart multiPart = (MimeMultipart)wiser.getMessages().get(0).getMimeMessage().getContent();
      testContext.assertEquals(2, multiPart.getCount());
      testContext.assertEquals(TEXT_BODY, conv2nl(inputStreamToString(multiPart.getBodyPart(0).getInputStream())));
      testContext.assertTrue(Arrays.equals(img.getBytes(), inputStreamToBytes(multiPart.getBodyPart(1).getInputStream())));
      testDKIMSign(dkimOps, testContext);
    });
  }

  @Test
  public void testMailSimpleRelaxedAttachment(TestContext testContext) {
    this.testContext = testContext;
    Buffer img = vertx.fileSystem().readFileBlocking("logo-white-big.png");
    testContext.assertTrue(img.length() > 0);
    MailAttachment attachment = MailAttachment.create().setName("logo-white-big.png").setData(img);
    MailMessage message = exampleMessage().setText(TEXT_BODY).setAttachment(attachment);

    DKIMSignOptions dkimOps = new DKIMSignOptions(dkimOptionsBase)
      .setHeaderCanonic(MessageCanonic.SIMPLE).setBodyCanonic(MessageCanonic.RELAXED);
    testSuccess(dkimMailClient(dkimOps), message, () -> {
      final MimeMultipart multiPart = (MimeMultipart)wiser.getMessages().get(0).getMimeMessage().getContent();
      testContext.assertEquals(2, multiPart.getCount());
      testContext.assertEquals(TEXT_BODY, conv2nl(inputStreamToString(multiPart.getBodyPart(0).getInputStream())));
      testContext.assertTrue(Arrays.equals(img.getBytes(), inputStreamToBytes(multiPart.getBodyPart(1).getInputStream())));
      testDKIMSign(dkimOps, testContext);
    });
  }

  @Test
  public void testMailRelaxedSimpleAttachment(TestContext testContext) {
    this.testContext = testContext;
    Buffer img = vertx.fileSystem().readFileBlocking("logo-white-big.png");
    testContext.assertTrue(img.length() > 0);
    MailAttachment attachment = MailAttachment.create().setName("logo-white-big.png").setData(img);
    MailMessage message = exampleMessage().setText(TEXT_BODY).setAttachment(attachment);

    DKIMSignOptions dkimOps = new DKIMSignOptions(dkimOptionsBase)
      .setHeaderCanonic(MessageCanonic.RELAXED).setBodyCanonic(MessageCanonic.SIMPLE);
    testSuccess(dkimMailClient(dkimOps), message, () -> {
      final MimeMultipart multiPart = (MimeMultipart)wiser.getMessages().get(0).getMimeMessage().getContent();
      testContext.assertEquals(2, multiPart.getCount());
      testContext.assertEquals(TEXT_BODY, conv2nl(inputStreamToString(multiPart.getBodyPart(0).getInputStream())));
      testContext.assertTrue(Arrays.equals(img.getBytes(), inputStreamToBytes(multiPart.getBodyPart(1).getInputStream())));
      testDKIMSign(dkimOps, testContext);
    });
  }

  @Test
  public void testMailRelaxedRelaxedAttachment(TestContext testContext) {
    this.testContext = testContext;
    Buffer img = vertx.fileSystem().readFileBlocking("logo-white-big.png");
    testContext.assertTrue(img.length() > 0);
    MailAttachment attachment = MailAttachment.create().setName("logo-white-big.png").setData(img);
    MailMessage message = exampleMessage().setText(TEXT_BODY).setAttachment(attachment);

    DKIMSignOptions dkimOps = new DKIMSignOptions(dkimOptionsBase)
      .setHeaderCanonic(MessageCanonic.RELAXED).setBodyCanonic(MessageCanonic.RELAXED);
    testSuccess(dkimMailClient(dkimOps), message, () -> {
      final MimeMultipart multiPart = (MimeMultipart)wiser.getMessages().get(0).getMimeMessage().getContent();
      testContext.assertEquals(2, multiPart.getCount());
      testContext.assertEquals(TEXT_BODY, conv2nl(inputStreamToString(multiPart.getBodyPart(0).getInputStream())));
      testContext.assertTrue(Arrays.equals(img.getBytes(), inputStreamToBytes(multiPart.getBodyPart(1).getInputStream())));
      testDKIMSign(dkimOps, testContext);
    });
  }

  @Test
  public void testMailRelaxedRelaxedHtmlWithAttachment(TestContext testContext) {
    this.testContext = testContext;
    Buffer img = vertx.fileSystem().readFileBlocking("logo-white-big.png");
    testContext.assertTrue(img.length() > 0);
    MailAttachment attachment = MailAttachment.create().setName("logo-white-big.png").setData(img);
    MailAttachment inLineAttachment = MailAttachment.create().setName("logo-inline").setData(img);
    MailMessage message = exampleMessage()
      .setText(TEXT_BODY)
      .setHtml(HTML_BODY)
      .setInlineAttachment(inLineAttachment)
      .setAttachment(attachment);

    DKIMSignOptions dkimOps = new DKIMSignOptions(dkimOptionsBase)
      .setHeaderCanonic(MessageCanonic.RELAXED).setBodyCanonic(MessageCanonic.RELAXED);
    testSuccess(dkimMailClient(dkimOps), message, () -> {

      // 1. alternative multi part
      //    1.1: text part
      //    1.2: html multi part with inline attachment
      //       1.2.1: html body part
      //       1.2.2: inline attachment
      // 2. attachment part
      final MimeMultipart multiPart = (MimeMultipart)wiser.getMessages().get(0).getMimeMessage().getContent();
      testContext.assertEquals(2, multiPart.getCount());
      MimeMultipart alternative = (MimeMultipart)multiPart.getBodyPart(0).getContent();
      testContext.assertEquals(2, alternative.getCount());
      testContext.assertEquals(TEXT_BODY, conv2nl(inputStreamToString(alternative.getBodyPart(0).getInputStream())));
      MimeMultipart htmlPart = (MimeMultipart)alternative.getBodyPart(1).getContent();
      testContext.assertEquals(2, htmlPart.getCount());
      testContext.assertEquals(HTML_BODY, conv2nl(inputStreamToString(htmlPart.getBodyPart(0).getInputStream())));
      testContext.assertTrue(Arrays.equals(img.getBytes(), inputStreamToBytes(htmlPart.getBodyPart(1).getInputStream())));

      testContext.assertTrue(Arrays.equals(img.getBytes(), inputStreamToBytes(multiPart.getBodyPart(1).getInputStream())));

      testDKIMSign(dkimOps, testContext);
    });
  }

  @Test
  public void testMailRelaxedRelaxedHtmlWithAttachmentWithLimit(TestContext testContext) {
    this.testContext = testContext;
    Buffer img = vertx.fileSystem().readFileBlocking("logo-white-big.png");
    testContext.assertTrue(img.length() > 0);
    MailAttachment attachment = MailAttachment.create().setName("logo-white-big.png").setData(img);
    MailAttachment inLineAttachment = MailAttachment.create().setName("logo-inline").setData(img);
    MailMessage message = exampleMessage()
      .setText(TEXT_BODY)
      .setHtml(HTML_BODY)
      .setInlineAttachment(inLineAttachment)
      .setAttachment(attachment);

    DKIMSignOptions dkimOps = new DKIMSignOptions(dkimOptionsBase).setBodyLimit(500)
      .setHeaderCanonic(MessageCanonic.RELAXED).setBodyCanonic(MessageCanonic.RELAXED);
    testSuccess(dkimMailClient(dkimOps), message, () -> {

      // 1. alternative multi part
      //    1.1: text part
      //    1.2: html multi part with inline attachment
      //       1.2.1: html body part
      //       1.2.2: inline attachment
      // 2. attachment part
      final MimeMultipart multiPart = (MimeMultipart)wiser.getMessages().get(0).getMimeMessage().getContent();
      testContext.assertEquals(2, multiPart.getCount());
      MimeMultipart alternative = (MimeMultipart)multiPart.getBodyPart(0).getContent();
      testContext.assertEquals(2, alternative.getCount());
      testContext.assertEquals(TEXT_BODY, conv2nl(inputStreamToString(alternative.getBodyPart(0).getInputStream())));
      MimeMultipart htmlPart = (MimeMultipart)alternative.getBodyPart(1).getContent();
      testContext.assertEquals(2, htmlPart.getCount());
      testContext.assertEquals(HTML_BODY, conv2nl(inputStreamToString(htmlPart.getBodyPart(0).getInputStream())));
      testContext.assertTrue(Arrays.equals(img.getBytes(), inputStreamToBytes(htmlPart.getBodyPart(1).getInputStream())));

      testContext.assertTrue(Arrays.equals(img.getBytes(), inputStreamToBytes(multiPart.getBodyPart(1).getInputStream())));

      testDKIMSign(dkimOps, testContext);
    });
  }

  @Test
  public void testMailRelaxedRelaxedHtmlWithAttachmentStream(TestContext testContext) {
    System.setProperty("vertx.mail.attachment.cache.file", "true");
    this.testContext = testContext;
    Buffer img = vertx.fileSystem().readFileBlocking("logo-white-big.png");
    ReadStream<Buffer> stream = vertx.fileSystem().openBlocking("logo-white-big.png", new OpenOptions());
    testContext.assertTrue(img.length() > 0);
    MailAttachment attachment = MailAttachment.create().setName("logo-white-big.png").setSize(img.length()).setStream(stream);
    MailAttachment inLineAttachment = MailAttachment.create().setName("logo-inline").setData(img);
    MailMessage message = exampleMessage()
      .setText(TEXT_BODY)
      .setHtml(HTML_BODY)
      .setInlineAttachment(inLineAttachment)
      .setAttachment(attachment);

    DKIMSignOptions dkimOps = new DKIMSignOptions(dkimOptionsBase)
      .setHeaderCanonic(MessageCanonic.RELAXED).setBodyCanonic(MessageCanonic.RELAXED);
    testSuccess(dkimMailClient(dkimOps), message, () -> {

      // 1. alternative multi part
      //    1.1: text part
      //    1.2: html multi part with inline attachment
      //       1.2.1: html body part
      //       1.2.2: inline attachment
      // 2. attachment part
      final MimeMultipart multiPart = (MimeMultipart)wiser.getMessages().get(0).getMimeMessage().getContent();
      testContext.assertEquals(2, multiPart.getCount());
      MimeMultipart alternative = (MimeMultipart)multiPart.getBodyPart(0).getContent();
      testContext.assertEquals(2, alternative.getCount());
      testContext.assertEquals(TEXT_BODY, conv2nl(inputStreamToString(alternative.getBodyPart(0).getInputStream())));
      MimeMultipart htmlPart = (MimeMultipart)alternative.getBodyPart(1).getContent();
      testContext.assertEquals(2, htmlPart.getCount());
      testContext.assertEquals(HTML_BODY, conv2nl(inputStreamToString(htmlPart.getBodyPart(0).getInputStream())));
      testContext.assertTrue(Arrays.equals(img.getBytes(), inputStreamToBytes(htmlPart.getBodyPart(1).getInputStream())));

      testContext.assertTrue(Arrays.equals(img.getBytes(), inputStreamToBytes(multiPart.getBodyPart(1).getInputStream())));

      testDKIMSign(dkimOps, testContext);
    });
  }

  @Test
  public void testMailSimpleSimpleAttachmentStream(TestContext testContext) {
    System.setProperty("vertx.mail.attachment.cache.file", "true");
    this.testContext = testContext;
    String path = "logo-white-big.png";
    Buffer img = vertx.fileSystem().readFileBlocking(path);
    ReadStream<Buffer> stream = vertx.fileSystem().openBlocking(path, new OpenOptions());
    MailAttachment attachment = MailAttachment.create().setName(path).setStream(stream).setSize(img.length());
    MailMessage message = exampleMessage().setText(TEXT_BODY).setAttachment(attachment);

    DKIMSignOptions dkimOps = new DKIMSignOptions(dkimOptionsBase)
      .setHeaderCanonic(MessageCanonic.SIMPLE).setBodyCanonic(MessageCanonic.SIMPLE);
    testSuccess(dkimMailClient(dkimOps), message, () -> {
      final MimeMultipart multiPart = (MimeMultipart)wiser.getMessages().get(0).getMimeMessage().getContent();
      testContext.assertEquals(2, multiPart.getCount());
      testContext.assertEquals(TEXT_BODY, conv2nl(inputStreamToString(multiPart.getBodyPart(0).getInputStream())));
      testContext.assertTrue(Arrays.equals(img.getBytes(), inputStreamToBytes(multiPart.getBodyPart(1).getInputStream())));
      testDKIMSign(dkimOps, testContext);
    });
  }

  @Test
  public void testMailSimpleRelaxedAttachmentStream(TestContext testContext) {
    System.setProperty("vertx.mail.attachment.cache.file", "false");
    this.testContext = testContext;
    String path = "logo-white-big.png";
    Buffer img = vertx.fileSystem().readFileBlocking(path);
    ReadStream<Buffer> stream = vertx.fileSystem().openBlocking(path, new OpenOptions());
    MailAttachment attachment = MailAttachment.create().setName("logo-white-big.png").setStream(stream).setSize(img.length());
    MailMessage message = exampleMessage().setText(TEXT_BODY).setAttachment(attachment);

    DKIMSignOptions dkimOps = new DKIMSignOptions(dkimOptionsBase)
      .setHeaderCanonic(MessageCanonic.SIMPLE).setBodyCanonic(MessageCanonic.RELAXED);
    testSuccess(dkimMailClient(dkimOps), message, () -> {
      final MimeMultipart multiPart = (MimeMultipart)wiser.getMessages().get(0).getMimeMessage().getContent();
      testContext.assertEquals(2, multiPart.getCount());
      testContext.assertEquals(TEXT_BODY, conv2nl(inputStreamToString(multiPart.getBodyPart(0).getInputStream())));
      testContext.assertTrue(Arrays.equals(img.getBytes(), inputStreamToBytes(multiPart.getBodyPart(1).getInputStream())));
      testDKIMSign(dkimOps, testContext);
    });
  }

  @Test
  public void testMailRelaxedSimpleAttachmentStream(TestContext testContext) {
    System.setProperty("vertx.mail.attachment.cache.file", "true");
    this.testContext = testContext;
    String path = "logo-white-big.png";
    Buffer img = vertx.fileSystem().readFileBlocking(path);
    ReadStream<Buffer> stream = vertx.fileSystem().openBlocking(path, new OpenOptions());
    MailAttachment attachment = MailAttachment.create().setName("logo-white-big.png").setStream(stream).setSize(img.length());
    MailMessage message = exampleMessage().setText(TEXT_BODY).setAttachment(attachment);

    DKIMSignOptions dkimOps = new DKIMSignOptions(dkimOptionsBase)
      .setHeaderCanonic(MessageCanonic.RELAXED).setBodyCanonic(MessageCanonic.SIMPLE);
    testSuccess(dkimMailClient(dkimOps), message, () -> {
      final MimeMultipart multiPart = (MimeMultipart)wiser.getMessages().get(0).getMimeMessage().getContent();
      testContext.assertEquals(2, multiPart.getCount());
      testContext.assertEquals(TEXT_BODY, conv2nl(inputStreamToString(multiPart.getBodyPart(0).getInputStream())));
      testContext.assertTrue(Arrays.equals(img.getBytes(), inputStreamToBytes(multiPart.getBodyPart(1).getInputStream())));
      testDKIMSign(dkimOps, testContext);
    });
  }

  @Test
  public void testMailRelaxedRelaxedAttachmentStream(TestContext testContext) {
    System.setProperty("vertx.mail.attachment.cache.file", "false");
    this.testContext = testContext;
    String path = "logo-white-big.png";
    Buffer img = vertx.fileSystem().readFileBlocking(path);
    ReadStream<Buffer> stream = vertx.fileSystem().openBlocking(path, new OpenOptions());
    MailAttachment attachment = MailAttachment.create().setName("logo-white-big.png").setStream(stream).setSize(img.length());
    MailMessage message = exampleMessage().setText(TEXT_BODY).setAttachment(attachment);

    DKIMSignOptions dkimOps = new DKIMSignOptions(dkimOptionsBase)
      .setHeaderCanonic(MessageCanonic.RELAXED).setBodyCanonic(MessageCanonic.RELAXED);
    testSuccess(dkimMailClient(dkimOps), message, () -> {
      final MimeMultipart multiPart = (MimeMultipart)wiser.getMessages().get(0).getMimeMessage().getContent();
      testContext.assertEquals(2, multiPart.getCount());
      testContext.assertEquals(TEXT_BODY, conv2nl(inputStreamToString(multiPart.getBodyPart(0).getInputStream())));
      testContext.assertTrue(Arrays.equals(img.getBytes(), inputStreamToBytes(multiPart.getBodyPart(1).getInputStream())));
      testDKIMSign(dkimOps, testContext);
    });
  }

  @Test
  public void testMailRelaxedRelaxedAttachmentStreamWithLimit(TestContext testContext) {
    System.setProperty("vertx.mail.attachment.cache.file", "true");
    this.testContext = testContext;
    String path = "logo-white-big.png";
    Buffer img = vertx.fileSystem().readFileBlocking(path);
    ReadStream<Buffer> stream = vertx.fileSystem().openBlocking(path, new OpenOptions());
    MailAttachment attachment = MailAttachment.create().setName("logo-white-big.png").setStream(stream).setSize(img.length());
    MailMessage message = exampleMessage().setText(TEXT_BODY).setAttachment(attachment);

    DKIMSignOptions dkimOps = new DKIMSignOptions(dkimOptionsBase).setBodyLimit(100)
      .setHeaderCanonic(MessageCanonic.RELAXED).setBodyCanonic(MessageCanonic.RELAXED);
    testSuccess(dkimMailClient(dkimOps), message, () -> {
      final MimeMultipart multiPart = (MimeMultipart)wiser.getMessages().get(0).getMimeMessage().getContent();
      testContext.assertEquals(2, multiPart.getCount());
      testContext.assertEquals(TEXT_BODY, conv2nl(inputStreamToString(multiPart.getBodyPart(0).getInputStream())));
      testContext.assertTrue(Arrays.equals(img.getBytes(), inputStreamToBytes(multiPart.getBodyPart(1).getInputStream())));
      testDKIMSign(dkimOps, testContext);
    });
  }

  @Test
  public void testMailRelaxedSimpleAttachmentStreamWithLimit(TestContext testContext) {
    System.setProperty("vertx.mail.attachment.cache.file", "false");
    this.testContext = testContext;
    String path = "logo-white-big.png";
    Buffer img = vertx.fileSystem().readFileBlocking(path);
    ReadStream<Buffer> stream = vertx.fileSystem().openBlocking(path, new OpenOptions());
    MailAttachment attachment = MailAttachment.create().setName("logo-white-big.png").setStream(stream).setSize(img.length());
    MailMessage message = exampleMessage().setText(TEXT_BODY).setAttachment(attachment);

    DKIMSignOptions dkimOps = new DKIMSignOptions(dkimOptionsBase).setBodyLimit(50)
      .setHeaderCanonic(MessageCanonic.RELAXED).setBodyCanonic(MessageCanonic.SIMPLE);
    testSuccess(dkimMailClient(dkimOps), message, () -> {
      final MimeMultipart multiPart = (MimeMultipart)wiser.getMessages().get(0).getMimeMessage().getContent();
      testContext.assertEquals(2, multiPart.getCount());
      testContext.assertEquals(TEXT_BODY, conv2nl(inputStreamToString(multiPart.getBodyPart(0).getInputStream())));
      testContext.assertTrue(Arrays.equals(img.getBytes(), inputStreamToBytes(multiPart.getBodyPart(1).getInputStream())));
      testDKIMSign(dkimOps, testContext);
    });
  }

  private Buffer fakeStreamData() {
    String path = "logo-white-big.png";
    return vertx.fileSystem().readFileBlocking(path);
  }

  private class FakeReadStream implements ReadStream<Buffer> {
    private final InboundBuffer<Buffer> pending;
    private Handler<Void> endHandler;

    private FakeReadStream(Context context) {
      this.pending = new InboundBuffer<Buffer>(context).emptyHandler(v -> checkEnd()).pause();
      context.runOnContext(h -> this.pending.write(fakeStreamData()));
    }

    private void checkEnd() {
      if (this.pending.isEmpty()) {
        if (endHandler != null) {
          endHandler.handle(null);
        }
      }
    }

    @Override
    public synchronized ReadStream<Buffer> exceptionHandler(Handler<Throwable> handler) {
      pending.exceptionHandler(handler);
      return this;
    }

    @Override
    public synchronized ReadStream<Buffer> handler(@Nullable Handler<Buffer> handler) {
      pending.handler(handler);
      return this;
    }

    @Override
    public synchronized ReadStream<Buffer> pause() {
      pending.pause();
      return this;
    }

    @Override
    public synchronized ReadStream<Buffer> resume() {
      pending.resume();
      return this;
    }

    @Override
    public synchronized ReadStream<Buffer> fetch(long amount) {
      pending.fetch(amount);
      return this;
    }

    @Override
    public synchronized ReadStream<Buffer> endHandler(@Nullable Handler<Void> endHandler) {
      this.endHandler = endHandler;
      return this;
    }
  }

  @Test
  public void testMailSimpleSimpleNonFileAttachmentStream(TestContext testContext) {
    System.setProperty("vertx.mail.attachment.cache.file", "false");
    this.testContext = testContext;
    Buffer fakeData = fakeStreamData();
    byte[] fakeDataBytes = fakeData.getBytes();
    ReadStream<Buffer> fakeStream = new FakeReadStream(vertx.getOrCreateContext());
    MailAttachment attachment = MailAttachment.create().setName("FakeStream")
      .setStream(fakeStream)
      .setSize(fakeDataBytes.length);
    MailMessage message = exampleMessage().setText(TEXT_BODY).setAttachment(attachment);

    DKIMSignOptions dkimOps = new DKIMSignOptions(dkimOptionsBase)
      .setHeaderCanonic(MessageCanonic.SIMPLE).setBodyCanonic(MessageCanonic.SIMPLE);
    testSuccess(dkimMailClient(dkimOps), message, () -> {
      final MimeMultipart multiPart = (MimeMultipart)wiser.getMessages().get(0).getMimeMessage().getContent();
      testContext.assertEquals(2, multiPart.getCount());
      testContext.assertEquals(TEXT_BODY, conv2nl(inputStreamToString(multiPart.getBodyPart(0).getInputStream())));
      testContext.assertTrue(Arrays.equals(fakeDataBytes, inputStreamToBytes(multiPart.getBodyPart(1).getInputStream())));
      testDKIMSign(dkimOps, testContext);
    });
  }

  @Test
  public void testMailSimpleSimpleNonFileAttachmentStreamWithLimit(TestContext testContext) {
    System.setProperty("vertx.mail.attachment.cache.file", "false");
    this.testContext = testContext;
    Buffer fakeData = fakeStreamData();
    byte[] fakeDataBytes = fakeData.getBytes();
    ReadStream<Buffer> fakeStream = new FakeReadStream(vertx.getOrCreateContext());
    MailAttachment attachment = MailAttachment.create().setName("FakeStream")
      .setStream(fakeStream)
      .setSize(fakeDataBytes.length);
    MailMessage message = exampleMessage().setText(TEXT_BODY).setAttachment(attachment);

    DKIMSignOptions dkimOps = new DKIMSignOptions(dkimOptionsBase).setBodyLimit(50)
      .setHeaderCanonic(MessageCanonic.SIMPLE).setBodyCanonic(MessageCanonic.SIMPLE);
    testSuccess(dkimMailClient(dkimOps), message, () -> {
      final MimeMultipart multiPart = (MimeMultipart)wiser.getMessages().get(0).getMimeMessage().getContent();
      testContext.assertEquals(2, multiPart.getCount());
      testContext.assertEquals(TEXT_BODY, conv2nl(inputStreamToString(multiPart.getBodyPart(0).getInputStream())));
      testContext.assertTrue(Arrays.equals(fakeDataBytes, inputStreamToBytes(multiPart.getBodyPart(1).getInputStream())));
      testDKIMSign(dkimOps, testContext);
    });
  }

  @Test
  public void testMailSimpleSimpleNonFileAttachmentStreamCacheInFile(TestContext testContext) {
    System.setProperty("vertx.mail.attachment.cache.file", "true");
    this.testContext = testContext;
    Buffer fakeData = fakeStreamData();
    byte[] fakeDataBytes = fakeData.getBytes();
    ReadStream<Buffer> fakeStream = new FakeReadStream(vertx.getOrCreateContext());
    MailAttachment attachment = MailAttachment.create().setName("FakeStream")
      .setStream(fakeStream)
      .setSize(fakeDataBytes.length);
    MailMessage message = exampleMessage().setText(TEXT_BODY).setAttachment(attachment);

    DKIMSignOptions dkimOps = new DKIMSignOptions(dkimOptionsBase)
      .setHeaderCanonic(MessageCanonic.SIMPLE).setBodyCanonic(MessageCanonic.SIMPLE);
    testSuccess(dkimMailClient(dkimOps), message, () -> {
      final MimeMultipart multiPart = (MimeMultipart)wiser.getMessages().get(0).getMimeMessage().getContent();
      testContext.assertEquals(2, multiPart.getCount());
      testContext.assertEquals(TEXT_BODY, conv2nl(inputStreamToString(multiPart.getBodyPart(0).getInputStream())));
      testContext.assertTrue(Arrays.equals(fakeDataBytes, inputStreamToBytes(multiPart.getBodyPart(1).getInputStream())));
      testDKIMSign(dkimOps, testContext);
    });
  }

  @Test
  public void testMailSimpleSimpleNonFileAttachmentStreamCacheInFileWithLimit(TestContext testContext) {
    System.setProperty("vertx.mail.attachment.cache.file", "true");
    this.testContext = testContext;
    Buffer fakeData = fakeStreamData();
    byte[] fakeDataBytes = fakeData.getBytes();
    ReadStream<Buffer> fakeStream = new FakeReadStream(vertx.getOrCreateContext());
    MailAttachment attachment = MailAttachment.create().setName("FakeStream")
      .setStream(fakeStream)
      .setSize(fakeDataBytes.length);
    MailMessage message = exampleMessage().setText(TEXT_BODY).setAttachment(attachment);

    DKIMSignOptions dkimOps = new DKIMSignOptions(dkimOptionsBase).setBodyLimit(50)
      .setHeaderCanonic(MessageCanonic.SIMPLE).setBodyCanonic(MessageCanonic.SIMPLE);
    testSuccess(dkimMailClient(dkimOps), message, () -> {
      final MimeMultipart multiPart = (MimeMultipart)wiser.getMessages().get(0).getMimeMessage().getContent();
      testContext.assertEquals(2, multiPart.getCount());
      testContext.assertEquals(TEXT_BODY, conv2nl(inputStreamToString(multiPart.getBodyPart(0).getInputStream())));
      testContext.assertTrue(Arrays.equals(fakeDataBytes, inputStreamToBytes(multiPart.getBodyPart(1).getInputStream())));
      testDKIMSign(dkimOps, testContext);
    });
  }

  private void testDKIMSign(DKIMSignOptions dkimOps, TestContext ctx) throws Exception {
    testDKIMSign(dkimOps, dkimOptionsBase.getSignedHeaders(), ctx);
  }

  private void testDKIMSign(DKIMSignOptions dkimOps, List<String> signHeaders, TestContext ctx) throws Exception {
    Message jamesMessage = new Message(new ByteArrayInputStream(wiser.getMessages().get(0).getData()));
    List<String> dkimHeaders = jamesMessage.getFields(DKIMSigner.DKIM_SIGNATURE_HEADER);
    ctx.assertEquals(1, dkimHeaders.size());
    String dkimSignTagsList = dkimHeaders.get(0);
    ctx.assertNotNull(dkimSignTagsList);
    Map<String, String> signTags = new HashMap<>();
    Arrays.stream(dkimSignTagsList.substring(dkimSignTagsList.indexOf(":") + 1).split(";")).map(String::trim).forEach(part -> {
      int idx = part.indexOf("=");
      signTags.put(part.substring(0, idx), part.substring(idx + 1));
    });
    ctx.assertEquals("1", signTags.get("v"));
    ctx.assertEquals(DKIMSignAlgorithm.RSA_SHA256.dkimAlgoName(), signTags.get("a"));
    ctx.assertEquals(dkimOps.getHeaderCanonic().canonic() + "/" + dkimOps.getBodyCanonic().canonic(), signTags.get("c"));
    ctx.assertEquals("example.com", signTags.get("d"));
    ctx.assertEquals("lgao", signTags.get("s"));
    ctx.assertEquals(String.join(":", signHeaders), signTags.get("h"));

    MockPublicKeyRecordRetriever recordRetriever = new MockPublicKeyRecordRetriever();
    recordRetriever.addRecord("lgao", "example.com", "v=DKIM1; k=rsa; p=" + pubKeyStr);
    DKIMVerifier dkimVerifier = new DKIMVerifier(recordRetriever);
    List<SignatureRecord> records = dkimVerifier.verify(jamesMessage, jamesMessage.getBodyInputStream());
    SignatureRecord record = records.get(0);
    ctx.assertNotNull(record);
    ctx.assertEquals("lgao", record.getSelector());
    ctx.assertEquals(IDENTITY, record.getIdentity());
    ctx.assertEquals("example.com", record.getDToken());
    ctx.assertEquals("sha-256", record.getHashAlgo());
  }

}

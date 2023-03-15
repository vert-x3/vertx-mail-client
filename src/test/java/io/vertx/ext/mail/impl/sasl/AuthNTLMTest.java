/*
 *  Copyright (c) 2011-2021 The original author or authors
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

package io.vertx.ext.mail.impl.sasl;

import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.SMTPTestDummy;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests NTLM Auth.
 *
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
@RunWith(VertxUnitRunner.class)
public class AuthNTLMTest extends SMTPTestDummy {

  @Test
  public void testAuthName() {
    AuthOperation result = new NTLMAuth("xxx", "yyy", null, null);
    assertEquals("NTLM", result.getName());
  }

  @Test
  public void testAuthUserName() {
    MailConfig config = configLogin().setUsername("CORP\\testuserA").setPassword("test_password");
    AuthOperation auth = new AuthOperationFactory(null).createAuth(config, "NTLM");
    assertNotNull(auth);
    assertTrue(auth instanceof NTLMAuth);
    NTLMAuth ntlmAuth = (NTLMAuth)auth;
    assertEquals("CORP", ntlmAuth.getDomain());
    assertEquals("testuserA", ntlmAuth.username);
    assertEquals("test_password", ntlmAuth.password);

    config = configLogin().setUsername("testuserA").setPassword("test_password").setNtDomain("CORP").setWorkstation("exVM");
    auth = new AuthOperationFactory(null).createAuth(config, "NTLM");
    assertNotNull(auth);
    assertTrue(auth instanceof NTLMAuth);
    ntlmAuth = (NTLMAuth)auth;
    assertEquals("CORP", ntlmAuth.getDomain());
    assertEquals("exVM", ntlmAuth.getWorkstation());
    assertEquals("testuserA", ntlmAuth.username);
    assertEquals("test_password", ntlmAuth.password);

  }

  @Test
  public void testNextStep() throws Exception {
    final AuthOperation auth = new NTLMAuth("xxx", "yyy", null, null);
    assertEquals("TlRMTVNTUAABAAAAAYIIogAAAAAoAAAAAAAAACgAAAAFASgKAAAADw==", auth.nextStep(null));

    String challenge = "TlRMTVNTUAACAAAAFgAWADgAAAA1goriZt7rI6Uq/ccAAAAAAAAAAGwAbABOAAAABQLODgAAAA9FAFgAQwBIAC0AQwBMAEkALQA2ADYAAgAWAEUAWABDAEgALQBDAEwASQAtADYANgABABYARQBYAEMASAAtAEMATABJAC0ANgA2AAQAFgBlAHgAYwBoAC0AYwBsAGkALQA2ADYAAwAWAGUAeABjAGgALQBjAGwAaQAtADYANgAAAAAA";
    assertNotNull(auth.nextStep(challenge));
  }

  @Test
  public void testNTLMAuth(TestContext testContext) {
    this.smtpServer.setDialogue(
            "220 HK2P15301CA0023.outlook.office365.com Microsoft ESMTP MAIL Service",
            "EHLO localhost",
            "250-HK2P15301CA0023.outlook.office365.com Hello [209.132.188.80]\n" +
                    "250-SIZE 157286400\n" +
                    "250-PIPELINING\n" +
                    "250-AUTH NTLM\n" +
                    "250-ENHANCEDSTATUSCODES\n" +
                    "250-CHUNKING\n" +
                    "250 SMTPUTF8",
            "AUTH NTLM TlRMTVNTUAABAAAAAYIIogAAAAAoAAAAAAAAACgAAAAFASgKAAAADw==",
            "334 TlRMTVNTUAACAAAAFgAWADgAAAA1goriZt7rI6Uq/ccAAAAAAAAAAGwAbABOAAAABQLODgAAAA9FAFgAQwBIAC0AQwBMAEkALQA2ADYAAgAWAEUAWABDAEgALQBDAEwASQAtADYANgABABYARQBYAEMASAAtAEMATABJAC0ANgA2AAQAFgBlAHgAYwBoAC0AYwBsAGkALQA2ADYAAwAWAGUAeABjAGgALQBjAGwAaQAtADYANgAAAAAA",
            "^TlRMTVNT[^\n]*",
            "235 2.7.0 Authentication successful",
            "MAIL FROM",
            "250 2.1.0 Ok",
            "RCPT TO",
            "250 2.1.5 Ok",
            "DATA",
            "354 End data with <CR><LF>.<CR><LF>",
            "250 2.0.0 Ok: queued as ABCD",
            "QUIT",
            "221 2.0.0 Bye"
    );
    final MailClient mailClient = MailClient.create(vertx, configLogin().setOwnHostname("localhost").setKeepAlive(false));
    final MailMessage email = exampleMessage();
    mailClient.sendMail(email).onComplete(testContext.asyncAssertSuccess(r2 -> mailClient.close().onComplete(testContext.asyncAssertSuccess())));
  }

}

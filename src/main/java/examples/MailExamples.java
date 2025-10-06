/*
 *  Copyright (c) 2011-2015 The original author or authors
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

package examples;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.docgen.Source;
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials;
import io.vertx.ext.mail.*;

/**
 * code chunks for the adoc documentation
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
@Source
public class MailExamples {

  public void createSharedClient(Vertx vertx) {
    MailConfig config = new MailConfig();
    MailClient mailClient = MailClient
      .createShared(vertx, config, "exampleclient");
  }

  public void createNonSharedClient(Vertx vertx) {
    MailConfig config = new MailConfig();
    MailClient mailClient = MailClient.create(vertx, config);
  }

  public void createClient2(Vertx vertx) {
    MailConfig config = new MailConfig();
    config.setHostname("mail.example.com");
    config.setPort(587);
    config.setStarttls(StartTLSOptions.REQUIRED);
    config.setUsername("user");
    config.setPassword("password");
    MailClient mailClient = MailClient.create(vertx, config);
  }

  public void dynamicCredentials(Vertx vertx) {
    MailConfig config = new MailConfig()
      .setHostname("mail.example.com")
      .setPort(587)
      .setStarttls(StartTLSOptions.REQUIRED);
    MailClient mailClient = MailClient.builder(vertx)
      .with(config)
      .withCredentialsSupplier(() -> {
        Future<UsernamePasswordCredentials> future = retrieveCredentialsAsync();
        return future;
      })
      .build();
  }

  private Future<UsernamePasswordCredentials> retrieveCredentialsAsync() {
    return Future.succeededFuture();
  }

  public void mailMessage() {
    MailMessage message = new MailMessage();
    message.setFrom("user@example.com (Example User)");
    message.setTo("recipient@example.org");
    message.setCc("Another User <another@example.net>");
    message.setText("this is the plain message text");
    message.setHtml("this is html text <a href=\"http://vertx.io\">vertx.io</a>");
  }

  public void attachment(MailMessage message) {
    MailAttachment attachment = MailAttachment.create();
    attachment.setContentType("text/plain");
    attachment.setData(Buffer.buffer("attachment file"));

    message.setAttachment(attachment);
  }

  public void inlineAttachment(MailMessage message) {
    MailAttachment attachment = MailAttachment.create();
    attachment.setContentType("image/jpeg");
    attachment.setData(Buffer.buffer("image data"));
    attachment.setDisposition("inline");
    attachment.setContentId("<image1@example.com>");

    message.setInlineAttachment(attachment);
  }

  public void createDKIMMailClient(Vertx vertx) {
    DKIMSignOptions dkimSignOptions = new DKIMSignOptions();
    dkimSignOptions.setPrivateKey("PKCS8 Private Key Base64 String");
    dkimSignOptions.setAuid("identifier@example.com");
    dkimSignOptions.setSelector("selector");
    dkimSignOptions.setSdid("example.com");
    MailConfig config = new MailConfig()
      .setDKIMSignOption(dkimSignOptions)
      .setEnableDKIM(true);

    MailClient mailClient = MailClient.createShared(vertx, config);
  }

  public void sendMail(MailMessage message, MailClient mailClient) {
    mailClient.sendMail(message)
      .onSuccess(System.out::println)
      .onFailure(Throwable::printStackTrace);
  }
}

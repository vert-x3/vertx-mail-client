/*
 * Copyright (c) 2011-2014 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 *     The Eclipse Public License is available at
 *     http://www.eclipse.org/legal/epl-v10.html
 *
 *     The Apache License v2.0 is available at
 *     http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */
/**
 * = Vert.x Mail (SMTP client implementation)
 *
 * link:apidocs/io/vertx/ext/mail/MailService.html[`MailService`] simple example:
 *
 * [source,java]
 * ----
 * import io.vertx.ext.mail.MailConfig;
 * import io.vertx.ext.mail.MailMessage;
 * import io.vertx.ext.mail.MailService;
 *
 * MailConfig mailConfig = ServerConfigs.configSendgrid()
 *   .setUsername(username)
 *   .setPassword(password);
 *
 * MailService mailService = MailService.create(vertx, mailConfig);
 *
 * MailMessage email = new MailMessage()
 *   .setFrom("address@example.com")
 *   .setTo("address@example.com")
 *   .setSubject("meaningful subject")
 *   .setText("this is a message");
 *
 * mailService.sendMail(email, result -> {
 *   if(result.succeeded()) {
 *     log.info(result.result().toString());
 *   } else {
 *     log.warn("got exception", result.cause());
 *   }
 * });
 * ----
 */
@Document(fileName = "index.adoc")
@GenModule(name = "vertx-mail")
package io.vertx.ext.mail;
import io.vertx.codegen.annotations.GenModule;
import io.vertx.docgen.Document;

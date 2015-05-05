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
 * {@link io.vertx.ext.mail.MailClient} simple example:
 *
 * [source,$lang]
 * ----
 * {@link examples.Examples#example1}
 * ----
 * attachments can be added as Buffer object
 *
 * [source,$lang]
 * ----
 * {@link examples.Examples#example2}
 * ----
 * the service interface can send mails via the eventbus if the service is running
 * on other machine in the cluster
 *
 * [source,$lang]
 * ----
 * {@link examples.Examples#example3}
 * ----
 */
@Document(fileName = "index.adoc")
@GenModule(name = "vertx-mail")
package io.vertx.ext.mail;
import io.vertx.codegen.annotations.GenModule;
import io.vertx.docgen.Document;

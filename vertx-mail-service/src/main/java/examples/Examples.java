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

import io.vertx.core.Vertx;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.MailService;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class Examples {

  public void createService(Vertx vertx) {
    MailService mailService = MailService.createEventBusProxy(vertx, "vertx.mail");
  }

  public void exampleService(Vertx vertx) {
    MailService mailService = MailService.createEventBusProxy(vertx, "vertx.mail");

    MailMessage email = new MailMessage()
      .setFrom("user@example.com")
      .setBounceAddress("bounce@example.com")
      .setTo("user@example.com");

    mailService.sendMail(email, result -> {
      System.out.println("mail finished");
      if (result.succeeded()) {
        System.out.println(result.result());
      } else {
        System.out.println("got exception");
        result.cause().printStackTrace();
      }
    });
  }

}

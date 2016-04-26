/*
 * Copyright 2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package io.vertx.rxjava.ext.mail;

import java.util.Map;
import rx.Observable;
import io.vertx.ext.mail.MailConfig;
import io.vertx.rxjava.core.Vertx;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.MailResult;

/**
 * SMTP mail client for Vert.x
 * <p>
 * A simple asynchronous API for sending mails from Vert.x applications
 *
 * <p/>
 * NOTE: This class has been automatically generated from the {@link io.vertx.ext.mail.MailClient original} non RX-ified interface using Vert.x codegen.
 */

public class MailClient {

  final io.vertx.ext.mail.MailClient delegate;

  public MailClient(io.vertx.ext.mail.MailClient delegate) {
    this.delegate = delegate;
  }

  public Object getDelegate() {
    return delegate;
  }

  /**
   * create a non shared instance of the mail client
   * @param vertx the Vertx instance the operation will be run in
   * @param config MailConfig configuration to be used for sending mails
   * @return MailClient instance that can then be used to send multiple mails
   */
  public static MailClient createNonShared(Vertx vertx, MailConfig config) { 
    MailClient ret = MailClient.newInstance(io.vertx.ext.mail.MailClient.createNonShared((io.vertx.core.Vertx)vertx.getDelegate(), config));
    return ret;
  }

  /**
   * Create a Mail client which shares its data source with any other Mongo clients created with the same
   * pool name
   * @param vertx the Vert.x instance
   * @param config the configuration
   * @param poolName the pool name
   * @return the client
   */
  public static MailClient createShared(Vertx vertx, MailConfig config, String poolName) { 
    MailClient ret = MailClient.newInstance(io.vertx.ext.mail.MailClient.createShared((io.vertx.core.Vertx)vertx.getDelegate(), config, poolName));
    return ret;
  }

  /**
   * Like {@link io.vertx.rxjava.ext.mail.MailClient#createShared} but with the default pool name
   * @param vertx the Vert.x instance
   * @param config the configuration
   * @return the client
   */
  public static MailClient createShared(Vertx vertx, MailConfig config) { 
    MailClient ret = MailClient.newInstance(io.vertx.ext.mail.MailClient.createShared((io.vertx.core.Vertx)vertx.getDelegate(), config));
    return ret;
  }

  /**
   * send a single mail via MailClient
   * @param email MailMessage object containing the mail text, from/to, attachments etc
   * @param resultHandler will be called when the operation is finished or it fails (may be null to ignore the result)
   * @return this MailClient instance so the method can be used fluently
   */
  public MailClient sendMail(MailMessage email, Handler<AsyncResult<MailResult>> resultHandler) { 
    delegate.sendMail(email, new Handler<AsyncResult<io.vertx.ext.mail.MailResult>>() {
      public void handle(AsyncResult<io.vertx.ext.mail.MailResult> ar) {
        if (ar.succeeded()) {
          resultHandler.handle(io.vertx.core.Future.succeededFuture(ar.result()));
        } else {
          resultHandler.handle(io.vertx.core.Future.failedFuture(ar.cause()));
        }
      }
    });
    return this;
  }

  /**
   * send a single mail via MailClient
   * @param email MailMessage object containing the mail text, from/to, attachments etc
   * @return 
   */
  public Observable<MailResult> sendMailObservable(MailMessage email) { 
    io.vertx.rx.java.ObservableFuture<MailResult> resultHandler = io.vertx.rx.java.RxHelper.observableFuture();
    sendMail(email, resultHandler.toHandler());
    return resultHandler;
  }

  /**
   * close the MailClient
   */
  public void close() { 
    delegate.close();
  }


  public static MailClient newInstance(io.vertx.ext.mail.MailClient arg) {
    return arg != null ? new MailClient(arg) : null;
  }
}

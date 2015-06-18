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
import io.vertx.lang.rxjava.InternalHelper;
import rx.Observable;
import io.vertx.ext.mail.MailConfig;
import io.vertx.rxjava.core.Vertx;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.MailResult;


public class MailClient {

  final io.vertx.ext.mail.MailClient delegate;

  public MailClient(io.vertx.ext.mail.MailClient delegate) {
    this.delegate = delegate;
  }

  public Object getDelegate() {
    return delegate;
  }

  public static MailClient createShared(Vertx vertx, MailConfig config) { 
    MailClient ret= MailClient.newInstance(io.vertx.ext.mail.MailClient.createShared((io.vertx.core.Vertx) vertx.getDelegate(), config));
    return ret;
  }

  public static MailClient createNonShared(Vertx vertx, MailConfig config) { 
    MailClient ret= MailClient.newInstance(io.vertx.ext.mail.MailClient.createNonShared((io.vertx.core.Vertx) vertx.getDelegate(), config));
    return ret;
  }

  public MailClient sendMail(MailMessage arg0, Handler<AsyncResult<MailResult>> arg1) { 
    this.delegate.sendMail(arg0, arg1);
    return this;
  }

  public Observable<MailResult> sendMailObservable(MailMessage arg0) { 
    io.vertx.rx.java.ObservableFuture<MailResult> arg1 = io.vertx.rx.java.RxHelper.observableFuture();
    sendMail(arg0, arg1.toHandler());
    return arg1;
  }

  public void close() { 
    this.delegate.close();
  }


  public static MailClient newInstance(io.vertx.ext.mail.MailClient arg) {
    return arg != null ? new MailClient(arg) : null;
  }
}

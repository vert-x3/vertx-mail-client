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

package io.vertx.groovy.ext.mail;
import groovy.transform.CompileStatic
import io.vertx.lang.groovy.InternalHelper
import io.vertx.core.json.JsonObject
import io.vertx.groovy.core.Vertx
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.ext.mail.MailMessage
import io.vertx.ext.mail.MailResult
/**
 * @author <a href="http://tfox.org">Tim Fox</a>
*/
@CompileStatic
public class MailService extends MailClient {
  private final def io.vertx.ext.mail.MailService delegate;
  public MailService(Object delegate) {
    super((io.vertx.ext.mail.MailService) delegate);
    this.delegate = (io.vertx.ext.mail.MailService) delegate;
  }
  public Object getDelegate() {
    return delegate;
  }
  /**
   * create a proxy of  MailService that delegates to the mail service running somewhere else via the event bus
   * @param vertx the Vertx instance the proxy will be run in
   * @param address the eb address of the mail service running somewhere, default is "vertx.mail"
   * @return MailService instance that can then be used to send multiple mails
   */
  public static MailService createEventBusProxy(Vertx vertx, String address) {
    def ret = InternalHelper.safeCreate(io.vertx.ext.mail.MailService.createEventBusProxy(vertx != null ? (io.vertx.core.Vertx)vertx.getDelegate() : null, address), io.vertx.groovy.ext.mail.MailService.class);
    return ret;
  }
  public MailService sendMail(Map<String, Object> email = [:], Handler<AsyncResult<Map<String, Object>>> resultHandler) {
    ((io.vertx.ext.mail.MailService) delegate).sendMail(email != null ? new io.vertx.ext.mail.MailMessage(new io.vertx.core.json.JsonObject(email)) : null, resultHandler != null ? new Handler<AsyncResult<io.vertx.ext.mail.MailResult>>() {
      public void handle(AsyncResult<io.vertx.ext.mail.MailResult> ar) {
        if (ar.succeeded()) {
          resultHandler.handle(io.vertx.core.Future.succeededFuture((Map<String, Object>)InternalHelper.wrapObject(ar.result()?.toJson())));
        } else {
          resultHandler.handle(io.vertx.core.Future.failedFuture(ar.cause()));
        }
      }
    } : null);
    return this;
  }
  public void close() {
    ((io.vertx.ext.mail.MailService) delegate).close();
  }
}

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
import io.vertx.ext.mail.MailConfig
import io.vertx.groovy.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.ext.mail.MailMessage
/**
 * SMTP mail client for Vert.x
 * <p>
 * A simple asynchronous API for sending mails from Vert.x applications
*/
@CompileStatic
public class MailClient {
  final def io.vertx.ext.mail.MailClient delegate;
  public MailClient(io.vertx.ext.mail.MailClient delegate) {
    this.delegate = delegate;
  }
  public Object getDelegate() {
    return delegate;
  }
  /**
   * create an instance of MailClient that is running in the local JVM
   * @param vertx the Vertx instance the operation will be run in
   * @param config MailConfig configuration to be used for sending mails (see <a href="../../../../../../../cheatsheet/MailConfig.html">MailConfig</a>)
   * @return MailClient instance that can then be used to send multiple mails
   */
  public static MailClient create(Vertx vertx, Map<String, Object> config) {
    def ret= new io.vertx.groovy.ext.mail.MailClient(io.vertx.ext.mail.MailClient.create((io.vertx.core.Vertx)vertx.getDelegate(), config != null ? new io.vertx.ext.mail.MailConfig(new io.vertx.core.json.JsonObject(config)) : null));
    return ret;
  }
  /**
   * send a single mail via MailClient
   * @param email MailMessage object containing the mail text, from/to, attachments etc (see <a href="../../../../../../../cheatsheet/MailMessage.html">MailMessage</a>)
   * @param resultHandler will be called when the operation is finished or it fails (may be null to ignore the result) the result JsonObject currently only contains {@code {"result":"success"}}
   * @return this MailClient instance so the method can be used fluently
   */
  public MailClient sendMail(Map<String, Object> email = [:], Handler<AsyncResult<Map<String, Object>>> resultHandler) {
    this.delegate.sendMail(email != null ? new io.vertx.ext.mail.MailMessage(new io.vertx.core.json.JsonObject(email)) : null, new Handler<AsyncResult<io.vertx.core.json.JsonObject>>() {
      public void handle(AsyncResult<io.vertx.core.json.JsonObject> event) {
        AsyncResult<Map<String, Object>> f
        if (event.succeeded()) {
          f = InternalHelper.<Map<String, Object>>result(event.result()?.getMap())
        } else {
          f = InternalHelper.<Map<String, Object>>failure(event.cause())
        }
        resultHandler.handle(f)
      }
    });
    return this;
  }
  /**
   * close the MailClient
   */
  public void close() {
    this.delegate.close();
  }
}

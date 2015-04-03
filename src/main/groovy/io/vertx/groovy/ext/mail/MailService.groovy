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
 * smtp mail service for vert.x
 * 
 * this Interface provides the methods to be used by the application program and is used to
 * generate the service in other languages
*/
@CompileStatic
public class MailService {
  final def io.vertx.ext.mail.MailService delegate;
  public MailService(io.vertx.ext.mail.MailService delegate) {
    this.delegate = delegate;
  }
  public Object getDelegate() {
    return delegate;
  }
  /**
   * create an instance of MailService that is running in the local JVM
   * @param vertx the Vertx instance the operation will be run in
   * @param config MailConfig configuration to be used for sending mails (see <a href="../../../../../../../cheatsheet/MailConfig.html">MailConfig</a>)
   * @return MailService instance that can then be used to send multiple mails
   */
  public static MailService create(Vertx vertx, Map<String, Object> config) {
    def ret= new io.vertx.groovy.ext.mail.MailService(io.vertx.ext.mail.MailService.create((io.vertx.core.Vertx)vertx.getDelegate(), config != null ? new io.vertx.ext.mail.MailConfig(new io.vertx.core.json.JsonObject(config)) : null));
    return ret;
  }
  /**
   * create an instance of  MailService that calls the mail service via the event bus running somewhere else
   * @param vertx the Vertx instance the operation will be run in
   * @param address the eb address of the mail service running somewhere, default is "vertx.mail"
   * @return MailService instance that can then be used to send multiple mails
   */
  public static MailService createEventBusProxy(Vertx vertx, String address) {
    def ret= new io.vertx.groovy.ext.mail.MailService(io.vertx.ext.mail.MailService.createEventBusProxy((io.vertx.core.Vertx)vertx.getDelegate(), address));
    return ret;
  }
  /**
   * send a single mail via MailService
   * @param email MailMessage object containing the mail text, from/to, attachments etc (see <a href="../../../../../../../cheatsheet/MailMessage.html">MailMessage</a>)
   * @param resultHandler will be called when the operation is finished or it fails (may be null to ignore the result) the result JsonObject currently only contains {@code {"result":"success"}}
   * @return this MailService instance so the method can be used fluently
   */
  public MailService sendMail(Map<String, Object> email = [:], Handler<AsyncResult<Map<String, Object>>> resultHandler) {
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
   * send a single mail via MailService that has been pregenerated already
   * <p>
   * this makes it possible to create a mail message with Javamail for example to
   * supports elements that are not supported by the mail encoder in vertx-mail-service
   * @param email MailMessage object containing from/to etc, the message content fields are not evaluated (see <a href="../../../../../../../cheatsheet/MailMessage.html">MailMessage</a>)
   * @param message String object that contains the complete mail note that the From/To headers are not evaluated, rather they are taken from the MailMessage object
   * @param resultHandler will be called when the operation is finished or it fails (may be null to ignore the result) the result JsonObject currently only contains {@code {"result":"success"}}
   * @return this MailService instance so the method can be used fluently
   */
  public MailService sendMailString(Map<String, Object> email = [:], String message, Handler<AsyncResult<Map<String, Object>>> resultHandler) {
    this.delegate.sendMailString(email != null ? new io.vertx.ext.mail.MailMessage(new io.vertx.core.json.JsonObject(email)) : null, message, new Handler<AsyncResult<io.vertx.core.json.JsonObject>>() {
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
   * start the MailServer instance if it is running locally (this operation is currently a no-op)
   */
  public void start() {
    this.delegate.start();
  }
  /**
   * stop the MailServer instance if it is running locally
   * <p>
   * this operation shuts down the connection pool, doesn't wait for completion of the close operations
   * when the mail service is running on the event bus, this operation has no effect
   */
  public void stop() {
    this.delegate.stop();
  }
}

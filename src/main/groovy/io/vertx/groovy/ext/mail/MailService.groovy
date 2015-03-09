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
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
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
  public static MailService create(Vertx vertx, Map<String, Object> config) {
    def ret= MailService.FACTORY.apply(io.vertx.ext.mail.MailService.create((io.vertx.core.Vertx)vertx.getDelegate(), config != null ? new io.vertx.ext.mail.MailConfig(new io.vertx.core.json.JsonObject(config)) : null));
    return ret;
  }
  public static MailService createEventBusProxy(Vertx vertx, String address) {
    def ret= MailService.FACTORY.apply(io.vertx.ext.mail.MailService.createEventBusProxy((io.vertx.core.Vertx)vertx.getDelegate(), address));
    return ret;
  }
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
  public void start() {
    this.delegate.start();
  }
  public void stop() {
    this.delegate.stop();
  }

  static final java.util.function.Function<io.vertx.ext.mail.MailService, MailService> FACTORY = io.vertx.lang.groovy.Factories.createFactory() {
    io.vertx.ext.mail.MailService arg -> new MailService(arg);
  };
}

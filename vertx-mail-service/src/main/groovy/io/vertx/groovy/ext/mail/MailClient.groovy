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
import io.vertx.ext.mail.MailConfig
import io.vertx.groovy.core.Vertx
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.ext.mail.MailMessage
import io.vertx.ext.mail.MailResult
@CompileStatic
public class MailClient {
  private final def io.vertx.ext.mail.MailClient delegate;
  public MailClient(Object delegate) {
    this.delegate = (io.vertx.ext.mail.MailClient) delegate;
  }
  public Object getDelegate() {
    return delegate;
  }
  public static MailClient createNonShared(Vertx vertx, Map<String, Object> config) {
    def ret = InternalHelper.safeCreate(io.vertx.ext.mail.MailClient.createNonShared(vertx != null ? (io.vertx.core.Vertx)vertx.getDelegate() : null, config != null ? new io.vertx.ext.mail.MailConfig(new io.vertx.core.json.JsonObject(config)) : null), io.vertx.groovy.ext.mail.MailClient.class);
    return ret;
  }
  public static MailClient createShared(Vertx vertx, Map<String, Object> config, String poolName) {
    def ret = InternalHelper.safeCreate(io.vertx.ext.mail.MailClient.createShared(vertx != null ? (io.vertx.core.Vertx)vertx.getDelegate() : null, config != null ? new io.vertx.ext.mail.MailConfig(new io.vertx.core.json.JsonObject(config)) : null, poolName), io.vertx.groovy.ext.mail.MailClient.class);
    return ret;
  }
  public static MailClient createShared(Vertx vertx, Map<String, Object> config) {
    def ret = InternalHelper.safeCreate(io.vertx.ext.mail.MailClient.createShared(vertx != null ? (io.vertx.core.Vertx)vertx.getDelegate() : null, config != null ? new io.vertx.ext.mail.MailConfig(new io.vertx.core.json.JsonObject(config)) : null), io.vertx.groovy.ext.mail.MailClient.class);
    return ret;
  }
  public MailClient sendMail(Map<String, Object> arg0 = [:], Handler<AsyncResult<Map<String, Object>>> arg1) {
    delegate.sendMail(arg0 != null ? new io.vertx.ext.mail.MailMessage(new io.vertx.core.json.JsonObject(arg0)) : null, arg1 != null ? new Handler<AsyncResult<io.vertx.ext.mail.MailResult>>() {
      public void handle(AsyncResult<io.vertx.ext.mail.MailResult> ar) {
        if (ar.succeeded()) {
          arg1.handle(io.vertx.core.Future.succeededFuture((Map<String, Object>)InternalHelper.wrapObject(ar.result()?.toJson())));
        } else {
          arg1.handle(io.vertx.core.Future.failedFuture(ar.cause()));
        }
      }
    } : null);
    return this;
  }
  public void close() {
    delegate.close();
  }
}

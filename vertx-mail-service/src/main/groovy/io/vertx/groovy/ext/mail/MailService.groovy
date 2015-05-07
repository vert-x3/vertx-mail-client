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
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.ext.mail.MailMessage
/**
 * @author <a href="http://tfox.org">Tim Fox</a>
*/
@CompileStatic
public class MailService extends MailClient {
  final def io.vertx.ext.mail.MailService delegate;
  public MailService(io.vertx.ext.mail.MailService delegate) {
    super(delegate);
    this.delegate = delegate;
  }
  public Object getDelegate() {
    return delegate;
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
  public void close() {
    this.delegate.close();
  }
}

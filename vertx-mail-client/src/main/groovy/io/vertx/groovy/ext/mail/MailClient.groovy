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
/**
 * SMTP mail client for Vert.x
 * <p>
 * A simple asynchronous API for sending mails from Vert.x applications
*/
@CompileStatic
public class MailClient {
  private final def io.vertx.ext.mail.MailClient delegate;
  public MailClient(Object delegate) {
    this.delegate = (io.vertx.ext.mail.MailClient) delegate;
  }
  public Object getDelegate() {
    return delegate;
  }
  /**
   * create a non shared instance of the mail client
   * @param vertx the Vertx instance the operation will be run in
   * @param config MailConfig configuration to be used for sending mails (see <a href="../../../../../../../cheatsheet/MailConfig.html">MailConfig</a>)
   * @return MailClient instance that can then be used to send multiple mails
   */
  public static MailClient createNonShared(Vertx vertx, Map<String, Object> config) {
    def ret = InternalHelper.safeCreate(io.vertx.ext.mail.MailClient.createNonShared(vertx != null ? (io.vertx.core.Vertx)vertx.getDelegate() : null, config != null ? new io.vertx.ext.mail.MailConfig(io.vertx.lang.groovy.InternalHelper.toJsonObject(config)) : null), io.vertx.groovy.ext.mail.MailClient.class);
    return ret;
  }
  /**
   * Create a Mail client which shares its data source with any other Mongo clients created with the same
   * pool name
   * @param vertx the Vert.x instance
   * @param config the configuration (see <a href="../../../../../../../cheatsheet/MailConfig.html">MailConfig</a>)
   * @param poolName the pool name
   * @return the client
   */
  public static MailClient createShared(Vertx vertx, Map<String, Object> config, String poolName) {
    def ret = InternalHelper.safeCreate(io.vertx.ext.mail.MailClient.createShared(vertx != null ? (io.vertx.core.Vertx)vertx.getDelegate() : null, config != null ? new io.vertx.ext.mail.MailConfig(io.vertx.lang.groovy.InternalHelper.toJsonObject(config)) : null, poolName), io.vertx.groovy.ext.mail.MailClient.class);
    return ret;
  }
  /**
   * Like {@link io.vertx.groovy.ext.mail.MailClient#createShared} but with the default pool name
   * @param vertx the Vert.x instance
   * @param config the configuration (see <a href="../../../../../../../cheatsheet/MailConfig.html">MailConfig</a>)
   * @return the client
   */
  public static MailClient createShared(Vertx vertx, Map<String, Object> config) {
    def ret = InternalHelper.safeCreate(io.vertx.ext.mail.MailClient.createShared(vertx != null ? (io.vertx.core.Vertx)vertx.getDelegate() : null, config != null ? new io.vertx.ext.mail.MailConfig(io.vertx.lang.groovy.InternalHelper.toJsonObject(config)) : null), io.vertx.groovy.ext.mail.MailClient.class);
    return ret;
  }
  /**
   * send a single mail via MailClient
   * @param email MailMessage object containing the mail text, from/to, attachments etc (see <a href="../../../../../../../cheatsheet/MailMessage.html">MailMessage</a>)
   * @param resultHandler will be called when the operation is finished or it fails (may be null to ignore the result)
   * @return this MailClient instance so the method can be used fluently
   */
  public MailClient sendMail(Map<String, Object> email = [:], Handler<AsyncResult<Map<String, Object>>> resultHandler) {
    delegate.sendMail(email != null ? new io.vertx.ext.mail.MailMessage(io.vertx.lang.groovy.InternalHelper.toJsonObject(email)) : null, resultHandler != null ? new Handler<AsyncResult<io.vertx.ext.mail.MailResult>>() {
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
  /**
   * close the MailClient
   */
  public void close() {
    delegate.close();
  }
}

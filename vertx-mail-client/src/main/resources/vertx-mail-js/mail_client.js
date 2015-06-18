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

/** @module vertx-mail-js/mail_client */
var utils = require('vertx-js/util/utils');

var io = Packages.io;
var JsonObject = io.vertx.core.json.JsonObject;
var JMailClient = io.vertx.ext.mail.MailClient;
var MailConfig = io.vertx.ext.mail.MailConfig;
var MailConfig = io.vertx.ext.mail.MailConfig;
var MailMessage = io.vertx.ext.mail.MailMessage;

/**
 SMTP mail client for Vert.x
 <p>
 A simple asynchronous API for sending mails from Vert.x applications

 @class
*/
var MailClient = function(j_val) {

  var j_mailClient = j_val;
  var that = this;

  /**
   send a single mail via MailClient

   @public
   @param email {Object} MailMessage object containing the mail text, from/to, attachments etc 
   @param resultHandler {function} will be called when the operation is finished or it fails (may be null to ignore the result) 
   @return {MailClient} this MailClient instance so the method can be used fluently
   */
  this.sendMail = function(email, resultHandler) {
    var __args = arguments;
    if (__args.length === 2 && typeof __args[0] === 'object' && typeof __args[1] === 'function') {
      j_mailClient["sendMail(io.vertx.ext.mail.MailMessage,io.vertx.core.Handler)"](email != null ? new MailMessage(new JsonObject(JSON.stringify(email))) : null, function(ar) {
      if (ar.succeeded()) {
        resultHandler(utils.convReturnJson(ar.result().toJson()), null);
      } else {
        resultHandler(null, ar.cause());
      }
    });
      return that;
    } else utils.invalidArgs();
  };

  /**
   close the MailClient

   @public

   */
  this.close = function() {
    var __args = arguments;
    if (__args.length === 0) {
      j_mailClient["close()"]();
    } else utils.invalidArgs();
  };

  // A reference to the underlying Java delegate
  // NOTE! This is an internal API and must not be used in user code.
  // If you rely on this property your code is likely to break if we change it / remove it without warning.
  this._jdel = j_mailClient;
};

/**
 create an instance of MailClient that is running in the local JVM. This client will share the connection pool with other instances
 using the equal MailConfig object

 @memberof module:vertx-mail-js/mail_client
 @param vertx {Vertx} the Vertx instance the operation will be run in 
 @param config {Object} MailConfig configuration to be used for sending mails 
 @return {MailClient} MailClient instance that can then be used to send multiple mails
 */
MailClient.createShared = function(vertx, config) {
  var __args = arguments;
  if (__args.length === 2 && typeof __args[0] === 'object' && __args[0]._jdel && typeof __args[1] === 'object') {
    return utils.convReturnVertxGen(JMailClient["createShared(io.vertx.core.Vertx,io.vertx.ext.mail.MailConfig)"](vertx._jdel, config != null ? new MailConfig(new JsonObject(JSON.stringify(config))) : null), MailClient);
  } else utils.invalidArgs();
};

/**
 create an instance of MailClient that is running in the local JVM. This client will not share the connection pool with other instances

 @memberof module:vertx-mail-js/mail_client
 @param vertx {Vertx} the Vertx instance the operation will be run in 
 @param config {Object} MailConfig configuration to be used for sending mails 
 @return {MailClient} MailClient instance that can then be used to send multiple mails
 */
MailClient.createNonShared = function(vertx, config) {
  var __args = arguments;
  if (__args.length === 2 && typeof __args[0] === 'object' && __args[0]._jdel && typeof __args[1] === 'object') {
    return utils.convReturnVertxGen(JMailClient["createNonShared(io.vertx.core.Vertx,io.vertx.ext.mail.MailConfig)"](vertx._jdel, config != null ? new MailConfig(new JsonObject(JSON.stringify(config))) : null), MailClient);
  } else utils.invalidArgs();
};

// We export the Constructor function
module.exports = MailClient;
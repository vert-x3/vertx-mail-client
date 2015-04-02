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

/** @module vertx-mail-js/mail_service */
var utils = require('vertx-js/util/utils');

var io = Packages.io;
var JsonObject = io.vertx.core.json.JsonObject;
var JMailService = io.vertx.ext.mail.MailService;
var MailConfig = io.vertx.ext.mail.MailConfig;
var MailMessage = io.vertx.ext.mail.MailMessage;
var MailMessage = io.vertx.ext.mail.MailMessage;

/**
 smtp mail service for vert.x
 
 @class
*/
var MailService = function(j_val) {

  var j_mailService = j_val;
  var that = this;

  /**
   send a single mail via MailService

   @public
   @param email {Object} MailMessage object containing the mail text, from/to, attachments etc 
   @param resultHandler {function} will be called when the operation is finished or it fails the result JsonObject currently only contains {@code {"result":"success"}} 
   @return {MailService} the MailService instance so the method can be used fluently
   */
  this.sendMail = function(email, resultHandler) {
    var __args = arguments;
    if (__args.length === 2 && typeof __args[0] === 'object' && typeof __args[1] === 'function') {
      j_mailService["sendMail(io.vertx.ext.mail.MailMessage,io.vertx.core.Handler)"](email != null ? new MailMessage(new JsonObject(JSON.stringify(email))) : null, function(ar) {
      if (ar.succeeded()) {
        resultHandler(utils.convReturnJson(ar.result()), null);
      } else {
        resultHandler(null, ar.cause());
      }
    });
      return that;
    } else utils.invalidArgs();
  };

  /**
   send a single mail via MailService that has been pregenerated already
   <p>
   this makes it possible to create a mail message with Javamail for example to
   supports elements that are not supported by the mail encoder in vertx-mail-service

   @public
   @param email {Object} MailMessage object containing from/to etc, the message content fields are not evaluated 
   @param message {string} String object that contains the complete mail note that the From/To headers are not evaluated, rather they are taken from the MailMessage object 
   @param resultHandler {function} will be called when the operation is finished or it fails the result JsonObject currently only contains {@code {"result":"success"}} 
   @return {MailService} the MailService instance so the method can be used fluently
   */
  this.sendMailString = function(email, message, resultHandler) {
    var __args = arguments;
    if (__args.length === 3 && typeof __args[0] === 'object' && typeof __args[1] === 'string' && typeof __args[2] === 'function') {
      j_mailService["sendMailString(io.vertx.ext.mail.MailMessage,java.lang.String,io.vertx.core.Handler)"](email != null ? new MailMessage(new JsonObject(JSON.stringify(email))) : null, message, function(ar) {
      if (ar.succeeded()) {
        resultHandler(utils.convReturnJson(ar.result()), null);
      } else {
        resultHandler(null, ar.cause());
      }
    });
      return that;
    } else utils.invalidArgs();
  };

  /**
   start the MailServer instance if it is running locally (this operation is currently a no-op)

   @public

   */
  this.start = function() {
    var __args = arguments;
    if (__args.length === 0) {
      j_mailService["start()"]();
    } else utils.invalidArgs();
  };

  /**
   stop the MailServer instance if it is running locally
   <p>
   this operation shuts down the connection pool but doesn't wait for completion
   when the mail service is running on the event bus, this operation has no effect

   @public

   */
  this.stop = function() {
    var __args = arguments;
    if (__args.length === 0) {
      j_mailService["stop()"]();
    } else utils.invalidArgs();
  };

  // A reference to the underlying Java delegate
  // NOTE! This is an internal API and must not be used in user code.
  // If you rely on this property your code is likely to break if we change it / remove it without warning.
  this._jdel = j_mailService;
};

/**
 create an instance of MailService that is running in the local JVM

 @memberof module:vertx-mail-js/mail_service
 @param vertx {Vertx} the Vertx instance the operation will be run in 
 @param config {Object} MailConfig configuration to be used for sending mails 
 @return {MailService} MailServer instance that can then be used to send multiple mails
 */
MailService.create = function(vertx, config) {
  var __args = arguments;
  if (__args.length === 2 && typeof __args[0] === 'object' && __args[0]._jdel && typeof __args[1] === 'object') {
    return new MailService(JMailService["create(io.vertx.core.Vertx,io.vertx.ext.mail.MailConfig)"](vertx._jdel, config != null ? new MailConfig(new JsonObject(JSON.stringify(config))) : null));
  } else utils.invalidArgs();
};

/**
 create an instance of  MailService that calls the mail service via the event bus running somewhere else

 @memberof module:vertx-mail-js/mail_service
 @param vertx {Vertx} the Vertx instance the operation will be run in 
 @param address {string} the eb address of the mail service running somewhere, default is "vertx.mail" 
 @return {MailService} MailServer instance that can then be used to send multiple mails
 */
MailService.createEventBusProxy = function(vertx, address) {
  var __args = arguments;
  if (__args.length === 2 && typeof __args[0] === 'object' && __args[0]._jdel && typeof __args[1] === 'string') {
    return new MailService(JMailService["createEventBusProxy(io.vertx.core.Vertx,java.lang.String)"](vertx._jdel, address));
  } else utils.invalidArgs();
};

// We export the Constructor function
module.exports = MailService;
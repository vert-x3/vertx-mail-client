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
var MailClient = require('vertx-mail-js/mail_client');

var io = Packages.io;
var JsonObject = io.vertx.core.json.JsonObject;
var JMailService = io.vertx.ext.mail.MailService;
var MailMessage = io.vertx.ext.mail.MailMessage;

/**

 @class
*/
var MailService = function(j_val) {

  var j_mailService = j_val;
  var that = this;
  MailClient.call(this, j_val);

  /**

   @public
   @param email {Object} 
   @param resultHandler {function} 
   @return {MailService}
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

   @public

   */
  this.close = function() {
    var __args = arguments;
    if (__args.length === 0) {
      j_mailService["close()"]();
    } else utils.invalidArgs();
  };

  // A reference to the underlying Java delegate
  // NOTE! This is an internal API and must not be used in user code.
  // If you rely on this property your code is likely to break if we change it / remove it without warning.
  this._jdel = j_mailService;
};

// We export the Constructor function
module.exports = MailService;
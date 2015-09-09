/*
 *  Copyright (c) 2011-2015 The original author or authors
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *       The Eclipse Public License is available at
 *       http://www.eclipse.org/legal/epl-v10.html
 *
 *       The Apache License v2.0 is available at
 *       http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 */

/** @module vertx-mail-js/mail_client */
var utils = require('vertx-js/util/utils');

var io = Packages.io;
var JsonObject = io.vertx.core.json.JsonObject;
var JMailClient = io.vertx.ext.mail.MailClient;
var MailConfig = io.vertx.ext.mail.MailConfig;
var MailConfig = io.vertx.ext.mail.MailConfig;
var MailConfig = io.vertx.ext.mail.MailConfig;
var MailMessage = io.vertx.ext.mail.MailMessage;

/**
 @class
*/
var MailClient = function(j_val) {

  var j_mailClient = j_val;
  var that = this;

  /**

   @public
   @param arg0 {Object} 
   @param arg1 {function} 
   @return {MailClient}
   */
  this.sendMail = function(arg0, arg1) {
    var __args = arguments;
    if (__args.length === 2 && typeof __args[0] === 'object' && typeof __args[1] === 'function') {
      j_mailClient["sendMail(io.vertx.ext.mail.MailMessage,io.vertx.core.Handler)"](arg0 != null ? new MailMessage(new JsonObject(JSON.stringify(arg0))) : null, function(ar) {
      if (ar.succeeded()) {
        arg1(utils.convReturnJson(ar.result().toJson()), null);
      } else {
        arg1(null, ar.cause());
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
      j_mailClient["close()"]();
    } else utils.invalidArgs();
  };

  // A reference to the underlying Java delegate
  // NOTE! This is an internal API and must not be used in user code.
  // If you rely on this property your code is likely to break if we change it / remove it without warning.
  this._jdel = j_mailClient;
};

/**

 @memberof module:vertx-mail-js/mail_client
 @param vertx {Vertx} 
 @param config {Object} 
 @return {MailClient}
 */
MailClient.createNonShared = function(vertx, config) {
  var __args = arguments;
  if (__args.length === 2 && typeof __args[0] === 'object' && __args[0]._jdel && typeof __args[1] === 'object') {
    return utils.convReturnVertxGen(JMailClient["createNonShared(io.vertx.core.Vertx,io.vertx.ext.mail.MailConfig)"](vertx._jdel, config != null ? new MailConfig(new JsonObject(JSON.stringify(config))) : null), MailClient);
  } else utils.invalidArgs();
};

/**

 @memberof module:vertx-mail-js/mail_client
 @param vertx {Vertx} 
 @param config {Object} 
 @param poolName {string} 
 @return {MailClient}
 */
MailClient.createShared = function() {
  var __args = arguments;
  if (__args.length === 2 && typeof __args[0] === 'object' && __args[0]._jdel && typeof __args[1] === 'object') {
    return utils.convReturnVertxGen(JMailClient["createShared(io.vertx.core.Vertx,io.vertx.ext.mail.MailConfig)"](__args[0]._jdel, __args[1] != null ? new MailConfig(new JsonObject(JSON.stringify(__args[1]))) : null), MailClient);
  }else if (__args.length === 3 && typeof __args[0] === 'object' && __args[0]._jdel && typeof __args[1] === 'object' && typeof __args[2] === 'string') {
    return utils.convReturnVertxGen(JMailClient["createShared(io.vertx.core.Vertx,io.vertx.ext.mail.MailConfig,java.lang.String)"](__args[0]._jdel, __args[1] != null ? new MailConfig(new JsonObject(JSON.stringify(__args[1]))) : null, __args[2]), MailClient);
  } else utils.invalidArgs();
};

// We export the Constructor function
module.exports = MailClient;
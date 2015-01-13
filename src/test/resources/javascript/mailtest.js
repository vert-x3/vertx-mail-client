//
// simple example to show that the generated interface works in javascript
// 

var Vertx = require("vertx-js/vertx");
var vertx = Vertx.vertx();

var MailService = require('vertx-mail-js/mail_service');

var config={
  "hostname": "mail.arcor.de",
  port: 587,
  "username": "xxx",
  "password": "xxx",
  "starttls": "required"
};

console.log("starting");

var service=MailService.create(vertx,config);

var email={
  "from": "lehmann333@arcor.de",
  "recipient": "lehmann333@arcor.de",
  "subject": "Test email",
  "text": "this is a mail message"
};

var handler=function(result) {
  console.log('Mail finished '+result);
};

service.sendMail(email, handler);

console.log("finished");

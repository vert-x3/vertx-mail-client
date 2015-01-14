
//simple example to show that the generated interface works in javascript

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
		"to": "lehmann333@arcor.de",
		"subject": "Test email",
		"text": "this is a mail message",
		"attachment":
			[
			 {
				 "data": "attachment file content",
				 "content-type": "text/plain"
			 },
			 {
				 "data": "\0\0\0\0"
//				 "content-type": "application/octet-stream"
			 }
			 ]
};

service.sendMail(email, function(result) {
	console.log('Mail finished '+JSON.stringify(result));
});

console.log("finished");

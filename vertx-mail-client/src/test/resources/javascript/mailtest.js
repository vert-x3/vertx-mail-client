// simple example to show that the generated interface works in javascript
// using vertx-unit, the execution of this script is synchronous since it uses await at the end

var TestSuite = require('vertx-unit-js/test_suite');
var Vertx = require("vertx-js/vertx");
var MailClient = require('vertx-mail-js/mail_client');

var suite = TestSuite.create("mailclient_suite");

suite.test("mail_test_case", function(context) {

  var async = context.async();

  var vertx = Vertx.vertx();

  var config = {
      "hostname" : "mail.arcor.de",
      port : 587,
      "username" : "xxx",
      "password" : "xxx",
      "starttls" : "required"
  };


  var client = MailClient.create(vertx, config);

  var email = {
      "from" : "user@example.com",
      "to" : "user@example.com",
      "subject" : "Test email",
      "text" : "this is a mail message",
      "attachment" : [ {
        "data" : "YXR0YWNobWVudCBmaWxlIGNvbnRlbnQ=",
        "content-type" : "text/plain"
      }, {
        "data" : "AAAA"
          // "content-type": "application/octet-stream"
      } ]
  };

  client.sendMail(email, function (result, result_err) {
    console.log('Mail finished');
    if (result != null) {
      console.log('result: ' + JSON.stringify(result));
      context.fail("was expecting an exception");
    } else {
      console.log('sending failed');
      result_err.printStackTrace();
      async.complete();
    }
  });
});

suite.run().awaitSuccess(10000);

console.log("finished");

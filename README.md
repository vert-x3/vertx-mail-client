# Vert.x mail-service (SMTP client)

A very preliminary version of a smtp client for vert.x.

The test classes shows how this could be used, the whole client is async and supports SSL, STARTTLS, a few methods of SASL.

For questions about the project, please write a message on the vert.x google group or drop by on irc.

`link:../../apidocs/io/vertx/ext/mail/MailService.html[MailService]`
simple example:

~~~~ {.java}
MailConfig mailConfig = new MailConfig()
.setHostname("mail.example.com")
.setPort(587)
.setUsername("user")
.setPassword("pw");

MailService mailService = MailService.create(vertx, mailConfig);

MailMessage email = new MailMessage()
.setFrom("address@example.com")
.setTo("address@example.com")
.setSubject("meaningful subject")
.setText("this is a message")
.setHtml("HTML message <a href=\"http://vertx.io\">vertx</a>");

mailService.sendMail(email, result -> {
  if (result.succeeded()) {
    System.out.println(result.result());
  } else {
    System.out.println("got exception");
    result.cause().printStackTrace();
  }
});
~~~~

attachments can be added by converting them to String representation of
the bytes

~~~~ {.java}
MailConfig mailConfig = new MailConfig();

MailService mailService = MailService.create(vertx, mailConfig);

MailMessage email = new MailMessage()
.setFrom("address@example.com")
.setTo("address@example.com")
.setSubject("your file")
.setText("please take a look at the attached file");

MailAttachment attachment = new MailAttachment()
.setName("file.dat")
.setData("ASDF1234\0\u0001\u0080\u00ff\n");

email.setAttachment(attachment);

mailService.sendMail(email, result -> {
  if (result.succeeded()) {
    System.out.println(result.result());
  } else {
    System.out.println("got exception");
    result.cause().printStackTrace();
  }
});
~~~~

the service interface can send mails via the eventbus if the service is
running on other machine in the cluster

~~~~ {.java}
MailService mailService = MailService.createEventBusProxy(vertx, "vertx.mail");

MailMessage email=new MailMessage()
.setFrom("user@example.com")
.setBounceAddress("bounce@example.com")
.setTo("user@example.com");

mailService.sendMail(email, result -> {
  System.out.println("mail finished");
  if (result.succeeded()) {
    System.out.println(result.result());
  } else {
    System.out.println("got exception");
    result.cause().printStackTrace();
  }
});
~~~~

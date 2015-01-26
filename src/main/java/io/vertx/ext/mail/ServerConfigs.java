package io.vertx.ext.mail;

// TODO: this is not used except for testing, could be removed
// for normal use of the mailservice listing the parameters into the normal constructor
// or in the config for the service is probably clearer

public class ServerConfigs {

  private ServerConfigs() {
    // utility class
  }

  public static MailConfig configGoogle() {
    return new MailConfig("smtp.googlemail.com", 587, StarttlsOption.REQUIRED, LoginOption.REQUIRED);
  }

  public static MailConfig configMailtrap() {
    return new MailConfig("mailtrap.io", 25, StarttlsOption.OPTIONAL, LoginOption.REQUIRED);
  }

  public static MailConfig configMailgun() {
    return new MailConfig("smtp.mailgun.org", 587, StarttlsOption.REQUIRED, LoginOption.REQUIRED);
  }

  public static MailConfig configSendgrid() {
    return new MailConfig("smtp.sendgrid.net", 587, StarttlsOption.REQUIRED, LoginOption.REQUIRED);
  }

}

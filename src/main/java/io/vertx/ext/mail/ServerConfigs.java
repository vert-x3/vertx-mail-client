package io.vertx.ext.mail;

public class ServerConfigs {

  public static MailConfig configGoogle() {
    return new MailConfig("smtp.googlemail.com", 587, StarttlsOption.REQUIRED, LoginOption.REQUIRED);
  }

  public static MailConfig configMailtrap() {
    return new MailConfig("mailtrap.io", 25, StarttlsOption.OPTIONAL, LoginOption.REQUIRED);
  }

  public static MailConfig configMailgun() {
    return new MailConfig("smtp.mailgun.org", 587, StarttlsOption.REQUIRED, LoginOption.REQUIRED);
  }
}

package io.vertx.ext.mail;

import io.vertx.core.json.JsonObject;

public class ServerConfigs {

  public static JsonObject configGoogle() {
    JsonObject config=new JsonObject();
    config.put("hostname", "smtp.googlemail.com");
    config.put("port", 587);
    config.put("starttls", "required");
    config.put("login", "required");
    return config;
  }

  public static JsonObject configMailtrap() {
    JsonObject config=new JsonObject();
    config.put("hostname", "mailtrap.io");
    config.put("port", 25);
    config.put("starttls", "optional");
    config.put("login", "required");
    return config;
  }

  public static JsonObject configMailgun() {
    JsonObject config=new JsonObject();
    config.put("hostname", "smtp.mailgun.org");
    config.put("port", 587);
    config.put("starttls", "required");
    config.put("login", "required");
    return config;
  }
}

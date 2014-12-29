package io.vertx.ext.mail;

import io.vertx.codegen.annotations.Options;
import io.vertx.core.json.JsonObject;

@Options
public class MailConfig {

  private String hostname;
  private int port;
  /**
   * StarttlsOption: DISABLED, OPTIONAL, REQUIRED
   */
  private StarttlsOption starttls;
  /**
   * LoginOption: DISABLED, NONE, REQUIRED
   *
   * if you choose NONE, you can also set the auth data to null with the same
   * effect
   */
  private LoginOption login;
  // TODO: it might be better to put username/password into
  // an object since other auth mechanisms may have other data
  // e.g. XOAUTH for google
  private String username;
  private String password;

  /**
   * use ssl on connect? (i.e. Port 465)
   */
  private boolean ssl;

  public MailConfig() {
    this.hostname = "localhost";
    this.port = 25;
    this.starttls = StarttlsOption.OPTIONAL;
    this.login = LoginOption.NONE;
  }

  public MailConfig(String hostname) {
    this.hostname = hostname;
    this.port = 25;
    this.starttls = StarttlsOption.OPTIONAL;
    this.login = LoginOption.NONE;
  }

  public MailConfig(String hostname, int port) {
    this.hostname = hostname;
    this.port = port;
    this.starttls = StarttlsOption.OPTIONAL;
    this.login = LoginOption.NONE;
  }

  public MailConfig(String hostname, int port, StarttlsOption starttls, LoginOption login) {
    this.hostname = hostname;
    this.port = port;
    this.starttls = starttls;
    this.login = login;
  }

  public MailConfig(MailConfig other) {
    this.hostname = other.hostname;
    this.port = other.port;
    this.starttls = other.starttls;
    this.login = other.login;
    this.username = other.username;
    this.password = other.password;
    this.ssl = other.ssl;
  }

  public MailConfig(JsonObject config) {
    hostname = config.getString("hostname");
    port = config.getInteger("port");
    String starttlsOption = config.getString("starttls");
    if (starttlsOption != null) {
      starttls = StarttlsOption.valueOf(starttlsOption.toUpperCase());
    }
    String loginOption = config.getString("login");
    if (loginOption != null) {
      login = LoginOption.valueOf(loginOption.toUpperCase());
    }
    username = config.getString("username");
    password = config.getString("password");
    Boolean sslOption = config.getBoolean("ssl");
    if (sslOption != null) {
      ssl = sslOption;
    }
  }

  public String getHostname() {
    return hostname;
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public StarttlsOption getStarttls() {
    return starttls;
  }

  public void setStarttls(StarttlsOption starttls) {
    this.starttls = starttls;
  }

  public LoginOption getLogin() {
    return login;
  }

  public void setLogin(LoginOption login) {
    this.login = login;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public boolean isSsl() {
    return ssl;
  }

  public void setSsl(boolean ssl) {
    this.ssl = ssl;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    if (hostname != null) {
      json.put("hostname", hostname);
    }
    json.put("port", port);
    if (starttls != null) {
      json.put("starttls", starttls);
    }
    if (login != null) {
      json.put("login", login);
    }
    if (username != null) {
      json.put("username", username);
    }
    if (password != null) {
      json.put("password", password);
    }
    json.put("ssl", ssl);

    return json;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    MailConfig config = (MailConfig) o;

    if (!hostname.equals(config.hostname)) {
      return false;
    }
    if (port != config.port) {
      return false;
    }
    if (starttls != config.starttls) {
      return false;
    }
    if (login != config.login) {
      return false;
    }
    if (!username.equals(config.username)) {
      return false;
    }
    if (!password.equals(config.password)) {
      return false;
    }
    return ssl == config.ssl;
  }

  @Override
  public int hashCode() {
    int result = hashCodeNull(hostname);
    result = 31 * result + port;
    result = 31 * result + hashCodeNull(starttls);
    result = 31 * result + hashCodeNull(login);
    result = 31 * result + hashCodeNull(username);
    result = 31 * result + hashCodeNull(password);
    result = 31 * result + (ssl ? 1 : 0);
    return result;
  }

  private int hashCodeNull(Object o) {
    return o == null ? 0 : o.hashCode();
  }

}

package io.vertx.ext.mail;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject
public class MailConfig {

  public static final LoginOption DEFAULT_LOGIN = LoginOption.NONE;
  public static final StarttlsOption DEFAULT_TLS = StarttlsOption.OPTIONAL;
  public static final int DEFAULT_PORT = 25;
  public static final String DEFAULT_HOST = "localhost";

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
    this.hostname = DEFAULT_HOST;
    this.port = DEFAULT_PORT;
    this.starttls = DEFAULT_TLS;
    this.login = DEFAULT_LOGIN;
  }

  public MailConfig(String hostname) {
    this.hostname = hostname;
    this.port = DEFAULT_PORT;
    this.starttls = DEFAULT_TLS;
    this.login = DEFAULT_LOGIN;
  }

  public MailConfig(String hostname, int port) {
    this.hostname = hostname;
    this.port = port;
    this.starttls = DEFAULT_TLS;
    this.login = DEFAULT_LOGIN;
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
    hostname = config.getString("hostname", DEFAULT_HOST);
    port = config.getInteger("port", DEFAULT_PORT);
    String starttlsOption = config.getString("starttls");
    if (starttlsOption != null) {
      starttls = StarttlsOption.valueOf(starttlsOption.toUpperCase(Locale.ENGLISH));
    }
    String loginOption = config.getString("login");
    if (loginOption != null) {
      login = LoginOption.valueOf(loginOption.toUpperCase(Locale.ENGLISH));
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

  public MailConfig setHostname(String hostname) {
    this.hostname = hostname;
    return this;
  }

  public int getPort() {
    return port;
  }

  public MailConfig setPort(int port) {
    this.port = port;
    return this;
  }

  public StarttlsOption getStarttls() {
    return starttls;
  }

  public MailConfig setStarttls(StarttlsOption starttls) {
    this.starttls = starttls;
    return this;
  }

  public LoginOption getLogin() {
    return login;
  }

  public MailConfig setLogin(LoginOption login) {
    this.login = login;
    return this;
  }

  public String getUsername() {
    return username;
  }

  public MailConfig setUsername(String username) {
    this.username = username;
    return this;
  }

  public String getPassword() {
    return password;
  }

  public MailConfig setPassword(String password) {
    this.password = password;
    return this;
  }

  public boolean isSsl() {
    return ssl;
  }

  public MailConfig setSsl(boolean ssl) {
    this.ssl = ssl;
    return this;
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
    if (ssl) {
      json.put("ssl", ssl);
    }

    return json;
  }

  private List<Object> getList() {
    final List<Object> objects = Arrays.asList(hostname, port, starttls, login, username, password, ssl);
    return objects;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final MailConfig config = (MailConfig) o;
    return getList().equals(config.getList());
  }

  @Override
  public int hashCode() {
    return getList().hashCode();
  }

}

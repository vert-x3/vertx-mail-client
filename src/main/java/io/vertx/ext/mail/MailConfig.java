package io.vertx.ext.mail;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetClientOptions;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * represents the configuration of a mail service with mail server hostname, port, security options, login options and login/password
 * 
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
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

  /**
   * trust all certificates
   * this is applied to ssl connect or STARTTLS operation
   */
  private boolean trustAll;

  
  /**
   * use NetClinetOptions if set instead of creating an options object based on the other options
   * (this is mainly useful for ssl certs)
   */
  private NetClientOptions netClientOptions;

  /**
   * construct a config object with default options
   */
  public MailConfig() {
    this.hostname = DEFAULT_HOST;
    this.port = DEFAULT_PORT;
    this.starttls = DEFAULT_TLS;
    this.login = DEFAULT_LOGIN;
  }

  /**
   * construct a config object with hostname and default options
   * @param hostname the hostname of the mail server
   */
  public MailConfig(String hostname) {
    this.hostname = hostname;
    this.port = DEFAULT_PORT;
    this.starttls = DEFAULT_TLS;
    this.login = DEFAULT_LOGIN;
  }

  /**
   * construct a config object with hostname/port and default options
   * @param hostname the hostname of the mail server
   * @param port the port of the mail server
   */
  public MailConfig(String hostname, int port) {
    this.hostname = hostname;
    this.port = port;
    this.starttls = DEFAULT_TLS;
    this.login = DEFAULT_LOGIN;
  }

  /**
   * construct a config object with hostname/port and security and login options
   * @param hostname the hostname of the mail server
   * @param port the port of the mail server
   * @param starttls whether to use TLS or not
   * @param login whether to use Login or not
   */
  public MailConfig(String hostname, int port, StarttlsOption starttls, LoginOption login) {
    this.hostname = hostname;
    this.port = port;
    this.starttls = starttls;
    this.login = login;
  }

  /**
   * copy config object from another MailConfig object 
   * @param other the object to be copied
   */
  public MailConfig(MailConfig other) {
    hostname = other.hostname;
    port = other.port;
    starttls = other.starttls;
    login = other.login;
    username = other.username;
    password = other.password;
    ssl = other.ssl;
    trustAll = other.trustAll;
    if(other.netClientOptions != null) {
      netClientOptions = new NetClientOptions(netClientOptions);
    }
  }

  /**
   * construct config object from Json representation
   * @param config the config to copy
   */
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
    ssl = config.getBoolean("ssl", false);
    trustAll = config.getBoolean("trustall", false);
    JsonObject options = config.getJsonObject("netclientoptions");
    if (options != null) {
      netClientOptions = new NetClientOptions(options);
    }
  }

  /**
   * get the hostname of the mailserver
   * @return hostname
   */
  public String getHostname() {
    return hostname;
  }

  /**
   * Set the hostname of the smtp server.
   *
   * @param hostname the hostname (default is localhost)
   * @return a reference to this, so the API can be used fluently
   */
  public MailConfig setHostname(String hostname) {
    this.hostname = hostname;
    return this;
  }

  /**
   * get the port of the mailserver
   * @return port
   */
  public int getPort() {
    return port;
  }

  /**
   * Set the port of the smtp server.
   *
   * @param port the port (default is 25)
   * @return a reference to this, so the API can be used fluently
   */
  public MailConfig setPort(int port) {
    this.port = port;
    return this;
  }

  /**
   * get security (TLS) options
   * @return the security options
   */
  public StarttlsOption getStarttls() {
    return starttls;
  }

  /**
   * Set the tls security mode for the connection.
   *
   * Either NONE, OPTIONAL or REQUIRED
   *
   * @param starttls (default is OPTIONAL)
   * @return a reference to this, so the API can be used fluently
   */
  public MailConfig setStarttls(StarttlsOption starttls) {
    this.starttls = starttls;
    return this;
  }

  /**
   * get login options
   * @return the login options
   */
  public LoginOption getLogin() {
    return login;
  }

  /**
   * Set the login mode for the connection.
   *
   * Either DISABLED, OPTIONAL or REQUIRED
   *
   * @param login (default is OPTIONAL)
   * @return a reference to this, so the API can be used fluently
   */
  public MailConfig setLogin(LoginOption login) {
    this.login = login;
    return this;
  }

  /**
   * get username
   * @return username
   */
  public String getUsername() {
    return username;
  }

  /**
   * Set the username for the login.
   *
   * @param username the username
   * @return a reference to this, so the API can be used fluently
   */
  public MailConfig setUsername(String username) {
    this.username = username;
    return this;
  }

  /**
   * get password
   * @return password
   */
  public String getPassword() {
    return password;
  }

  /**
   * Set the password for the login.
   *
   * @param password the password
   * @return a reference to this, so the API can be used fluently
   */
  public MailConfig setPassword(String password) {
    this.password = password;
    return this;
  }

  /**
   * get whether ssl is used on connect
   * @return ssl option
   */
  public boolean isSsl() {
    return ssl;
  }

  /**
   * Set the sslOnConnect mode for the connection.
   * 
   * @param ssl true is ssl is used
   * @return a reference to this, so the API can be used fluently
   */
  public MailConfig setSsl(boolean ssl) {
    this.ssl = ssl;
    return this;
  }

  /**
   * get whether to trust all certificates on ssl connect
   * @return trustAll option
   */
  public boolean isTrustAll() {
    return trustAll;
  }

  /**
   * set whether to trust all certificates on ssl connect
   * the option is also applied to STARTTLS operation
   * @param trustAll trust all certificates
   * @return a reference to this, so the API can be used fluently
   */
  public MailConfig setTrustAll(boolean trustAll) {
    this.trustAll = trustAll;
    return this;
  }

  /**
   * get the NetClientOptions to be used when opening SMTP connections
   * @return the netClientOptions
   */
  public NetClientOptions getNetClientOptions() {
    return netClientOptions;
  }

  /**
   * set the NetClientOptions to be used when opening SMTP connections
   *
   * if not set, an options object will be created based on other settings
   * (ssl and trustAll)
   * @param netClientOptions the netClientOptions to set
   * @return a reference to this, so the API can be used fluently
   */
  public MailConfig setNetClientOptions(NetClientOptions netClientOptions) {
    this.netClientOptions = netClientOptions;
    return this;
  }

  /**
   * convert config object to Json representation
   * @return json object of the config
   */
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
    if (trustAll) {
      json.put("trustall", trustAll);
    }
// TODO: how do we do this, if it is necessary at all?
//    if(netClientOptions != null) {
//      json.put("netclientoptions", netClientOptions);
//    }

    return json;
  }

  private List<Object> getList() {
    final List<Object> objects = Arrays.asList(hostname, port, starttls, login, username, password, ssl, trustAll, netClientOptions);
    return objects;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || !(o instanceof MailConfig)) {
      return false;
    }
    final MailConfig config = (MailConfig) o;
    return getList().equals(config.getList());
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return getList().hashCode();
  }

}

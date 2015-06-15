package io.vertx.ext.mail;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * represents the configuration of a mail service with mail server hostname,
 * port, security options, login options and login/password
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
@DataObject
public class MailConfig {

  public static final LoginOption DEFAULT_LOGIN = LoginOption.NONE;
  public static final StartTLSOptions DEFAULT_TLS = StartTLSOptions.OPTIONAL;
  public static final int DEFAULT_PORT = 25;
  public static final String DEFAULT_HOST = "localhost";
  public static final int DEFAULT_MAX_POOL_SIZE = 10;
  public static final int DEFAULT_IDLE_TIMEOUT = 300;

  private String hostname;
  private int port;
  private StartTLSOptions starttls;
  private LoginOption login;
  private String authMethods;
  private String username;
  private String password;
  private boolean ssl;
  private boolean trustAll;
  private String keyStore;
  private String keyStorePassword;
  private String ownHostname;
  private int maxPoolSize;
  private int idleTimeout;
  private boolean keepAlive;
  private boolean allowRcptErrors;

  /**
   * construct a config object with default options
   */
  public MailConfig() {
    hostname = DEFAULT_HOST;
    port = DEFAULT_PORT;
    starttls = DEFAULT_TLS;
    login = DEFAULT_LOGIN;
    maxPoolSize = DEFAULT_MAX_POOL_SIZE;
    idleTimeout = DEFAULT_IDLE_TIMEOUT;
    keepAlive = true;
  }

  /**
   * construct a config object with hostname and default options
   *
   * @param hostname the hostname of the mail server
   */
  public MailConfig(String hostname) {
    this.hostname = hostname;
    port = DEFAULT_PORT;
    starttls = DEFAULT_TLS;
    login = DEFAULT_LOGIN;
    maxPoolSize = DEFAULT_MAX_POOL_SIZE;
    idleTimeout = DEFAULT_IDLE_TIMEOUT;
    keepAlive = true;
  }

  /**
   * construct a config object with hostname/port and default options
   *
   * @param hostname the hostname of the mail server
   * @param port     the port of the mail server
   */
  public MailConfig(String hostname, int port) {
    this.hostname = hostname;
    this.port = port;
    starttls = DEFAULT_TLS;
    login = DEFAULT_LOGIN;
    maxPoolSize = DEFAULT_MAX_POOL_SIZE;
    idleTimeout = DEFAULT_IDLE_TIMEOUT;
    keepAlive = true;
  }

  /**
   * construct a config object with hostname/port and security and login options
   *
   * @param hostname the hostname of the mail server
   * @param port     the port of the mail server
   * @param starttls whether to use TLS or not
   * @param login    whether to use Login or not
   */
  public MailConfig(String hostname, int port, StartTLSOptions starttls, LoginOption login) {
    this.hostname = hostname;
    this.port = port;
    this.starttls = starttls;
    this.login = login;
    maxPoolSize = DEFAULT_MAX_POOL_SIZE;
    idleTimeout = DEFAULT_IDLE_TIMEOUT;
    keepAlive = true;
  }

  /**
   * copy config object from another MailConfig object
   *
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
    keyStore = other.keyStore;
    keyStorePassword = other.keyStorePassword;
    authMethods = other.authMethods;
    ownHostname = other.ownHostname;
    maxPoolSize = other.maxPoolSize;
    idleTimeout = other.idleTimeout;
    keepAlive = other.keepAlive;
    allowRcptErrors = other.allowRcptErrors;
  }

  /**
   * construct config object from Json representation
   *
   * @param config the config to copy
   */
  public MailConfig(JsonObject config) {
    hostname = config.getString("hostname", DEFAULT_HOST);
    port = config.getInteger("port", DEFAULT_PORT);
    String starttlsOption = config.getString("starttls");
    if (starttlsOption != null) {
      starttls = StartTLSOptions.valueOf(starttlsOption.toUpperCase(Locale.ENGLISH));
    }
    String loginOption = config.getString("login");
    if (loginOption != null) {
      login = LoginOption.valueOf(loginOption.toUpperCase(Locale.ENGLISH));
    }
    username = config.getString("username");
    password = config.getString("password");
    ssl = config.getBoolean("ssl", false);
    trustAll = config.getBoolean("trustall", false);
    keyStore = config.getString("key_store");
    keyStorePassword = config.getString("key_store_password");
    authMethods = config.getString("auth_methods");
    ownHostname = config.getString("own_hostname");
    maxPoolSize = config.getInteger("max_pool_size", DEFAULT_MAX_POOL_SIZE);
    idleTimeout = config.getInteger("idle_timeout", DEFAULT_IDLE_TIMEOUT);
    keepAlive = config.getBoolean("keep_alive", true);
    allowRcptErrors = config.getBoolean("allow_rcpt_errors", false);
  }

  /**
   * get the hostname of the mailserver
   *
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
   *
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
    if (port < 0 || port > 65535) {
      throw new IllegalArgumentException("port must be >=0 && <= 65535");
    }

    this.port = port;
    return this;
  }

  /**
   * get security (TLS) options
   *
   * @return the security options
   */
  public StartTLSOptions getStarttls() {
    return starttls;
  }

  /**
   * Set the tls security mode for the connection.
   * <p>
   * Either NONE, OPTIONAL or REQUIRED
   *
   * @param starttls (default is OPTIONAL)
   * @return a reference to this, so the API can be used fluently
   */
  public MailConfig setStarttls(StartTLSOptions starttls) {
    this.starttls = starttls;
    return this;
  }

  /**
   * get login options
   *
   * @return the login options
   */
  public LoginOption getLogin() {
    return login;
  }

  /**
   * Set the login mode for the connection.
   * <p>
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
   *
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
   *
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
   *
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
   *
   * @return trustAll option
   */
  public boolean isTrustAll() {
    return trustAll;
  }

  /**
   * set whether to trust all certificates on ssl connect the option is also
   * applied to STARTTLS operation
   *
   * @param trustAll trust all certificates
   * @return a reference to this, so the API can be used fluently
   */
  public MailConfig setTrustAll(boolean trustAll) {
    this.trustAll = trustAll;
    return this;
  }

  /**
   * get the key store filename to be used when opening SMTP connections
   * @return the keyStore
   */
  public String getKeyStore() {
    return keyStore;
  }

  /**
   * get the key store filename to be used when opening SMTP connections
   * <p>
   * if not set, an options object will be created based on other settings (ssl
   * and trustAll)
   *
   * @param keyStore the key store filename to be set
   * @return a reference to this, so the API can be used fluently
   */
  public MailConfig setKeyStore(String keyStore) {
    this.keyStore = keyStore;
    return this;
  }

  /**
   * get the key store password to be used when opening SMTP connections
   * @return the keyStorePassword
   */
  public String getKeyStorePassword() {
    return keyStorePassword;
  }

  /**
   * get the key store password to be used when opening SMTP connections
   * @param keyStorePassword the key store passwords to be set
   * @return a reference to this, so the API can be used fluently
   */
  public MailConfig setKeyStorePassword(String keyStorePassword) {
    this.keyStorePassword = keyStorePassword;
    return this;
  }

  /**
   * get string of allowed auth methods, if set only these methods will be used
   * if the server supports them. If null or empty all supported methods may be
   * used
   *
   * @return the authMethods
   */
  public String getAuthMethods() {
    return authMethods;
  }

  /**
   * set string of allowed auth methods.
   * if set only these methods will be used
   * if the server supports them. If null or empty all supported methods may be
   * used
   *
   * @param authMethods the authMethods to set
   * @return a reference to this, so the API can be used fluently
   */
  public MailConfig setAuthMethods(String authMethods) {
    this.authMethods = authMethods;
    return this;
  }

  /**
   * get the hostname to be used for HELO/EHLO and the Message-ID
   *
   * @return my own hostname
   */
  public String getOwnHostname() {
    return ownHostname;
  }

  /**
   * set the hostname to be used for HELO/EHLO and the Message-ID
   *
   * @param ownHostname my own hostname to set
   * @return a reference to this, so the API can be used fluently
   */
  public MailConfig setOwnHostname(String ownHostname) {
    this.ownHostname = ownHostname;
    return this;
  }

  /**
   * get the max allowed number of open connections to the mailserver
   * if not set the default is 10
   *
   * @return max pool size value
   */
  public int getMaxPoolSize() {
    return maxPoolSize;
  }

  /**
   * set the max allowed number of open connections to the mail server
   * if not set the default is 10
   *
   * @return this to be able to use the object fluently
   */
  public MailConfig setMaxPoolSize(int maxPoolSize) {
    if (maxPoolSize < 1) {
      throw new IllegalArgumentException("maxPoolSize must be > 0");
    }
    this.maxPoolSize = maxPoolSize;
    return this;
  }

  /**
   * get the timeout for idle smtp connections (in seconds)
   * if not set the default is 300 seconds
   *
   * @return idle timeout value
   */
  public int getIdleTimeout() {
    return idleTimeout;
  }

  /**
   * set the timeout for idle smtp connections (in seconds)
   * if not set, the default is 300 seconds
   *
   * @return this to be able to use the object fluently
   */
  public MailConfig setIdleTimeout(int idleTimeout) {
    if (idleTimeout < 1) {
      throw new IllegalArgumentException("idleTimeout must be > 0");
    }
    this.idleTimeout = idleTimeout;
    return this;
  }

  /**
   * get if connection pool is enabled
   * default is true
   *<p>
   * if the connection pooling is disabled, the max number of sockets is enforced nevertheless
   *<p>
   * @return keep alive value
   */
  public boolean isKeepAlive() {
    return keepAlive;
  }

  /**
   * set if connection pool is enabled
   * default is true
   *<p>
   * if the connection pooling is disabled, the max number of sockets is enforced nevertheless
   *<p>
   * @return this to be able to use the object fluently
   */
  public MailConfig setKeepAlive(boolean keepAlive) {
    this.keepAlive = keepAlive;
    return this;
  }

  /**
   * get if sending allows rcpt errors (default is false)
   *<p>
   * if true, the mail will be sent to the recipients that the server accepted, if any
   *<p>
   * @return the allowRcptErrors
   */
  public boolean isAllowRcptErrors() {
    return allowRcptErrors;
  }

  /**
   * set if sending allows rcpt errors
   * @param allowRcptErrors the allowRcptErrors to set (default is false)
   *<p>
   * if true, the mail will be sent to the recipients that the server accepted, if any
   *<p>
   * @return this to be able to use the object fluently
   */
  public MailConfig setAllowRcptErrors(boolean allowRcptErrors) {
    this.allowRcptErrors = allowRcptErrors;
    return this;
  }

  /**
   * convert config object to Json representation
   *
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
    if (keyStore != null) {
      json.put("key_store", keyStore);
    }
    if (keyStorePassword != null) {
      json.put("key_store_password", keyStorePassword);
    }
    if (authMethods != null) {
      json.put("auth_methods", authMethods);
    }
    if (ownHostname != null) {
      json.put("own_hostname", ownHostname);
    }
    json.put("max_pool_size", maxPoolSize);
    json.put("idle_timeout", idleTimeout);
    if (!keepAlive) {
      json.put("keep_alive", keepAlive);
    }
    if (allowRcptErrors) {
      json.put("allow_rcpt_errors", allowRcptErrors);
    }

    return json;
  }

  private List<Object> getList() {
    return Arrays.asList(hostname, port, starttls, login, username, password, ssl, trustAll, keyStore,
        keyStorePassword, authMethods, ownHostname, maxPoolSize, idleTimeout, keepAlive, allowRcptErrors);
  }

  /*
   * (non-Javadoc)
   * 
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

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return getList().hashCode();
  }

}

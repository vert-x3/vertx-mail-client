package io.vertx.ext.mail;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetClientOptions;

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
  /**
   * StarttlsOption: DISABLED, OPTIONAL, REQUIRED
   */
  private StartTLSOptions starttls;
  /**
   * LoginOption: DISABLED, NONE, REQUIRED
   * <p>
   * if you choose NONE, you can also set the auth data to null with the same
   * effect
   */
  private LoginOption login;

  /**
   * String of allowed auth methods, if set only these methods will be used if
   * the server supports them.
   * <p>
   * e.g. setting it to a single word will use this method and fail otherwise.
   */
  private String authMethods;

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
   * trust all certificates this is applied to ssl connect or STARTTLS operation
   */
  private boolean trustAll;

  /**
   * use NetClinetOptions if set instead of creating an options object based on
   * the other options (this is mainly useful for ssl certs)
   */
  private NetClientOptions netClientOptions;

  /**
   * use this hostname for HELO/EHLO command
   */
  private String ownHostname;

  /**
   * maximum number of connections to keep open in the connection pool in one instance
   * of MailClient
   */
  private int maxPoolSize;

  /**
   * time until an open idle connection is closed
   */
  private int idleTimeout;

  /**
   * set keepAlive = false to disable connection pool
   */
  private boolean keepAlive;

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
    if (other.netClientOptions != null) {
      netClientOptions = new NetClientOptions(other.netClientOptions);
    }
    authMethods = other.authMethods;
    ownHostname = other.ownHostname;
    maxPoolSize = other.maxPoolSize;
    idleTimeout = other.idleTimeout;
    keepAlive = other.keepAlive;
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
    JsonObject options = config.getJsonObject("netclientoptions");
    if (options != null) {
      netClientOptions = new NetClientOptions(options);
    }
    authMethods = config.getString("auth_methods");
    ownHostname = config.getString("own_hostname");
    maxPoolSize = config.getInteger("max_pool_size", DEFAULT_MAX_POOL_SIZE);
    idleTimeout = config.getInteger("idle_timeout", DEFAULT_IDLE_TIMEOUT);
    keepAlive = config.getBoolean("keep_alive", true);
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
   * get the NetClientOptions to be used when opening SMTP connections
   * <p>
   * when using a custom key store, the NetClientOptions are necessary to the
   * set the correct jks options, see this example {@code io/vertx/ext/mail/MailLocalTest}
   *
   * @return the netClientOptions
   */
  public NetClientOptions getNetClientOptions() {
    return netClientOptions;
  }

  /**
   * set the NetClientOptions to be used when opening SMTP connections
   * <p>
   * if not set, an options object will be created based on other settings (ssl
   * and trustAll)
   *
   * @param netClientOptions the netClientOptions to set
   * @return a reference to this, so the API can be used fluently
   */
  public MailConfig setNetClientOptions(NetClientOptions netClientOptions) {
    this.netClientOptions = netClientOptions;
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
    // TODO: how do we do this, if it is necessary at all?
    // if(netClientOptions != null) {
    //   json.put("netclientoptions", netClientOptions);
    // }
    if (authMethods != null) {
      json.put("auth_methods", authMethods);
    }
    if (ownHostname != null) {
      json.put("own_hostname", ownHostname);
    }
    json.put("max_pool_size", maxPoolSize);
    json.put("idle_timeout", idleTimeout);
    if (keepAlive == false) {
      json.put("keep_alive", keepAlive);
    }

    return json;
  }

  private List<Object> getList() {
    return Arrays.asList(hostname, port, starttls, login, username, password, ssl, trustAll, netClientOptions,
        authMethods, ownHostname, maxPoolSize, idleTimeout, keepAlive);
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

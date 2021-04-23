/*
 *  Copyright (c) 2011-2015 The original author or authors
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *       The Eclipse Public License is available at
 *       http://www.eclipse.org/legal/epl-v10.html
 *
 *       The Apache License v2.0 is available at
 *       http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 */

package io.vertx.ext.mail;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JdkSSLEngineOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.OpenSSLEngineOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.PfxOptions;
import io.vertx.core.net.ProxyOptions;
import io.vertx.core.net.SSLEngineOptions;
import io.vertx.core.net.TrustOptions;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * represents the configuration of a mail service with mail server hostname,
 * port, security options, login options and login/password
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
@DataObject
public class MailConfig extends NetClientOptions {

  public static final LoginOption DEFAULT_LOGIN = LoginOption.NONE;
  public static final StartTLSOptions DEFAULT_TLS = StartTLSOptions.OPTIONAL;
  public static final int DEFAULT_PORT = 25;
  public static final String DEFAULT_HOST = "localhost";
  public static final int DEFAULT_MAX_POOL_SIZE = 10;
  public static final boolean DEFAULT_ALLOW_RCPT_ERRORS = false;
  public static final boolean DEFAULT_KEEP_ALIVE = true;
  public static final boolean DEFAULT_DISABLE_ESMTP = false;
  private static final boolean DEFAULT_ENABLE_DKIM = false;
  public static final String DEFAULT_USER_AGENT = "vertxmail";
  public static final boolean DEFAULT_ENABLE_PIPELINING = true;
  public static final boolean DEFAULT_MULTI_PART_ONLY = false;

  /**
   * Default pool cleaner period = 1000 ms (1 second)
   */
  public static final int DEFAULT_POOL_CLEANER_PERIOD = 1000;
  public static final TimeUnit DEFAULT_POOL_CLEANER_PERIOD_TIMEOUT_UNIT = TimeUnit.MILLISECONDS;

  /**
   * The default keep alive timeout for SMTP connection = 5 minutes
   */
  public static final int DEFAULT_KEEP_ALIVE_TIMEOUT = 300;
  public static final TimeUnit DEFAULT_KEEP_ALIVE_TIMEOUT_UNIT = TimeUnit.SECONDS;

  private String hostname = DEFAULT_HOST;
  private int port = DEFAULT_PORT;
  private StartTLSOptions starttls = DEFAULT_TLS;
  private LoginOption login = DEFAULT_LOGIN;
  private String authMethods;
  private String username;
  private String password;
  private String ownHostname;
  private int maxPoolSize = DEFAULT_MAX_POOL_SIZE;
  private boolean keepAlive = DEFAULT_KEEP_ALIVE;
  private boolean allowRcptErrors = DEFAULT_ALLOW_RCPT_ERRORS;
  private boolean disableEsmtp = DEFAULT_DISABLE_ESMTP;
  private String userAgent = DEFAULT_USER_AGENT;
  private boolean enableDKIM = DEFAULT_ENABLE_DKIM;
  private List<DKIMSignOptions> dkimSignOptions;
  private boolean pipelining = DEFAULT_ENABLE_PIPELINING;
  private boolean multiPartOnly = DEFAULT_MULTI_PART_ONLY;
  private int poolCleanerPeriod = DEFAULT_POOL_CLEANER_PERIOD;
  private TimeUnit poolCleanerPeriodUnit = DEFAULT_POOL_CLEANER_PERIOD_TIMEOUT_UNIT;
  private int keepAliveTimeout = DEFAULT_KEEP_ALIVE_TIMEOUT;
  private TimeUnit keepAliveTimeoutUnit = DEFAULT_KEEP_ALIVE_TIMEOUT_UNIT;

  // https://tools.ietf.org/html/rfc5322#section-3.2.3, atext
  private static final Pattern A_TEXT_PATTERN = Pattern.compile("[a-zA-Z0-9!#$%&'*+-/=?^_`{|}~ ]+");

  /**
   * construct a config object with default options
   */
  public MailConfig() {
    // Use the default values.
  }

  /**
   * construct a config object with hostname and default options
   *
   * @param hostname the hostname of the mail server
   */
  public MailConfig(String hostname) {
    this();
    this.hostname = hostname;
  }

  /**
   * construct a config object with hostname/port and default options
   *
   * @param hostname the hostname of the mail server
   * @param port     the port of the mail server
   */
  public MailConfig(String hostname, int port) {
    this();
    this.hostname = hostname;
    this.port = port;
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
    this(hostname, port);
    this.starttls = starttls;
    this.login = login;
  }

  /**
   * copy config object from another MailConfig object
   *
   * @param other the object to be copied
   */
  public MailConfig(MailConfig other) {
    super(other);
    hostname = other.hostname;
    port = other.port;
    starttls = other.starttls;
    login = other.login;
    username = other.username;
    password = other.password;
    authMethods = other.authMethods;
    ownHostname = other.ownHostname;
    maxPoolSize = other.maxPoolSize;
    keepAlive = other.keepAlive;
    allowRcptErrors = other.allowRcptErrors;
    disableEsmtp = other.disableEsmtp;
    userAgent = other.userAgent;
    enableDKIM = other.enableDKIM;
    if (other.dkimSignOptions != null && !other.dkimSignOptions.isEmpty()) {
      dkimSignOptions = other.dkimSignOptions.stream().map(DKIMSignOptions::new).collect(Collectors.toList());
    }
    pipelining = other.pipelining;
    multiPartOnly = other.multiPartOnly;
    poolCleanerPeriod = other.poolCleanerPeriod;
    poolCleanerPeriodUnit =  other.poolCleanerPeriodUnit;
    keepAliveTimeout = other.keepAliveTimeout;
    keepAliveTimeoutUnit = other.keepAliveTimeoutUnit;
  }

  /**
   * construct config object from Json representation
   *
   * @param config the config to copy
   */
  public MailConfig(JsonObject config) {
    super(config);
    hostname = config.getString("hostname", DEFAULT_HOST);
    port = config.getInteger("port", DEFAULT_PORT);

    String starttlsOption = config.getString("starttls");
    if (starttlsOption != null) {
      starttls = StartTLSOptions.valueOf(starttlsOption.toUpperCase(Locale.ENGLISH));
    } else {
      starttls = DEFAULT_TLS;
    }

    String loginOption = config.getString("login");
    if (loginOption != null) {
      login = LoginOption.valueOf(loginOption.toUpperCase(Locale.ENGLISH));
    } else {
      login = DEFAULT_LOGIN;
    }

    username = config.getString("username");
    password = config.getString("password");
    // Handle these for compatiblity
    if (config.containsKey("keyStore")) {
      setKeyStore(config.getString("keyStore"));
    }
    if (config.containsKey("keyStorePassword")) {
      setKeyStorePassword(config.getString("keyStorePassword"));
    }
    authMethods = config.getString("authMethods");
    ownHostname = config.getString("ownHostname");
    maxPoolSize = config.getInteger("maxPoolSize", DEFAULT_MAX_POOL_SIZE);
    keepAlive = config.getBoolean("keepAlive", DEFAULT_KEEP_ALIVE);
    allowRcptErrors = config.getBoolean("allowRcptErrors", DEFAULT_ALLOW_RCPT_ERRORS);
    userAgent = config.getString("userAgent", DEFAULT_USER_AGENT);
    enableDKIM = config.getBoolean("enableDKIM", DEFAULT_ENABLE_DKIM);
    JsonArray dkimOps = config.getJsonArray("dkimSignOptions");
    if (dkimOps != null) {
      dkimSignOptions = new ArrayList<>();
      dkimOps.stream().map(dkim -> new DKIMSignOptions((JsonObject)dkim)).forEach(dkimSignOptions::add);
    }
    pipelining = config.getBoolean("pipelining", DEFAULT_ENABLE_PIPELINING);
    multiPartOnly = config.getBoolean("multiPartOnly", DEFAULT_MULTI_PART_ONLY);
    poolCleanerPeriod = config.getInteger("poolCleanerPeriod", DEFAULT_POOL_CLEANER_PERIOD);
    keepAliveTimeout = config.getInteger("keepAliveTimeout", DEFAULT_KEEP_ALIVE_TIMEOUT);
    Object keepAliveTU = config.getValue("keepAliveTimeoutUnit");
    if (keepAliveTU instanceof String) {
      keepAliveTimeoutUnit = TimeUnit.valueOf((String)keepAliveTU);
    } else {
      keepAliveTimeoutUnit = DEFAULT_KEEP_ALIVE_TIMEOUT_UNIT;
    }
    Object poolCleanerU = config.getValue("poolCleanerPeriodUnit");
    if (keepAliveTU instanceof String) {
      poolCleanerPeriodUnit = TimeUnit.valueOf((String)poolCleanerU);
    } else {
      poolCleanerPeriodUnit = DEFAULT_POOL_CLEANER_PERIOD_TIMEOUT_UNIT;
    }
  }

  public MailConfig setSendBufferSize(int sendBufferSize) {
    super.setSendBufferSize(sendBufferSize);
    return this;
  }

  public MailConfig setReceiveBufferSize(int receiveBufferSize) {
    super.setReceiveBufferSize(receiveBufferSize);
    return this;
  }

  public MailConfig setReuseAddress(boolean reuseAddress) {
    super.setReuseAddress(reuseAddress);
    return this;
  }

  public MailConfig setReusePort(boolean reusePort) {
    super.setReusePort(reusePort);
    return this;
  }

  public MailConfig setTrafficClass(int trafficClass) {
    super.setTrafficClass(trafficClass);
    return this;
  }

  public MailConfig setTcpNoDelay(boolean tcpNoDelay) {
    super.setTcpNoDelay(tcpNoDelay);
    return this;
  }

  public MailConfig setTcpKeepAlive(boolean tcpKeepAlive) {
    super.setTcpKeepAlive(tcpKeepAlive);
    return this;
  }

  public MailConfig setSoLinger(int soLinger) {
    super.setSoLinger(soLinger);
    return this;
  }

  public MailConfig setIdleTimeout(int idleTimeout) {
    super.setIdleTimeout(idleTimeout);
    return this;
  }

  public MailConfig setIdleTimeoutUnit(TimeUnit idleTimeoutUnit) {
    super.setIdleTimeoutUnit(idleTimeoutUnit);
    return this;
  }

  public MailConfig setKeyCertOptions(KeyCertOptions options) {
    super.setKeyCertOptions(options);
    return this;
  }

  public MailConfig setKeyStoreOptions(JksOptions options) {
    super.setKeyStoreOptions(options);
    return this;
  }

  public MailConfig setPfxKeyCertOptions(PfxOptions options) {
    super.setPfxKeyCertOptions(options);
    return this;
  }

  public MailConfig setPemKeyCertOptions(PemKeyCertOptions options) {
    super.setPemKeyCertOptions(options);
    return this;
  }

  public MailConfig setTrustOptions(TrustOptions options) {
    super.setTrustOptions(options);
    return this;
  }

  public MailConfig setTrustStoreOptions(JksOptions options) {
    super.setTrustStoreOptions(options);
    return this;
  }

  public MailConfig setPemTrustOptions(PemTrustOptions options) {
    super.setPemTrustOptions(options);
    return this;
  }

  public MailConfig setPfxTrustOptions(PfxOptions options) {
    super.setPfxTrustOptions(options);
    return this;
  }

  public MailConfig addEnabledCipherSuite(String suite) {
    super.addEnabledCipherSuite(suite);
    return this;
  }

  public MailConfig addEnabledSecureTransportProtocol(String protocol) {
    super.addEnabledSecureTransportProtocol(protocol);
    return this;
  }

  public MailConfig removeEnabledSecureTransportProtocol(String protocol) {
    super.removeEnabledSecureTransportProtocol(protocol);
    return this;
  }

  public MailConfig setUseAlpn(boolean useAlpn) {
    super.setUseAlpn(useAlpn);
    return this;
  }

  public MailConfig setSslEngineOptions(SSLEngineOptions sslEngineOptions) {
    super.setSslEngineOptions(sslEngineOptions);
    return this;
  }

  public MailConfig setJdkSslEngineOptions(JdkSSLEngineOptions sslEngineOptions) {
    super.setJdkSslEngineOptions(sslEngineOptions);
    return this;
  }

  public MailConfig setTcpFastOpen(boolean tcpFastOpen) {
    super.setTcpFastOpen(tcpFastOpen);
    return this;
  }

  public MailConfig setTcpCork(boolean tcpCork) {
    super.setTcpCork(tcpCork);
    return this;
  }

  public MailConfig setTcpQuickAck(boolean tcpQuickAck) {
    super.setTcpQuickAck(tcpQuickAck);
    return this;
  }

  public MailConfig setOpenSslEngineOptions(OpenSSLEngineOptions sslEngineOptions) {
    super.setOpenSslEngineOptions(sslEngineOptions);
    return this;
  }

  public MailConfig addCrlPath(String crlPath) throws NullPointerException {
    super.addCrlPath(crlPath);
    return this;
  }

  public MailConfig addCrlValue(Buffer crlValue) throws NullPointerException {
    super.addCrlValue(crlValue);
    return this;
  }

  public MailConfig setConnectTimeout(int connectTimeout) {
    super.setConnectTimeout(connectTimeout);
    return this;
  }

  public MailConfig setMetricsName(String metricsName) {
    super.setMetricsName(metricsName);
    return this;
  }

  public MailConfig setReconnectAttempts(int attempts) {
    super.setReconnectAttempts(attempts);
    return this;
  }

  public MailConfig setReconnectInterval(long interval) {
    super.setReconnectInterval(interval);
    return this;
  }

  public MailConfig setHostnameVerificationAlgorithm(String hostnameVerificationAlgorithm) {
    super.setHostnameVerificationAlgorithm(hostnameVerificationAlgorithm);
    return this;
  }

  public MailConfig setLogActivity(boolean logEnabled) {
    super.setLogActivity(logEnabled);
    return this;
  }

  public MailConfig setProxyOptions(ProxyOptions proxyOptions) {
    super.setProxyOptions(proxyOptions);
    return this;
  }

  public MailConfig setLocalAddress(String localAddress) {
    super.setLocalAddress(localAddress);
    return this;
  }

  public MailConfig setSslHandshakeTimeout(long sslHandshakeTimeout) {
    super.setSslHandshakeTimeout(sslHandshakeTimeout);
    return this;
  }

  public MailConfig setSslHandshakeTimeoutUnit(TimeUnit sslHandshakeTimeoutUnit) {
    super.setSslHandshakeTimeoutUnit(sslHandshakeTimeoutUnit);
    return this;
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

  // Maintain compatibility of return type
  @Override
  public MailConfig setSsl(boolean isSsl) {
    super.setSsl(isSsl);
    return this;
  }

  // Maintain compatibility of return type
  @Override
  public MailConfig setEnabledSecureTransportProtocols(Set<String> enabledSecureTransportProtocols) {
    super.setEnabledSecureTransportProtocols(enabledSecureTransportProtocols);
    return this;
  }

  // Maintain compatibility of return type
  @Override
  public MailConfig setTrustAll(boolean trustAll) {
    super.setTrustAll(trustAll);
    return this;
  }

  /**
   * get the key store filename to be used when opening SMTP connections
   *
   * @return the keyStore
   * @deprecated use {@link #getTrustStoreOptions}
   */
  @Deprecated
  public String getKeyStore() {
    // Get the trust store options and if there are any get the path
    String keyStore = null;
    JksOptions options = getTrustStoreOptions();
    if (options != null) {
      keyStore = options.getPath();
    }
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
   * @deprecated use {@link #getTrustStoreOptions}
   */
  @Deprecated
  public MailConfig setKeyStore(String keyStore) {
    JksOptions options = getTrustStoreOptions();
    if (options == null) {
      options = new JksOptions();
      this.setTrustOptions(options);
    }
    options.setPath(keyStore);
    return this;
  }

  /**
   * get the key store password to be used when opening SMTP connections
   *
   * @return the keyStorePassword
   * @deprecated use {@link #getTrustStoreOptions}
   */
  @Deprecated
  public String getKeyStorePassword() {
    // Get the trust store options and if there are any get the password
    String keyStorePassword = null;
    JksOptions options = getTrustStoreOptions();
    if (options != null) {
      keyStorePassword = options.getPassword();
    }
    return keyStorePassword;
  }

  /**
   * get the key store password to be used when opening SMTP connections
   *
   * @param keyStorePassword the key store passwords to be set
   * @return a reference to this, so the API can be used fluently
   * @deprecated use {@link #getTrustStoreOptions}
   */
  @Deprecated
  public MailConfig setKeyStorePassword(String keyStorePassword) {
    JksOptions options = getTrustStoreOptions();
    if (options == null) {
      options = new JksOptions();
      this.setTrustOptions(options);
    }
    options.setPassword(keyStorePassword);
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
   * get if connection pool is enabled
   * default is true
   * <p>
   * if the connection pooling is disabled, the max number of sockets is enforced nevertheless
   * <p>
   *
   * @return keep alive value
   */
  public boolean isKeepAlive() {
    return keepAlive;
  }

  /**
   * set if connection pool is enabled
   * default is true
   * <p>
   * if the connection pooling is disabled, the max number of sockets is enforced nevertheless
   * <p>
   *
   * @return this to be able to use the object fluently
   */
  public MailConfig setKeepAlive(boolean keepAlive) {
    this.keepAlive = keepAlive;
    return this;
  }

  /**
   * get if sending allows rcpt errors (default is false)
   * <p>
   * if true, the mail will be sent to the recipients that the server accepted, if any
   * <p>
   *
   * @return the allowRcptErrors
   */
  public boolean isAllowRcptErrors() {
    return allowRcptErrors;
  }

  /**
   * set if sending allows rcpt errors
   * <p>
   * if true, the mail will be sent to the recipients that the server accepted, if any
   * <p>
   *
   * @param allowRcptErrors the allowRcptErrors to set (default is false)
   * @return this to be able to use the object fluently
   */
  public MailConfig setAllowRcptErrors(boolean allowRcptErrors) {
    this.allowRcptErrors = allowRcptErrors;
    return this;
  }

  /**
   * get if ESMTP should be tried as first command (EHLO) (default is true)
   * <p>
   * rfc 1869 states that clients should always attempt EHLO as first command to determine if ESMTP
   * is supported, if this returns an error code, HELO is tried to use old SMTP.
   * If there is a server that does not support EHLO and does not give an error code back, the connection
   * should be closed and retried with HELO. We do not do that and rather support turning off ESMTP with a
   * setting. The odds of this actually happening are very small since the client will not connect to arbitrary
   * smtp hosts on the internet. Since the client knows that is connects to a host that doesn't support ESMTP/EHLO
   * in that way, the property has to be set to false.
   * <p>
   *
   * @return the disableEsmtp
   */
  public boolean isDisableEsmtp() {
    return disableEsmtp;
  }

  /**
   * set if ESMTP should be tried as first command (EHLO)
   * <p>
   * rfc 1869 states that clients should always attempt EHLO as first command to determine if ESMTP
   * is supported, if this returns an error code, HELO is tried to use old SMTP.
   * If there is a server that does not support EHLO and does not give an error code back, the connection
   * should be closed and retried with HELO. We do not do that and rather support turning off ESMTP with a
   * setting. The odds of this actually happening are very small since the client will not connect to arbitrary
   * smtp hosts on the internet. Since the client knows that is connects to a host that doesn't support ESMTP/EHLO
   * in that way, the property has to be set to false.
   * <p>
   *
   * @param disableEsmtp the disableEsmtp to set (default is true)
   * @return this to be able to use the object fluently
   */
  public MailConfig setDisableEsmtp(boolean disableEsmtp) {
    this.disableEsmtp = disableEsmtp;
    return this;
  }

  /**
   * Gets the Mail User Agent(MUA) name that will be used to generate boundary and message id.
   *
   * @return the Mail User Agent(MUA) name used to generate boundary and message id
   */
  public String getUserAgent() {
    return userAgent;
  }

  /**
   * Sets the Mail User Agent(MUA) name.
   *
   *<p>
   * It is used to generate the boundary in case of MultiPart email and the Message-ID.
   *
   * If <code>null</code> is set, DEFAULT_USER_AGENT is used.
   *</p>
   *
   * @param userAgent the Mail User Agent(MUA) name used to generate boundary and message id
   *                  length of userAgent must be smaller than 40 so that the generated boundary
   *                  has no longer 70 characters.
   * @return this to be able to use the object fluently
   */
  public MailConfig setUserAgent(String userAgent) {
    this.userAgent = userAgent;
    if (this.userAgent == null) this.userAgent = DEFAULT_USER_AGENT;
    if (this.userAgent.length() > 40) {
      throw new IllegalArgumentException("Length of Mail User Agent should be less than 40");
    }
    if (!A_TEXT_PATTERN.matcher(this.userAgent).matches()) {
      throw new IllegalArgumentException("Not a valid User Agent name");
    }
    return this;
  }

  /**
   * Is DKIM enabled, defaults to false.
   *
   * @return enableDKIM
   */
  public boolean isEnableDKIM() {
    return enableDKIM;
  }

  /**
   * Sets true to enable DKIM Signatures, sets false to disable it.
   *
   * <p>
   *     This is used most for temporary disable DKIM without removing DKIM opations from current config.
   * </p>
   *
   * @param enableDKIM if DKIM Singature should be enabled
   * @return this to be able to use the object fluently
   */
  public MailConfig setEnableDKIM(boolean enableDKIM) {
    this.enableDKIM = enableDKIM;
    return this;
  }

  /**
   * Gets the DKIM options.
   *
   * @return dkimSignOptions
   */
  public List<DKIMSignOptions> getDKIMSignOptions() {
    return dkimSignOptions;
  }

  /**
   * Adds a DKIMSignOptions.
   *
   * @param dkimSignOptions the DKIMSignOptions
   * @return this to be able to use the object fluently
   */
  public MailConfig addDKIMSignOption(DKIMSignOptions dkimSignOptions) {
    Objects.requireNonNull(dkimSignOptions);
    if (this.dkimSignOptions == null) {
      this.dkimSignOptions = new ArrayList<>();
    }
    if (!this.dkimSignOptions.contains(dkimSignOptions)) {
      this.dkimSignOptions.add(dkimSignOptions);
    }
    return this;
  }

  /**
   * Sets DKIMSignOptions.
   *
   * @param dkimSignOptions the DKIM options
   * @return this to be able to use the object fluently
   */
  public MailConfig setDKIMSignOptions(List<DKIMSignOptions> dkimSignOptions) {
    this.dkimSignOptions = dkimSignOptions;
    return this;
  }

  /**
   * Sets one DKIMSignOptions for convenient.
   *
   * @param dkimSignOptions the DKIM options
   * @return this to be able to use the object fluently
   */
  public MailConfig setDKIMSignOption(DKIMSignOptions dkimSignOptions) {
    Objects.requireNonNull(dkimSignOptions);
    this.dkimSignOptions = Collections.singletonList(dkimSignOptions);
    return this;
  }

  /**
   * Gets the DKIM options.
   *
   * @return dkimSignOptions of the first one or null if nothing specified yet.
   */
  public DKIMSignOptions getDKIMSignOption() {
    return dkimSignOptions == null || dkimSignOptions.isEmpty() ? null : dkimSignOptions.get(0);
  }

  /**
   * Is the pipelining will be used if SMTP server supports it. Default to true.
   *
   * @return if enable pipelining capability if SMTP server supports it.
   */
  public boolean isPipelining() {
    return pipelining;
  }

  /**
   * Sets to enable/disable the pipelining capability if SMTP server supports it.
   *
   * @param pipelining enable pipelining or not
   * @return this to be able to use the object fluently
   */
  public MailConfig setPipelining(boolean pipelining) {
    this.pipelining = pipelining;
    return this;
  }

  /**
   * Should the mail message be always encoded as multipart.
   *
   * @return if the mail message will be encoded as multipart only.
   */
  public boolean isMultiPartOnly() {
    return multiPartOnly;
  }

  /**
   * Sets to encode multipart only or not.
   *
   * When sets to <code>true</code>, the mail message will be encoded as multipart even for simple mails without
   * attachments, see https://github.com/vert-x3/vertx-mail-client/issues/161.
   *
   * @param multiPartOnly encoded as multipart only or not, default to <code>false</code>.
   * @return this to be able to use the object fluently
   */
  public MailConfig setMultiPartOnly(boolean multiPartOnly) {
    this.multiPartOnly = multiPartOnly;
    return this;
  }

  /**
   * @return the connection pool cleaner period in ms.
   */
  public int getPoolCleanerPeriod() {
    return poolCleanerPeriod;
  }

  /**
   * Set the connection pool cleaner period, defaults in milli seconds, a non positive value disables expiration checks and connections
   * will remain in the pool until they are closed.
   *
   * @param poolCleanerPeriod the pool cleaner period
   * @return a reference to this, so the API can be used fluently
   */
  public MailConfig setPoolCleanerPeriod(int poolCleanerPeriod) {
    this.poolCleanerPeriod = poolCleanerPeriod;
    return this;
  }

  /**
   * @return the keep alive timeout value in seconds for SMTP connections
   */
  public int getKeepAliveTimeout() {
    return keepAliveTimeout;
  }

  /**
   * Set the keep alive timeout for SMTP connection, Defaults in seconds.
   * <p/>
   * This value determines how long a connection remains unused in the pool before being evicted and closed.
   * <p/>
   * A timeout of {@code 0} means there is no timeout.
   *
   * @param keepAliveTimeout the timeout, in seconds
   * @return a reference to this, so the API can be used fluently
   */
  public MailConfig setKeepAliveTimeout(int keepAliveTimeout) {
    if (keepAliveTimeout < 0) {
      throw new IllegalArgumentException("keepAliveTimeout must be >= 0");
    }
    this.keepAliveTimeout = keepAliveTimeout;
    return this;
  }

  /**
   * Gets the {@code TimeUnit} of pool cleaning period. Defaults to {@link TimeUnit#MILLISECONDS}
   *
   * @return the {@code TimeUnit} of the pool cleaning period.
   */
  public TimeUnit getPoolCleanerPeriodUnit() {
    return poolCleanerPeriodUnit;
  }

  /**
   * Sets the {@code TimeUnit} of pool cleaning period.
   *
   * @param poolCleanerPeriodUnit the {@code TimeUnit} of the pool cleaning period.
   * @return a reference to this, so the API can be used fluently
   */
  public MailConfig setPoolCleanerPeriodUnit(TimeUnit poolCleanerPeriodUnit) {
    this.poolCleanerPeriodUnit = poolCleanerPeriodUnit;
    return this;
  }

  /**
   * Gets the {@code TimeUnit} of keeping the connections alive timeout. Defaults to {@link TimeUnit#SECONDS}
   *
   * @return the {@code TimeUnit} of keeping the connections alive timeout
   */
  public TimeUnit getKeepAliveTimeoutUnit() {
    return keepAliveTimeoutUnit;
  }

  /**
   * Sets {@code TimeUnit} of keeping connections in the pool alive.
   *
   * @param keepAliveTimeoutUnit the {@code TimeUnit} of keeping the connections alive timeout
   * @return a reference to this, so the API can be used fluently
   */
  public MailConfig setKeepAliveTimeoutUnit(TimeUnit keepAliveTimeoutUnit) {
    this.keepAliveTimeoutUnit = keepAliveTimeoutUnit;
    return this;
  }

  /**
   * convert config object to Json representation
   *
   * @return json object of the config
   */
  public JsonObject toJson() {
    JsonObject json = super.toJson();
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
    if (authMethods != null) {
      json.put("authMethods", authMethods);
    }
    if (ownHostname != null) {
      json.put("ownHostname", ownHostname);
    }
    json.put("maxPoolSize", maxPoolSize);
    if (!keepAlive) {
      json.put("keepAlive", false);
    }
    if (allowRcptErrors) {
      json.put("allowRcptErrors", true);
    }
    if (disableEsmtp) {
      json.put("disableEsmtp", true);
    }
    if (userAgent != null) {
      json.put("userAgent", userAgent);
    }
    if (enableDKIM) {
      json.put("enableDKIM", true);
    }
    if (dkimSignOptions != null) {
      JsonArray array = new JsonArray();
      dkimSignOptions.forEach(array::add);
      json.put("dkimSignOptions", array);
    }
    json.put("pipelining", pipelining);
    json.put("multiFormatOnly", multiPartOnly);
    json.put("poolCleanerPeriod", poolCleanerPeriod);
    json.put("keepAliveTimeout", keepAliveTimeout);
    json.put("poolCleanerPeriodUnit", poolCleanerPeriodUnit.name());
    json.put("keepAliveTimeoutUnit", keepAliveTimeoutUnit.name());

    return json;
  }

  private List<Object> getList() {
    return Arrays.asList(hostname, port, starttls, login, username, password, authMethods, ownHostname, maxPoolSize,
      keepAlive, allowRcptErrors, disableEsmtp, userAgent, enableDKIM, dkimSignOptions, pipelining, multiPartOnly,
      poolCleanerPeriod, keepAliveTimeout, poolCleanerPeriodUnit, keepAliveTimeoutUnit);
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
    } else if (!(o instanceof NetClientOptions)) {
      return false;
    } else if (!super.equals(o)) {
      return false;
    } else {
      final MailConfig that = (MailConfig) o;
      return getList().equals(that.getList());
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    int result = super.hashCode();
    return 31 * result + getList().hashCode();
  }
}

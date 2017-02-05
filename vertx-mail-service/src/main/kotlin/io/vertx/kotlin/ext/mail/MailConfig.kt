package io.vertx.kotlin.ext.mail

import io.vertx.ext.mail.MailConfig
import io.vertx.ext.mail.LoginOption
import io.vertx.ext.mail.StartTLSOptions

fun MailConfig(
  allowRcptErrors: Boolean? = null,
  authMethods: String? = null,
  disableEsmtp: Boolean? = null,
  hostname: String? = null,
  keepAlive: Boolean? = null,
  keyStore: String? = null,
  keyStorePassword: String? = null,
  login: LoginOption? = null,
  maxPoolSize: Int? = null,
  ownHostname: String? = null,
  password: String? = null,
  port: Int? = null,
  ssl: Boolean? = null,
  starttls: StartTLSOptions? = null,
  trustAll: Boolean? = null,
  username: String? = null): MailConfig = io.vertx.ext.mail.MailConfig().apply {

  if (allowRcptErrors != null) {
    this.setAllowRcptErrors(allowRcptErrors)
  }
  if (authMethods != null) {
    this.setAuthMethods(authMethods)
  }
  if (disableEsmtp != null) {
    this.setDisableEsmtp(disableEsmtp)
  }
  if (hostname != null) {
    this.setHostname(hostname)
  }
  if (keepAlive != null) {
    this.setKeepAlive(keepAlive)
  }
  if (keyStore != null) {
    this.setKeyStore(keyStore)
  }
  if (keyStorePassword != null) {
    this.setKeyStorePassword(keyStorePassword)
  }
  if (login != null) {
    this.setLogin(login)
  }
  if (maxPoolSize != null) {
    this.setMaxPoolSize(maxPoolSize)
  }
  if (ownHostname != null) {
    this.setOwnHostname(ownHostname)
  }
  if (password != null) {
    this.setPassword(password)
  }
  if (port != null) {
    this.setPort(port)
  }
  if (ssl != null) {
    this.setSsl(ssl)
  }
  if (starttls != null) {
    this.setStarttls(starttls)
  }
  if (trustAll != null) {
    this.setTrustAll(trustAll)
  }
  if (username != null) {
    this.setUsername(username)
  }
}


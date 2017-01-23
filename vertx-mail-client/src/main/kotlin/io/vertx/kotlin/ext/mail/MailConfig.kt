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
    this.isAllowRcptErrors = allowRcptErrors
  }

  if (authMethods != null) {
    this.authMethods = authMethods
  }

  if (disableEsmtp != null) {
    this.isDisableEsmtp = disableEsmtp
  }

  if (hostname != null) {
    this.hostname = hostname
  }

  if (keepAlive != null) {
    this.isKeepAlive = keepAlive
  }

  if (keyStore != null) {
    this.keyStore = keyStore
  }

  if (keyStorePassword != null) {
    this.keyStorePassword = keyStorePassword
  }

  if (login != null) {
    this.login = login
  }

  if (maxPoolSize != null) {
    this.maxPoolSize = maxPoolSize
  }

  if (ownHostname != null) {
    this.ownHostname = ownHostname
  }

  if (password != null) {
    this.password = password
  }

  if (port != null) {
    this.port = port
  }

  if (ssl != null) {
    this.isSsl = ssl
  }

  if (starttls != null) {
    this.starttls = starttls
  }

  if (trustAll != null) {
    this.isTrustAll = trustAll
  }

  if (username != null) {
    this.username = username
  }

}


package io.vertx.ext.mail.impl;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailClientBuilder;
import io.vertx.ext.mail.MailConfig;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public class MailClientBuilderImpl implements MailClientBuilder {

  private final Vertx vertx;
  private MailConfig configuration;
  private Supplier<Future<UsernamePasswordCredentials>> credentialsSupplier;
  private String poolName;

  public MailClientBuilderImpl(Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  public MailClientBuilder with(MailConfig configuration) {
    this.configuration = Objects.requireNonNull(configuration, "Configuration cannot be null");
    return this;
  }

  @Override
  public MailClientBuilder withCredentialsSupplier(Supplier<Future<UsernamePasswordCredentials>> credentialsSupplier) {
    this.credentialsSupplier = credentialsSupplier;
    return this;
  }

  @Override
  public MailClientBuilder shared(String poolName) {
    this.poolName = Objects.requireNonNull(poolName, "Shared pool name cannot be null");
    return this;
  }

  @Override
  public MailClient build() {
    String poolName = this.poolName != null ? this.poolName : UUID.randomUUID().toString();
    return new MailClientImpl(vertx, configuration, poolName, credentialsSupplier);
  }
}

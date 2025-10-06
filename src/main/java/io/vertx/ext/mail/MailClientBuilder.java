package io.vertx.ext.mail;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials;

import java.util.function.Supplier;

/**
 * A builder for {@link MailClient}.
 */
@VertxGen
public interface MailClientBuilder {

  /**
   * Set a configuration object.
   *
   * @param configuration to be used for sending mails
   * @return a reference to this, so the API can be used fluently
   */
  @Fluent
  MailClientBuilder with(MailConfig configuration);

  /**
   * Set a credentials supplier for the mail client.
   * <p>
   * By default, the credentials are fixed at creation time with {@link MailConfig#setUsername(String)} and {@link MailConfig#setPassword(String)}.
   *
   * @return a reference to this, so the API can be used fluently
   */
  @Fluent
  MailClientBuilder withCredentialsSupplier(Supplier<Future<UsernamePasswordCredentials>> credentialsSupplier);

  /**
   * Set a shared client pool name.
   * <p>
   * The created client will share its connection pool with any other client created with the same pool name.
   *
   * @param poolName the shared pool name
   * @return a reference to this, so the API can be used fluently
   */
  @Fluent
  MailClientBuilder shared(String poolName);

  /**
   * Build and return the client.
   *
   * @return the client as configured by this builder
   */
  MailClient build();
}

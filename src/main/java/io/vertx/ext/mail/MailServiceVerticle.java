package io.vertx.ext.mail;

import io.vertx.core.AbstractVerticle;
import io.vertx.serviceproxy.ProxyHelper;

/**
 * event bus verticle that supports sending mails via a MailService instance running on
 * another machine via the event bus
 * <p>
 * when the standard config can be used (send mails via localhost:25 without login)
 * it is possible to deploy the verticle with the id
 * <pre>{@code vertx run service:io.vertx.mail-service}</pre>
 * and send mails from other machines via the event bus with the service address vertx.mail
 * (on the other hand, if you can send mails via localhost:25, you do not really need the event bus)
 * 
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class MailServiceVerticle extends AbstractVerticle {

  private MailService service;

  @Override
  public void start() {

    // Create the service object
    service = MailService.create(vertx, new MailConfig(config()));

    // And register it on the event bus against the configured address
    final String address = config().getString("address");
    if (address == null) {
      throw new IllegalStateException("address field must be specified in config for service verticle");
    }
    ProxyHelper.registerService(MailService.class, vertx, service, address);

    // Start it
    service.start();
  }

  @Override
  public void stop() {
    service.stop();
  }
}

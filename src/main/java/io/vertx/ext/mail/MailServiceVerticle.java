package io.vertx.ext.mail;

import io.vertx.core.AbstractVerticle;
import io.vertx.serviceproxy.ProxyHelper;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class MailServiceVerticle extends AbstractVerticle {

  MailService service;

  @Override
  public void start() throws Exception {

    // Create the service object
    service = MailService.create(vertx, new MailConfig(config()));

    // And register it on the event bus against the configured address
    String address = config().getString("address");
    if (address == null) {
      throw new IllegalStateException("address field must be specified in config for service verticle");
    }
    ProxyHelper.registerService(MailService.class, vertx, service, address);

    // Start it
    service.start();
  }

  @Override
  public void stop() throws Exception {
    service.stop();
  }
}

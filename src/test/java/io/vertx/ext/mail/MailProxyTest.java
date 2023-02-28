/*
 *  Copyright (c) 2011-2020 The original author or authors
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

import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.net.ProxyOptions;
import io.vertx.core.net.ProxyType;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.test.proxy.SocksProxy;
import io.vertx.test.proxy.TestProxyBase;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests by setting up proxy
 *
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
@RunWith(VertxUnitRunner.class)
public class MailProxyTest extends SMTPTestWiser {

  private static final Logger log = LoggerFactory.getLogger(MailProxyTest.class);

  private TestProxyBase proxy;

  @Test
  public void testSetUpProxy(TestContext context) throws Exception {
    this.testContext = context;
    MailConfig mailConfig = configLogin().setProxyOptions(new ProxyOptions().setType(ProxyType.SOCKS5).setPort(11080));
    proxy = new SocksProxy();
    proxy.start(vertx);
    MailClient client = MailClient.createShared(vertx, mailConfig);
    client.sendMail(exampleMessage(), context.asyncAssertSuccess(r -> {
      assertEquals("localhost:1587", proxy.getLastUri());
      client.close(context.asyncAssertSuccess());
    }));
  }

  @Test
  public void testSetUpProxyAuth(TestContext context) throws Exception {
    this.testContext = context;
    MailConfig mailConfig = configLogin().setProxyOptions(new ProxyOptions()
      .setType(ProxyType.SOCKS5)
      .setPort(11080)
      .setUsername("proxyUser")
      .setPassword("proxyUser")
    );
    proxy = new SocksProxy();
    proxy.username("proxyUser");
    proxy.start(vertx);
    MailClient client = MailClient.createShared(vertx, mailConfig);
    client.sendMail(exampleMessage(), context.asyncAssertSuccess(r -> {
      assertEquals("localhost:1587", proxy.getLastUri());
      client.close(context.asyncAssertSuccess());
    }));
  }

  @Override
  public void tearDown() throws Exception {
    super.tearDown();
    if (proxy != null) {
      log.debug("stop the proxy server.");
      proxy.stop();
    }
  }

}

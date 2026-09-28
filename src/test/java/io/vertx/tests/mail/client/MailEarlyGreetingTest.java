/*
 *  Copyright (c) 2011-2026 The original author or authors
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

package io.vertx.tests.mail.client;

import io.vertx.ext.mail.MailClient;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The server greeting may reach the socket before the connection is initialized by the pool, it must not be lost.
 * <p>
 * The dummy server sends its greeting as soon as the connection is accepted, concurrent sends from non Vert.x
 * threads open connections on several event loops, which makes the greeting race with the connection init.
 */
@RunWith(VertxUnitRunner.class)
public class MailEarlyGreetingTest extends SMTPTestDummy {

  private static final int BURSTS = 50;
  private static final int BURST_SIZE = 8;

  @Test
  public void testConcurrentSendsOnNewConnections(TestContext testContext) throws Exception {
    // no keep alive: every mail opens a new connection and receives a new greeting
    MailClient mailClient = MailClient.create(vertx, configNoSSL().setKeepAlive(false).setMaxPoolSize(BURST_SIZE));
    ExecutorService executor = Executors.newFixedThreadPool(BURST_SIZE);
    try {
      for (int i = 0; i < BURSTS; i++) {
        CyclicBarrier barrier = new CyclicBarrier(BURST_SIZE);
        List<CompletableFuture<Void>> results = new ArrayList<>();
        for (int j = 0; j < BURST_SIZE; j++) {
          CompletableFuture<Void> result = new CompletableFuture<>();
          results.add(result);
          executor.execute(() -> {
            try {
              barrier.await();
              mailClient.sendMail(exampleMessage()).onComplete(ar -> {
                if (ar.succeeded()) {
                  result.complete(null);
                } else {
                  result.completeExceptionally(ar.cause());
                }
              });
            } catch (Exception e) {
              result.completeExceptionally(e);
            }
          });
        }
        try {
          CompletableFuture.allOf(results.toArray(new CompletableFuture[0])).get(2, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
          long lost = results.stream().filter(r -> !r.isDone()).count();
          testContext.fail("burst " + i + ": " + lost + "/" + BURST_SIZE + " sends never completed");
        }
      }
    } finally {
      executor.shutdownNow();
    }
    mailClient.close().onComplete(testContext.asyncAssertSuccess());
  }
}

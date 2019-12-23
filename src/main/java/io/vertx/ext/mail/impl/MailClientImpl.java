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

package io.vertx.ext.mail.impl;

import io.vertx.core.*;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.shareddata.Shareable;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.MailResult;
import io.vertx.ext.mail.impl.dkim.DKIMSigner;
import io.vertx.ext.mail.mailencoder.EncodedPart;
import io.vertx.ext.mail.mailencoder.MailEncoder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * MailClient implementation for sending mails inside the local JVM
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
public class MailClientImpl implements MailClient {

  private static final Logger log = LoggerFactory.getLogger(MailClientImpl.class);

  private static final String POOL_LOCAL_MAP_NAME = "__vertx.MailClient.pools";

  private final Vertx vertx;
  private final MailConfig config;
  private final SMTPConnectionPool connectionPool;
  private final Context resourceContext;
  private final MailHolder holder;
  // hostname will cache getOwnhostname/getHostname result, we have to resolve only once
  // this cannot be done in the constructor since it is async, so its not final
  private volatile String hostname = null;

  private volatile boolean closed = false;

  // DKIMSigners may be initialized in the constructor to reuse on each send.
  // the constructor may throw IllegalStateException because of wrong DKIM configuration.
  private final List<DKIMSigner> dkimSigners;

  public MailClientImpl(Vertx vertx, MailConfig config, String poolName) {
    Objects.requireNonNull(vertx, "Vertx cannot be null");
    Objects.requireNonNull(config, "MailConfig cannot be null");
    Objects.requireNonNull(poolName, "poolName cannot be null");
    this.vertx = vertx;
    this.resourceContext = vertx.getOrCreateContext();
    this.config = config;
    if (config.getOwnHostname() != null) {
      this.hostname = config.getOwnHostname();
    }
    this.holder = lookupHolder(poolName, config);
    this.connectionPool = holder.pool();
    if (config.isEnableDKIM() && config.getDKIMSignOptions() != null) {
      dkimSigners = config.getDKIMSignOptions().stream().map(ops -> new DKIMSigner(ops, vertx)).collect(Collectors.toList());
    } else {
      dkimSigners = Collections.emptyList();
    }
  }

  @Override
  public void close() {
    if (closed) {
      throw new IllegalStateException("Already closed");
    }
    holder.close();
    closed = true;
  }

  @Override
  public MailClient sendMail(MailMessage message, Handler<AsyncResult<MailResult>> resultHandler) {
    if (!closed) {
      if (validateHeaders(message, resultHandler)) {
        if (hostname == null) {
          vertx.<String>executeBlocking(
            fut -> {
              try {
                fut.complete(Utils.getHostname());
              } catch (Exception e) {
                fut.fail(e);
              }
            },
            res -> {
              if (res.succeeded()) {
                hostname = res.result();
                getConnection(message, resultHandler);
              } else {
                handleError(res.cause(), resultHandler);
              }
            });
        } else {
          getConnection(message, resultHandler);
        }
      }
    } else {
      handleError("mail client has been closed", resultHandler);
    }
    return this;
  }

  private void getConnection(MailMessage message, Handler<AsyncResult<MailResult>> resultHandler) {
    ContextInternal ctx = (ContextInternal)vertx.getOrCreateContext();
    Handler<AsyncResult<MailResult>> ctxResultHandler = resultHandler == null ? null : h -> ctx.dispatch(h, resultHandler);
    this.resourceContext.runOnContext(a -> this.connectionPool.getConnection(this.hostname, result -> {
      if (result.succeeded()) {
        final SMTPConnection connection = result.result();
        // when the connection is retrieved from the pool, it cannot be retrieved by other clients,
        // and it is ready for mail transactions
        connection.setContext(this.resourceContext);
        connection.setErrorHandler(th -> handleError(th, ctxResultHandler));
        sendMessage(message, connection, ctxResultHandler);
      } else {
        handleError(result.cause(), ctxResultHandler);
      }
    }));
  }

  private Future<Void> dkimFuture(EncodedPart encodedPart) {
    List<Future> dkimFutures = new ArrayList<>();
    // run dkim sign, and add email header after that.
    dkimSigners.forEach(dkim -> dkimFutures.add(dkim.signEmail(this.resourceContext, encodedPart)));
    return CompositeFuture.all(dkimFutures).map(f -> {
      List<String> dkimHeaders = dkimFutures.stream().map(fr -> fr.result().toString()).collect(Collectors.toList());
      encodedPart.headers().add(DKIMSigner.DKIM_SIGNATURE_HEADER, dkimHeaders);
      return null;
    });
  }

  private void sendMessage(MailMessage email, SMTPConnection conn, Handler<AsyncResult<MailResult>> resultHandler) {
    log.debug("sendMessage()");
    final Handler<AsyncResult<MailResult>> sentResultHandler = result -> {
      if (result.succeeded()) {
        conn.returnToPool();
      } else {
        conn.setBroken();
      }
      returnResult(result, resultHandler);
    };
    try {
      final MailEncoder encoder = new MailEncoder(email, this.hostname);
      final EncodedPart encodedPart = encoder.encodeMail();
      final String messageId = encoder.getMessageID();

      final SMTPSendMail sendMail = new SMTPSendMail(conn, email, config, encodedPart, messageId, sentResultHandler);
      if (dkimSigners.isEmpty()) {
        sendMail.start();
      } else {
        // generate the DKIM header before start
        dkimFuture(encodedPart).setHandler(dkim -> {
          if (dkim.succeeded()) {
            sendMail.start();
          } else {
            sentResultHandler.handle(Future.failedFuture(dkim.cause()));
          }
        });
      }
    } catch (Exception e) {
      handleError(e, sentResultHandler);
    }
  }

  // do some validation before we open the connection
  // return true on successful validation so we can stop processing above
  private boolean validateHeaders(MailMessage email, Handler<AsyncResult<MailResult>> resultHandler) {
    if (email.getBounceAddress() == null && email.getFrom() == null) {
      handleError("sender address is not present", resultHandler);
      return false;
    } else if ((email.getTo() == null || email.getTo().size() == 0)
        && (email.getCc() == null || email.getCc().size() == 0)
        && (email.getBcc() == null || email.getBcc().size() == 0)) {
      log.warn("no recipient addresses are present");
      handleError("no recipient addresses are present", resultHandler);
      return false;
    } else {
      return true;
    }
  }

  private void handleError(String message, Handler<AsyncResult<MailResult>> resultHandler) {
    log.debug("handleError:" + message);
    returnResult(Future.failedFuture(message), resultHandler);
  }

  private void handleError(Throwable t, Handler<AsyncResult<MailResult>> resultHandler) {
    log.debug("handleError", t);
    returnResult(Future.failedFuture(t), resultHandler);
  }

  private void returnResult(AsyncResult<MailResult> result, Handler<AsyncResult<MailResult>> resultHandler) {
    // Note - results must always be executed on the right resourceContext, asynchronously, not directly!
    if (resultHandler != null) {
      resultHandler.handle(result);
    } else {
      if (result.succeeded()) {
        log.debug("dropping sendMail result");
      } else {
        log.info("dropping sendMail failure", result.cause());
      }
    }
  }

  SMTPConnectionPool getConnectionPool() {
    return connectionPool;
  }

  private MailHolder lookupHolder(String poolName, MailConfig config) {
    synchronized (vertx) {
      LocalMap<String, MailHolder> map = vertx.sharedData().getLocalMap(POOL_LOCAL_MAP_NAME);
      MailHolder theHolder = map.get(poolName);
      if (theHolder == null) {
        theHolder = new MailHolder(vertx, config, () -> removeFromMap(map, poolName));
        map.put(poolName, theHolder);
      } else {
        theHolder.incRefCount();
      }
      return theHolder;
    }
  }

  private void removeFromMap(LocalMap<String, MailHolder> map, String poolName) {
    synchronized (vertx) {
      map.remove(poolName);
      if (map.isEmpty()) {
        map.close();
      }
    }
  }

  private class MailHolder implements Shareable {
    final SMTPConnectionPool pool;
    final Runnable closeRunner;
    int refCount = 1;

    MailHolder(Vertx vertx, MailConfig config, Runnable closeRunner) {
      this.closeRunner = closeRunner;
      this.pool= new SMTPConnectionPool(vertx, resourceContext, config);
    }

    SMTPConnectionPool pool() {
      return pool;
    }

    synchronized void incRefCount() {
      refCount++;
    }

    synchronized void close() {
      if (--refCount == 0) {
        pool.close();
        if (closeRunner != null) {
          closeRunner.run();
        }
      }
    }
  }
}

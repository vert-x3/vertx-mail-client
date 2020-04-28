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
import io.vertx.ext.mail.StartTLSOptions;
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
 * @author <a href="mailto: aoingl@gmail.com">Lin Gao</a>
 */
public class MailClientImpl implements MailClient {

  private static final Logger log = LoggerFactory.getLogger(MailClientImpl.class);

  private static final String POOL_LOCAL_MAP_NAME = "__vertx.MailClient.pools";

  private final Vertx vertx;
  private final MailConfig config;
  private final SMTPConnectionPool connectionPool;
  private final MailHolder holder;
  private final ContextInternal context;
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
    this.context = (ContextInternal)vertx.getOrCreateContext();
    this.config = config;
    String verification = config.getHostnameVerificationAlgorithm();
    if ((verification == null || verification.isEmpty()) && !config.isTrustAll() &&
      (config.isSsl() || config.getStarttls() != StartTLSOptions.DISABLED)) {
      // we can use HTTPS verification, which matches the requirements for SMTPS
      this.config.setHostnameVerificationAlgorithm("HTTPS");
    }
    this.holder = lookupHolder(poolName);
    this.connectionPool = holder.pool();

    if (config.getOwnHostname() != null) {
      this.hostname = config.getOwnHostname();
      connectionPool.setOwnerHostName(hostname);
    }
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
    log.trace("sendMail()");
    final Handler<AsyncResult<MailResult>> theHandler;
    if (resultHandler == null) {
      // add default handler to just logging
      theHandler = mr -> {
        if (mr.failed()) {
          log.error("Failed to sendMail", mr.cause());
        } else {
          log.info("Mail sent with result: " + mr.toString());
        }
      };
    } else {
      final Context ctx = vertx.getOrCreateContext();
      theHandler = h -> ctx.runOnContext(v -> resultHandler.handle(h));
    }
    if (!closed) {
      if (validateHeaders(message, theHandler)) {
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
                connectionPool.setOwnerHostName(hostname);
                sendMailInternal(message).onComplete(theHandler);
              } else {
                theHandler.handle(Future.failedFuture(res.cause()));
              }
            });
        } else {
          sendMailInternal(message).onComplete(theHandler);
        }
      }
    } else {
      theHandler.handle(Future.failedFuture("mail client has been closed"));
    }
    return this;
  }

  private Future<MailResult> sendMailInternal(MailMessage message) {
    log.trace("sendMailInternal()");
    return connectionPool.getConnection(context)
      .flatMap(conn -> sendMessage(message, conn));
  }

  private Future<Void> dkimFuture(EncodedPart encodedPart) {
    log.trace("dkimFuture()");
    List<Future> dkimFutures = new ArrayList<>();
    // run dkim sign, and add email header after that.
    dkimSigners.forEach(dkim -> dkimFutures.add(dkim.signEmail(this.context, encodedPart)));
    return CompositeFuture.all(dkimFutures).map(f -> {
      List<String> dkimHeaders = dkimFutures.stream().map(fr -> fr.result().toString()).collect(Collectors.toList());
      encodedPart.headers().add(DKIMSigner.DKIM_SIGNATURE_HEADER, dkimHeaders);
      return null;
    });
  }

  private Future<MailResult> sendMessage(MailMessage email, SMTPConnection conn) {
    log.trace("sendMessage()");
    Promise<MailResult> promise = context.promise();
    try {
      final MailEncoder encoder = new MailEncoder(email, this.hostname);
      final EncodedPart encodedPart = encoder.encodeMail();
      final String messageId = encoder.getMessageID();
      final SMTPSendMail sendMail = new SMTPSendMail(conn, email, config, encodedPart, messageId);
      if (dkimSigners.isEmpty()) {
        sendMail.startMailTransaction(promise);
      } else {
        // generate the DKIM header before start
        dkimFuture(encodedPart).onComplete(dkim -> {
          if (dkim.succeeded()) {
            sendMail.startMailTransaction(promise);
          } else {
            promise.handle(Future.failedFuture(dkim.cause()));
          }
        });
      }
    } catch (Exception e) {
      promise.handle(Future.failedFuture(e));
    }
    return promise.future().onComplete(mr -> {
      if (mr.failed()) {
        conn.fail(mr.cause());
      } else {
        conn.returnToPool();
      }
    });
  }

  // do some validation before we open the connection
  // return true on successful validation so we can stop processing above
  private boolean validateHeaders(MailMessage email, Handler<AsyncResult<MailResult>> resultHandler) {
    if (email.getBounceAddress() == null && email.getFrom() == null) {
      resultHandler.handle(Future.failedFuture("sender address is not present"));
      return false;
    } else if ((email.getTo() == null || email.getTo().size() == 0)
        && (email.getCc() == null || email.getCc().size() == 0)
        && (email.getBcc() == null || email.getBcc().size() == 0)) {
      log.warn("no recipient addresses are present");
      resultHandler.handle(Future.failedFuture("no recipient addresses are present"));
      return false;
    } else {
      return true;
    }
  }

  SMTPConnectionPool getConnectionPool() {
    return connectionPool;
  }

  private MailHolder lookupHolder(String poolName) {
    synchronized (vertx) {
      LocalMap<String, MailHolder> map = vertx.sharedData().getLocalMap(POOL_LOCAL_MAP_NAME);
      MailHolder theHolder = map.get(poolName);
      if (theHolder == null) {
        theHolder = new MailHolder(() -> removeFromMap(map, poolName));
        map.put(poolName, theHolder);
      } else {
        theHolder.incRefCount();
      }
      return theHolder;
    }
  }

  private void removeFromMap(LocalMap<String, MailHolder> map, String dataSourceName) {
    synchronized (vertx) {
      map.remove(dataSourceName);
      if (map.isEmpty()) {
        map.close();
      }
    }
  }

  private class MailHolder implements Shareable {
    final SMTPConnectionPool pool;
    final Runnable closeRunner;
    int refCount = 1;

    MailHolder(Runnable closeRunner) {
      this.closeRunner = closeRunner;
      this.pool= new SMTPConnectionPool(context, config);
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

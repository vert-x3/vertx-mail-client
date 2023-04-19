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
  private final MailHolder holder;
  // hostname will cache getOwnhostname/getHostname result, we have to resolve only once
  // this cannot be done in the constructor since it is async, so its not final
  private volatile String hostname = null;

  private volatile boolean closed = false;

  // DKIMSigners may be initialized in the constructor to reuse on each send.
  // the constructor may throw IllegalStateException because of wrong DKIM configuration.
  private final List<DKIMSigner> dkimSigners;

  public MailClientImpl(Vertx vertx, MailConfig config, String poolName) {
    this.vertx = vertx;
    this.config = config;
    this.holder = lookupHolder(poolName, config);
    this.connectionPool = holder.pool();
    if (config != null && config.isEnableDKIM() && config.getDKIMSignOptions() != null) {
      dkimSigners = config.getDKIMSignOptions().stream().map(ops -> new DKIMSigner(ops, vertx)).collect(Collectors.toList());
    } else {
      dkimSigners = Collections.emptyList();
    }
  }

  @Override
  public Future<Void> close() {
    if (closed) {
      throw new IllegalStateException("Already closed");
    }
    closed = true;
    return holder.close();
  }

  @Override
  public Future<MailResult> sendMail(MailMessage email) {
    ContextInternal context = (ContextInternal) vertx.getOrCreateContext();
    Promise<MailResult> promise = context.promise();
    if (!closed) {
      validateHeaders(email, context)
        .flatMap(ignored -> getHostname())
        .flatMap(ignored -> getConnection(promise::fail, context))
        .flatMap(conn -> sendMessage(email, conn, context).compose(
          result -> conn.returnToPool().transform(ignored -> context.succeededFuture(result)),
          failure -> conn.quitCloseConnection().transform(ignored -> context.failedFuture(failure))))
        .onComplete(promise);
    } else {
      promise.fail("mail client has been closed");
    }
    return promise.future();
  }

  private Future<Void> validateHeaders(MailMessage email, ContextInternal context) {
    if (email.getBounceAddress() == null && email.getFrom() == null) {
      return context.failedFuture("sender address is not present");
    } else if ((email.getTo() == null || email.getTo().size() == 0)
      && (email.getCc() == null || email.getCc().size() == 0)
      && (email.getBcc() == null || email.getBcc().size() == 0)) {
      log.warn("no recipient addresses are present");
      return context.failedFuture("no recipient addresses are present");
    } else {
      return context.succeededFuture();
    }
  }

  private Future<Void> getHostname() {
    if (hostname != null) {
      return Future.succeededFuture();
    }

    return vertx.executeBlocking(promise -> {
      String hname;
      if (config.getOwnHostname() != null) {
        hname = config.getOwnHostname();
      } else {
        hname = Utils.getHostname();
      }
      hostname = hname;
      promise.complete();
    });
  }

  private Future<SMTPConnection> getConnection(Handler<Throwable> errorHandler, ContextInternal context) {
    return connectionPool.getConnection(hostname, context)
      .map(conn -> {
        conn.setErrorHandler(errorHandler);
        return conn;
      });
  }

  private Future<Void> dkimSign(ContextInternal context, EncodedPart encodedPart) {
    if (dkimSigners.isEmpty()) {
      return context.succeededFuture();
    }

    List<Future> dkimFutures = new ArrayList<>();
    // run dkim sign, and add email header after that.
    dkimSigners.forEach(dkim -> dkimFutures.add(dkim.signEmail(context, encodedPart)));
    return CompositeFuture.all(dkimFutures).map(f -> {
      List<String> dkimHeaders = dkimFutures.stream().map(fr -> fr.result().toString()).collect(Collectors.toList());
      encodedPart.headers().add(DKIMSigner.DKIM_SIGNATURE_HEADER, dkimHeaders);
      return null;
    });
  }

  private Future<MailResult> sendMessage(MailMessage email, SMTPConnection conn, ContextInternal context) {
    try {
      final MailEncoder encoder = new MailEncoder(email, hostname, config);
      final EncodedPart encodedPart = encoder.encodeMail(); // may throw
      final String messageId = encoder.getMessageID();
      final SMTPSendMail sendMail = new SMTPSendMail(context, conn, email, config, encodedPart, messageId);

      return dkimSign(context, encodedPart)
        .flatMap(ignored -> sendMail.startMailTransaction());
    } catch (Exception e) {
      return context.failedFuture(e);
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

  private void removeFromMap(LocalMap<String, MailHolder> map, String dataSourceName) {
    synchronized (vertx) {
      map.remove(dataSourceName);
      if (map.isEmpty()) {
        map.close();
      }
    }
  }

  private static class MailHolder implements Shareable {
    final SMTPConnectionPool pool;
    final Runnable closeRunner;
    int refCount = 1;

    MailHolder(Vertx vertx, MailConfig config, Runnable closeRunner) {
      this.closeRunner = closeRunner;
      this.pool = new SMTPConnectionPool(vertx, config);
    }

    SMTPConnectionPool pool() {
      return pool;
    }

    synchronized void incRefCount() {
      refCount++;
    }

    synchronized Future<Void> close() {
      if (--refCount == 0) {
        Future<Void> result = pool.doClose();
        if (closeRunner != null) {
          closeRunner.run();
        }
        return result;
      } else {
        return Future.succeededFuture();
      }
    }
  }
}

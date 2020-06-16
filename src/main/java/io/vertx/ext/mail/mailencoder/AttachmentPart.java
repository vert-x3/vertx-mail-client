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

package io.vertx.ext.mail.mailencoder;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.streams.ReadStream;
import io.vertx.ext.mail.MailAttachment;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

class AttachmentPart extends EncodedPart {

  private static final Logger log = LoggerFactory.getLogger(AttachmentPart.class);

  // Whether to cache the ReadStream into an AsyncFile when DKIM is enabled.
  private static final boolean CACHE_IN_FILE = Boolean.getBoolean("vertx.mail.attachment.cache.file");

  private String cachedFilePath;

  private final MailAttachment attachment;

  AttachmentPart(MailAttachment attachment) {
    this.attachment = attachment;
    if (this.attachment.getData() == null && this.attachment.getStream() == null) {
      throw new IllegalArgumentException("Either data or stream of the attachment cannot be null");
    }
    if (this.attachment.getStream() != null && this.attachment.getSize() < 0) {
      log.warn("Size of the attachment should be specified when using stream");
    }
    headers = MultiMap.caseInsensitiveMultiMap();;
    String name = attachment.getName();
    String contentType;
    if (attachment.getContentType() != null) {
      contentType = attachment.getContentType();
    } else {
      contentType = "application/octet-stream";
    }
    if (name != null) {
      int index = contentType.length() + 22;
      contentType += "; name=\"" + Utils.encodeHeader(name, index) + "\"";
    }
    headers.set("Content-Type", contentType);
    headers.set("Content-Transfer-Encoding", "base64");

    if (attachment.getDescription() != null) {
      headers.set("Content-Description", attachment.getDescription());
    }
    String disposition;
    if (attachment.getDisposition() != null) {
      disposition = attachment.getDisposition();
    } else {
      disposition = "attachment";
    }
    if (name != null) {
      int index = disposition.length() + 33;
      disposition += "; filename=\"" + Utils.encodeHeader(name, index) + "\"";
    }
    headers.set("Content-Disposition", disposition);
    if (attachment.getContentId() != null) {
      headers.set("Content-ID", attachment.getContentId());
    }
    if (attachment.getHeaders() != null) {
      headers.addAll(attachment.getHeaders());
    }

    if (attachment.getData() != null) {
      part = Utils.base64(attachment.getData().getBytes());
    }
  }

  @Override
  public synchronized ReadStream<Buffer> bodyStream(Context context) {
    ReadStream<Buffer> attachStream = this.attachment.getStream();
    if (attachStream == null) {
      return null;
    }
    return new BodyReadStream(context, attachStream, false);
  }

  @Override
  public synchronized ReadStream<Buffer> dkimBodyStream(Context context) {
    ReadStream<Buffer> attachStream = this.attachment.getStream();
    if (attachStream == null) {
      return null;
    }
    return new BodyReadStream(context, attachStream, true);
  }

  @Override
  public int size() {
    if (attachment.getData() == null) {
      return attachment.getSize() < 0 ? 0 : (attachment.getSize() / 3) * 4;
    }
    return super.size();
  }

  // what we need: strings line by line with CRLF as line terminator
  private class BodyReadStream implements ReadStream<Buffer> {

    private final Context context;
    private final ReadStream<Buffer> stream;
    private Handler<Throwable> exceptionHandler;

    private final boolean cacheInMemory;
    private final Buffer cachedBuffer;

    private final boolean cacheInFile;
    private final String cachedFilePath;
    private AsyncFile cachedFile;
    private static final String cacheFilePrefix = "_vertx_mail_attach_";
    private static final String cachFileSuffix = ".data";

    // 57 / 3 * 4 = 76, plus CRLF is 78, which is the email line length limit.
    // see: https://tools.ietf.org/html/rfc5322#section-2.1.1
    private final int size = 57;
    private Buffer streamBuffer;
    private Handler<Buffer> handler;
    private Handler<Void> endHandler;
    private boolean caching;
    private final AtomicBoolean streamEnded = new AtomicBoolean();

    private BodyReadStream(Context context, ReadStream<Buffer> stream, boolean tryReset) {
      Objects.requireNonNull(stream, "ReadStream cannot be null");
      this.stream = stream;
      this.context = context;
      this.streamBuffer = Buffer.buffer();
      if (tryReset) {
        // cache
        if (CACHE_IN_FILE) {
          cacheInFile = true;
          cachedFilePath = context.owner().fileSystem().createTempFileBlocking(cacheFilePrefix, cachFileSuffix);
          cacheInMemory = false;
          cachedBuffer = null;
        } else {
          // cache in memory then
          cacheInFile = false;
          cachedFilePath = null;
          cacheInMemory = true;
          cachedBuffer = Buffer.buffer();
        }
      } else {
        this.cacheInMemory = false;
        this.cachedBuffer = null;
        this.cacheInFile = false;
        this.cachedFilePath = null;
      }
    }

    @Override
    public synchronized BodyReadStream exceptionHandler(Handler<Throwable> handler) {
      if (handler != null) {
        stream.exceptionHandler(handler);
        this.exceptionHandler = handler;
      }
      return this;
    }

    @Override
    public synchronized BodyReadStream handler(@Nullable Handler<Buffer> handler) {
      if (handler == null) {
        stream.handler(null);
        return this;
      }
      this.handler = handler;
      stream.handler(b -> {
        if (streamEnded.get()) {
          handleEventInContext(this.exceptionHandler, new IllegalStateException("Stream has been closed, no more reading."));
          return;
        }
        Buffer buffer = streamBuffer.appendBuffer(b);
        Buffer bufferToSent = Buffer.buffer();
        int start = 0;
        while(start + size < buffer.length()) {
          final String theLine = Utils.base64(buffer.getBytes(start, start + size));
          bufferToSent.appendBuffer(Buffer.buffer(theLine + "\r\n"));
          start += size;
        }
        streamBuffer = buffer.getBuffer(start, buffer.length());
        handleEventInContext(this.handler, bufferToSent);
        if (cacheInMemory || cacheInFile) {
          cacheBuffer(b).onComplete(r -> {
            synchronized (BodyReadStream.this) {
              caching = false;
              if (r.failed()) {
                handleEventInContext(this.exceptionHandler, r.cause());
              }
              checkEnd();
            }
          });
        }
      });
      return this;
    }

    // when this method is called, either cacheInMemory or cacheInFile is true
    private synchronized Future<Void> cacheBuffer(Buffer buffer) {
      caching = true;
      Promise<Void> promise = Promise.promise();
      if (cacheInMemory) {
        cachedBuffer.appendBuffer(buffer);
        promise.complete();
      } else {
        if (cachedFile == null) {
          context.owner().fileSystem().open(cachedFilePath, new OpenOptions().setAppend(true))
            .onComplete(c -> context.runOnContext(h -> {
              if (c.succeeded()) {
                synchronized (BodyReadStream.this) {
                  cachedFile = c.result();
                  cachedFile.write(buffer, promise);
                }
              } else {
                promise.fail(c.cause());
              }
            }));
        } else {
          cachedFile.write(buffer, promise);
        }
      }
      return promise.future();
    }

    private synchronized void checkEnd() {
      if (streamEnded.get() && !caching) {
        if (cacheInFile) {
          // cache in an AsyncFile
          AttachmentPart.this.attachment.setStream(cachedFile);
          AttachmentPart.this.cachedFilePath = cachedFilePath;
          handleEventInContext(endHandler, null);
        } else if (cacheInMemory) {
          // next read will be the body in memory.
          part = Utils.base64(cachedBuffer.getBytes());
          handleEventInContext(endHandler, null);
        } else {
          // normal stream, may need to delete the cached file if the cachedFilePath is not null
          if (AttachmentPart.this.cachedFilePath != null) {
            String tmpPath = AttachmentPart.this.cachedFilePath;
            AttachmentPart.this.cachedFilePath = null;
            context.owner().fileSystem().delete(tmpPath).onComplete(deleteCacheFile -> {
              if (deleteCacheFile.succeeded()) {
                handleEventInContext(endHandler, null);
              } else {
                new File(tmpPath).deleteOnExit();
                handleEventInContext(this.exceptionHandler, deleteCacheFile.cause());
              }
            });
          } else {
            handleEventInContext(endHandler, null);
          }
        }
      }
    }

    @Override
    public synchronized BodyReadStream pause() {
      stream.pause();
      return this;
    }

    @Override
    public synchronized BodyReadStream resume() {
      stream.resume();
      return this;
    }

    @Override
    public synchronized BodyReadStream fetch(long amount) {
      stream.fetch(amount);
      return this;
    }

    @Override
    public synchronized BodyReadStream endHandler(@Nullable Handler<Void> endHandler) {
      if (endHandler == null) {
        stream.endHandler(null);
        return this;
      }
      this.endHandler = endHandler;
      stream.endHandler(v -> {
        if (!streamEnded.compareAndSet(false, true)) {
          return;
        }
        if (streamBuffer.length() > 0 && this.handler != null) {
          String theLine = Utils.base64(streamBuffer.getBytes());
          Buffer buffer = Buffer.buffer(theLine + "\r\n");
          handleEventInContext(this.handler, buffer);
        }
        checkEnd();
      });
      return this;
    }

    private <T> void handleEventInContext(Handler<T> handler, T t) {
      if (handler != null) {
        context.runOnContext(h -> handler.handle(t));
      }
    }

  }

}

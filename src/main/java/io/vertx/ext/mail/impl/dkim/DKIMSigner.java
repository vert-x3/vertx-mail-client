/*
 *  Copyright (c) 2011-2019 The original author or authors
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

package io.vertx.ext.mail.impl.dkim;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.streams.Pipe;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;
import io.vertx.ext.mail.DKIMSignOptions;
import io.vertx.ext.mail.CanonicalizationAlgorithm;
import io.vertx.ext.mail.mailencoder.EncodedPart;
import io.vertx.ext.mail.mailencoder.Utils;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * DKIM Signature Singer to sign the email according to the configurations.
 *
 * Refer to: https://tools.ietf.org/html/rfc6376
 *
 * @author <a href="mailto: aoingl@gmail.com">Lin Gao</a>
 */
public class DKIMSigner {

  public static final String DKIM_SIGNATURE_HEADER = "DKIM-Signature";

  private static final Logger logger = LoggerFactory.getLogger(DKIMSigner.class);

  private final DKIMSignOptions dkimSignOptions;
  private final String signatureTemplate;
  private final Signature signatureService;
  private static final Pattern DELIMITER = Pattern.compile("\n");

  /**
   * The Constuctor of DKIMSigner.
   *
   * It validates the {@link DKIMSignOptions} which may throws IllegalStateException.
   *
   * It tries to initialize a {@link Signature} so that it can be reused on each sign.
   *
   * @param dkimSignOptions the {@link DKIMSignOptions} used to perform the DKIM Sign.
   * @throws IllegalStateException the exception to throw on invalid configurations.
   */
  public DKIMSigner(DKIMSignOptions dkimSignOptions, final Vertx vertx) {
    this.dkimSignOptions = dkimSignOptions;
    validate(this.dkimSignOptions);
    this.signatureTemplate = dkimSignatureTemplate();
    try {
      KeyFactory kf = KeyFactory.getInstance("RSA");
      String secretKey = dkimSignOptions.getPrivateKey();
      if (secretKey == null) {
        // private key file should be small, read it directly.
        secretKey = vertx.fileSystem().readFileBlocking(dkimSignOptions.getPrivateKeyPath()).toString();
      }
      final PKCS8EncodedKeySpec keyspec = new PKCS8EncodedKeySpec(Base64.getMimeDecoder().decode(secretKey));
      final PrivateKey privateKey = kf.generatePrivate(keyspec);
      signatureService = Signature.getInstance(dkimSignOptions.getSignAlgo().signatureAlgorithm());
      signatureService.initSign(privateKey);
    } catch (NoSuchAlgorithmException | InvalidKeyException | InvalidKeySpecException e) {
      throw new IllegalStateException("Failed to init the Signature", e);
    }
  }

  /**
   * Validate whether the values are following the spec.
   *
   * @throws IllegalStateException on any specification violence.
   */
  private void validate(DKIMSignOptions ops) throws IllegalStateException {
    // required fields check
    checkRequiredFields(ops);
    // check identity and sdid
    final String auid = ops.getAuid();
    if (auid != null) {
      String sdid = ops.getSdid();
      if (!auid.toLowerCase().endsWith("@" + sdid.toLowerCase())
        && !auid.toLowerCase().endsWith("." + sdid.toLowerCase())) {
        throw new IllegalStateException("Identity domain mismatch, expected is: [xx]@[xx.]sdid");
      }
    }

    // check required signed header field: 'from'
    if (ops.getSignedHeaders().stream().noneMatch(h -> h.equalsIgnoreCase("from"))) {
      throw new IllegalStateException("From field must be selected to sign.");
    }

  }

  private void checkRequiredFields(DKIMSignOptions ops) {
    if (ops.getSignAlgo() == null) {
      throw new IllegalStateException("Sign Algorithm is required: rsa-sha1 or rsa-sha256");
    }
    if (ops.getPrivateKey() == null && ops.getPrivateKeyPath() == null) {
      throw new IllegalStateException("Either private key or private key file path must be specified to sign");
    }
    if (ops.getSignedHeaders() == null || ops.getSignedHeaders().isEmpty()) {
      throw new IllegalStateException("Email header fields to sign must be set");
    }
    if (ops.getSdid() == null) {
      throw new IllegalStateException("Singing Domain Identifier(SDID) must be specified");
    }
    if (ops.getSelector() == null) {
      throw new IllegalStateException("The selector must be specified to be able to verify");
    }
  }

  String dkimSignatureTemplate() {
    final StringBuilder sb = new StringBuilder();
    // version is always 1
    sb.append("v=1; ");
    // sign algorithm
    sb.append("a=").append(this.dkimSignOptions.getSignAlgo().dkimAlgoName()).append("; ");
    // optional message algoName
    CanonicalizationAlgorithm bodyCanonic = this.dkimSignOptions.getBodyCanonAlgo();
    CanonicalizationAlgorithm headerCanonic = this.dkimSignOptions.getHeaderCanonAlgo();
    sb.append("c=").append(headerCanonic.algoName()).append("/").append(bodyCanonic.algoName()).append("; ");

    // sdid
    String sdid = dkimQuotedPrintable(this.dkimSignOptions.getSdid());
    sb.append("d=").append(sdid).append("; ");
    // optional auid
    String auid = this.dkimSignOptions.getAuid();
    if (auid != null) {
      sb.append("i=").append(dkimQuotedPrintable(auid)).append("; ");
    } else {
      sb.append("i=").append("@").append(sdid).append("; ");
    }
    // selector
    sb.append("s=").append(dkimQuotedPrintable(this.dkimSignOptions.getSelector())).append("; ");
    // h=
    String signHeadersString = String.join(":", this.dkimSignOptions.getSignedHeaders());
    sb.append("h=").append(signHeadersString).append("; ");
    // body limit
    if (this.dkimSignOptions.getBodyLimit() > 0) {
      sb.append("l=").append(this.dkimSignOptions.getBodyLimit()).append("; ");
    }
    // optional sign time
    if (this.dkimSignOptions.isSignatureTimestamp() || this.dkimSignOptions.getExpireTime() > 0) {
      long time = System.currentTimeMillis() / 1000; // in seconds
      sb.append("t=").append(time).append("; ");
      if (this.dkimSignOptions.getExpireTime() > 0) {
        long expire = time + this.dkimSignOptions.getExpireTime();
        sb.append("x=").append(expire).append("; ");
      }
    }
    return sb.toString();
  }

  /**
   * Perform the DKIM Signature sign action.
   *
   * @param context the Vert.x Context so that it can run the blocking code like calculating the body hash
   * @param encodedMessage The Encoded Message to be ready to sent to the wire
   * @return The Future with a result as the value of header: 'DKIM-Signature'
   */
  public Future<String> signEmail(Context context, EncodedPart encodedMessage) {
    return bodyHashing(context, encodedMessage).map(bh -> {
      if (logger.isDebugEnabled()) {
        logger.debug("DKIM Body Hash: " + bh);
      }
      try {
        final StringBuilder dkimTagListBuilder = dkimTagList(encodedMessage).append("bh=").append(bh).append("; b=");
        String dkimSignHeaderCanonic = canonicHeader(DKIM_SIGNATURE_HEADER, dkimTagListBuilder.toString());
        final String tobeSigned = headersToSign(encodedMessage).append(dkimSignHeaderCanonic).toString();
        if (logger.isDebugEnabled()) {
          logger.debug("To be signed DKIM header: " + tobeSigned);
        }
        String returnStr;
        synchronized (signatureService) {
          signatureService.update(tobeSigned.getBytes());
          String sig = Base64.getEncoder().encodeToString(signatureService.sign());
          returnStr = dkimTagListBuilder.append(sig).toString();
        }
        if (logger.isDebugEnabled()) {
          logger.debug(DKIM_SIGNATURE_HEADER + ": " + returnStr);
        }
        return returnStr;
      } catch (Exception e) {
        throw new RuntimeException("Cannot sign email", e);
      }
    });
  }

  // the attachPart is a base64 encoded stream already when this method is called.
  private void walkThroughAttachStream(MessageDigest md, ReadStream<Buffer> stream, AtomicInteger written, Promise<Void> promise) {
    final Pipe<Buffer> pipe = stream.pipe();
    Promise<Void> pipePromise = Promise.promise();
    pipePromise.future().onComplete(pr -> {
      pipe.close();
      if (pr.succeeded()) {
        promise.complete();
      } else {
        promise.fail(pr.cause());
      }
    });
    pipe.to(new WriteStream<Buffer>() {
      private final AtomicBoolean ended = new AtomicBoolean(false);

      @Override
      public WriteStream<Buffer> exceptionHandler(Handler<Throwable> handler) {
        return this;
      }

      @Override
      public Future<Void> write(Buffer data) {
        if (!ended.get() && !digest(md, data.getBytes(), written)) {
          // can be end now
          ended.set(true);
        }
        return Future.succeededFuture();
      }

      @Override
      public Future<Void> end() {
        ended.set(true);
        return Future.succeededFuture();
      }

      @Override
      public WriteStream<Buffer> setWriteQueueMaxSize(int maxSize) {
        return this;
      }

      @Override
      public boolean writeQueueFull() {
        return false;
      }

      @Override
      public WriteStream<Buffer> drainHandler(@Nullable Handler<Void> handler) {
        return this;
      }
    }).onComplete(pipePromise);
  }

  private boolean digest(MessageDigest md, byte[] bytes, AtomicInteger written) {
    if (this.dkimSignOptions.getBodyLimit() > 0) {
      int left = this.dkimSignOptions.getBodyLimit() - written.get();
      if (left > 0) {
        int len = Math.min(left, bytes.length);
        md.update(bytes, 0, len);
        written.getAndAdd(len);
      } else {
        return false;
      }
    } else {
      md.update(bytes);
    }
    return true;
  }

  private Future<Boolean> walkBoundaryStartAndHeadersFuture(MessageDigest md, String boundaryStart, EncodedPart part, AtomicInteger written) {
    Promise<Boolean> promise = Promise.promise();
    try {
      StringBuilder sb = new StringBuilder();
      sb.append(boundaryStart);
      part.headers().forEach(entry -> sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n"));
      sb.append("\r\n");
      promise.complete(digest(md, sb.toString().getBytes(), written));
    } catch (Exception e) {
      promise.fail(e);
    }
    return promise.future();
  }

  private void walkThroughMultiPart(Context context, MessageDigest md, EncodedPart multiPart, int index,
                                    AtomicInteger written, Promise<Void> promise) {
    String boundaryStart = "--" + multiPart.boundary() + "\r\n";
    String boundaryEnd = "--" + multiPart.boundary() + "--";
    if (index < multiPart.parts().size()) {
      EncodedPart part = multiPart.parts().get(index);

      Promise<Void> nextPartPromise = Promise.promise();
      nextPartPromise.future().onComplete(r -> {
        if (r.succeeded()) {
          walkThroughMultiPart(context, md, multiPart, index + 1, written, promise);
        } else {
          promise.fail(r.cause());
        }
      });
      // boundary and header, then body
      walkBoundaryStartAndHeadersFuture(md, boundaryStart, part, written).onComplete(r -> {
        if (r.succeeded()) {
          if (r.result()) {
            if (part.parts() != null && part.parts().size() > 0) {
              // part is a multipart as well
              walkThroughMultiPart(context, md, part, 0, written, nextPartPromise);
            } else {
              // walk through part body
              if (part.body() != null) {
                String canonicBody = dkimMailBody(part.body());
                digest(md, canonicBody.getBytes(), written);
                nextPartPromise.complete();
              } else {
                ReadStream<Buffer> dkimAttachStream = part.dkimBodyStream(context);
                if (dkimAttachStream != null) {
                  walkThroughAttachStream(md, dkimAttachStream, written, nextPartPromise);
                } else {
                  nextPartPromise.fail("No data and stream found.");
                }
              }
            }
          } else {
            promise.complete();
          }
        } else {
          promise.fail(r.cause());
        }
      });
    } else {
      // after last part has been walked through
      digest(md, (boundaryEnd + "\r\n").getBytes(), written);
      promise.complete();
    }
  }

  // https://tools.ietf.org/html/rfc6376#section-3.7
  private Future<String> bodyHashing(Context context, EncodedPart encodedMessage) {
    Promise<String> bodyHashPromise = Promise.promise();
    try {
      final MessageDigest md = MessageDigest.getInstance(dkimSignOptions.getSignAlgo().hashAlgorithm());
      if (encodedMessage.parts() != null && encodedMessage.parts().size() > 0) {
        Promise<Void> multiPartWalkThrough = Promise.promise();
        multiPartWalkThrough.future().onComplete(r -> {
          if (r.succeeded()) {
            try {
              // MD has been updated through reading the whole multipart message.
              String bh = Base64.getEncoder().encodeToString(md.digest());
              bodyHashPromise.complete(bh);
            } catch (Exception e) {
              bodyHashPromise.fail(e);
            }
          } else {
            bodyHashPromise.fail(r.cause());
          }
        });
        walkThroughMultiPart(context, md, encodedMessage, 0, new AtomicInteger(), multiPartWalkThrough);
      } else {
        String canonicBody = dkimMailBody(encodedMessage.body());
        digest(md, canonicBody.getBytes(), new AtomicInteger());
        String bh = Base64.getEncoder().encodeToString(md.digest());
        bodyHashPromise.complete(bh);
      }
    } catch (Exception e) {
      bodyHashPromise.fail(e);
    }
    return bodyHashPromise.future();
  }

  private StringBuilder headersToSign(EncodedPart encodedMessage) {
    final StringBuilder signHeaders = new StringBuilder();
    // keep the order in the list, see: https://tools.ietf.org/html/rfc6376#section-3.7
    // multiple instances of one header name may be specified: https://tools.ietf.org/html/rfc6376#section-5.4.2
    final Map<String, Integer> valueIdx = new HashMap<>();
    // in the order of specified signed headers list
    for (String header: dkimSignOptions.getSignedHeaders()) {
      // this is case insensitive headers
      List<String> values = encodedMessage.headers().getAll(header);
      final int size = values.size();
      if (size > 0) {
        // ignore the non-existed headers
        int idx = valueIdx.computeIfAbsent(header.toUpperCase(Locale.ENGLISH), s -> size - 1);
        if (idx >= 0) {
          signHeaders.append(canonicHeader(header, values.get(idx))).append("\r\n");
          valueIdx.put(header.toUpperCase(Locale.ENGLISH), idx - 1);
        }
      }
    }
    return signHeaders;
  }

  /**
   * Computes DKIM Signature Sign tag list.
   *
   * @param encodedMessage the encoded message which is ready to write to the wire
   * @return the StringBuilder represents the tag list based on the specified {@link DKIMSignOptions}
   */
  private StringBuilder dkimTagList(EncodedPart encodedMessage) {
    final StringBuilder dkimTagList = new StringBuilder(this.signatureTemplate);
    // optional copied headers
    if (dkimSignOptions.getCopiedHeaders() != null && dkimSignOptions.getCopiedHeaders().size() > 0) {
      dkimTagList.append("z=").append(copiedHeaders(dkimSignOptions.getCopiedHeaders(), encodedMessage)).append("; ");
    }
    return dkimTagList;
  }

  private String copiedHeaders(List<String> headers, EncodedPart encodedMessage) {
    return headers.stream().map(h -> {
      String hValue = encodedMessage.headers().get(h);
      if (hValue != null) {
        return h + ":" + dkimQuotedPrintableCopiedHeader(hValue);
      }
      throw new RuntimeException("Unknown email header: " + h + " in copied headers.");
    }).collect(Collectors.joining("|"));
  }

  // https://tools.ietf.org/html/rfc6376#section-2.11
  private static String dkimQuotedPrintable(String str) {
    String dkimStr = Utils.encodeQP(str);
    dkimStr = dkimStr.replaceAll(";", "=3B");
    dkimStr = dkimStr.replaceAll(" ", "=20");
    return dkimStr;
  }

  // https://tools.ietf.org/html/rfc6376#page-25
  private String dkimQuotedPrintableCopiedHeader(String value) {
    return dkimQuotedPrintable(value).replaceAll("\\|", "=7C");
  }

  /**
   * Do Email Header Canonicalization.
   *
   * https://tools.ietf.org/html/rfc6376#section-3.4.1
   * https://tools.ietf.org/html/rfc6376#section-3.4.2
   *
   * @param emailHeaderName the email header name used for the canonicalization.
   * @param emailHeaderValue the email header value for the canonicalization.
   * @return the canonicalization email header in format of 'Name':'Value'.
   */
  String canonicHeader(String emailHeaderName, String emailHeaderValue) {
    if (this.dkimSignOptions.getHeaderCanonAlgo() == CanonicalizationAlgorithm.SIMPLE) {
      return emailHeaderName + ": " + emailHeaderValue;
    }
    String headerName = emailHeaderName.trim().toLowerCase();
    return headerName + ":" + canonicalLine(emailHeaderValue, this.dkimSignOptions.getHeaderCanonAlgo());
  }

  String dkimMailBody(String mailBody) {
    Scanner scanner = new Scanner(mailBody).useDelimiter(DELIMITER);
    StringBuilder sb = new StringBuilder();
    while (scanner.hasNext()) {
      sb.append(canonicBodyLine(scanner.nextLine()));
      sb.append("\r\n");
    }
    return sb.toString().replaceFirst("[\r\n]*$", "\r\n");
  }

  // this is shared by header and body for each line's canonicalization.
  private String canonicalLine(String line, CanonicalizationAlgorithm canon) {
    if (CanonicalizationAlgorithm.RELAXED == canon) {
      line = line.replaceAll("[\r\n\t ]+", " ");
      line = line.replaceAll("[\r\n\t ]+$", "");
    }
    return line;
  }

  String canonicBodyLine(String line) {
    return canonicalLine(line, this.dkimSignOptions.getBodyCanonAlgo());
  }

}

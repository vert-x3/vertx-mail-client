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

package io.vertx.ext.mail;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 *
 * This represents the options used to perform DKIM Signature signing action.
 *
 * See: https://tools.ietf.org/html/rfc6376
 *
 * @author <a href="mailto: aoingl@gmail.com">Lin Gao</a>
 */
@DataObject(generateConverter = true)
public class DKIMSignOptions {

  private static final List<String> DEFAULT_HEADERS = new ArrayList<>();
  static {
    DEFAULT_HEADERS.add("From");
    DEFAULT_HEADERS.add("Reply-to");
    DEFAULT_HEADERS.add("Subject");
    DEFAULT_HEADERS.add("Date");
    DEFAULT_HEADERS.add("To");
    DEFAULT_HEADERS.add("Cc");
    DEFAULT_HEADERS.add("Content-Type");
    DEFAULT_HEADERS.add("Message-ID");
  }

  private String privateKey;
  private String privateKeyPath;

  private DKIMSignAlgorithm signAlgo = DKIMSignAlgorithm.RSA_SHA256;
  private List<String> signedHeaders;
  private String sdid;
  private String auid;
  private String selector;
  private MessageCanonic headerCanonic = MessageCanonic.SIMPLE;
  private MessageCanonic bodyCanonic = MessageCanonic.SIMPLE;
  private int bodyLimit = -1;
  private boolean signatureTimestamp;
  private long expireTime = -1L;
  private List<String> copiedHeaders;

  /**
   * Default Constructor.
   */
  public DKIMSignOptions() {
    signedHeaders = new ArrayList<>(DEFAULT_HEADERS);
  }

  public DKIMSignOptions (DKIMSignOptions other) {
    privateKey = other.privateKey;
    privateKeyPath = other.privateKeyPath;
    signAlgo = other.signAlgo;
    if (other.signedHeaders != null && !other.signedHeaders.isEmpty()) {
      signedHeaders = new ArrayList<>(other.signedHeaders);
    } else {
      signedHeaders = new ArrayList<>(DEFAULT_HEADERS);
    }
    sdid = other.sdid;
    auid = other.auid;
    selector = other.selector;
    headerCanonic = other.headerCanonic;
    bodyCanonic = other.bodyCanonic;
    bodyLimit = other.bodyLimit;
    signatureTimestamp = other.signatureTimestamp;
    expireTime = other.expireTime;
    if (other.copiedHeaders != null && !other.copiedHeaders.isEmpty()) {
      copiedHeaders = new ArrayList<>(other.copiedHeaders);
    }
  }

  /**
   * Constructor from a JsonObject.
   *
   * @param config the JsonObject configuration
   */
  public DKIMSignOptions(JsonObject config) {
    DKIMSignOptionsConverter.fromJson(config, this);
    if (signedHeaders == null || signedHeaders.isEmpty()) {
      signedHeaders = new ArrayList<>(DEFAULT_HEADERS);
    }
  }

  /**
   * Converts to JsonObject
   *
   * @return the JsonObject which represents current configuration.
   */
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    DKIMSignOptionsConverter.toJson(this, json);
    return json;
  }

  /**
   * Gets the signing algorithm.
   *
   * @return the signing algorithm
   */
  public DKIMSignAlgorithm getSignAlgo() {
    return signAlgo;
  }

  /**
   * Gets the PKCS#8 format private key used to sign the email.
   *
   * @return the private key
   */
  public String getPrivateKey() {
    return privateKey;
  }

  /**
   * Sets the PKCS#8 format private key used to sign the email.
   *
   * @param privateKey the base64 encdoing private key content.
   * @return a reference to this, so the API can be used fluently
   */
  public DKIMSignOptions setPrivateKey(String privateKey) {
    this.privateKey = privateKey;
    return this;
  }

  /**
   * Gets the PKCS#8 format private key file path.
   *
   * @return the PKCS#8 format private key file path.
   */
  public String getPrivateKeyPath() {
    return privateKeyPath;
  }

  /**
   * Sets the PKCS#8 format private key file path.
   *
   * @param privateKeyPath The PKCS#8 format private key file path.
   * @return a reference to this, so the API can be used fluently
   */
  public DKIMSignOptions setPrivateKeyPath(String privateKeyPath) {
    this.privateKeyPath = privateKeyPath;
    return this;
  }

  /**
   * Sets the signing algorithm.
   *
   * @param signAlgo the signing algorithm
   * @return a reference to this, so the API can be used fluently
   */
  public DKIMSignOptions setSignAlgo(DKIMSignAlgorithm signAlgo) {
    this.signAlgo = signAlgo;
    return this;
  }

  /**
   * Gets the email signedHeaders used to sign.
   *
   * The order in the list matters.
   *
   * @return the email signedHeaders used to sign
   */
  public List<String> getSignedHeaders() {
    return signedHeaders;
  }

  /**
   * Sets the email signedHeaders used to sign.
   *
   * @param signedHeaders the email signedHeaders
   * @return a reference to this, so the API can be used fluently
   */
  public DKIMSignOptions setSignedHeaders(List<String> signedHeaders) {
    this.signedHeaders = signedHeaders;
    return this;
  }

  /**
   * Adds the signed header
   *
   * @param header the header name
   * @return a reference to this, so the API can be used fluently
   */
  public DKIMSignOptions addSignedHeader(String header) {
    if (this.signedHeaders == null) {
      this.signedHeaders = new ArrayList<>();
    }
    this.signedHeaders.add(header);
    return this;
  }

  /**
   * Gets the Singing Domain Identifier(SDID).
   *
   * @return the signing domain identifier
   */
  public String getSdid() {
    return sdid;
  }

  /**
   * Sets the Singing Domain Identifier(SDID).
   *
   * @param sdid the signing domain identifier
   * @return a reference to this, so the API can be used fluently
   */
  public DKIMSignOptions setSdid(String sdid) {
    this.sdid = sdid;
    return this;
  }

  /**
   * Gets the selector used to query public key.
   *
   * @return the selector
   */
  public String getSelector() {
    return selector;
  }

  /**
   * Sets the selector used to query the public key.
   *
   * @param selector the selector
   * @return a reference to this, so the API can be used fluently
   */
  public DKIMSignOptions setSelector(String selector) {
    this.selector = selector;
    return this;
  }

  /**
   * Gets the signedHeaders message canonicalization.
   *
   * @return the message canonicalization for the email signedHeaders
   */
  public MessageCanonic getHeaderCanonic() {
    return headerCanonic;
  }

  /**
   * Sets the message canonicalization for email signedHeaders.
   *
   * @param headerCanonic the message canonicalization for email signedHeaders
   * @return a reference to this, so the API can be used fluently
   */
  public DKIMSignOptions setHeaderCanonic(MessageCanonic headerCanonic) {
    this.headerCanonic = headerCanonic;
    return this;
  }

  /**
   * Gets the email body message canonicalization.
   *
   * @return the message canonicalization for the email body
   */
  public MessageCanonic getBodyCanonic() {
    return bodyCanonic;
  }

  /**
   * Sets the message canonicalization for email body.
   *
   * @param bodyCanonic the message canonicalization for email body
   * @return a reference to this, so the API can be used fluently
   */
  public DKIMSignOptions setBodyCanonic(MessageCanonic bodyCanonic) {
    this.bodyCanonic = bodyCanonic;
    return this;
  }

  /**
   * Gets the Agent or User Identifier(AUID)
   *
   * @return the auid
   */
  public String getAuid() {
    return auid;
  }

  /**
   * Sets the Agent or User Identifier(AUID)
   *
   * @param auid the AUID
   * @return a reference to this, so the API can be used fluently
   */
  public DKIMSignOptions setAuid(String auid) {
    this.auid = auid;
    return this;
  }

  /**
   * Gets the body limit to sign.
   *
   * @return the body limit
   */
  public int getBodyLimit() {
    return bodyLimit;
  }

  /**
   * Sets the body limit to sign.
   *
   * @param bodyLimit the body limit
   * @return a reference to this, so the API can be used fluently
   */
  public DKIMSignOptions setBodyLimit(int bodyLimit) {
    if (bodyLimit <= 0) {
      throw new IllegalArgumentException("Body Limit to calculate the hash must be larger than 0");
    }
    this.bodyLimit = bodyLimit;
    return this;
  }

  /**
   * Adds signature sign timestamp or not.
   *
   * @return true if yes, false otherwise
   */
  public boolean isSignatureTimestamp() {
    return signatureTimestamp;
  }

  /**
   * Sets to enable or disable signature sign timestmap. Default is disabled.
   *
   * @param signatureTimestamp if enable signature sign timestamp or not
   * @return a reference to this, so the API can be used fluently
   */
  public DKIMSignOptions setSignatureTimestamp(boolean signatureTimestamp) {
    this.signatureTimestamp = signatureTimestamp;
    return this;
  }

  /**
   * Gets the expire time in seconds when the signature sign will be expired.
   *
   * @return expire time of signature. Positive value means the signature sign timestamp is enabled.
   */
  public long getExpireTime() {
    return expireTime;
  }

  /**
   * Sets the expire time in seconds when the signature sign will be expired.
   *
   * Success call of this method indicates that the signature sign timestamp is enabled.
   *
   * @param expireTime the expire time in seconds
   * @return a reference to this, so the API can be used fluently
   */
  public DKIMSignOptions setExpireTime(long expireTime) {
    if (expireTime <= 0) {
      throw new IllegalArgumentException("Expire time must be larger than 0");
    }
    this.expireTime = expireTime;
    return this;
  }

  /**
   * Gets the copied headers used in DKIM.
   *
   * @return the copied headers
   */
  public List<String> getCopiedHeaders() {
    return copiedHeaders;
  }

  /**
   * Sets the copied headers used in DKIM.
   *
   * @param copiedHeaders the copied headers
   * @return a reference to this, so the API can be used fluently
   */
  public DKIMSignOptions setCopiedHeaders(List<String> copiedHeaders) {
    this.copiedHeaders = copiedHeaders;
    return this;
  }

  /**
   * Adds a copied header.
   *
   * @param header an email header
   * @returna reference to this, so the API can be used fluently
   */
  public DKIMSignOptions addCopiedHeader(String header) {
    if (this.copiedHeaders == null) {
      this.copiedHeaders = new ArrayList<>();
    }
    if (!this.copiedHeaders.contains(header)) {
      this.copiedHeaders.add(header);
    }
    return this;
  }

  private List<Object> getList() {
    return Arrays.asList(privateKey, privateKeyPath, signAlgo, signedHeaders, sdid, selector, headerCanonic, bodyCanonic
    , auid, bodyLimit, signatureTimestamp, expireTime, copiedHeaders);
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DKIMSignOptions)) {
      return false;
    }
    final DKIMSignOptions ops = (DKIMSignOptions) o;
    return getList().equals(ops.getList());
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return getList().hashCode();
  }

}

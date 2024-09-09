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

/**
 * delegate class to instantiate a SSLSocketFactory using our test keystore
 *
 */
package io.vertx.tests.mail.client;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
public class KeyStoreSSLSocketFactory extends SSLSocketFactory {

  private static final String KEY_STORE_FILE = "src/test/resources/certs/server2.jks";
  private static final String KEY_STORE_PASSWORD = "password";

  private final SSLSocketFactory delegate;

  /**
   * @throws GeneralSecurityException
   * @throws IOException
   */
  public KeyStoreSSLSocketFactory() throws GeneralSecurityException, IOException {
    final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
    final KeyStore keyStore = KeyStore.getInstance("JKS");
    keyStore.load(new FileInputStream(KEY_STORE_FILE), null);
    keyManagerFactory.init(keyStore, KEY_STORE_PASSWORD.toCharArray());
    final SSLContext context = SSLContext.getInstance("TLS");
    context.init(keyManagerFactory.getKeyManagers(), null, null);
    delegate = context.getSocketFactory();
  }

  /*
   * (non-Javadoc)
   *
   * @see javax.net.ssl.SSLSocketFactory#createSocket(java.net.Socket,
   * java.lang.String, int, boolean)
   */
  @Override
  public Socket createSocket(final Socket s, final String host, final int port, final boolean autoClose)
    throws IOException {
    return delegate.createSocket(s, host, port, autoClose);
  }

  /*
   * (non-Javadoc)
   *
   * @see javax.net.ssl.SSLSocketFactory#getDefaultCipherSuites()
   */
  @Override
  public String[] getDefaultCipherSuites() {
    return delegate.getDefaultCipherSuites();
  }

  /*
   * (non-Javadoc)
   *
   * @see javax.net.ssl.SSLSocketFactory#getSupportedCipherSuites()
   */
  @Override
  public String[] getSupportedCipherSuites() {
    return delegate.getSupportedCipherSuites();
  }

  /*
   * (non-Javadoc)
   *
   * @see javax.net.SocketFactory#createSocket(java.lang.String, int)
   */
  @Override
  public Socket createSocket(final String arg0, final int arg1) throws IOException, UnknownHostException {
    return delegate.createSocket(arg0, arg1);
  }

  /*
   * (non-Javadoc)
   *
   * @see javax.net.SocketFactory#createSocket(java.net.InetAddress, int)
   */
  @Override
  public Socket createSocket(final InetAddress arg0, final int arg1) throws IOException {
    return delegate.createSocket(arg0, arg1);
  }

  /*
   * (non-Javadoc)
   *
   * @see javax.net.SocketFactory#createSocket(java.lang.String, int,
   * java.net.InetAddress, int)
   */
  @Override
  public Socket createSocket(final String arg0, final int arg1, final InetAddress arg2, final int arg3)
    throws IOException, UnknownHostException {
    return delegate.createSocket(arg0, arg1, arg2, arg3);
  }

  /*
   * (non-Javadoc)
   *
   * @see javax.net.SocketFactory#createSocket(java.net.InetAddress, int,
   * java.net.InetAddress, int)
   */
  @Override
  public Socket createSocket(final InetAddress arg0, final int arg1, final InetAddress arg2, final int arg3)
    throws IOException {
    return delegate.createSocket(arg0, arg1, arg2, arg3);
  }

}

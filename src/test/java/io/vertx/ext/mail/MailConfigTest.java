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

package io.vertx.ext.mail;

import io.vertx.core.json.JsonObject;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class MailConfigTest {

  @Test
  public void toJsonTest() {
    MailConfig mailConfig = new MailConfig();
    assertTrue(
      mailConfig.toJson().toString()
        .contains("\"hostname\":\"localhost\",\"port\":25,\"starttls\":\"OPTIONAL\",\"login\":\"NONE\",\"maxPoolSize\":10,\"userAgent\":\"" + MailConfig.DEFAULT_USER_AGENT + "\""));
  }

  @Test
  public void toJsonDoesNotIntroduceNullValues() {
    MailConfig mailConfig = new MailConfig();
    MailConfig config = new MailConfig(mailConfig.toJson());
    assertNotNull(config.getHostname());
    assertEquals(config.getPort(), 25);
    assertEquals(config.getStarttls(), StartTLSOptions.OPTIONAL);
    assertEquals(config.getLogin(), LoginOption.NONE);
    assertEquals(config.getMaxPoolSize(), 10);
  }

  @Test
  public void toJsonTest2() {
    MailConfig mailConfig = new MailConfig();
    mailConfig.setUsername("username").setPassword("password");
    assertTrue(
      mailConfig.toJson().toString()
        .contains("\"hostname\":\"localhost\",\"port\":25,\"starttls\":\"OPTIONAL\",\"login\":\"NONE\",\"username\":\"username\",\"password\":\"password\",\"maxPoolSize\":10,\"userAgent\":\"" + MailConfig.DEFAULT_USER_AGENT + "\""));
  }

  @Test
  public void toJsonTest3() {
    MailConfig mailConfig = new MailConfig();
    mailConfig.setHostname(null).setPort(0).setStarttls(null).setLogin(null);
    assertTrue(mailConfig.toJson().toString().contains("\"port\":0,\"maxPoolSize\":10"));
  }

  @Test
  public void toJsonTest4() {
    MailConfig mailConfig = new MailConfig();
    mailConfig.setAuthMethods("PLAIN");
    mailConfig.setOwnHostname("example.com");
    assertTrue(
      mailConfig.toJson().toString()
        .contains("\"hostname\":\"localhost\",\"port\":25,\"starttls\":\"OPTIONAL\",\"login\":\"NONE\",\"authMethods\":\"PLAIN\",\"ownHostname\":\"example.com\",\"maxPoolSize\":10,\"userAgent\":\"" + MailConfig.DEFAULT_USER_AGENT + "\""));
  }

  @Test
  public void toJsonTest5() {
    MailConfig mailConfig = new MailConfig();
    mailConfig.setKeepAlive(false);
    mailConfig.setAllowRcptErrors(true);
    mailConfig.setDisableEsmtp(true);
    assertTrue(
      mailConfig.toJson().toString()
        .contains("\"hostname\":\"localhost\",\"port\":25,\"starttls\":\"OPTIONAL\",\"login\":\"NONE\",\"maxPoolSize\":10,\"keepAlive\":false,\"allowRcptErrors\":true,\"disableEsmtp\":true,\"userAgent\":\"" + MailConfig.DEFAULT_USER_AGENT + "\""));
  }

  @Test
  public void newJsonTest() {
    JsonObject json = new MailConfig("somehost", 25).toJson();
    json.put("ssl", true);
    MailConfig mailConfig = new MailConfig(json);
    assertEquals("somehost", mailConfig.getHostname());
    assertEquals(StartTLSOptions.OPTIONAL, mailConfig.getStarttls());
    assertTrue(mailConfig.isSsl());
  }

  @Test
  public void newJsonEmptyTest() {
    JsonObject json = new JsonObject("{}");
    MailConfig mailConfig = new MailConfig(json);
    assertTrue(
      mailConfig.toJson().toString()
        .contains("\"hostname\":\"localhost\",\"port\":25,\"starttls\":\"OPTIONAL\",\"login\":\"NONE\",\"maxPoolSize\":10,\"userAgent\":\"" + MailConfig.DEFAULT_USER_AGENT + "\""));
  }

  @Test
  public void testConstructorFromMailConfig() {
    MailConfig mailConfig = new MailConfig();
    mailConfig.setHostname("asdfasdf").setPort(1234);
    MailConfig fromConfig = new MailConfig(mailConfig);
    assertEquals(fromConfig.getHostname(), "asdfasdf");
    assertEquals(fromConfig.getPort(), 1234);
  }

  @Test
  public void testConstructorHostname() {
    MailConfig mailConfig = new MailConfig("somehost");
    assertEquals("somehost", mailConfig.getHostname());
  }

  @Test
  public void testConstructorHostnamePost() {
    MailConfig mailConfig = new MailConfig("somehost", 12345);
    assertEquals("somehost", mailConfig.getHostname());
    assertEquals(12345, mailConfig.getPort());
  }

  @Test
  public void testConstructorAll() {
    MailConfig mailConfig = new MailConfig("somehost", 12345, StartTLSOptions.DISABLED, LoginOption.DISABLED);
    assertEquals("somehost", mailConfig.getHostname());
    assertEquals(12345, mailConfig.getPort());
    assertEquals(StartTLSOptions.DISABLED, mailConfig.getStarttls());
    assertEquals(LoginOption.DISABLED, mailConfig.getLogin());
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorFromMailConfigNull() {
    final MailConfig config = new MailConfig((MailConfig) null);
    assertNotNull(config);
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorFromJsonNull() {
    final MailConfig config = new MailConfig((JsonObject) null);
    assertNotNull(config);
  }

  @Test
  public void testPort() {
    MailConfig mailConfig = new MailConfig();
    assertEquals(12345, mailConfig.setPort(12345).getPort());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testPortIllegal() {
    new MailConfig().setPort(-1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testPortIllegal2() {
    new MailConfig().setPort(65536);
  }

  @Test
  public void testStarttls() {
    MailConfig mailConfig = new MailConfig();
    mailConfig.setStarttls(StartTLSOptions.REQUIRED);
    assertEquals(StartTLSOptions.REQUIRED, mailConfig.getStarttls());
  }

  @Test
  public void testLogin() {
    MailConfig mailConfig = new MailConfig();
    mailConfig.setLogin(LoginOption.REQUIRED);
    assertEquals(LoginOption.REQUIRED, mailConfig.getLogin());
  }

  @Test
  public void testSsl() {
    MailConfig mailConfig = new MailConfig();
    mailConfig.setSsl(true);
    assertTrue(mailConfig.isSsl());
  }

  @Test
  public void testEnabledSecureTransportProtocols() {
    Set<String> enabledProtocols = new LinkedHashSet<>(Arrays.asList("TLSv1"));
    MailConfig mailConfig = new MailConfig();
    mailConfig.setEnabledSecureTransportProtocols(enabledProtocols);
    assertEquals(enabledProtocols, mailConfig.getEnabledSecureTransportProtocols());
  }

  @Test
  public void testUsername() {
    MailConfig mailConfig = new MailConfig();
    mailConfig.setUsername("asdfasdf");
    assertEquals("asdfasdf", mailConfig.getUsername());
  }

  @Test
  public void testPassword() {
    MailConfig mailConfig = new MailConfig();
    mailConfig.setPassword("secret");
    assertEquals("secret", mailConfig.getPassword());
  }

  @Test
  public void testTrustAll() {
    MailConfig mailConfig = new MailConfig();
    mailConfig.setTrustAll(true);
    assertTrue(mailConfig.isTrustAll());
  }

  @Test
  public void testAuthMethods() {
    MailConfig mailConfig = new MailConfig();
    mailConfig.setAuthMethods("PLAIN CRAM-MD5");
    assertEquals("PLAIN CRAM-MD5", mailConfig.getAuthMethods());
  }

  @Test
  public void testKeyStore() {
    MailConfig mailConfig = new MailConfig();
    mailConfig.setKeyStore("asdfasdf");
    assertEquals("asdfasdf", mailConfig.getKeyStore());
  }

  @Test
  public void testKeyStorePasswprd() {
    MailConfig mailConfig = new MailConfig();
    mailConfig.setKeyStorePassword("qwertyqwerty");
    assertEquals("qwertyqwerty", mailConfig.getKeyStorePassword());
  }

  @Test
  public void testOwnHostname() {
    MailConfig mailConfig = new MailConfig();
    mailConfig.setOwnHostname("localhost.localdomain");
    assertEquals("localhost.localdomain", mailConfig.getOwnHostname());
  }

  @Test
  public void testMaxPoolSize() {
    MailConfig mailConfig = new MailConfig();
    mailConfig.setMaxPoolSize(123);
    assertEquals(123, mailConfig.getMaxPoolSize());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMaxPoolSizeIllegal() {
    new MailConfig().setMaxPoolSize(0);
  }

  @Test
  public void testKeepAlive() {
    MailConfig mailConfig = new MailConfig();
    mailConfig.setKeepAlive(false);
    assertFalse(mailConfig.isKeepAlive());
    mailConfig.setKeepAlive(true);
    assertTrue(mailConfig.isKeepAlive());
  }

  @Test
  public void testAllowRcptErrors() {
    MailConfig mailConfig = new MailConfig();
    mailConfig.setAllowRcptErrors(false);
    assertFalse(mailConfig.isAllowRcptErrors());
    mailConfig.setAllowRcptErrors(true);
    assertTrue(mailConfig.isAllowRcptErrors());
  }

  @Test
  public void testDisableEsmtp() {
    MailConfig mailConfig = new MailConfig();
    mailConfig.setDisableEsmtp(false);
    assertFalse(mailConfig.isDisableEsmtp());
    mailConfig.setDisableEsmtp(true);
    assertTrue(mailConfig.isDisableEsmtp());
  }

  @Test
  public void testEquals() {
    MailConfig mailConfig = new MailConfig();
    assertEquals(mailConfig, mailConfig);
// Not true for NetClientOptions, so can't be true for sub-class
//    assertEquals(mailConfig, new MailConfig());
  }

  @Ignore("Not true for NetClientOptions, so can't be true for sub-class")
  @Test
  public void testHashcode() {
    MailConfig mailConfig = new MailConfig();
    assertEquals(mailConfig.hashCode(), new MailConfig().hashCode());
  }

  @Test
  public void testUserAgent() {
    MailConfig mailConfig = new MailConfig();
    mailConfig.setUserAgent("Mail Client");
    assertEquals("Mail Client", mailConfig.getUserAgent());
    mailConfig.setUserAgent("Mail-Client");
    assertEquals("Mail-Client", mailConfig.getUserAgent());
    mailConfig.setUserAgent("Mail_Client");
    assertEquals("Mail_Client", mailConfig.getUserAgent());
    mailConfig.setUserAgent("Mail-Client!");
    assertEquals("Mail-Client!", mailConfig.getUserAgent());
    mailConfig.setUserAgent("Mail.Client");
    assertEquals("Mail.Client", mailConfig.getUserAgent());
    mailConfig.setUserAgent("Mail*Client");
    assertEquals("Mail*Client", mailConfig.getUserAgent());
    mailConfig.setUserAgent("Mail?Client");
    assertEquals("Mail?Client", mailConfig.getUserAgent());
    mailConfig.setUserAgent("Mail%Client");
    assertEquals("Mail%Client", mailConfig.getUserAgent());
    try {
      mailConfig.setUserAgent("Mail@Client");
      fail("Not here");
    } catch (IllegalArgumentException iae) {
      assertEquals("Not a valid User Agent name", iae.getMessage());
    }
  }

}

package io.vertx.ext.mail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetClientOptions;

import org.junit.Test;

public class MailConfigTest {

  @Test
  public void toJsonTest() {
    MailConfig mailConfig = new MailConfig();
    assertEquals("{\"hostname\":\"localhost\",\"port\":25,\"starttls\":\"OPTIONAL\",\"login\":\"NONE\"}", mailConfig
        .toJson().toString());
  }

  @Test
  public void toJsonTest2() {
    MailConfig mailConfig = new MailConfig();
    mailConfig.setUsername("username").setPassword("password").setSsl(true);
    assertEquals(
        "{\"hostname\":\"localhost\",\"port\":25,\"starttls\":\"OPTIONAL\",\"login\":\"NONE\",\"username\":\"username\",\"password\":\"password\",\"ssl\":true}",
        mailConfig.toJson().toString());
  }

  @Test
  public void toJsonTest3() {
    MailConfig mailConfig = new MailConfig();
    mailConfig.setHostname(null).setPort(0).setStarttls(null).setLogin(null);
    assertEquals("{\"port\":0}", mailConfig.toJson().toString());
  }

  @Test
  public void toJsonTest4() {
    MailConfig mailConfig = new MailConfig();
    mailConfig.setTrustAll(true);
    mailConfig.setAuthMethods("PLAIN");
    mailConfig.setEhloHostname("example.com");
    assertEquals(
        "{\"hostname\":\"localhost\",\"port\":25,\"starttls\":\"OPTIONAL\",\"login\":\"NONE\",\"trustall\":true,\"auth_methods\":\"PLAIN\",\"ehlo_hostname\":\"example.com\"}",
        mailConfig.toJson().toString());
  }

  @Test
  public void newJsonTest() {
    JsonObject json = new MailConfig("somehost", 25).toJson();
    json.put("ssl", true);
    MailConfig mailConfig = new MailConfig(json);
    assertEquals("somehost", mailConfig.getHostname());
    assertEquals(StarttlsOption.OPTIONAL, mailConfig.getStarttls());
    assertTrue(mailConfig.isSsl());
  }

  @Test
  public void newJsonEmptyTest() {
    JsonObject json = new JsonObject("{}");
    MailConfig mailConfig = new MailConfig(json);
    assertEquals("{\"hostname\":\"localhost\",\"port\":25}", mailConfig.toJson().encode());
  }

  @Test
  public void testConstructorFromMailConfig() {
    MailConfig mailConfig = new MailConfig();
    mailConfig.setHostname("asdfasdf").setPort(1234);
    assertEquals("{\"hostname\":\"asdfasdf\",\"port\":1234,\"starttls\":\"OPTIONAL\",\"login\":\"NONE\"}",
        new MailConfig(mailConfig).toJson().encode());
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
    MailConfig mailConfig = new MailConfig("somehost", 12345, StarttlsOption.DISABLED, LoginOption.DISABLED);
    assertEquals("somehost", mailConfig.getHostname());
    assertEquals(12345, mailConfig.getPort());
    assertEquals(StarttlsOption.DISABLED, mailConfig.getStarttls());
    assertEquals(LoginOption.DISABLED, mailConfig.getLogin());
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorFromMailConfigNull() {
    new MailConfig((MailConfig) null);
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorFromJsonNull() {
    new MailConfig((JsonObject) null);
  }

  @Test
  public void testStarttls() {
    MailConfig mailConfig = new MailConfig();
    mailConfig.setStarttls(StarttlsOption.REQUIRED);
    assertEquals(StarttlsOption.REQUIRED, mailConfig.getStarttls());
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
  public void testNetClientOptions() {
    MailConfig mailConfig = new MailConfig();
    mailConfig.setNetClientOptions(new NetClientOptions());
    assertEquals(new NetClientOptions(), mailConfig.getNetClientOptions());
  }

  @Test
  public void testEhloHostname() {
    MailConfig mailConfig = new MailConfig();
    mailConfig.setEhloHostname("localhost.localdomain");
    assertEquals("localhost.localdomain", mailConfig.getEhloHostname());
  }

  @Test
  public void testEquals() {
    MailConfig mailConfig = new MailConfig();
    assertEquals(mailConfig, mailConfig);
    assertEquals(mailConfig, new MailConfig());
    assertFalse(mailConfig.equals(null));
    assertFalse(mailConfig.equals(""));
  }

  @Test
  public void testHashcode() {
    MailConfig mailConfig = new MailConfig();
    assertEquals(mailConfig.hashCode(), new MailConfig().hashCode());
  }

}

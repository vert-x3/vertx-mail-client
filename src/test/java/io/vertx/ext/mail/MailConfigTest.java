package io.vertx.ext.mail;

import static org.junit.Assert.assertEquals;
import io.vertx.core.json.JsonObject;

import org.junit.Test;

public class MailConfigTest {

  @Test
  public void toJsonTest() {
    MailConfig mailConfig=new MailConfig();
    assertEquals("{\"hostname\":\"localhost\",\"port\":25,\"starttls\":\"OPTIONAL\",\"login\":\"NONE\"}",mailConfig.toJson().toString());
  }

  @Test
  public void newJsonTest() {
    JsonObject json=new MailConfig("somehost",25).toJson();
    MailConfig mailConfig=new MailConfig(json);
    assertEquals("somehost", mailConfig.getHostname());
    assertEquals(StarttlsOption.OPTIONAL, mailConfig.getStarttls());
  }

  @Test
  public void newJsonEmptyTest() {
    JsonObject json=new JsonObject("{}");
    MailConfig mailConfig = new MailConfig(json);
    assertEquals("{\"hostname\":\"localhost\",\"port\":25}", mailConfig.toJson().encode());
  }
  
  @Test
  public void testConstructorFromMailConfig() {
    MailConfig mailConfig = new MailConfig();
    mailConfig.setHostname("asdfasdf")
      .setPort(1234);
    assertEquals("{\"hostname\":\"asdfasdf\",\"port\":1234,\"starttls\":\"OPTIONAL\",\"login\":\"NONE\"}",
        new MailConfig(mailConfig).toJson().encode());
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorFromMailConfigNull() {
    new MailConfig((MailConfig) null);
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorFromJsonNull() {
    new MailConfig((JsonObject) null);
  }

}

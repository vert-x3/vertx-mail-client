package io.vertx.ext.mail;

import static org.junit.Assert.assertEquals;
import io.vertx.core.json.JsonObject;

import org.junit.Test;

public class MailConfigTest {

  @Test
  public void toJsonTest() {
    MailConfig mailConfig=new MailConfig();
    assertEquals("{\"hostname\":\"localhost\",\"port\":25,\"starttls\":\"OPTIONAL\",\"login\":\"NONE\",\"ssl\":false}",mailConfig.toJson().toString());
  }

  @Test
  public void newJsonTest() {
    JsonObject json=new MailConfig("somehost",25).toJson();
    MailConfig mailConfig=new MailConfig(json);
    assertEquals("somehost", mailConfig.getHostname());
    assertEquals(StarttlsOption.OPTIONAL, mailConfig.getStarttls());
  }

}

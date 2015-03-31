package io.vertx.ext.mail.mailencoder;

import static org.junit.Assert.*;

import org.junit.Test;

public class UtilsTest {

  @Test
  public void testMustEncodeChar() {
    StringBuilder sb=new StringBuilder();

    for(int i=0;i<256;i++) {
      if(!Utils.mustEncode((char)i)) {
        sb.append((char)i);
      }
    }
    assertEquals("\n !\"#$%&'()*+,-./0123456789:;<>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\u007f", sb.toString());
  }

  @Test
  public void testDate() {
    System.out.println(Utils.generateDate());
  }

  @Test
  public void testBase64() {
    assertEquals("", Utils.base64(""));
    assertEquals("Kg==", Utils.base64("*"));
    assertEquals("KioqKioqKioqKioqKioqKioqKioqKioqKioqKioqKioqKioqKioqKioqKioqKioqKioqKioqKioq\n" + 
        "KioqKioqKioqKioqKioqKioqKioqKioqKioqKioqKioqKioqKg==",
        conv2nl(Utils.base64("**********************************************************************************************")));
  }

  // convert windows style line endings
  // TODO: not sure why the java8 base64 classes use CRLF
  private String conv2nl(String string) {
      return string.replace("\r\n", "\n");
  }
  
}

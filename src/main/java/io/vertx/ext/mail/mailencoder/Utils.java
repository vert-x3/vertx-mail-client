package io.vertx.ext.mail.mailencoder;

import java.io.UnsupportedEncodingException;
import java.util.Base64;

class Utils {

  private Utils() {
  }

  static String encodeQP(String text) {
    StringBuilder sb=new StringBuilder();

    int column=0;
    for(int i=0;i<text.length();i++) {
      char ch=text.charAt(i);
      if(ch=='\n') {
        column=0;
      } else {
        if(column>76) {
          sb.append("=\n");
          column=0;
        }
      }
      if(mustEncode(ch)) {
        sb.append(String.format("=%02X", ch&0xff));
        column+=3;
      } else {
        sb.append(ch);
        column++;
      }
    }
    return sb.toString();
  }

  static boolean mustEncode(char ch) {
    return (ch&0xff)>=128 || ch>=0 && ch<10 || ch>=11 && ch<32 || ch=='=';
  }

  static String base64(String string) {
    try {
      return Base64.getMimeEncoder().encodeToString(string.getBytes("ISO-8859-1"));
    } catch (UnsupportedEncodingException e) {
      // doesn't happen
      return "";
    }
  }

  static int count=0;

  static String generateBoundary() {

    return "--vertx_mail_"+Thread.currentThread().hashCode()+"_"+System.currentTimeMillis()+"_"+(count++);
  }

}

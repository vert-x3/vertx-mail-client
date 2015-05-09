package io.vertx.ext.mail.impl.sasl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class AuthDigestMD5Test {

  @Test
  public void testAuthDigestMD5() {
    final AuthDigestMD5 auth = new AuthDigestMD5("xxx", "yyy");

    assertNotNull(auth);
    assertEquals("DIGEST-MD5", auth.getName());
  }

  @Test
  public void testGetName() {
    assertEquals("DIGEST-MD5", new AuthDigestMD5("xxx", "yyy").getName());
  }

  /*
   * this is example is from rfc 2831
   */
  @Test
  public void testNextStep() {
    final AuthDigest auth = new AuthDigestMD5("chris@elwood.innosoft.com", "secret") {
      @Override
      public String getCnonce() {
        return "OA6MHXh6VqTrRk";
      }

      @Override
      public String getDigestUri() {
        return "imap/elwood.innosoft.com";
      }
    };
    assertEquals("", auth.nextStep(null));
    final String step1 = auth
        .nextStep("realm=\"elwood.innosoft.com\",nonce=\"OA6MG9tEQGm2hh\",qop=\"auth\",algorithm=md5-sess,charset=utf-8");
    assertEquals(
        sortMap("charset=utf-8,username=\"chris\",realm=\"elwood.innosoft.com\",nonce=\"OA6MG9tEQGm2hh\",nc=00000001,cnonce=\"OA6MHXh6VqTrRk\",digest-uri=\"imap/elwood.innosoft.com\",response=d388dad90d4bbd760a152321f2143af7,qop=auth"),
        sortMap(step1));
    assertEquals("", auth.nextStep("rspauth=ea40f60335c427b5527b84dbabcdfffd"));
    assertEquals(null, auth.nextStep(""));
  }

  @Test
  public void testSmtpServer() {
    AuthDigest auth = new AuthDigestMD5("user@example.com", "password") {
      @Override
      public String getCnonce() {
        return "asdf1234";
      }
    };
    assertEquals("", auth.nextStep(null));
    final String step1 = auth
        .nextStep("nonce=\"ZlGsMYH7FAd+ABU/iap0MjLBWn/CUxypfDsC9zyfn74=\",realm=\"server.example.com\",qop=\"auth\",charset=utf-8,algorithm=md5-sess");
    assertEquals(
        sortMap("charset=utf-8,cnonce=\"asdf1234\",digest-uri=\"smtp/\",nc=00000001,nonce=\"ZlGsMYH7FAd+ABU/iap0MjLBWn/CUxypfDsC9zyfn74=\",qop=auth,realm=\"example.com\",response=6c660aaff71160689ac4ef31b6eb964c,username=\"user\""),
        sortMap(step1));
    assertEquals("", auth.nextStep("rspauth=423e7f36a2e584985fc1ea3035de81d3"));
    assertEquals(null, auth.nextStep(""));
  }

  /**
   * test that we get a failure when the server doesn't authenticate to the
   * client
   */
  @Test
  public void testServerAuthFailed() {
    AuthDigest auth = new AuthDigestMD5("user@example.com", "password") {
      @Override
      public String getCnonce() {
        return "asdf1234";
      }
    };
    assertEquals("", auth.nextStep(null));
    final String step1 = auth
        .nextStep("nonce=\"ZlGsMYH7FAd+ABU/iap0MjLBWn/CUxypfDsC9zyfn74=\",realm=\"server.example.com\",qop=\"auth\",charset=utf-8,algorithm=md5-sess");
    assertEquals(
        sortMap("charset=utf-8,cnonce=\"asdf1234\",digest-uri=\"smtp/\",nc=00000001,nonce=\"ZlGsMYH7FAd+ABU/iap0MjLBWn/CUxypfDsC9zyfn74=\",qop=auth,realm=\"example.com\",response=6c660aaff71160689ac4ef31b6eb964c,username=\"user\""),
        sortMap(step1));
    assertEquals(null, auth.nextStep("rspauth=00000000000000000000000000000000"));
  }

  /**
   * @param string
   * @return
   */
  private String sortMap(String string) {
    String[] elements = string.split(",");
    Arrays.sort(elements);
    return String.join(",", elements);
  }

  private String mapSortToString(Map<String, String> map) {
    StringBuilder sb = new StringBuilder();
    List<String> keys = new ArrayList<String>(map.keySet());
    Collections.sort(keys);
    boolean first = true;
    for (String k : keys) {
      if (first) {
        first = false;
      } else {
        sb.append(", ");
      }
      sb.append(k);
      sb.append("=");
      sb.append(map.get(k));
    }
    return sb.toString();
  }

  @Test
  public void testParseToMap() {
    assertEquals("", mapSortToString(AuthDigest.parseToMap("")));
    assertEquals("a=1", mapSortToString(AuthDigest.parseToMap("a=1")));
    assertEquals("a=1, b=2", mapSortToString(AuthDigest.parseToMap("a=1,b=2")));
    assertEquals("a=aaa", mapSortToString(AuthDigest.parseToMap("a=\"aaa\"")));
    assertEquals("a=\"\", b=", mapSortToString(AuthDigest.parseToMap("a=\"\\\"\\\"\",b=\"\"")));
    assertEquals("a=\"\", b=", mapSortToString(AuthDigest.parseToMap("a=\\\"\\\",b=\"\"")));
    assertEquals("a=a,b", mapSortToString(AuthDigest.parseToMap("a=\"a,b\"")));
    assertEquals(
        "algorithm=md5-sess, charset=utf-8, nonce=OA6MG9tEQGm2hh, qop=auth, realm=elwood.innosoft.com",
        mapSortToString(AuthDigest
            .parseToMap("realm=\"elwood.innosoft.com\",nonce=\"OA6MG9tEQGm2hh\",qop=\"auth\",algorithm=md5-sess,charset=utf-8")));
  }

}

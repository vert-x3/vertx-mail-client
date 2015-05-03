/**
 * 
 */
package io.vertx.ext.mail.impl.sasl;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
abstract class AuthDigest extends AuthBaseClass {

  private int counter;
  final MessageDigest digest;

  private String serverResponse;

  /**
   * @param username
   * @param password
   */
  protected AuthDigest(String username, String password, String hash) {
    super(username, password);
    counter = 0;
    try {
      digest = MessageDigest.getInstance(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("hash " + hash + " not found", e);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see io.vertx.ext.mail.impl.AuthBaseClass#nextStep(java.lang.String)
   */
  @Override
  public String nextStep(String data) {
    switch (counter++) {
      case 0:
        return "";
      case 1:
        String reply = calcStep1(data);
        return reply;
      case 2:
        if (data.equals("rspauth=" + serverResponse)) {
          return "";
        } else {
          return null;
        }
      default:
        return null;
    }
  }

  /**
   * @param data
   * @return
   */
  private String calcStep1(String data) {
    Map<String, String> digestChallenge = parseToMap(data);
    String qop = digestChallenge.get("qop");
    String nonce = digestChallenge.get("nonce");
    String realm = digestChallenge.get("realm");

    Map<String, String> digestResponse = new HashMap<String, String>();

    String user;
    if (username.contains("@")) {
      int index = username.indexOf('@');
      user = username.substring(0, index);
      realm = username.substring(index + 1);
    } else {
      user = username;
      // realm is used from the challenge
    }

    digestResponse.put("nonce", addQuotes(nonce));
    digestResponse.put("realm", addQuotes(realm));
    digestResponse.put("username", addQuotes(user));
    final String cnonce = getCnonce();
    digestResponse.put("cnonce", addQuotes(cnonce));
    final String nc = "00000001";
    digestResponse.put("nc", nc);
    final String digestUri = getDigestUri();
    digestResponse.put("digest-uri", addQuotes(digestUri));
    digestResponse.put("qop", qop);
    digestResponse.put("charset", "utf-8");

    digestResponse.put("response", response(user, realm, nonce, cnonce, qop, nc, digestUri, "AUTHENTICATE"));
    serverResponse = response(user, realm, nonce, cnonce, qop, nc, digestUri, "");

    return encodeMap(digestResponse);
  }

  /**
   * the digest implementation is directly from the rfc impl
   *
   * @param user
   * @param realm
   * @param nonce
   * @param cnonce
   * @param qop
   * @param nc
   * @param digestUri
   *
   * @return Digest-MD5 value of the input params
   */
  private String response(final String user, final String realm, final String nonce, final String cnonce, String qop,
      final String nc, final String digestUri, final String operation) {
    final byte[] colon = b(":");

    byte[] A1 = concatBytes(hash(concatBytes(b(user), colon, b(realm), colon, b(password))), colon, b(nonce), colon,
        b(cnonce));
    byte[] A2 = concatBytes(b(operation), colon, b(digestUri));

    String responseValue = hexKd(hexHash(A1), nonce + ":" + nc + ":" + cnonce + ":" + qop + ":" + hexHash(A2));
    return responseValue;
  }

  private byte[] b(String str) {
    try {
      return str.getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      return null;
    }
  }

  private byte[] concatBytes(byte[]... bytes) {
    int length = 0;
    for (byte[] b : bytes) {
      length += b.length;
    }

    byte[] newArray = new byte[length];

    int index = 0;
    for (byte[] b : bytes) {
      System.arraycopy(b, 0, newArray, index, b.length);
      index += b.length;
    }
    return newArray;
  }

  /**
   * @param string
   * @return
   */
  private byte[] hash(byte[] data) {
    return digest.digest(data);
  }

  /**
   * @param string
   * @return
   */
  private String hexKd(String string, String string2) {
    return hexHash(concatBytes(b(string), b(":"), b(string2)));
  }

  /**
   * @param a1
   * @return
   */
  private String hexHash(byte[] data) {
    return CryptUtils.encodeHex(hash(data));
  }

  /**
   * @param digestResponse
   * @return
   */
  private String encodeMap(Map<String, String> digestResponse) {
    StringBuilder sb = new StringBuilder();

    boolean first = true;
    for (Map.Entry<String, String> entry : digestResponse.entrySet()) {
      if (first) {
        first = false;
      } else {
        sb.append(',');
      }
      sb.append(entry.getKey() + "=" + entry.getValue());
    }
    return sb.toString();
  }

  /**
   * @param string
   * @return
   */
  private String addQuotes(String string) {
    return "\"" + string.replaceAll("\"", "\\\"") + "\"";
  }

  /**
   * parse a key/value list from the challenge
   *
   * @param data
   * @return
   */
  static Map<String, String> parseToMap(String data) {
    List<String> fields = new ArrayList<String>();

    boolean inQuote = false;
    int startIndex = 0;
    for (int i = 0; i < data.length(); i++) {
      char ch = data.charAt(i);
      if (ch == '\\') {
        i++;
      } else {
        if (inQuote) {
          if (ch == '"') {
            inQuote = false;
          }
        } else {
          if (ch == '"') {
            inQuote = true;
          } else if (ch == ',') {
            fields.add(data.substring(startIndex, i));
            startIndex = i + 1;
          }
        }
      }
    }
    fields.add(data.substring(startIndex));

    Map<String, String> map = new HashMap<String, String>();
    for (String f : fields) {
      int equalsIndex = f.indexOf('=');
      if (equalsIndex >= 0) {
        String key = f.substring(0, equalsIndex);
        String val = f.substring(equalsIndex + 1);
        map.put(key, removeQuotes(val));
      }
    }

    return map;
  }

  /**
   * @param string
   * @return
   */
  private static String removeQuotes(String string) {
    StringBuilder sb = new StringBuilder(string.length());

    boolean backslash = false;
    for (int i = 0; i < string.length(); i++) {
      char ch = string.charAt(i);
      if (backslash) {
        sb.append(ch);
        backslash = false;
      } else {
        if (ch == '\\') {
          backslash = true;
        } else if (ch != '"') {
          sb.append(ch);
        }
      }
    }
    return sb.toString();
  }

  /**
   * this is overridable to accommodate unit testing
   *
   * @return a nonce string
   */
  protected String getCnonce() {
    SecureRandom random = new SecureRandom();
    byte[] randomBytes = new byte[16];
    random.nextBytes(randomBytes);
    return CryptUtils.base64(randomBytes);
  }

  protected String getDigestUri() {
    return "smtp/";
  }

}

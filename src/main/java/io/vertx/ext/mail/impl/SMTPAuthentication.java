package io.vertx.ext.mail.impl;

import io.vertx.core.Handler;
import io.vertx.core.impl.NoStackTraceThrowable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.mail.LoginOption;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.StarttlsOption;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Set;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Handle the authentication flow
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
class SMTPAuthentication {

  SMTPConnection connection;
  MailConfig config;
  Handler<Void> finishedHandler;
  Handler<Throwable> errorHandler;

  private static final Logger log = LoggerFactory.getLogger(SMTPAuthentication.class);

  public SMTPAuthentication(SMTPConnection connection, MailConfig config,
      Handler<Void> finishedHandler, Handler<Throwable> errorHandler) {
    this.connection = connection;
    this.config = config;
    this.finishedHandler = finishedHandler;
    this.errorHandler = errorHandler;
  }

  public void startAuthentication() {
    if (!connection.isSsl() && config.getStarttls() == StarttlsOption.REQUIRED) {
      log.warn("STARTTLS required but not supported by server");
      handleError("STARTTLS required but not supported by server");
    } else {
      if (config.getLogin() != LoginOption.DISABLED && config.getUsername() != null
          && config.getPassword() != null && !connection.getCapa().getCapaAuth().isEmpty()) {
        authCmd();
      } else {
        if (config.getLogin() == LoginOption.REQUIRED) {
          if (connection.getCapa().getCapaAuth().isEmpty()) {
            handleError("login is required, but no AUTH methods available. You may need to do STARTTLS");
          } else {
            handleError("login is required, but no credentials supplied");
          }
        } else {
          finished();
        }
      }
    }
  }

  public void authCmd() {
    Set<String> allowedMethods;
    if(config.getAuthMethods() != null && !config.getAuthMethods().isEmpty()) {
      allowedMethods = Utils.parseCapaAuth(config.getAuthMethods());
    } else {
      allowedMethods = null;
    }

    if ((allowedMethods == null || allowedMethods.contains("CRAM-MD5")) && connection.getCapa().getCapaAuth().contains("CRAM-MD5")) {
      connection.write("AUTH CRAM-MD5", message -> {
        log.debug("AUTH result: " + message);
        if (!StatusCode.isStatusOk(message)) {
          log.warn("authentication failed");
          handleError("authentication failed");
        } else {
          cramMD5Step1(message.substring(4));
        }
      });
    } else if ((allowedMethods == null || allowedMethods.contains("PLAIN")) && connection.getCapa().getCapaAuth().contains("PLAIN")) {
      String authdata = base64("\0" + config.getUsername() + "\0" + config.getPassword());
      connection.write("AUTH PLAIN " + authdata, 11, message -> {
        log.debug("AUTH result: " + message);
        if (!StatusCode.isStatusOk(message)) {
          log.warn("authentication failed");
          handleError("authentication failed");
        } else {
          finished();
        }
      });
    } else if ((allowedMethods == null || allowedMethods.contains("LOGIN")) && connection.getCapa().getCapaAuth().contains("LOGIN")) {
      connection.write("AUTH LOGIN", message -> {
        log.debug("AUTH result: " + message);
        if (!StatusCode.isStatusOk(message)) {
          log.warn("authentication failed");
          handleError("authentication failed");
        } else {
          sendUsername();
        }
      });
    } else {
      log.warn("cannot find supported auth method");
      handleError("cannot find supported auth method");
    }
  }

  /**
   * 
   */
  private void finished() {
    finishedHandler.handle(null);
  }

  private void cramMD5Step1(String string) {
    String message = decodeb64(string);
    log.debug("message " + message);
    String reply = hmacMD5hex(config.getPassword(), message);
    connection.write(base64(config.getUsername() + " " + reply), 0, message2 -> {
      log.debug("AUTH step 2 result: " + message2);
      cramMD5Step2(message2);
    });
  }

  static String hmacMD5hex(String keyString, String message) {
    try {
      SecretKey key = new SecretKeySpec(keyString.getBytes("UTF-8"), "HmacMD5");
      Mac mac = Mac.getInstance(key.getAlgorithm());
      mac.init(key);
      return encodeHex(mac.doFinal(message.getBytes("UTF-8")));
    } catch (UnsupportedEncodingException | NoSuchAlgorithmException | InvalidKeyException e) {
      // doesn't happen, auth will fail in that case
      return "";
    }
  }

  /**
   * @param outBytes
   * @return
   */
  static String encodeHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      final int v = ((int) b) & 0xff;
      if (v < 16) {
        sb.append('0');
      }
      sb.append(Integer.toHexString(v));
    }
    return sb.toString();
  }

  private void cramMD5Step2(String message) {
    log.debug(message);
    if (StatusCode.isStatusOk(message)) {
      finished();
    } else {
      log.warn("authentication failed");
      handleError("authentication failed");
    }
  }

  private void sendUsername() {
    connection.write(base64(config.getUsername()), 0, message -> {
      log.debug("username result: " + message);
      if (!StatusCode.isStatusOk(message)) {
        log.warn("authentication failed");
        handleError("authentication failed");
      } else {
        sendPw();
      }
    });
  }

  private void sendPw() {
    connection.write(base64(config.getPassword()), 0, message -> {
      log.debug("pw result: " + message);
      if (StatusCode.isStatusOk(message)) {
        finished();
      } else {
        log.warn("authentication failed");
        handleError("authentication failed");
      }
    });
  }

  private String base64(String string) {
    try {
      // this call does not create multi-line base64 data
      // (if someone uses a password longer than 57 chars or
      // one of the other SASL replies is longer than 76 chars)
      return Base64.getEncoder().encodeToString(string.getBytes("UTF-8"));
    } catch (UnsupportedEncodingException e) {
      // doesn't happen
      return "";
    }
  }

  private String decodeb64(String string) {
    try {
      return new String(Base64.getDecoder().decode(string), "UTF-8");
    } catch (UnsupportedEncodingException e) {
      // doesn't happen
      return "";
    }
  }

  private void handleError(String message) {
    errorHandler.handle(new NoStackTraceThrowable(message));
  }

//  private void handleError(Throwable throwable) {
//    errorHandler.handle(throwable);
//  }

}

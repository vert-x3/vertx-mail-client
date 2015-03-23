package io.vertx.ext.mail;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.mail.mailencoder.EmailAddress;
import io.vertx.ext.mail.mailencoder.MailEncoder;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/*
 * main operation of the smtp client
 *
 * this class takes care of the different SMTP steps, AUTH etc
 * and generates the mail text by using the Email object
 */

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
class MailMain {

  private Vertx vertx;
  private Handler<AsyncResult<JsonObject>> finishedHandler;
  private MailConfig config;
  private static final Logger log = LoggerFactory.getLogger(MailMain.class);

  private MailMessage email;
  private String mailMessage;

  private Set<String> capaAuth = Collections.emptySet();
  private boolean capaStartTLS = false;
  private int capaSize;
  private SMTPConnection connection = null;

  // 8BITMIME can be used if the server supports it, currently this is not
  // implemented
  // private boolean capa8BitMime = false;
  // PIPELINING is not yet used
  // private boolean capaPipelining = false;

  public MailMain(Vertx vertx, MailConfig config, Handler<AsyncResult<JsonObject>> finishedHandler) {
    this.vertx = vertx;
    this.config = config;
    this.finishedHandler = finishedHandler;
  }

  /**
   * start a mail send operation using the MailMessage object
   * 
   * @param email
   *          the mail to send
   */
  void sendMail(MailMessage email) {
    this.email = email;
    sendMail();
  }

  /**
   * start a mail send operation using the parameters from MailMessage object
   * and the pregenerated message provided as String
   * 
   * @param email
   *          the mail parameters (from, to, etc)
   * @param message
   *          the message to send
   */
  void sendMail(MailMessage email, String message) {
    this.email = email;
    mailMessage = message;
    sendMail();
  }

  private void sendMail() {
    // do a bit of validation before we open the connection
    validateHeaders();
    connection = new SMTPConnection();
    connection.initializeConnection(vertx, config, this::serverGreeting, this::throwError);
  }

  private void validateHeaders() {
    if (email.getBounceAddress() == null && email.getFrom() == null) {
      throwError("sender address is not present");
    } else if ((email.getTo() == null || email.getTo().size() == 0)
        && (email.getCc() == null || email.getCc().size() == 0)
        && (email.getBcc() == null || email.getBcc().size() == 0)) {
      throwError("no recipient addresses are present");
    }
  }

  private void serverGreeting(final String message) {
    log.debug("server greeting: " + message);
    if (isStatusOk(message)) {
      if (isEsmtpSupported(message)) {
        ehloCmd();
      } else {
        heloCmd();
      }
    } else {
      throwError("got error response " + message);
    }
  }

  private boolean isEsmtpSupported(String message) {
    return message.contains("ESMTP");
  }

  private int getStatusCode(String message) {
    if (message.length() < 4) {
      return 500;
    }
    if (message.charAt(3) != ' ' && message.charAt(3) != '-') {
      return 500;
    }
    try {
      return Integer.valueOf(message.substring(0, 3));
    } catch (NumberFormatException n) {
      return 500;
    }
  }

  private boolean isStatusOk(String message) {
    int statusCode = getStatusCode(message);
    return statusCode >= 200 && statusCode < 400;
  }

  // private boolean isStatusFatal(String message) {
  // return getStatusCode(message) >= 500;
  // }

  private boolean isStatusTemporary(String message) {
    int statusCode = getStatusCode(message);
    return statusCode >= 400 && statusCode < 500;
  }

  private void ehloCmd() {
    connection
        .write(
            "EHLO " + getMyHostname(),
            message -> {
              log.debug("EHLO result: " + message);
              if (isStatusOk(message)) {
                setCapabilities(message);

                // fail if we are exceeding size as early as possible
                createMailMessage();
                if (capaSize > 0 && mailMessage.length() > capaSize) {
                  throwError("message exceeds allowed size limit");
                } else {
                  if (capaStartTLS
                      && !connection.isSsl()
                      && (config.getStarttls() == StarttlsOption.REQUIRED || config.getStarttls() == StarttlsOption.OPTIONAL)) {
                    // do not start TLS if we are connected with SSL
                    // or are already in TLS
                    startTLSCmd();
                  } else {
                    if (!connection.isSsl() && config.getStarttls() == StarttlsOption.REQUIRED) {
                      log.warn("STARTTLS required but not supported by server");
                      throwError("STARTTLS required but not supported by server");
                    } else {
                      if (config.getLogin() != LoginOption.DISABLED && config.getUsername() != null
                          && config.getPassword() != null && !capaAuth.isEmpty()) {
                        authCmd();
                      } else {
                        if (config.getLogin() == LoginOption.REQUIRED) {
                          if (capaAuth.isEmpty()) {
                            throwError("login is required, but no AUTH methods available. You may need to do STARTTLS");
                          } else {
                            throwError("login is required, but no credentials supplied");
                          }
                        } else {
                          mailFromCmd();
                        }
                      }
                    }
                  }
                }
              } else {
                // if EHLO fails, assume we have to do HELO
                if (isStatusTemporary(message)) {
                  heloCmd();
                } else {
                  throwError("EHLO failed with " + message);
                }
              }
            });
  }

  /**
   * @param message
   */
  private void setCapabilities(String message) {
    List<String> capabilities = parseEhlo(message);
    for (String c : capabilities) {
      if (c.equals("STARTTLS")) {
        capaStartTLS = true;
      }
      if (c.startsWith("AUTH ")) {
        capaAuth = parseCapaAuth(c);
      }
      // if (c.equals("8BITMIME")) {
      // capa8BitMime = true;
      // }
      if (c.startsWith("SIZE ")) {
        try {
          capaSize = Integer.parseInt(c.substring(5));
        } catch (NumberFormatException n) {
          capaSize = 0;
        }
      }
    }
  }

  /**
   * @param c
   * @return
   */
  private Set<String> parseCapaAuth(String c) {
    Set<String> authSet = new HashSet<String>();
    for (String a : splitByChar(c.substring(5), ' ')) {
      authSet.add(a);
    }
    return authSet;
  }

  private void heloCmd() {
    connection.write("HELO " + getMyHostname(), message -> {
      log.debug("HELO result: " + message);
      mailFromCmd();
    });
  }

  /**
   * @return
   */
  private String getMyHostname() {
    try {
      InetAddress ip = InetAddress.getLocalHost();
      return ip.getCanonicalHostName();
    } catch (UnknownHostException e) {
      // as a last resort, use localhost
      return "localhost";
    }
  }

  /**
   * 
   */
  private void startTLSCmd() {
    connection.write("STARTTLS", message -> {
      log.debug("STARTTLS result: " + message);
      connection.upgradeToSsl(v -> {
        log.debug("ssl started");
        // capabilities may have changed, e.g.
        // if a service only announces PLAIN/LOGIN
        // on secure channel (e.g. googlemail)
          ehloCmd();
        });
    });
  }

  private List<String> parseEhlo(String message) {
    // parse ehlo and other multiline replies
    List<String> v = new ArrayList<String>();

    String resultCode = message.substring(0, 3);

    for (String l : splitByChar(message, '\n')) {
      if (!l.startsWith(resultCode) || l.charAt(3) != '-' && l.charAt(3) != ' ') {
        log.error("format error in multiline response");
        throwError("format error in multiline response");
      } else {
        v.add(l.substring(4));
      }
    }

    return v;
  }

  /**
   * split string at each occurrence of a character (e.g. \n)
   * 
   * @param message
   *          the string to split
   * @param ch
   *          the char between which we split
   * @return List<String> of the split lines
   */
  private List<String> splitByChar(String message, char ch) {
    List<String> lines = new ArrayList<String>();
    int index = 0;
    int nextIndex;
    while ((nextIndex = message.indexOf(ch, index)) != -1) {
      lines.add(message.substring(index, nextIndex));
      index = nextIndex + 1;
    }
    lines.add(message.substring(index));
    return lines;
  }

  private void authCmd() {
    if (capaAuth.contains("CRAM-MD5")) {
      connection.write("AUTH CRAM-MD5", message -> {
        log.debug("AUTH result: " + message);
        cramMD5Step1(message.substring(4));
      });
    } else if (capaAuth.contains("PLAIN")) {
      String authdata = base64("\0" + config.getUsername() + "\0" + config.getPassword());
      connection.write("AUTH PLAIN " + authdata, 10, message -> {
        log.debug("AUTH result: " + message);
        if (!isStatusOk(message)) {
          log.warn("authentication failed");
          throwError("authentication failed");
        } else {
          mailFromCmd();
        }
      });
    } else if (capaAuth.contains("LOGIN")) {
      connection.write("AUTH LOGIN", message -> {
        log.debug("AUTH result: " + message);
        sendUsername();
      });
    } else {
      log.warn("cannot find supported auth method");
      throwError("cannot find supported auth method");
    }
  }

  private void cramMD5Step1(String string) {
    String message = decodeb64(string);
    log.debug("message " + message);
    String reply = hmacMD5hex(message, config.getPassword());
    connection.write(base64(config.getUsername() + " " + reply), 0, message2 -> {
      log.debug("AUTH step 2 result: " + message2);
      cramMD5Step2(message2);
    });
  }

  private String hmacMD5hex(String message, String pw) {
    try {
      SecretKey key = new SecretKeySpec(pw.getBytes("UTF-8"), "HmacMD5");
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
  private String encodeHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      if (b < 16) {
        sb.append('0');
      }
      sb.append(Integer.toHexString(b).toUpperCase());
    }
    return sb.toString();
  }

  private void cramMD5Step2(String message) {
    log.debug(message);
    if (isStatusOk(message)) {
      mailFromCmd();
    } else {
      log.warn("authentication failed");
      throwError("authentication failed");
    }
  }

  private void sendUsername() {
    connection.write(base64(config.getUsername()), 0, message -> {
      log.debug("username result: " + message);
      sendPw();
    });
  }

  private void sendPw() {
    connection.write(base64(config.getPassword()), 0, message -> {
      log.debug("pw result: " + message);
      if (isStatusOk(message)) {
        mailFromCmd();
      } else {
        log.warn("authentication failed");
        throwError("authentication failed");
      }
    });
  }

  private void mailFromCmd() {
    try {
      String fromAddr;
      String bounceAddr = email.getBounceAddress();
      if (bounceAddr != null && !bounceAddr.isEmpty()) {
        fromAddr = bounceAddr;
      } else {
        fromAddr = email.getFrom();
      }
      EmailAddress from = new EmailAddress(fromAddr);
      connection.write("MAIL FROM:<" + from.getEmail() + ">", message -> {
        log.debug("MAIL FROM result: " + message);
        if (isStatusOk(message)) {
          rcptToCmd();
        } else {
          log.warn("sender address not accepted: " + message);
          throwError("sender address not accepted: " + message);
        }
      });
    } catch (IllegalArgumentException e) {
      log.error("address exception", e);
      throwError(e);
    }
  }

  private void rcptToCmd() {
    List<String> recipientAddrs = new ArrayList<String>();
    if (email.getTo() != null) {
      recipientAddrs.addAll(email.getTo());
    }
    if (email.getCc() != null) {
      recipientAddrs.addAll(email.getCc());
    }
    if (email.getBcc() != null) {
      recipientAddrs.addAll(email.getBcc());
    }
    rcptToCmd(recipientAddrs, 0);
  }

  private void rcptToCmd(List<String> recipientAddrs, int i) {
    try {
      EmailAddress toAddr = new EmailAddress(recipientAddrs.get(i));
      connection.write("RCPT TO:<" + toAddr.getEmail() + ">", message -> {
        log.debug("RCPT TO result: " + message);
        if (isStatusOk(message)) {
          if (i + 1 < recipientAddrs.size()) {
            rcptToCmd(recipientAddrs, i + 1);
          } else {
            dataCmd();
          }
        } else {
          log.warn("recipient address not accepted: " + message);
          throwError("recipient address not accepted: " + message);
        }
      });
    } catch (IllegalArgumentException e) {
      log.error("address exception", e);
      throwError(e);
    }
  }

  private void dataCmd() {
    connection.write("DATA", message -> {
      log.debug("DATA result: " + message);
      if (isStatusOk(message)) {
        sendMaildata();
      } else {
        log.warn("DATA command not accepted: " + message);
        throwError("DATA command not accepted: " + message);
      }
    });
  }

  private void sendMaildata() {
    // create the message here if it hasn't been created
    // for the size check above
    createMailMessage();
    // convert message to escape . at the start of line
    // TODO: this is probably bad for large messages
    // TODO: it is probably required to convert \n to \r\n to be completely
    // SMTP compliant
    connection.write(mailMessage.replaceAll("\n\\.", "\n..") + "\r\n.", message -> {
      log.debug("maildata result: " + message);
      if (isStatusOk(message)) {
        quitCmd();
      } else {
        log.warn("sending data failed: " + message);
        throwError("sending data failed: " + message);
      }
    });
  }

  /**
   * @return
   */
  private void createMailMessage() {
    if (mailMessage == null) {
      MailEncoder encoder = new MailEncoder(email);
      mailMessage = encoder.encode();
    }
  }

  private void quitCmd() {
    connection.write("QUIT", message -> {
      log.debug("QUIT result: " + message);
      if (isStatusOk(message)) {
        finishMail();
      } else {
        log.warn("quit failed: " + message);
        throwError("quit failed: " + message);
      }
    });
  }

  private void finishMail() {
    JsonObject result = new JsonObject();
    result.put("result", "success");
    returnResult(Future.succeededFuture(result));
  }

  private void throwError(Throwable throwable) {
    returnResult(Future.failedFuture(throwable));
  }

  private void throwError(String message) {
    returnResult(Future.failedFuture(message));
  }

  private void returnResult(Future<JsonObject> result) {
    if (connection != null) {
      connection.shutdown();
    }
    finishedHandler.handle(result);
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

}

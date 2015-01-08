package io.vertx.ext.mail;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;
import io.vertx.ext.mail.mailutil.BounceGetter;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.mail.Email;
import org.bouncycastle.crypto.Mac;
import org.bouncycastle.crypto.digests.MD5Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;

/*
 first implementation of a SMTP client
 */
// TODO: this is not really a verticle yet

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
public class MailVerticle {

  private Vertx vertx;
  private Handler<AsyncResult<JsonObject>> finishedHandler;

  public MailVerticle(Vertx vertx, Handler<AsyncResult<JsonObject>> finishedHandler) {
    this.vertx = vertx;
    this.finishedHandler = finishedHandler;
  }

  private void shutdown() {
    commandResultHandler = null;
    if (ns != null) {
      ns.close();
      ns = null;
    }
    if (client != null) {
      client.close();
      client = null;
    }
  }

  /*
   * write command without masking anything
   */
  private void write(String str, Handler<AsyncResult<String>> commandResultHandler) {
    write(str, -1, commandResultHandler);
  }

  /*
   * write command masking everything after position blank
   */
  private void write(String str, int blank, Handler<AsyncResult<String>> commandResultHandler) {
    this.commandResultHandler = commandResultHandler;
    if (log.isDebugEnabled()) {
      String logStr;
      if (blank >= 0) {
        StringBuilder sb = new StringBuilder();
        for (int i = blank; i < str.length(); i++) {
          sb.append('*');
        }
        logStr = str.substring(0, blank + 1) + sb;
      } else {
        logStr = str;
      }
      // avoid logging large mail body
      if (logStr.length() < 1000) {
        log.debug("command: " + logStr);
      } else {
        log.debug("command: " + logStr.substring(0, 1000) + "...");
      }
    }
    ns.write(str + "\r\n");
  }

  private static final Logger log = LoggerFactory.getLogger(MailVerticle.class);
  NetSocket ns;

  Handler<AsyncResult<String>> commandResultHandler;

  private boolean capaStartTLS = false;
  private Set<String> capaAuth = Collections.emptySet();
  // 8BITMIME can be used if the server supports it, currently this is not
  // implemented
  private boolean capa8BitMime = false;
  // PIPELINING is not yet used
  private boolean capaPipelining = false;
  private int capaSize = 0;

  Email email;
  String mailMessage;
  String username;
  String pw;
  LoginOption login;
  NetClient client;

  public void sendMail(Email email, String username, String password, LoginOption login) {
    this.email = email;
    this.username = username;
    pw = password;
    this.login = login;

    NetClientOptions netClientOptions = new NetClientOptions().setSsl(email.isSSLOnConnect());
    client = vertx.createNetClient(netClientOptions);

    client.connect(Integer.parseInt(email.getSmtpPort()), email.getHostName(), asyncResult -> {
      if (asyncResult.succeeded()) {
        ns = asyncResult.result();
        commandResultHandler = (result -> serverGreeting(result));
        final Handler<Buffer> mlp = new MultilineParser(buffer -> {
          if (commandResultHandler == null) {
            log.debug("dropping reply arriving after we stopped processing \"" + buffer.toString() + "\"");
          } else {
            commandResultHandler.handle(Future.succeededFuture(buffer.toString()));
          }
        });
        ns.handler(mlp);
      } else {
        log.error("exception on connect", asyncResult.cause());
        throwAsyncResult(asyncResult.cause());
      }
    });
  }

  private void serverGreeting(AsyncResult<String> result) {
    String message = result.result();
    log.debug("server greeting: " + message);
    if (isStatusOk(message)) {
      if (isEsmtpSupported(message)) {
        ehloCmd();
      } else {
        heloCmd();
      }
    } else {
      throwAsyncResult("got error response " + message);
    }
  }

  private boolean isEsmtpSupported(String message) {
    return message.contains("ESMTP");
  }

  private int getStatusCode(String message) {
    if (message.length() < 4) {
      return 500;
    }
    if (!message.substring(3, 4).equals(" ") && !message.substring(3, 4).equals("-")) {
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

  private boolean isStatusFatal(String message) {
    return getStatusCode(message) >= 500;
  }

  private boolean isStatusTemporary(String message) {
    int statusCode = getStatusCode(message);
    return statusCode >= 400 && statusCode < 500;
  }

  private void ehloCmd() {
    write("EHLO " + getMyHostname(), result -> {
      String message = result.result();
      log.debug("EHLO result: " + message);
      if (isStatusOk(message)) {
        setCapabilities(message);

        // fail if we are exceeding size as early as possible
        if(mailMessage==null) {
          mailMessage = createMailMessage();
        }
        if (capaSize > 0 && mailMessage.length() > capaSize) {
          throwAsyncResult("message exceeds allowed size limit");
        } else {
          if (capaStartTLS && !ns.isSsl() && (email.isStartTLSRequired() || email.isStartTLSEnabled())) {
            // do not start TLS if we are connected with SSL
            // or are already in TLS
            startTLSCmd();
          } else {
            if (!ns.isSsl() && email.isStartTLSRequired()) {
              log.warn("STARTTLS required but not supported by server");
              throwAsyncResult("STARTTLS required but not supported by server");
            } else {
              if (login != LoginOption.DISABLED && username != null && pw != null && !capaAuth.isEmpty()) {
                authCmd();
              } else {
                if (login == LoginOption.REQUIRED) {
                  if (username != null && pw != null) {
                    throwAsyncResult("login is required, but no AUTH methods available. You may need do to STARTTLS");
                  } else {
                    throwAsyncResult("login is required, but no credentials supplied");
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
          throwAsyncResult("EHLO failed with " + message);
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
        capaAuth = new HashSet<String>(Arrays.asList(c.substring(5).split(" ")));
      }
      if (c.equals("8BITMIME")) {
        capa8BitMime = true;
      }
      if (c.startsWith("SIZE ")) {
        try {
          capaSize = Integer.parseInt(c.substring(5));
        } catch (NumberFormatException n) {
          capaSize = 0;
        }
      }
    }
  }

  private void heloCmd() {
    write("HELO " + getMyHostname(), result -> {
      String message = result.result();
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
      return "unknown";
    }
  }

  /**
   * 
   */
  private void startTLSCmd() {
    write("STARTTLS", result -> {
      String message = result.result();
      log.debug("STARTTLS result: " + message);
      upgradeTLS();
    });
  }

  private void upgradeTLS() {
    ns.upgradeToSsl(v -> {
      log.debug("ssl started");
      // capabilities may have changed, e.g.
      // if a service only announces PLAIN/LOGIN
      // on secure channel
      ehloCmd();
    });
  }

  private List<String> parseEhlo(String message) {
    // parse ehlo and other multiline replies
    List<String> v = new ArrayList<String>();

    String resultCode = message.substring(0, 3);

    for (String l : message.split("\n")) {
      if (!l.startsWith(resultCode) || l.charAt(3) != '-' && l.charAt(3) != ' ') {
        log.error("format error in multiline response");
        throwAsyncResult("format error in multiline response");
      } else {
        v.add(l.substring(4));
      }
    }

    return v;
  }

  private void authCmd() {
    if (capaAuth.contains("CRAM-MD5")) {
      write("AUTH CRAM-MD5", result -> {
        String message = result.result();
        log.debug("AUTH result: " + message);
        cramMD5Step1(message.substring(4));
      });
    } else if (capaAuth.contains("PLAIN")) {
      String authdata = base64("\0" + username + "\0" + pw);
      write("AUTH PLAIN " + authdata, 10, result -> {
        String message = result.result();
        log.debug("AUTH result: " + message);
        if (!message.toString().startsWith("2")) {
          log.warn("authentication failed");
          throwAsyncResult("authentication failed");
        } else {
          mailFromCmd();
        }
      });
    } else if (capaAuth.contains("LOGIN")) {
      write("AUTH LOGIN", result -> {
        String message = result.result();
        log.debug("AUTH result: " + message);
        sendUsername();
      });
    } else {
      log.warn("cannot find supported auth method");
      throwAsyncResult("cannot find supported auth method");
    }
  }

  private void cramMD5Step1(String string) {
    String message = decodeb64(string);
    log.debug("message " + message);
    String reply = hmacMD5hex(message, pw);
    write(base64(username + " " + reply), 0, result -> {
      String message2 = result.result();
      log.debug("AUTH step 2 result: " + message2);
      cramMD5Step2(message2);
    });
  }

  private String hmacMD5hex(String message, String pw) {
    KeyParameter keyparameter;
    try {
      keyparameter = new KeyParameter(pw.getBytes("utf-8"));
      Mac mac = new HMac(new MD5Digest());
      mac.init(keyparameter);
      byte[] messageBytes = message.getBytes("utf-8");
      mac.update(messageBytes, 0, messageBytes.length);
      byte[] outBytes = new byte[mac.getMacSize()];
      mac.doFinal(outBytes, 0);
      return Hex.encodeHexString(outBytes);
    } catch (UnsupportedEncodingException e) {
      // doesn't happen, auth will fail in that case
      return "";
    }
  }

  private void cramMD5Step2(String message) {
    log.debug(message);
    if (isStatusOk(message)) {
      mailFromCmd();
    } else {
      log.warn("authentication failed");
      throwAsyncResult("authentication failed");
    }
  }

  private void sendUsername() {
    write(base64(username), 0, result -> {
      String message = result.result();
      log.debug("username result: " + message);
      sendPw();
    });
  }

  private void sendPw() {
    write(base64(pw), 0, result -> {
      String message = result.result();
      log.debug("pw result: " + message);
      if (isStatusOk(message)) {
        mailFromCmd();
      } else {
        log.warn("authentication failed");
        throwAsyncResult("authentication failed");
      }
    });
  }

  private void mailFromCmd() {
    try {
      // prefer bounce address over from address
      // currently (1.3.3) commons mail is missing the getter for bounceAddress
      // I have requested that https://issues.apache.org/jira/browse/EMAIL-146
      String fromAddr = email.getFromAddress().getAddress();
      if (email instanceof BounceGetter) {
        String bounceAddr = ((BounceGetter) email).getBounceAddress();
        if (bounceAddr != null && !bounceAddr.isEmpty()) {
          fromAddr = bounceAddr;
        }
      }
      InternetAddress.parse(fromAddr, true);
      write("MAIL FROM:<" + fromAddr + ">", result -> {
        String message = result.result();
        log.debug("MAIL FROM result: " + message);
        if (isStatusOk(message)) {
          rcptToCmd();
        } else {
          log.warn("sender address not accepted: " + message);
          throwAsyncResult("sender address not accepted: " + message);
        }
      });
    } catch (AddressException e) {
      log.error("address exception", e);
      throwAsyncResult(e);
    }
  }

  private void rcptToCmd() {
    final List<InternetAddress> toAddrs = email.getToAddresses();
    rcptToCmd(toAddrs, 0);
  }

  private void rcptToCmd(List<InternetAddress> toAddrs, int i) {
    try {
      String toAddr = toAddrs.get(i).getAddress();
      InternetAddress.parse(toAddr, true);
      write("RCPT TO:<" + toAddr + ">", result -> {
        String message = result.result();
        log.debug("RCPT TO result: " + message);
        if (isStatusOk(message)) {
          if (i + 1 < toAddrs.size()) {
            rcptToCmd(toAddrs, i + 1);
          } else {
            dataCmd();
          }
        } else {
          log.warn("recipient address not accepted: " + message);
          throwAsyncResult("recipient address not accepted: " + message);
        }
      });
    } catch (AddressException e) {
      log.error("address exception", e);
      throwAsyncResult(e);
    }
  }

  private void dataCmd() {
    write("DATA", result -> {
      String message = result.result();
      log.debug("DATA result: " + message);
      if (isStatusOk(message)) {
        sendMaildata();
      } else {
        log.warn("DATA command not accepted: " + message);
        throwAsyncResult("DATA command not accepted: " + message);
      }
    });
  }

  private void sendMaildata() {
    if (mailMessage == null) {
      mailMessage = createMailMessage();
    }
    // convert message to escape . at the start of line
    // TODO: this is probably bad for large messages
    write(mailMessage.replaceAll("\n\\.", "\n..") + "\r\n.", result -> {
      String message = result.result();
      log.debug("maildata result: " + message);
      if (isStatusOk(message)) {
        quitCmd();
      } else {
        log.warn("sending data failed: " + message);
        throwAsyncResult("sending data failed: " + message);
      }
    });
  }

  /**
   * @return
   */
  private String createMailMessage() {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try {
      email.buildMimeMessage();
      email.getMimeMessage().writeTo(bos);
    } catch (Exception e) {
      log.error("cannot create mime message", e);
      throwAsyncResult(e);
    }
    return bos.toString();
  }

  private void quitCmd() {
    write("QUIT", result -> {
      String message = result.result();
      log.debug("QUIT result: " + message);
      if (isStatusOk(message)) {
        finishMail();
      } else {
        log.warn("quit failed: " + message);
        throwAsyncResult("quit failed: " + message);
      }
    });
  }

  private void finishMail() {
    shutdown();
    JsonObject result = new JsonObject();
    result.put("result", "success");
    finishedHandler.handle(Future.succeededFuture(result));
  }

  private void throwAsyncResult(Throwable throwable) {
    shutdown();
    finishedHandler.handle(Future.failedFuture(throwable));
  }

  private void throwAsyncResult(String message) {
    shutdown();
    finishedHandler.handle(Future.failedFuture(message));
  }

  private String base64(String string) {
    try {
      // this call does not create multi-line base64 data
      // (if someone uses a password longer than 57 chars or
      // one of the other SASL replies is longer than 76 chars)
      return Base64.encodeBase64String(string.getBytes("UTF-8"));
    } catch (UnsupportedEncodingException e) {
      // doesn't happen
      return "";
    }
  }

  private String decodeb64(String string) {
    try {
      return new String(Base64.decodeBase64(string), "UTF-8");
    } catch (UnsupportedEncodingException e) {
      // doesn't happen
      return "";
    }
  }

}

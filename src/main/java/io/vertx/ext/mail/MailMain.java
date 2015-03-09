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
import io.vertx.ext.mail.mailencoder.EmailAddress;
import io.vertx.ext.mail.mailencoder.MailEncoder;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
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
 * this class takes care of the different smtp steps, AUTH etc
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
  private NetSocket ns;
  private boolean socketClosed;
  private boolean socketShutDown;

  private Handler<AsyncResult<String>> commandResultHandler;

  private MailMessage email;
  private String mailMessage;
  private NetClient client;

  private Set<String> capaAuth = Collections.emptySet();
  private boolean capaStartTLS = false;
  private int capaSize = 0;
  // 8BITMIME can be used if the server supports it, currently this is not
  // implemented
//  private boolean capa8BitMime = false;
  // PIPELINING is not yet used
//  private boolean capaPipelining = false;

  public MailMain(Vertx vertx, MailConfig config, Handler<AsyncResult<JsonObject>> finishedHandler) {
    this.vertx = vertx;
    this.config = config;
    this.finishedHandler = finishedHandler;
  }

  private void shutdown() {
    commandResultHandler = null;
    socketShutDown = true;
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
    if(socketClosed) {
      throwAsyncResult("connection was closed by server");
    }
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

  /**
   * start a mail send operation using the MailMessage object
   * @param email the mail to send
   */
  void sendMail(MailMessage email) {
    this.email = email;
    sendMail();
  }

  /**
   * start a mail send operation using the parameters from MailMessage object
   * and the pregenerated message provided as String
   * @param email the mail parameters (from, to, etc)
   * @param message the message to send
   */
  void sendMail(MailMessage email, String message) {
    this.email = email;
    mailMessage = message;
    sendMail();
  }

  private void sendMail() {
    NetClientOptions netClientOptions = new NetClientOptions().setSsl(config.isSsl());
    client = vertx.createNetClient(netClientOptions);

    client.connect(config.getPort(), config.getHostname(), asyncResult -> {
      if (asyncResult.succeeded()) {
        ns = asyncResult.result();
        socketClosed = false;
        ns.exceptionHandler(e -> {
          // avoid returning two exceptions
          if(!socketClosed && !socketShutDown) {
            log.debug("got an exception on the netsocket", e);
            throwAsyncResult(e);
          }
        });
        ns.closeHandler(v -> {
          // avoid exception if we regularly shut down the socket on our side
          if(!socketShutDown) {
            log.debug("socket has been closed");
            socketClosed = true;
            throwAsyncResult("connection has been closed by the server");
          }
        });
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

//  private boolean isStatusFatal(String message) {
//    return getStatusCode(message) >= 500;
//  }

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
        createMailMessage();
        if (capaSize > 0 && mailMessage.length() > capaSize) {
          throwAsyncResult("message exceeds allowed size limit");
        } else {
          if (capaStartTLS && !ns.isSsl() && (config.getStarttls()==StarttlsOption.REQUIRED || config.getStarttls()==StarttlsOption.OPTIONAL)) {
            // do not start TLS if we are connected with SSL
            // or are already in TLS
            startTLSCmd();
          } else {
            if (!ns.isSsl() && config.getStarttls()==StarttlsOption.REQUIRED) {
              log.warn("STARTTLS required but not supported by server");
              throwAsyncResult("STARTTLS required but not supported by server");
            } else {
              if (config.getLogin() != LoginOption.DISABLED && config.getUsername() != null && config.getPassword() != null && !capaAuth.isEmpty()) {
                authCmd();
              } else {
                if (config.getLogin() == LoginOption.REQUIRED) {
                  if (config.getUsername() != null && config.getPassword() != null) {
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
//      if (c.equals("8BITMIME")) {
//        capa8BitMime = true;
//      }
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
      String authdata = base64("\0" + config.getUsername() + "\0" + config.getPassword());
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
    String reply = hmacMD5hex(message, config.getPassword());
    write(base64(config.getUsername() + " " + reply), 0, result -> {
      String message2 = result.result();
      log.debug("AUTH step 2 result: " + message2);
      cramMD5Step2(message2);
    });
  }

  private String hmacMD5hex(String message, String pw) {
    try {
      SecretKey key = new SecretKeySpec(pw.getBytes("utf-8"), "HmacMD5");
      Mac mac = Mac.getInstance(key.getAlgorithm());
      mac.init(key);
      return encodeHex(mac.doFinal(message.getBytes("utf-8")));
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
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
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
    write(base64(config.getUsername()), 0, result -> {
      String message = result.result();
      log.debug("username result: " + message);
      sendPw();
    });
  }

  private void sendPw() {
    write(base64(config.getPassword()), 0, result -> {
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
      String fromAddr = email.getFrom();
      String bounceAddr = email.getBounceAddress();
      if (bounceAddr != null && !bounceAddr.isEmpty()) {
        fromAddr = bounceAddr;
      }
      new EmailAddress(fromAddr);
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
    } catch (IllegalArgumentException e) {
      log.error("address exception", e);
      throwAsyncResult(e);
    }
  }

  private void rcptToCmd() {
    List<String> recipientAddrs = new ArrayList<String>();
    if(email.getTo()!=null) {
      recipientAddrs.addAll(email.getTo());
    }
    if(email.getCc()!=null) {
      recipientAddrs.addAll(email.getCc());
    }
    if(email.getBcc()!=null) {
      recipientAddrs.addAll(email.getBcc());
    }
    rcptToCmd(recipientAddrs, 0);
  }

  private void rcptToCmd(List<String> recipientAddrs, int i) {
    try {
      String toAddr = recipientAddrs.get(i);
      new EmailAddress(toAddr);
      write("RCPT TO:<" + toAddr + ">", result -> {
        String message = result.result();
        log.debug("RCPT TO result: " + message);
        if (isStatusOk(message)) {
          if (i + 1 < recipientAddrs.size()) {
            rcptToCmd(recipientAddrs, i + 1);
          } else {
            dataCmd();
          }
        } else {
          log.warn("recipient address not accepted: " + message);
          throwAsyncResult("recipient address not accepted: " + message);
        }
      });
    } catch (IllegalArgumentException e) {
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
    // make sure we create the message here if it hasn't been created
    // for the size check above
    createMailMessage();
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
  private void createMailMessage() {
    if(mailMessage==null) {
      MailEncoder encoder = new MailEncoder(email);
      mailMessage = encoder.encode();
    }
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

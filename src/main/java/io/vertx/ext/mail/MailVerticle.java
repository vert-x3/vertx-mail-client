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
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
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
//  private Handler<AsyncResult<JsonObject>> finishedHandler;

  public MailVerticle(Vertx vertx, Handler<AsyncResult<JsonObject>> finishedHandler) {
    this.vertx = vertx;
    mailResult=Future.future();
    mailResult.setHandler(finishedHandler);
  }

  private void write(NetSocket netSocket, String str) {
    write(netSocket, str, str);
  }

  // avoid logging password data
  private void write(NetSocket netSocket, String str, String logStr) {
    log.info("command: " + logStr);
    netSocket.write(str + "\r\n");
  }

  private static final Logger log = LoggerFactory.getLogger(MailVerticle.class);
  NetSocket ns;

  Future<String> commandResult;
  Future<JsonObject> mailResult;

  private boolean capaStartTLS = false;
  private Set<String> capaAuth = Collections.emptySet();
  // 8BITMIME can be used if the server supports it, currently this is not
  // implemented
  private boolean capa8BitMime = false;
  private int capaSize = 0;

  Email email;
  String username;
  String pw;
  LoginOption login;

  public void sendMail(Email email, String username, String password, LoginOption login) {
    this.email = email;
    this.username = username;
    pw = password;
    this.login=login;

    NetClientOptions netClientOptions = new NetClientOptions().setSsl(email.isSSLOnConnect());
    NetClient client = vertx.createNetClient(netClientOptions);

    client.connect(Integer.parseInt(email.getSmtpPort()), email.getHostName(),
        asyncResult -> {
          if (asyncResult.succeeded()) {
            ns = asyncResult.result();
            commandResult = Future.future();
            commandResult.setHandler(message -> serverGreeting(message));
            final Handler<Buffer> mlp = new MultilineParser(
                buffer -> commandResult.complete(buffer.toString()));
            ns.handler(mlp);
          } else {
            log.error("exception", asyncResult.cause());
            throwAsyncResult(asyncResult.cause());
          }
        });
  }

  private void serverGreeting(AsyncResult<String> message) {
    log.info("server greeting: " + message);
    ehloCmd();
  }

  private void ehloCmd() {
    // TODO: get real hostname
    write(ns, "EHLO windows7");
    commandResult = Future.future();
    commandResult.setHandler(result -> {
      String message=result.result();
      log.info("EHLO result: " + message);
      List<String> capabilities = parseEhlo(message);
      for (String c : capabilities) {
        if (c.equals("STARTTLS")) {
          capaStartTLS = true;
        }
        if (c.startsWith("AUTH ")) {
          capaAuth = new HashSet<String>(Arrays.asList(c.substring(5)
              .split(" ")));
        }
        if (c.equals("8BITMIME")) {
          capa8BitMime = true;
        }
        if (c.startsWith("SIZE ")) {
          capaSize = Integer.parseInt(c.substring(5));
        }
      }

      if (capaStartTLS && !ns.isSsl()
          && (email.isStartTLSRequired() || email.isStartTLSEnabled())) {
        // do not start TLS if we are connected with SSL
        // or are already in TLS
        startTLSCmd();
      } else {
        if (!ns.isSsl() && email.isStartTLSRequired()) {
          log.warn("STARTTLS required but not supported by server");
          commandResult.fail("STARTTLS required but not supported by server");
        } else {
          if (login!=LoginOption.DISABLED && username != null && pw != null && !capaAuth.isEmpty()) {
            authCmd();
          } else {
            if(login==LoginOption.REQUIRED) {
              if(username != null && pw != null) {
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
    });
  }

  /**
   * 
   */
  private void startTLSCmd() {
    write(ns, "STARTTLS");
    commandResult = Future.future();
    commandResult.setHandler(result -> {
      String message=result.result();
      log.info("STARTTLS result: " + message);
      upgradeTLS();
    });
  }

  private void upgradeTLS() {
    ns.upgradeToSsl(v -> {
      log.info("ssl started");
      ehloCmd();
    });
  }

  private List<String> parseEhlo(String message) {
    // parse ehlo and other multiline replies
    List<String> v = new ArrayList<String>();

    String resultCode = message.substring(0, 3);

    for (String l : message.split("\n")) {
      if (!l.startsWith(resultCode) || l.charAt(3) != '-' && l.charAt(3) != ' ') {
        log.error("format error in ehlo response");
        throwAsyncResult("format error in ehlo response");
      } else {
        v.add(l.substring(4));
      }
    }

    return v;
  }

  private void authCmd() {
    if (capaAuth.contains("CRAM-MD5")) {
      write(ns, "AUTH CRAM-MD5");
      commandResult = Future.future();
      commandResult.setHandler(result -> {
        String message=result.result();
        log.info("AUTH result: " + message);
        cramMD5Step1(message.substring(4));
      });
    } else if (capaAuth.contains("PLAIN")) {
      String authdata = base64("\0" + username + "\0" + pw);
      String authdummy = base64("\0dummy\0XXX");
      write(ns, "AUTH PLAIN " + authdata, "AUTH PLAIN " + authdummy);
      commandResult = Future.future();
      commandResult.setHandler(result -> {
        String message=result.result();
        log.info("AUTH result: " + message);
        if (!message.toString().startsWith("2")) {
          log.warn("authentication failed");
          throwAsyncResult("authentication failed");
        } else {
          mailFromCmd();
        }
      });
    } else if (capaAuth.contains("LOGIN")) {
      write(ns, "AUTH LOGIN");
      commandResult = Future.future();
      commandResult.setHandler(result -> {
        String message=result.result();
        log.info("AUTH result: " + message);
        sendUsername();
      });
    } else {
      log.warn("cannot find supported auth method");
      throwAsyncResult("cannot find supported auth method");
    }
  }

  private void cramMD5Step1(String string) {
    String message = decodeb64(string);
    log.info("message " + message);
    String reply = hmacMD5hex(message, pw);
    write(ns, base64(username + " " + reply), base64("dummy XXX"));
    commandResult = Future.future();
    commandResult.setHandler(result -> {
      String message2 = result.result();
      log.info("AUTH step 2 result: " + message2);
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

  private void cramMD5Step2(String message2) {
    log.info(message2);
    if (message2.startsWith("2")) {
      mailFromCmd();
    } else {
      log.warn("authentication failed");
      throwAsyncResult("authentication failed");
    }
  }

  private void sendUsername() {
    write(ns, base64(username), base64("dummy"));
    commandResult = Future.future();
    commandResult.setHandler(result -> {
      String message=result.result();
      log.info("username result: " + message);
      sendPw();
    });
  }

  private void sendPw() {
    write(ns, base64(pw), base64("XXX"));
    commandResult = Future.future();
    commandResult.setHandler(result -> {
      String message=result.result();
      log.info("username result: " + message);
      if (!message.toString().startsWith("2")) {
        log.warn("authentication failed");
        throwAsyncResult("authentication failed");
      } else {
        mailFromCmd();
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
      write(ns, "MAIL FROM:<" + fromAddr + ">");
      commandResult = Future.future();
      commandResult.setHandler(result -> {
        String message=result.result();
        log.info("MAIL FROM result: " + message);
        rcptToCmd();
      });
    } catch (AddressException e) {
      log.error("address exception", e);
      throwAsyncResult(e);
    }
  }

  private void rcptToCmd() {
    try {
      // FIXME: have to handle all addresses
      String toAddr = email.getToAddresses().get(0).getAddress();
      InternetAddress.parse(toAddr, true);
      write(ns, "RCPT TO:<" + toAddr + ">");
      commandResult = Future.future();
      commandResult.setHandler(result -> {
        String message=result.result();
        log.info("RCPT TO result: " + message);
        dataCmd();
      });
    } catch (AddressException e) {
      log.error("address exception", e);
      throwAsyncResult(e);
    }
  }

  private void dataCmd() {
    write(ns, "DATA");
    commandResult = Future.future();
    commandResult.setHandler(result -> {
      String message=result.result();
      log.info("DATA result: " + message);
      sendMaildata();
    });
  }

  private void sendMaildata() {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try {
      email.buildMimeMessage();
      email.getMimeMessage().writeTo(bos);
    } catch (IOException | MessagingException | EmailException e) {
      log.error("cannot create mime message", e);
      throwAsyncResult("cannot create mime message");
    }
    String message=bos.toString();
    // fail delivery if we exceed size
    if(capaSize>0 && message.length()>capaSize) {
      throwAsyncResult("message exceeds allowed size");
    }
    // convert message to escape . at the start of line
    // TODO: this is probably bad for large messages
    write(ns, message.replaceAll("\n\\.", "\n..") + "\r\n.");
    commandResult = Future.future();
    commandResult.setHandler(result -> {
      String messageReply=result.result();
      log.info("maildata result: " + messageReply);
      quitCmd();
    });
  }

  private void quitCmd() {
    write(ns, "QUIT");
    commandResult = Future.future();
    commandResult.setHandler(result -> {
      String message=result.result();
      log.info("QUIT result: " + message);
      shutdownConnection();
    });
  }

  private void shutdownConnection() {
    ns.close();
    JsonObject result=new JsonObject();
    result.put("result", "success");
    mailResult.complete(result);
  }

  private void throwAsyncResult(Throwable throwable) {
    mailResult.fail(throwable);
  }

  private void throwAsyncResult(String message) {
    mailResult.fail(message);
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

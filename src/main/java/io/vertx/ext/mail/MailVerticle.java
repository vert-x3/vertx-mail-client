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
  private Handler<AsyncResult<JsonObject>> finishedHandler;

  public MailVerticle(Vertx vertx, Handler<AsyncResult<JsonObject>> finishedHandler) {
    this.vertx = vertx;
    this.finishedHandler = finishedHandler;
  }

  private void write(NetSocket netSocket, String str) {
    log.info("command: " + str);
    netSocket.write(str + "\r\n");
  }

  Logger log = LoggerFactory.getLogger(this.getClass());;
  NetSocket ns;

  Future<String> commandResult;
  private boolean capaStartTLS = false;
  private Set<String> capaAuth = Collections.emptySet();
  // 8BITMIME can be used if the server supports it, currently this is not
  // implemented
  private boolean capa8BitMime = false;
  // SIZE is not implemented yet
  private int capaSize = 0;

  Email email;
  String username;
  String pw;
  String login;

  // public void start() {
  // log=container.logger();
  // log.info("starting");
  // }

  public void sendMail(Email email, String username, String password, String login) {
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
            commandResult = new CommandResultFuture(
                event -> serverGreeting(event));
            final Handler<Buffer> mlp = new MultilineParser(
                buffer -> commandResult.complete(buffer.toString()));
            ns.handler(mlp);
          } else {
            log.error("exception", asyncResult.cause());
            throwAsyncResult(asyncResult.cause());
          }
        });
  }

  private void serverGreeting(String buffer) {
    log.info("server greeting: " + buffer);
    ehloCmd();
  }

  private void ehloCmd() {
    // TODO: get real hostname
    write(ns, "EHLO windows7");
    commandResult = new CommandResultFuture(buffer -> {
      log.info("EHLO result: " + buffer);
      List<String> capabilities = parseEhlo(buffer);
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
        // avoid starting TLS if we already have connected with SSL or are in
        // TLS
        startTLSCmd();
      } else {
        if (!ns.isSsl() && email.isStartTLSRequired()) {
          log.warn("STARTTLS required but not supported by server");
          throwAsyncResult(new Exception("STARTTLS required but not supported by server"));
        } else {
          if (!login.equals("disabled") && username != null && pw != null && !capaAuth.isEmpty()) {
            authCmd();
          } else {
            if(login.equals("required")) {
              if(username != null && pw != null) {
                throwAsyncResult(new Exception("login is required, but no AUTH methods available. You may need do to STARTTLS"));
              } else {
                throwAsyncResult(new Exception("login is required, but no credentials supplied"));
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
    commandResult = new CommandResultFuture(buffer -> {
      log.info("STARTTLS result: " + buffer);
      upgradeTLS();
    });
  }

  private void upgradeTLS() {
    ns.upgradeToSsl(v -> {
      log.info("ssl started");
      ehloCmd();
    });
  }

  private List<String> parseEhlo(String string) {
    // parse ehlo and other multiline replies
    List<String> v = new ArrayList<String>();

    String resultCode = string.substring(0, 3);

    for (String l : string.split("\n")) {
      if (!l.startsWith(resultCode) || l.charAt(3) != '-' && l.charAt(3) != ' ') {
        log.error("format error in ehlo response");
        throwAsyncResult(new Exception("format error in ehlo response"));
      } else {
        v.add(l.substring(4));
      }
    }

    return v;
  }

  private void authCmd() {
    if (capaAuth.contains("CRAM-MD5")) {
      write(ns, "AUTH CRAM-MD5");
      commandResult = new CommandResultFuture(buffer -> {
        log.info("AUTH result: " + buffer);
        cramMD5Step1(buffer.substring(4));
      });
    } else if (capaAuth.contains("PLAIN")) {
      String authdata = base64("\0" + username + "\0" + pw);
      write(ns, "AUTH PLAIN " + authdata);
      commandResult = new CommandResultFuture(buffer -> {
        log.info("AUTH result: " + buffer);
        if (!buffer.toString().startsWith("2")) {
          log.warn("authentication failed");
          throwAsyncResult(new Exception("authentication failed"));
        } else {
          mailFromCmd();
        }
      });
    } else if (capaAuth.contains("LOGIN")) {
      write(ns, "AUTH LOGIN");
      commandResult = new CommandResultFuture(buffer -> {
        log.info("AUTH result: " + buffer);
        sendUsername();
      });
    } else {
      log.warn("cannot find supported auth method");
      throwAsyncResult(new Exception("cannot find supported auth method"));
    }
  }

  private void cramMD5Step1(String string) {
    String message = decodeb64(string);
    log.info("message " + message);
    String reply = hmacMD5hex(message, pw);
    write(ns, base64(username + " " + reply));
    commandResult = new CommandResultFuture(buffer -> {
      log.info("AUTH step 2 result: " + buffer);
      cramMD5Step2(buffer);
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

  private void cramMD5Step2(String string) {
    log.info(string);
    if (string.startsWith("2")) {
      mailFromCmd();
    } else {
      log.warn("authentication failed");
      throwAsyncResult(new Exception("authentication failed"));
    }
  }

  private void sendUsername() {
    write(ns, base64(username));
    commandResult = new CommandResultFuture(buffer -> {
      log.info("username result: " + buffer);
      sendPw();
    });
  }

  private void sendPw() {
    write(ns, base64(pw));
    commandResult = new CommandResultFuture(buffer -> {
      log.info("username result: " + buffer);
      if (!buffer.toString().startsWith("2")) {
        log.warn("authentication failed");
        throwAsyncResult(new Exception("authentication failed"));
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
      commandResult = new CommandResultFuture(buffer -> {
        log.info("MAIL FROM result: " + buffer);
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
      commandResult = new CommandResultFuture(buffer -> {
        log.info("RCPT TO result: " + buffer);
        dataCmd();
      });
    } catch (AddressException e) {
      log.error("address exception", e);
      throwAsyncResult(e);
    }
  }

  private void dataCmd() {
    write(ns, "DATA");
    commandResult = new CommandResultFuture(buffer -> {
      log.info("DATA result: " + buffer);
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
      throwAsyncResult(new Exception("cannot create mime message"));
    }
    String message=bos.toString();
    // fail delivery if we exceed size
    if(capaSize>0 && message.length()>capaSize) {
      throwAsyncResult(new Exception("message exceeds allowed size"));
    }
    // convert message to escape . at the start of line
    // TODO: this is probably bad for large messages
    write(ns, message.replaceAll("\n\\.", "\n..") + "\r\n.");
    commandResult = new CommandResultFuture(buffer -> {
      log.info("maildata result: " + buffer);
      quitCmd();
    });
  }

  private void quitCmd() {
    write(ns, "QUIT");
    commandResult = new CommandResultFuture(buffer -> {
      log.info("QUIT result: " + buffer);
      shutdownConnection();
    });
  }

  private void shutdownConnection() {
    ns.close();
    JsonObject result=new JsonObject();
    result.put("result", "success");
    finishedHandler.handle(createSuccess(result));
  }

  private void throwAsyncResult(Throwable throwable) {
    finishedHandler.handle(createFailure(throwable));
  }

  private AsyncResult<JsonObject> createSuccess(JsonObject result) {
    return new AsyncResult<JsonObject>() {
      @Override
      public JsonObject result() {
        return result;
      }

      @Override
      public Throwable cause() {
        return null;
      }

      @Override
      public boolean succeeded() {
        return true;
      }

      @Override
      public boolean failed() {
        return false;
      }
    };
  }

  private AsyncResult<JsonObject> createFailure(Throwable throwable) {
    return new AsyncResult<JsonObject>() {
      @Override
      public JsonObject result() {
        return null;
      }

      @Override
      public Throwable cause() {
        return throwable;
      }

      @Override
      public boolean succeeded() {
        return false;
      }

      @Override
      public boolean failed() {
        return true;
      }
    };
  }

  private String base64(String string) {
    try {
      // this call does not create multi-line base64 data
      // (if someone uses a password longer than 57 chars or
      // one of the other SASL replies is longer than 76 chars)
      return Base64.encodeBase64String(string.getBytes("UTF-8"));
//      return new String(Base64.encodeBase64Chunked(string.getBytes("UTF-8")));
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

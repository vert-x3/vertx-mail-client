/*
 *  Copyright (c) 2011-2015 The original author or authors
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *       The Eclipse Public License is available at
 *       http://www.eclipse.org/legal/epl-v10.html
 *
 *       The Apache License v2.0 is available at
 *       http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 */

package io.vertx.ext.mail;

import io.vertx.core.Vertx;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.NetSocket;
import io.vertx.core.parsetools.RecordParser;

import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * really dumb mock test server that just replays a number of lines
 * as response. It checks the commands sent by the client either as substring or as regexp
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
public class TestSmtpServer {

  private static final Logger log = LoggerFactory.getLogger(TestSmtpServer.class);

  private NetServer netServer;
  private String[][] dialogue;
  private boolean closeImmediately = false;
  private int closeWaitTime = 10;

  private boolean ssl;
  private String keystore;
  /*
   * set up server with a default reply that works for EHLO and no login with one recipient
   */
  public TestSmtpServer(Vertx vertx, boolean ssl, String keystore) {
    log.debug("starting TestSmtpServer");
    setDialogue("220 example.com ESMTP",
        "EHLO",
        "250-example.com\n"
            + "250-SIZE 1000000\n"
            + "250 PIPELINING",
        "MAIL FROM:",
        "250 2.1.0 Ok",
        "RCPT TO:",
        "250 2.1.5 Ok",
        "DATA",
        "354 End data with <CR><LF>.<CR><LF>",
        "250 2.0.0 Ok: queued as ABCDDEF0123456789",
        "QUIT",
        "221 2.0.0 Bye");
    this.ssl = ssl;
    this.keystore = keystore;
    startServer(vertx);
  }

  private void startServer(Vertx vertx) {
    NetServerOptions nsOptions = new NetServerOptions();
    int port = ssl ? 1465 : 1587;
    log.debug("listening on port " + port);
    nsOptions.setPort(port);
    if (keystore == null) {
      keystore = "src/test/resources/certs/server2.jks";
    }
    JksOptions jksOptions = new JksOptions().setPath(keystore).setPassword("password");
    nsOptions.setKeyStoreOptions(jksOptions);
    if (ssl) {
      nsOptions.setSsl(true);
    }
    netServer = vertx.createNetServer(nsOptions);

    netServer.connectHandler(socket -> {
      writeResponses(socket, dialogue[0]);
      if (dialogue.length == 1) {
        if (closeImmediately) {
          log.debug("closeImmediately");
          socket.close();
        } else {
          log.debug("waiting " + closeWaitTime + " secs to close");
          vertx.setTimer(closeWaitTime * 1000, v -> socket.close());
        }
      } else {
        final AtomicInteger lines = new AtomicInteger(1);
        final AtomicInteger skipUntilDot = new AtomicInteger(0);
        final AtomicBoolean holdFire = new AtomicBoolean(false);
        final AtomicInteger inputLineIndex = new AtomicInteger(0);
        socket.handler(RecordParser.newDelimited("\r\n", buffer -> {
          final String inputLine = buffer.toString();
          log.debug("C:" + inputLine);
          if (skipUntilDot.get() == 1) {
            if (inputLine.equals(".")) {
              skipUntilDot.set(0);
              if (!holdFire.get() && lines.get() < dialogue.length) {
                writeResponses(socket, dialogue[lines.getAndIncrement()]);
              }
            }
          } else {
            int currentLine = lines.get();
            if (currentLine < dialogue.length) {
              boolean inputValid = false;
              holdFire.compareAndSet(false, true);
              if (inputLineIndex.get() < dialogue[currentLine].length) {
                String thisLine = dialogue[currentLine][inputLineIndex.get()];
                boolean isRegexp = thisLine.startsWith("^");
                if (!isRegexp && inputLine.contains(thisLine) || isRegexp && inputLine.matches(thisLine)) {
                  inputValid = true;
                  if (inputLineIndex.get() == dialogue[currentLine].length - 1) {
                    holdFire.compareAndSet(true, false);
                    lines.getAndIncrement();
                    inputLineIndex.set(0);
                  } else {
                    inputLineIndex.getAndIncrement();
                  }
                }
              }
              if (!inputValid) {
                socket.write("500 didn't expect commands (\"" + String.join(",", dialogue[currentLine]) + "\"/\"" + inputLine + "\")\r\n");
                log.info("sending 500 didn't expect commands (\"" + String.join(",", dialogue[currentLine]) + "\"/\"" + inputLine + "\")");
                // stop here
                lines.set(dialogue.length);
              }
            } else {
              log.info("out of lines, sending error reply");
              socket.write("500 out of lines\r\n");
            }
            if (inputLine.toUpperCase(Locale.ENGLISH).equals("DATA")) {
              skipUntilDot.set(1);
            }
            if (!holdFire.get() && inputLine.toUpperCase(Locale.ENGLISH).equals("STARTTLS")) {
              writeResponses(socket, dialogue[lines.getAndIncrement()]);
              //TODO loop
              socket.upgradeToSsl(v -> {
                log.debug("tls upgrade finished");
              });
            } else if (!holdFire.get() && lines.get() < dialogue.length) {
              writeResponses(socket, dialogue[lines.getAndIncrement()]);
            }
            if (inputLine.equals("QUIT")) {
              log.debug("Got QUIT, response has been written back, close the socket");
              socket.close();
            }
          }
          if (lines.get() == dialogue.length) {
            if (closeImmediately) {
              log.debug("closeImmediately");
              socket.close();
            } else {
              log.debug("waiting " + closeWaitTime + " secs to close");
              vertx.setTimer(closeWaitTime * 1000, v -> socket.close());
            }
          }
        }));
      }
    });
    CountDownLatch latch = new CountDownLatch(1);
    netServer.listen(r -> latch.countDown());
    try {
      latch.await();
    } catch (InterruptedException e) {
      log.error("interrupted while waiting for countdown latch", e);
    }
  }

  private void writeResponses(NetSocket socket, String[] responses) {
    for (String line: responses) {
      log.debug("S:" + line);
      socket.write(line + "\r\n");
    }
  }

  public TestSmtpServer setDialogue(String... dialogue) {
    this.dialogue = new String[dialogue.length][1];
    for (int i = 0; i < dialogue.length; i ++) {
      this.dialogue[i] = new String[]{dialogue[i]};
    }
    return this;
  }

  /**
   * Sets the dialogue array.
   *
   * This is useful in case of pipelining is supported to group commands and responses.
   *
   * @param dialogue the dialogues
   * @return a reference to this, so the API can be used fluently
   */
  public TestSmtpServer setDialogueArray(String[][] dialogue) {
    this.dialogue = dialogue;
    return this;
  }

  public TestSmtpServer setCloseImmediately(boolean close) {
    closeImmediately = close;
    return this;
  }

  public TestSmtpServer setCloseWaitTime(int time) {
    log.debug("setting closeWaitTime to " + time);
    closeWaitTime = time;
    return this;
  }

  // this assumes we are in a @After method of junit
  // i.e. we are synchronous
  public void stop() {
    if (netServer != null) {
      CountDownLatch latch = new CountDownLatch(1);
      netServer.close(v -> latch.countDown());
      try {
        latch.await();
      } catch (InterruptedException e) {
        log.error("interrupted while waiting for countdown latch", e);
      }
      netServer = null;
    }
  }

}

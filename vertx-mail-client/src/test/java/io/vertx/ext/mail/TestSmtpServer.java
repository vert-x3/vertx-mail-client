package io.vertx.ext.mail;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.parsetools.RecordParser;

import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/*
 * really dumb mock test server that just replays a number of lines
 * as response. this doesn't check any conditions at all.  
 */
public class TestSmtpServer {

  private static final Logger log = LoggerFactory.getLogger(TestSmtpServer.class);

  private NetServer netServer;
  private String[] dialogue;
  private boolean closeImmediately = false;

  /*
   * set up server with a default reply that works for EHLO and no login with
   * one recipient
   */
  public TestSmtpServer(Vertx vertx) {
    setDialogue("220 example.com ESMTP",
      "EHLO",
      "250-example.com\n" +
        "250-SIZE 1000000\n" +
        "250 PIPELINING",
      "MAIL FROM:",
      "250 2.1.0 Ok",
      "RCPT TO:",
      "250 2.1.5 Ok",
      "DATA",
      "354 End data with <CR><LF>.<CR><LF>",
      "250 2.0.0 Ok: queued as ABCDDEF0123456789",
      "QUIT",
      "221 2.0.0 Bye");
    startServer(vertx);
  }

  public TestSmtpServer(Vertx vertx, String... answers) {
    setDialogue(answers);
    startServer(vertx);
  }

  private void startServer(Vertx vertx) {
    NetServerOptions nsOptions = new NetServerOptions();
    nsOptions.setPort(1587);
    netServer = vertx.createNetServer(nsOptions);

    netServer.connectHandler(socket -> {
      socket.write(dialogue[0] + "\r\n");
      log.debug("S:" + dialogue[0]);
      if (dialogue.length == 1) {
        if (closeImmediately) {
          log.debug("closeImmediately");
          socket.close();
        } else {
          log.debug("waiting 10 secs to close");
          vertx.setTimer(10000, v -> socket.close());
        }
      } else {
        final AtomicInteger lines = new AtomicInteger(1);
        final AtomicInteger skipUntilDot = new AtomicInteger(0);
        socket.handler(RecordParser.newDelimited("\r\n", buffer -> {
          final String inputLine = buffer.toString();
          log.debug("C:" + inputLine);
          if (skipUntilDot.get() == 1) {
            if (inputLine.equals(".")) {
              skipUntilDot.set(0);
              if (lines.get() < dialogue.length) {
                log.debug("S:" + dialogue[lines.get()]);
                socket.write(dialogue[lines.getAndIncrement()] + "\r\n");
              }
            }
          } else {
            int currentLine = lines.getAndIncrement();
            if (currentLine < dialogue.length) {
              if (!inputLine.contains(dialogue[currentLine])) {
                socket.write("500 didn't expect that command\r\n");
              }
            } else {
//              socket.write("500 out of lines\r\n");
              log.info("out of lines, not sending additional reply");
            }
            if (inputLine.toUpperCase(Locale.ENGLISH).equals("DATA")) {
              skipUntilDot.set(1);
            }
            if (lines.get() < dialogue.length) {
              log.debug("S:" + dialogue[lines.get()]);
              socket.write(dialogue[lines.getAndIncrement()] + "\r\n");
            }
          }
          if (lines.get() == dialogue.length) {
            if (closeImmediately) {
              log.debug("closeImmediately");
              socket.close();
            } else {
              log.debug("waiting 10 secs to close");
              vertx.setTimer(10000, v -> socket.close());
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
      e.printStackTrace();
    }
  }

  public void setDialogue(String... dialogue) {
    this.dialogue = dialogue;
  }

  public void setCloseImmediately(boolean close) {
    closeImmediately = close;
  }

  // TODO: this assumes we are in a @After method of junit
  public void stop() {
    if (netServer != null) {
      CountDownLatch latch = new CountDownLatch(1);
      netServer.close(v -> latch.countDown());
      try {
        latch.await();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      netServer = null;
    }
  }

}

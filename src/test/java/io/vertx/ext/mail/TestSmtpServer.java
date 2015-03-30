package io.vertx.ext.mail;

import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.parsetools.RecordParser;

/*
 * really dumb mock test server that just replays a number of lines
 * as response. this doesn't check any conditions at all.  
 */
public class TestSmtpServer {

  private static final Logger log = LoggerFactory.getLogger(TestSmtpServer.class);

  private NetServer netServer;
  private String[] answers;
  private boolean closeImmediately = false;

  /*
   * set up server with a default reply that works for EHLO and no login with
   * one recipient
   */
  public TestSmtpServer(Vertx vertx) {
    setAnswers("220 example.com ESMTP",
        "250-example.com\n" +
            "250-SIZE 1000000\n" +
            "250 PIPELINING",
            "250 2.1.0 Ok",
            "250 2.1.5 Ok",
            "354 End data with <CR><LF>.<CR><LF>",
            "250 2.0.0 Ok: queued as ABCDDEF0123456789",
        "221 2.0.0 Bye");
    startServer(vertx);
  }

  public TestSmtpServer(Vertx vertx, String... answers) {
    setAnswers(answers);
    startServer(vertx);
  }

  private void startServer(Vertx vertx) {
    NetServerOptions nsOptions = new NetServerOptions();
    nsOptions.setPort(1587);
    netServer = vertx.createNetServer(nsOptions);

    netServer.connectHandler(socket -> {
      socket.write(answers[0] + "\r\n");
      log.debug("S:" + answers[0]);
      final AtomicInteger lines = new AtomicInteger(1);
      final AtomicInteger skipUntilDot = new AtomicInteger(0);
      socket.handler(RecordParser.newDelimited("\r\n", buffer -> {
        log.debug("C:" + buffer);
        if(skipUntilDot.get() == 1) {
          if(buffer.toString().equals(".")) {
            skipUntilDot.set(0);
            log.debug("S:" + answers[lines.get()]);
            socket.write(answers[lines.getAndIncrement()] + "\r\n");
          }
        } else {
          if(buffer.toString().toUpperCase(Locale.ENGLISH).equals("DATA")) {
            skipUntilDot.set(1);
          }
          if (lines.get() < answers.length) {
            log.debug("S:" + answers[lines.get()]);
            socket.write(answers[lines.getAndIncrement()] + "\r\n");
          } else {
            // wait 10 seconds for the protocol to finish
            // unless we want to simulate protocol errors
            if (closeImmediately) {
              socket.close();
            } else {
              vertx.setTimer(10000, v -> socket.close());
            }
          }
        }
      }));
    });
    CountDownLatch latch = new CountDownLatch(1);
    netServer.listen(r -> latch.countDown());
    try {
      latch.await();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public void setAnswers(String answers) {
    this.answers = new String[] { answers };
  }

  public void setAnswers(String... answers) {
    this.answers = answers;
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

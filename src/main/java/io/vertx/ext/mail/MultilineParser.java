package io.vertx.ext.mail;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.core.parsetools.RecordParser;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
public class MultilineParser implements Handler<Buffer> {
  Logger log = LoggerFactory.getLogger(MultilineParser.class);
  boolean initialized = false;
  boolean crlfMode = false;
  Buffer result;
  Handler<Buffer> output;
  RecordParser rp;

  public MultilineParser(Handler<Buffer> output) {
    Handler<Buffer> mlp = new Handler<Buffer>() {

      @Override
      public void handle(Buffer buffer) {
        // log.info("handle:\""+buffer+"\"");
        if (!initialized) {
          initialized = true;
          // process the first line to determine CRLF mode
          String line = buffer.toString();
          if (line.endsWith("\r")) {
            log.info("setting crlf line mode");
            crlfMode = true;
            rp.delimitedMode("\r\n");
            line = line.substring(0, line.length() - 1);
            appendOrHandle(Buffer.buffer(line));
          } else {
            appendOrHandle(buffer);
          }
        } else {
          appendOrHandle(buffer);
        }
      }

      private void appendOrHandle(Buffer buffer) {
        if (result == null) {
          result = buffer;
        } else {
          result.appendString("\n");
          result.appendBuffer(buffer);
        }
        if (isFinalLine(buffer)) {
          output.handle(result);
          result = null;
        }
      }

      private boolean isFinalLine(Buffer buffer) {
        String line = buffer.toString();
        return !line.matches("^\\d+-.*");
      }

    };

    this.rp = RecordParser.newDelimited("\n", mlp);
    this.output = output;
  }

  @Override
  public void handle(Buffer event) {
    rp.handle(event);
  }

}

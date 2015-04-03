package io.vertx.ext.mail.impl;

import java.util.regex.Pattern;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.core.parsetools.RecordParser;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
class MultilineParser implements Handler<Buffer> {
  private static final Pattern STATUS_LINE_CONTINUE = Pattern.compile("^\\d{3}-.*");
  private static final Logger log = LoggerFactory.getLogger(MultilineParser.class);
  private boolean initialized = false;
  private Buffer result;
  private RecordParser rp;

  public MultilineParser(Handler<Buffer> output) {
    Handler<Buffer> mlp = new Handler<Buffer>() {

      @Override
      public void handle(final Buffer buffer) {
        if (!initialized) {
          initialized = true;
          // process the first line to determine CRLF mode
          final String line = buffer.toString();
          if (line.endsWith("\r")) {
            log.debug("setting crlf line mode");
            rp.delimitedMode("\r\n");
            appendOrHandle(Buffer.buffer(line.substring(0, line.length() - 1)));
          } else {
            appendOrHandle(buffer);
          }
        } else {
          appendOrHandle(buffer);
        }
      }

      private void appendOrHandle(final Buffer buffer) {
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

      private boolean isFinalLine(final Buffer buffer) {
        String line = buffer.toString();
        return !STATUS_LINE_CONTINUE.matcher(line).matches();
      }

    };

    this.rp = RecordParser.newDelimited("\n", mlp);
  }

  @Override
  public void handle(final Buffer event) {
    rp.handle(event);
  }

}

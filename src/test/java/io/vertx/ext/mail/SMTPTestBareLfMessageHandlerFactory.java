package io.vertx.ext.mail;

import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import org.subethamail.smtp.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class SMTPTestBareLfMessageHandlerFactory implements MessageHandlerFactory {

  private static final Logger log = LoggerFactory.getLogger("SMTPTestBareLFMessageHandler");
  private final MessageHandlerFactory originalFactory;
  public SMTPTestBareLfMessageHandlerFactory(MessageHandlerFactory originalFactory) {
    this.originalFactory = originalFactory;
  }


  @Override
  public MessageHandler create(MessageContext ctx) {
    final MessageHandler originalHandler = originalFactory.create(ctx);
    return new MessageHandler() {
      @Override
      public void from(String from) throws RejectException {
        originalHandler.from(from);
      }

      @Override
      public void recipient(String recipient) throws RejectException {
        originalHandler.recipient(recipient);
      }

      @Override
      public void data(InputStream data) throws RejectException, TooMuchDataException, IOException {
        String dataString = TestUtils.inputStreamToString(data);

        List<Integer> bareLfIndexes = new ArrayList<>();
        for (int i = 0; i < dataString.length(); i++) {
          if (dataString.charAt(i) == '\n') {
            if (i == 0 || dataString.charAt(i - 1) != '\r') {
              log.warn(
                String.format("bare <LF> detected near \"%s\"",
                  dataString.substring(Math.max(0, i-10), i+10)
                    .replaceAll("\n", "\\\\n")
                    .replaceAll("\r", "\\\\r"))
              );
              bareLfIndexes.add(i);
            }
          }
        }

        if (!bareLfIndexes.isEmpty()) {
          throw new RejectException(String.format("bare <LF> received after DATA %s", bareLfIndexes));
        }

        InputStream stream = new ByteArrayInputStream(dataString.getBytes(StandardCharsets.UTF_8));
        originalHandler.data(stream);
      }

      @Override
      public void done() {
        originalHandler.done();
      }
    };
  }
}

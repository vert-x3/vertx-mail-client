package io.vertx.ext.mail;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
public class CommandResultFuture implements Future<String> {

  private static final Logger log = LoggerFactory.getLogger(CommandResultFuture.class);

  private Handler<AsyncResult<String>> handler;
  private String result;
  private Throwable cause;

  public CommandResultFuture(Handler<String> handler) {
    this.setHandler(event -> {
      if (event.succeeded()) {
        handler.handle(event.result());
      } else {
        // FIXME: how to propagate the exception properly?
        log.warn("caught exception", event.cause());
//        fail(event.cause());
//        event.cause().printStackTrace();
      }
    });
  }

  @Override
  public String result() {
    return result;
  }

  @Override
  public Throwable cause() {
    return cause;
  }

  @Override
  public boolean succeeded() {
    return result != null;
  }

  @Override
  public boolean failed() {
    return cause != null;
  }

  @Override
  public boolean isComplete() {
    return result != null || cause != null;
  }

  @Override
  public void setHandler(Handler<AsyncResult<String>> handler) {
    this.handler = handler;
  }

  @Override
  public void complete(String string) {
    result = string;
    cause=null;
    handler.handle(this);
  }

  @Override
  public void fail(Throwable throwable) {
    result=null;
    cause = throwable;
    handler.handle(this);
  }

  @Override
  public void complete() {
    log.warn("complete() is not correct implemented");
    result="";
    cause=null;
    handler.handle(this);
  }

  @Override
  public void fail(String failureMessage) {
    log.warn("fail(String) is not correct implemented");
    result=null;
    cause=new Exception(failureMessage);
    handler.handle(this);
  }

}

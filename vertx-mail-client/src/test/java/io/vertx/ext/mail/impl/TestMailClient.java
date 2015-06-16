package io.vertx.ext.mail.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.MailResult;

/**
 * MailClient providing a few internal getters for unit tests
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
public class TestMailClient implements MailClient {

  private final MailClientImpl mailClient;

  /**
   * @param vertx
   * @param config
   */
  public TestMailClient(Vertx vertx, MailConfig config) {
    mailClient = new MailClientImpl(vertx, config);
  }

  /* (non-Javadoc)
   * @see io.vertx.ext.mail.MailClient#sendMail(io.vertx.ext.mail.MailMessage, io.vertx.core.Handler)
   */
  @Override
  public MailClient sendMail(MailMessage email, Handler<AsyncResult<MailResult>> resultHandler) {
    return mailClient.sendMail(email, resultHandler);
  }

  /* (non-Javadoc)
   * @see io.vertx.ext.mail.MailClient#close()
   */
  @Override
  public void close() {
    mailClient.close();
  }

  /**
   * get the connection pool to be able to assert things about the connections
   * @return SMTPConnectionPool
   */
  public SMTPConnectionPool getConnectionPool() {
    return mailClient.getConnectionPool();
  }

  /**
   * get the connection count of the pool
   * @return SMTPConnectionPool
   */
  public int connCount() {
    return mailClient.getConnectionPool().connCount();
  }
}

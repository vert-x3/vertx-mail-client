/**
 * 
 */
package io.vertx.ext.mail.impl;

import java.util.concurrent.ConcurrentHashMap;

import io.vertx.ext.mail.MailConfig;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
class SharedPools {

  private static ConcurrentHashMap<MailConfig, SMTPConnectionPool> pools = new ConcurrentHashMap<MailConfig, SMTPConnectionPool>();

  private SharedPools() {
  }

  static SMTPConnectionPool findPool(MailConfig config) {
    SMTPConnectionPool pool = pools.get(config);
    if (pool != null) {
      if (pool.isClosed()) {
        pools.remove(config);
        pool = null;
      } else {
        pool.use();
      }
    }
    return pool;
  }

  /**
   * @param config
   * @param pool
   */
  public static void storePool(MailConfig config, SMTPConnectionPool pool) {
    pools.put(config, pool);
    pool.use();
  }

}

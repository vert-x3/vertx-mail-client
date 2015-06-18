/**
 * a few tests to check that shared/unshared clients are really doing that
 * 
 */
package io.vertx.ext.mail.impl;

import static org.junit.Assert.*;
import io.vertx.core.Vertx;
import io.vertx.ext.mail.MailConfig;

import org.junit.Test;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
public class SharedUnsharedTest {

  @Test
  public final void testSharedClient() {
    MailConfig config = new MailConfig();

    TestMailClient client1 = new TestMailClient(Vertx.vertx(), config, true);
    TestMailClient client2 = new TestMailClient(Vertx.vertx(), config, true);

    assertTrue("shared clients do not have the same pool", client1.getConnectionPool() == client2.getConnectionPool());

    client1.close();
    client2.close();
  }

  @Test
  public final void testSharedDifferentConfig() {
    MailConfig config1 = new MailConfig();
    MailConfig config2 = new MailConfig().setPort(1234);

    TestMailClient client1 = new TestMailClient(Vertx.vertx(), config1, true);
    TestMailClient client2 = new TestMailClient(Vertx.vertx(), config2, true);

    assertFalse("different config clients have the same pool",
        client1.getConnectionPool() == client2.getConnectionPool());

    client1.close();
    client2.close();
  }

  @Test
  public final void testUnsharedClient() {
    MailConfig config = new MailConfig();

    TestMailClient client1 = new TestMailClient(Vertx.vertx(), config, false);
    TestMailClient client2 = new TestMailClient(Vertx.vertx(), config, false);

    assertFalse("unshared clients have the same pool", client1.getConnectionPool() == client2.getConnectionPool());

    client1.close();
    client2.close();
  }

  @Test
  public final void testDoesNotGetClosedPool() {
    MailConfig config = new MailConfig();

    TestMailClient client = new TestMailClient(Vertx.vertx(), config, true);
    client.close();

    TestMailClient client2 = new TestMailClient(Vertx.vertx(), config, true);
    assertFalse(client2.getConnectionPool().isClosed());
    client2.close();

  }

}

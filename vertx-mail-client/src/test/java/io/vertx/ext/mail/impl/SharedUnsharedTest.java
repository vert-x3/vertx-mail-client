/**
 * a few tests to check that shared/unshared clients are really doing that.
 * <p>
 * the keys in HashMap are considered identical if .equals is true so we use two identical configs, two equal configs
 * and two configs that are different
 */

package io.vertx.ext.mail.impl;

import io.vertx.core.Vertx;
import io.vertx.ext.mail.MailConfig;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
public class SharedUnsharedTest {

  @Test
  public final void testSharedClient() {
    final MailConfig config = new MailConfig();

    final TestMailClient client1 = new TestMailClient(Vertx.vertx(), config, true);
    final TestMailClient client2 = new TestMailClient(Vertx.vertx(), config, true);

    assertSame("shared clients do not have the same pool", client1.getConnectionPool(), client2.getConnectionPool());

    client1.close();
    client2.close();
  }

  @Test
  public final void testSharedClientEqualConfig() {
    final MailConfig config1 = new MailConfig();
    final MailConfig config2 = new MailConfig();

    final TestMailClient client1 = new TestMailClient(Vertx.vertx(), config1, true);
    final TestMailClient client2 = new TestMailClient(Vertx.vertx(), config2, true);

    assertSame("clients with equal config do not have the same pool", client1.getConnectionPool(),
        client2.getConnectionPool());

    client1.close();
    client2.close();
  }

  @Test
  public final void testSharedDifferentConfig() {
    final MailConfig config1 = new MailConfig();
    final MailConfig config2 = new MailConfig().setPort(1234);

    final TestMailClient client1 = new TestMailClient(Vertx.vertx(), config1, true);
    final TestMailClient client2 = new TestMailClient(Vertx.vertx(), config2, true);

    assertNotSame("different config clients have the same pool", client1.getConnectionPool(),
        client2.getConnectionPool());
    assertNotEquals("different config clients have the same pool", client1.getConnectionPool(),
        client2.getConnectionPool());

    client1.close();
    client2.close();
  }

  @Test
  public final void testUnsharedClient() {
    final MailConfig config = new MailConfig();

    final TestMailClient client1 = new TestMailClient(Vertx.vertx(), config, false);
    final TestMailClient client2 = new TestMailClient(Vertx.vertx(), config, false);

    assertNotSame("unshared clients have the same pool", client1.getConnectionPool(), client2.getConnectionPool());
    assertNotEquals("unshared clients have the same pool", client1.getConnectionPool(), client2.getConnectionPool());

    client1.close();
    client2.close();
  }

  @Test
  public final void testDoesNotGetClosedPool() {
    final MailConfig config = new MailConfig();

    final TestMailClient client = new TestMailClient(Vertx.vertx(), config, true);
    client.close();

    final TestMailClient client2 = new TestMailClient(Vertx.vertx(), config, true);
    assertFalse(client2.getConnectionPool().isClosed());
    client2.close();

  }

}

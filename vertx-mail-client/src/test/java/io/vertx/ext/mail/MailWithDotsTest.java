package io.vertx.ext.mail;

import javax.mail.internet.MimeMessage;

import org.junit.Test;

/**
 * test messages containing a few dots
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
public class MailWithDotsTest extends SMTPTestWiser {

  @Test
  public void mailTest() {
    mailTestText(".\n");
  }

  @Test
  public void mailTest2() {
    mailTestText(".\n.\n.\n.\n.\n.\n");
  }

  @Test
  public void mailTest3() {
    mailTestText(".\n..\n...\n");
  }

  @Test
  public void mailTest4() {
    mailTestText(".xxx\n");
  }

  @Test
  public void mailTest5() {
    mailTestText(" .\n");
  }

  @Test
  public void mailTest6() {
    mailTestText(".\nX\n.\n");
  }

  @Test
  public void mailTestLarge() {
    StringBuilder sb = new StringBuilder();
    sb.append("................................................................................\n");
    for (int i = 0; i < 10; i++) {
      sb.append(sb);
    }
    mailTestText(sb.toString());
  }

  @Test
  public void mailTestMissingNL() {
    MailMessage message = exampleMessage();
    // the protocol adds a newline at the end if there isn't one
    message.setText(".");
    testSuccess(mailClientLogin(), message, () -> {
      final MimeMessage mimeMessage = wiser.getMessages().get(0).getMimeMessage();
      assertEquals(".\n", TestUtils.conv2nl(TestUtils.inputStreamToString(mimeMessage.getInputStream())));
    });
  }

  private void mailTestText(final String text) {
    MailMessage message = exampleMessage();
    message.setText(text);
    testSuccess(mailClientLogin(), message, () -> {
      final MimeMessage mimeMessage = wiser.getMessages().get(0).getMimeMessage();
      assertEquals(text, TestUtils.conv2nl(TestUtils.inputStreamToString(mimeMessage.getInputStream())));
    });
  }

}

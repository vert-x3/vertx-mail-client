package io.vertx.ext.mail;

import org.junit.Test;

import io.vertx.core.Starter;

/**
 * @author alex
 *
 * simulate "vertx run mailtest.js"
 * to run a javascript test file in e.g. eclipse
 */
public class JavascriptTest {

  @Test
  public void runJavascriptTest() {
    Starter.main(new String[] {"run", "javascript/mailtest.js"});
  }

}

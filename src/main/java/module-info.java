module io.vertx.client.mail {
  requires static io.vertx.docgen;
  requires static io.vertx.codegen.api;
  requires static io.vertx.codegen.json;
  requires io.vertx.auth.common;
  requires io.vertx.core;
  requires io.vertx.core.logging;
  exports io.vertx.ext.mail;
}

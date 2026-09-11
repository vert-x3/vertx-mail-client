---
name: writing-tests
description: >
  Testing patterns for Vert.x Mail Client: test frameworks, async testing,
  mock SMTP servers, test base classes, and how to run tests.
---

# Writing Tests

Vert.x Mail Client uses JUnit 4 with Vert.x-specific test utilities and embedded mock SMTP servers. Tests are mandatory for contributions.

## Test Framework

- **JUnit 4** (version 4.13.2) — standard test framework
- **VertxUnitRunner** — JUnit 4 runner for async tests
- **TestContext** — async test utility for handling asynchronous operations
- **AssertJ** — fluent assertions for synchronous checks
- No Docker required — all SMTP servers are embedded

## Test Base Class Hierarchy

All SMTP-related tests should extend one of these base classes:

### `SMTPTestBase` (abstract)

The root base class. Handles `Vertx` lifecycle (`@Before`/`@After`), provides helper methods and a 10-second timeout rule. Subclasses implement `startSMTP()` and `stopSMTP()`.

Key helpers:
- `testSuccess()` / `testSuccess(MailClient, MailMessage)` — send and assert success
- `testSuccess(MailClient, MailMessage, AdditionalAsserts)` — send, assert success, then run extra assertions
- `testException()` / `testException(MailClient)` — send and assert failure
- `exampleMessage()` — returns a simple `MailMessage` (from/to/subject/body)
- `mailClientDefault()` / `mailClientLogin()` / `mailClientTLS()` — pre-configured `MailClient` factories
- `defaultConfig()` / `configLogin()` / `configNoSSL()` — pre-configured `MailConfig` factories

### `SMTPTestDummy extends SMTPTestBase`

Uses `TestSmtpServer` — a Vert.x `NetServer` that replays scripted SMTP dialogues. Best for:
- Protocol-level testing (SMTP command/response sequences)
- Error code handling
- Connection management and pipelining
- Custom dialogue scenarios

### `SMTPTestWiser extends SMTPTestBase`

Uses `Wiser` (SubEtha SMTP) — a real lightweight SMTP server on port 1587. Best for:
- End-to-end message delivery tests
- Inspecting received message content (headers, body, attachments)
- TLS/SSL testing

## Test Annotations and Patterns

### Pattern 1: Simple Success/Failure Using Base Class Helpers

The most common pattern. Set `this.testContext` and call a helper:

```java
@RunWith(VertxUnitRunner.class)
public class MyTest extends SMTPTestDummy {

  @Test
  public void testSendMail(TestContext testContext) {
    this.testContext = testContext;
    testSuccess();
  }

  @Test
  public void testSendFails(TestContext testContext) {
    this.testContext = testContext;
    testException();
  }
}
```

### Pattern 2: Custom Message with Success Assertion

```java
@RunWith(VertxUnitRunner.class)
public class MyTest extends SMTPTestDummy {

  @Test
  public void testCustomMessage(TestContext testContext) {
    this.testContext = testContext;
    MailMessage email = new MailMessage()
      .setFrom("sender@example.com")
      .setTo("recipient@example.com")
      .setSubject("Test")
      .setText("Hello");
    testSuccess(email);
  }
}
```

### Pattern 3: Using asyncAssertSuccess Directly

When you need more control than the base class helpers provide:

```java
@RunWith(VertxUnitRunner.class)
public class MyTest extends SMTPTestDummy {

  @Test
  public void testWithDirectAsync(TestContext testContext) {
    this.testContext = testContext;
    MailClient client = mailClientDefault();
    client.sendMail(exampleMessage())
      .onComplete(testContext.asyncAssertSuccess(result -> {
        testContext.assertNotNull(result.getMessageID());
        client.close().onComplete(testContext.asyncAssertSuccess());
      }));
  }
}
```

### Pattern 4: Message Inspection with Wiser

When you need to verify the actual received message:

```java
@RunWith(VertxUnitRunner.class)
public class MyTest extends SMTPTestWiser {

  @Test
  public void testMessageContent(TestContext testContext) {
    this.testContext = testContext;
    testSuccess(mailClientLogin(), exampleMessage(), () -> {
      WiserMessage message = wiser.getMessages().get(0);
      testContext.assertEquals("from@example.com", message.getEnvelopeSender());
      MimeMessage mime = message.getMimeMessage();
      testContext.assertEquals("Subject", mime.getSubject());
    });
  }
}
```

### Pattern 5: Custom TestSmtpServer Dialogue

For protocol-level tests with custom SMTP responses:

```java
@RunWith(VertxUnitRunner.class)
public class MyTest extends SMTPTestDummy {

  @Override
  protected void startSMTP() {
    smtpServer = new TestSmtpServer(vertx, false, null);
    // Customize the dialogue if needed
  }

  @Test
  public void testCustomDialogue(TestContext testContext) {
    this.testContext = testContext;
    testSuccess();
  }
}
```

## Test Packages

Tests use different packages from production code:

| Production Package | Test Package |
|---|---|
| `io.vertx.ext.mail` | `io.vertx.tests.mail.client` |
| `io.vertx.ext.mail.impl` | `io.vertx.tests.mail.internal` |
| `io.vertx.ext.mail.impl.dkim` | `io.vertx.tests.mail.internal.dkim` |
| `io.vertx.ext.mail.impl.sasl` | `io.vertx.tests.mail.internal.sasl` |
| `io.vertx.ext.mail.mailencoder` | `io.vertx.tests.mail.encoder` |

## Assertions

### TestContext Assertions (async-safe)

Use these inside async callbacks — they route failures to the test framework:

```java
testContext.assertEquals(expected, actual);
testContext.assertTrue(condition);
testContext.assertFalse(condition);
testContext.assertNull(value);
testContext.assertNotNull(value);
```

### AssertJ Assertions (synchronous)

For richer assertions outside async contexts. Inside async callbacks, use the `SMTPTestBase.assertThat` helper which catches `AssertionError` and routes it to `testContext`:

```java
assertThat(mimeMessage.getContentType(), s -> s.contains("text/plain"));
```

## Running Tests

```bash
mvn test                           # run unit tests
mvn verify                         # run unit + integration tests
mvn test -Dtest=MyTest             # run specific test class
mvn test -Dtest=MyTest#testMethod  # run specific test method
```

## Common Pitfalls

1. **Forgetting `this.testContext = testContext`** — base class helpers use the `testContext` field; failing to set it causes NPE.
2. **Not closing the `MailClient`** — always close after use; the `testSuccess()`/`testException()` helpers do this automatically.
3. **Using `async.complete()` with `asyncAssertSuccess`** — `asyncAssertSuccess` manages its own async; adding an explicit `Async` and completing it can cause double-completion.
4. **Writing SMTP setup from scratch** — extend `SMTPTestDummy` or `SMTPTestWiser` instead.

## Test Requirements

- All new features must include tests
- Tests must clean up resources (mail clients, connections)
- Async tests must use `VertxUnitRunner` and `TestContext`

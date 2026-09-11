# Agent Guidelines - vertx-mail-client

Instructions for AI coding agents working in this repository.
Checkout relevant `.agents/skills/` to accomplish specific tasks.

## Build & Verify Commands

```bash
mvn test-compile         # compile code and tests
mvn test                 # run unit tests (no Docker required)
mvn verify               # run unit + integration tests + spotless check
mvn spotless:check       # verify formatting
mvn spotless:apply       # auto-fix formatting
```

## Project Structure

This is a **single-module** Maven project providing an SMTP mail client for Eclipse Vert.x.

- **Artifact**: `io.vertx:vertx-mail-client`
- **JPMS module**: `io.vertx.mail.client`

### Source Layout

- `src/main/java/io/vertx/ext/mail/`: Public API interfaces, enums, `@DataObject` classes
- `src/main/java/io/vertx/ext/mail/impl/`: Core implementation (not exported)
- `src/main/java/io/vertx/ext/mail/impl/dkim/`: DKIM signing implementation
- `src/main/java/io/vertx/ext/mail/impl/sasl/`: SASL authentication mechanisms
- `src/main/java/io/vertx/ext/mail/mailencoder/`: MIME message encoding
- `src/main/generated/`: Codegen output (converters) — do not edit by hand
- `src/main/asciidoc/`: AsciiDoc documentation
- `src/test/java/io/vertx/tests/mail/client/`: Client-level tests (SMTP interaction)
- `src/test/java/io/vertx/tests/mail/internal/`: Internal component tests (DKIM, SASL)
- `src/test/java/io/vertx/tests/mail/encoder/`: MIME encoder unit tests
- `src/test/resources/certs/`: SSL/TLS test certificates

### Key Public API Classes

- `MailClient` / `MailClientBuilder` — client entry points (static factory `MailClient.create(vertx, config)`)
- `MailConfig` — SMTP connection and authentication configuration
- `MailMessage` — email message (from, to, subject, body, attachments)
- `MailAttachment` — attachment data
- `MailResult` — send result
- `DKIMSignOptions` — DKIM signing configuration

## General Coding Rules

These rules apply when **writing or modifying code**. Code review is the checkpoint where compliance is verified.

### Logging

In production code, use the Vert.x internal logger, never SLF4J, Log4j, or `java.util.logging` directly.

```java
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;

private static final Logger logger = LoggerFactory.getLogger(MyClass.class);
```

Vert.x internal logging API doesn't support parameter placeholders.
Check the active level before debug or trace logging.

```java
if (logger.isDebugEnabled()) {
  logger.debug("Connection state: " + state);
}
```

### Async Patterns

Use Vert.x `Future<T>` and `Promise<T>` throughout. Do not use raw callbacks or `CompletableFuture` in production code.

### API Design

- Public contracts are interfaces in `io.vertx.ext.mail`
- Implementations go in `io.vertx.ext.mail.impl` subpackages
- Annotate public API interfaces and methods with `@VertxGen` for code-generation support
- Annotate configuration/result classes with `@DataObject`
- Expose construction via static factory methods, not constructors

### Module Boundaries

`module-info.java` governs exports. Only `io.vertx.ext.mail` is exported.
Internal packages must not be widened without discussion.
The test module descriptor (`src/test/java/module-info.java`) can be modified freely.

### Copyright Header

New Java files must include the license header matching existing files:

```java
/*
 *  Copyright (c) 2011-2026 The original author or authors
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *       The Eclipse Public License is available at
 *       http://www.eclipse.org/legal/epl-v10.html
 *
 *       The Apache License v2.0 is available at
 *       http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 */
```

The range is always `2011-[current year]`.

## Testing Guidelines

For comprehensive testing patterns and examples, see `.agents/skills/writing-tests/SKILL.md`.

### Test Framework

- Use **JUnit 4** (version 4.13.2) for all tests
- Async tests use **VertxUnitRunner** (JUnit 4 runner) and **TestContext**
- Assertions: **TestContext** for async contexts, **AssertJ** for synchronous assertions
- No Docker required — all tests use **embedded mock SMTP servers**

### Mock SMTP Servers

Two mock server implementations are available:

1. **`TestSmtpServer`** — Vert.x `NetServer`-based fake that replays scripted SMTP dialogues. Use for protocol-level testing (error codes, connection handling, pipelining).
2. **`Wiser`** (SubEtha SMTP) — real lightweight SMTP server. Use for end-to-end tests where you need to inspect the received message content.

### Test Base Class Hierarchy

- **`SMTPTestBase`** (abstract) — creates `Vertx` in `@Before`, closes in `@After`. Provides helpers: `testSuccess()`, `testException()`, `exampleMessage()`, config factories. Has a 10-second `@Rule Timeout`.
- **`SMTPTestDummy extends SMTPTestBase`** — starts `TestSmtpServer`. Extend this for protocol-level tests.
- **`SMTPTestWiser extends SMTPTestBase`** — starts `Wiser` on port 1587. Extend this for message-inspection tests.

### Test Patterns

```java
@RunWith(VertxUnitRunner.class)
public class MyTest extends SMTPTestDummy {

  @Test
  public void testSendMail(TestContext testContext) {
    this.testContext = testContext;
    testSuccess(mailClientDefault(), exampleMessage());
  }
}
```

### Test Packages

Tests use a **different package** from production code:
- Production: `io.vertx.ext.mail` / `io.vertx.ext.mail.impl`
- Tests: `io.vertx.tests.mail.client` / `io.vertx.tests.mail.internal` / `io.vertx.tests.mail.encoder`

### Running Tests

```bash
mvn test                           # run unit tests
mvn verify                         # run unit + integration tests
mvn test -Dtest=MyTest             # run specific test class
mvn test -Dtest=MyTest#testMethod  # run specific test method
```

### Test Requirements

- All new features must include tests
- Tests must clean up resources (mail clients, connections)
- Extend `SMTPTestDummy` or `SMTPTestWiser` rather than writing SMTP setup from scratch

## Development Workflow

### Incremental Development

When making changes:
1. Compile frequently: `mvn compile`
2. Run affected tests: `mvn test -Dtest=RelevantTest`
3. Verify formatting: `mvn spotless:check`
4. Run full build before PR: `mvn clean verify`

### Build Optimization

```bash
# Skip tests during development
mvn compile -DskipTests

# Compile tests without running them
mvn test-compile
```

## Specialized Skills

When performing specific tasks, read the relevant skill file for detailed guidance:

- **Writing tests** — Read `.agents/skills/writing-tests/SKILL.md` when creating or modifying tests

## Contribution Process

- All commits must be signed off: `git commit -s` (DCO)
- Commit messages should end with: `Assisted-by: [Provider] [Model-Family] ([Version/ID])` (replace placeholders)
- Contributors must have signed the [Eclipse Contributor Agreement (ECA)](https://www.eclipse.org/legal/ECA.php)

## Code Review Guidelines

### Verify

- General coding rules above are followed
- Test coverage is present; async tests use `VertxUnitRunner` and `TestContext`
- No breaking changes to public API interfaces without prior discussion

### Do Not Comment On

- Patterns already used consistently throughout the codebase

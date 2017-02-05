package io.vertx.kotlin.ext.mail

import io.vertx.ext.mail.MailResult

fun MailResult(
  messageID: String? = null,
  recipients: Iterable<String>? = null): MailResult = io.vertx.ext.mail.MailResult().apply {

  if (messageID != null) {
    this.setMessageID(messageID)
  }
  if (recipients != null) {
    this.setRecipients(recipients.toList())
  }
}


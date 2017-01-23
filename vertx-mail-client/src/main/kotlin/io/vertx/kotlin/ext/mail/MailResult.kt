package io.vertx.kotlin.ext.mail

import io.vertx.ext.mail.MailResult

fun MailResult(
    messageID: String? = null,
  recipients: List<String>? = null): MailResult = io.vertx.ext.mail.MailResult().apply {

  if (messageID != null) {
    this.messageID = messageID
  }

  if (recipients != null) {
    this.recipients = recipients
  }

}


package io.vertx.kotlin.ext.mail

import io.vertx.ext.mail.MailMessage
import io.vertx.ext.mail.MailAttachment

fun MailMessage(
  attachment: Iterable<io.vertx.ext.mail.MailAttachment>? = null,
  bcc: Iterable<String>? = null,
  bounceAddress: String? = null,
  cc: Iterable<String>? = null,
  fixedHeaders: Boolean? = null,
  from: String? = null,
  headers: Map<String, String>? = null,
  html: String? = null,
  inlineAttachment: Iterable<io.vertx.ext.mail.MailAttachment>? = null,
  subject: String? = null,
  text: String? = null,
  to: Iterable<String>? = null): MailMessage = io.vertx.ext.mail.MailMessage().apply {

  if (attachment != null) {
    this.setAttachment(attachment.toList())
  }
  if (bcc != null) {
    this.setBcc(bcc.toList())
  }
  if (bounceAddress != null) {
    this.setBounceAddress(bounceAddress)
  }
  if (cc != null) {
    this.setCc(cc.toList())
  }
  if (fixedHeaders != null) {
    this.setFixedHeaders(fixedHeaders)
  }
  if (from != null) {
    this.setFrom(from)
  }
  if (headers != null) {
    for (item in headers) {
      this.addHeader(item.key, item.value)
    }
  }
  if (html != null) {
    this.setHtml(html)
  }
  if (inlineAttachment != null) {
    this.setInlineAttachment(inlineAttachment.toList())
  }
  if (subject != null) {
    this.setSubject(subject)
  }
  if (text != null) {
    this.setText(text)
  }
  if (to != null) {
    this.setTo(to.toList())
  }
}


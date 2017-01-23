package io.vertx.kotlin.ext.mail

import io.vertx.ext.mail.MailMessage

fun MailMessage(
    attachment: List<io.vertx.ext.mail.MailAttachment>? = null,
  bcc: List<String>? = null,
  bounceAddress: String? = null,
  cc: List<String>? = null,
  fixedHeaders: Boolean? = null,
  from: String? = null,
  html: String? = null,
  inlineAttachment: List<io.vertx.ext.mail.MailAttachment>? = null,
  subject: String? = null,
  text: String? = null,
  to: List<String>? = null): MailMessage = io.vertx.ext.mail.MailMessage().apply {

  if (attachment != null) {
    this.attachment = attachment
  }

  if (bcc != null) {
    this.bcc = bcc
  }

  if (bounceAddress != null) {
    this.bounceAddress = bounceAddress
  }

  if (cc != null) {
    this.cc = cc
  }

  if (fixedHeaders != null) {
    this.isFixedHeaders = fixedHeaders
  }

  if (from != null) {
    this.from = from
  }

  if (html != null) {
    this.html = html
  }

  if (inlineAttachment != null) {
    this.inlineAttachment = inlineAttachment
  }

  if (subject != null) {
    this.subject = subject
  }

  if (text != null) {
    this.text = text
  }

  if (to != null) {
    this.to = to
  }

}


package io.vertx.kotlin.ext.mail

import io.vertx.ext.mail.MailAttachment

fun MailAttachment(
  contentId: String? = null,
  contentType: String? = null,
  data: io.vertx.core.buffer.Buffer? = null,
  description: String? = null,
  disposition: String? = null,
  headers: Map<String, String>? = null,
  name: String? = null): MailAttachment = io.vertx.ext.mail.MailAttachment().apply {

  if (contentId != null) {
    this.setContentId(contentId)
  }
  if (contentType != null) {
    this.setContentType(contentType)
  }
  if (data != null) {
    this.setData(data)
  }
  if (description != null) {
    this.setDescription(description)
  }
  if (disposition != null) {
    this.setDisposition(disposition)
  }
  if (headers != null) {
    for (item in headers) {
      this.addHeader(item.key, item.value)
    }
  }
  if (name != null) {
    this.setName(name)
  }
}


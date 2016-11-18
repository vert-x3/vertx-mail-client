package io.vertx.kotlin.ext.mail

import io.vertx.ext.mail.MailAttachment

fun MailAttachment(
        contentId: String? = null,
    contentType: String? = null,
    data: io.vertx.core.buffer.Buffer? = null,
    description: String? = null,
    disposition: String? = null,
    name: String? = null): MailAttachment = io.vertx.ext.mail.MailAttachment().apply {

    if (contentId != null) {
        this.contentId = contentId
    }

    if (contentType != null) {
        this.contentType = contentType
    }

    if (data != null) {
        this.data = data
    }

    if (description != null) {
        this.description = description
    }

    if (disposition != null) {
        this.disposition = disposition
    }

    if (name != null) {
        this.name = name
    }

}


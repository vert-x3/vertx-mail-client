/*
 *  Copyright (c) 2011-2015 The original author or authors
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

package io.vertx.ext.mail.mailencoder;

import io.vertx.core.http.CaseInsensitiveHeaders;
import io.vertx.ext.mail.MailAttachment;

class AttachmentPart extends EncodedPart {

  public AttachmentPart(MailAttachment attachment) {
    headers = new CaseInsensitiveHeaders();
    String name = attachment.getName();
    String contentType;
    if (attachment.getContentType() != null) {
      contentType = attachment.getContentType();
    } else {
      contentType = "application/octet-stream";
    }
    if (name != null) {
      int index = contentType.length() + 22;
      contentType += "; name=\"" + Utils.encodeHeader(name, index) + "\"";
    }
    headers.set("Content-Type", contentType);
    headers.set("Content-Transfer-Encoding", "base64");

    if (attachment.getDescription() != null) {
      headers.set("Content-Description", attachment.getDescription());
    }
    String disposition;
    if (attachment.getDisposition() != null) {
      disposition = attachment.getDisposition();
    } else {
      disposition = "attachment";
    }
    if (name != null) {
      int index = disposition.length() + 33;
      disposition += "; filename=\"" + Utils.encodeHeader(name, index) + "\"";
    }
    headers.set("Content-Disposition", disposition);
    if (attachment.getContentId() != null) {
      headers.set("Content-ID", attachment.getContentId());
    }
    if (attachment.getHeaders() != null) {
      headers.addAll(attachment.getHeaders());
    }

    part = Utils.base64(attachment.getData().getBytes());
  }

}

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
      contentType += "; name=" + name;
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
      disposition += "; filename=" + name;
    }
    headers.set("Content-Disposition", disposition);
    headers.set("Content-ID", "<" + name + ">");
    part = Utils.base64(attachment.getData().getBytes());
  }

}

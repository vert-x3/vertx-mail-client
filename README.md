# Vert.x SMTP client

A very preliminary version of a smtp client for vert.x.

Ported the first version of the client to vert.x 3.0, its missing a few bits to be
really useful most importantly it has no real vert.x api yet (the service interface is not yet working). The test class shows how this could be used, the whole client is async and
supports SSL, STARTTLS, SASL).


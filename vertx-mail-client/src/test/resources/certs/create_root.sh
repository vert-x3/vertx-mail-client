#! /bin/bash
# generate test certs
# password is always "password", there are a few commands that didn't accept a pw on the commandline

#openssl genrsa -out rootCA.key 2048
#openssl req -x509 -new -nodes -key rootCA.key -sha256 -days 1024 -out rootCA.pem -config openssl.cnf
#keytool -importcert -storepass password -keystore client.jks -file rootCA.pem -alias rootca

cp client.jks server.jks
cat openssl.cnf.head altnames_ip >openssl.cnf
openssl genrsa -out server.key 2048
openssl req -new -key server.key -out server.csr -config openssl.cnf
openssl x509 -req -in server.csr -CA rootCA.pem -CAkey rootCA.key -CAcreateserial -out server.crt -days 500 -sha256 -extensions req_ext -extfile openssl.cnf
openssl pkcs12 -export -inkey server.key -in server.crt -certfile rootCA.pem -out server.p12
keytool -importkeystore -srckeystore server.p12 -srcstorepass password -destkeystore server.jks -deststorepass password -srcstoretype pkcs12 -deststoretype jks

cp client.jks server2.jks
cat openssl.cnf.head altnames >openssl.cnf
openssl genrsa -out server2.key 2048
openssl req -new -key server2.key -out server2.csr -config openssl.cnf
openssl x509 -req -in server2.csr -CA rootCA.pem -CAkey rootCA.key -CAcreateserial -out server2.crt -days 500 -sha256 -extensions req_ext -extfile openssl.cnf
openssl pkcs12 -export -inkey server2.key -in server2.crt -certfile rootCA.pem -out server2.p12
keytool -importkeystore -srckeystore server2.p12 -srcstorepass password -destkeystore server2.jks -deststorepass password -srcstoretype pkcs12 -deststoretype jks

cp client.jks server3.jks
cp openssl.cnf.head openssl.cnf
openssl genrsa -out server3.key 2048
openssl req -new -key server3.key -out server3.csr -config openssl.cnf
openssl x509 -req -in server3.csr -CA rootCA.pem -CAkey rootCA.key -CAcreateserial -out server3.crt -days 500 -sha256
openssl pkcs12 -export -inkey server3.key -in server3.crt -certfile rootCA.pem -out server3.p12
keytool -importkeystore -srckeystore server3.p12 -srcstorepass password -destkeystore server3.jks -deststorepass password -srcstoretype pkcs12 -deststoretype jks

# this certificate is used for a cert unit test only, so we do not need a jks
cat openssl.cnf.head altnames_wildcard >openssl.cnf
openssl x509 -req -in server.csr -CA rootCA.pem -CAkey rootCA.key -CAcreateserial -out server_wildcard.crt -days 500 -sha256 -extensions req_ext -extfile openssl.cnf

# lolx-auth

FIXME

## Prerequisites

You will need [Leiningen][] 2.0.0 or above installed.

[leiningen]: https://github.com/technomancy/leiningen

## Running

To start a web server for the application, run:

    lein ring server 8082

## RSA Keys

to generate priv-pub key: ssh-keygen
to generate DER format for public key (for java): openssl rsa -in private_key.pem -pubout -outform DER -out public_key.der
to generate public key in RSA format: openssl rsa -in private -pubout > mykey.pub

## License

Copyright © 2016 FIXME

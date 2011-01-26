To use SSL, you will need a certificate.  A self-signed certificate for
localhost is included, but if you want to use a different address you will
need to create another.  To generate a self-signed certificate for an arbitrary
host name, you can use (for example):

  keytool -keystore keystore -alias jetty -genkey -keyalg RSA -validity 365

Be sure and give the CN as the name that will be used with -bindAddress (which
will be 127.0.0.1 if not provided).

Note that self-signed certificates will cause the browser to prompt the user
to accept the certificate -- this should be fine for development, but if not
you can purchase a real web server certificate from a trusted CA and convert
it to keystore format using openssl and keytool.

You can use your own keystore like this:
  -server :keystore=/path/to/keystore,password=password
OR
  -server :keystore=/path/to/keystore,pwfile=/path/to/password/file

Using the password option exposes the password to other users on your system,
so the pwfile option is recommended instead if you care about keeping the
password secret.

You can also set the clientAuth parameter to request or require client
certificates (which must have a suitable certificate chain in the keystore),
like this:
  -server :keystore=/path/to/keystore,password=password,clientAuth=WANT
OR
  -server :keystore=/path/to/keystore,password=password,clientAuth=REQUIRE

You can use a default localhost-only self-signed certificate by just using
  -server :ssl
     


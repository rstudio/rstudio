/*
 * CryptoTests.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <iterator>

#include <gsl/gsl>

#include <core/system/Crypto.hpp>

#include <tests/TestThat.hpp>

#include <fcntl.h>

#include <openssl/err.h>
#include <openssl/bio.h>
#include <openssl/buffer.h>
#include <openssl/evp.h>
#include <openssl/pem.h>
#include <openssl/rsa.h>
#include <openssl/x509.h>
#include <openssl/x509v3.h>

namespace rstudio {
namespace core {
namespace system {
namespace tests {

test_context("CryptoTests")
{
   test_that("Can AES encrypt/decrypt")
   {
      // generate a random 128-bit key and IV
      std::vector<unsigned char> key;
      std::vector<unsigned char> iv;
      Error error = core::system::crypto::random(16, key);
      REQUIRE_FALSE(error);
      error = core::system::crypto::random(16, iv);
      REQUIRE_FALSE(error);

      // construct the data to encrypt
      std::string payload = "Hello, world! This is a secret.";
      std::vector<unsigned char> data;
      std::copy(payload.begin(), payload.end(), std::back_inserter(data));

      // encrypt the data
      std::vector<unsigned char> encryptedData;
      error = core::system::crypto::aesEncrypt(data, key, iv, encryptedData);
      REQUIRE_FALSE(error);

      // decrypt the encrypted data
      std::vector<unsigned char> decryptedData;
      error = core::system::crypto::aesDecrypt(encryptedData, key, iv, decryptedData);
      REQUIRE_FALSE(error);

      // verify that the decryption gives us back the original data
      std::string decryptedPayload;
      std::copy(decryptedData.begin(), decryptedData.end(), std::back_inserter(decryptedPayload));
      REQUIRE(payload == decryptedPayload);
   }

   test_that("Can generate RSA file cert")
   {
      FilePath certFilePath, keyFilePath;
      REQUIRE_FALSE(FilePath::tempFilePath(".cert", certFilePath));
      REQUIRE_FALSE(FilePath::tempFilePath(".key", keyFilePath));
      Error error = core::system::crypto::generateRsaCertAndKeyFiles("testCN", certFilePath, keyFilePath);
      REQUIRE_FALSE(error);

      REQUIRE(certFilePath.exists());
      REQUIRE(keyFilePath.exists());

      BIO* certBIO = BIO_new_file(certFilePath.getAbsolutePath().c_str(), "r");
      BIO* keyBIO = BIO_new_file(keyFilePath.getAbsolutePath().c_str(), "r");
      X509* cert = PEM_read_bio_X509(certBIO, NULL, 0, NULL);
      EVP_PKEY* key = PEM_read_bio_PrivateKey(keyBIO, NULL, NULL, NULL);

      // check the common name we gave it matches
      REQUIRE(X509_check_host(cert, (const char *) "testCN", 0, 0, NULL) == 1);
      // check that the private key matches the public key in the cert
      REQUIRE(X509_check_private_key(cert, key) == 1);

      EVP_PKEY_free(key);
      X509_free(cert);
      BIO_free(certBIO);
      BIO_free(keyBIO);
 
      certFilePath.remove();
      keyFilePath.remove();
   }

   test_that("Can generate RSA string cert")
   {
      std::string certStr, keyStr;
      Error error = core::system::crypto::generateRsaCertAndKeyPair("testCN", certStr, keyStr);
      REQUIRE_FALSE(error);

      BIO* certBIO = BIO_new_mem_buf(const_cast<char*>(certStr.c_str()), gsl::narrow_cast<int>(certStr.size()));
      BIO* keyBIO = BIO_new_mem_buf(const_cast<char*>(keyStr.c_str()), gsl::narrow_cast<int>(keyStr.size()));
      X509* cert = PEM_read_bio_X509(certBIO, NULL, 0, NULL);
      EVP_PKEY* key = PEM_read_bio_PrivateKey(keyBIO, NULL, NULL, NULL);

      // check the common name we gave it matches
      REQUIRE(X509_check_host(cert, (const char *) "testCN", 0, 0, NULL) == 1);
      // check that the private key matches the public key in the cert
      REQUIRE(X509_check_private_key(cert, key) == 1);

      EVP_PKEY_free(key);
      X509_free(cert);
      BIO_free(certBIO);
      BIO_free(keyBIO);
   }

   test_that("Roundtrip RSA signing and verification works")
   {
      std::string message = "message from the key holder";
      std::string pub, priv, sig;
      REQUIRE_FALSE(core::system::crypto::generateRsaKeyPair(&pub, &priv));
      REQUIRE_FALSE(core::system::crypto::rsaSign(message, priv, &sig));
      REQUIRE_FALSE(core::system::crypto::rsaVerify(message, sig, pub));
      // Sanity checks.
      REQUIRE(pub.rfind("-----BEGIN PUBLIC KEY-----", 0) == 0);
      REQUIRE(priv.rfind("-----BEGIN PRIVATE KEY-----", 0) == 0);
      REQUIRE(sig.size() == 256);
   }

   test_that("SHA-256 hashing works correctly")
   {
      // Generated with openssl sha256 -hex.
      std::string message = "secret message";
      std::vector<unsigned char> raw = {
         0xbb, 0x0b, 0x57, 0x00, 0x5f, 0x01, 0x01, 0x8b, 0x19, 0xc2, 0x78, 0xc5,
         0x52, 0x73, 0xa6, 0x01, 0x18, 0xff, 0xdd, 0x3e, 0x57, 0x90, 0xcc, 0xc8,
         0xa4, 0x8c, 0xad, 0x03, 0x90, 0x7f, 0xa5, 0x21
      };
      std::string expected(raw.begin(), raw.end());
      std::string hash;
      REQUIRE_FALSE(core::system::crypto::sha256(message, &hash));
      REQUIRE(hash.size() == 32);
      REQUIRE(hash == expected);
   }
}

} // end namespace tests
} // end namespace system
} // end namespace core
} // end namespace rstudio

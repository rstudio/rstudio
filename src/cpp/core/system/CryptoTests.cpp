/*
 * CryptoTests.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <iterator>

#include <gsl/gsl-lite.hpp>

#include <core/system/Crypto.hpp>

#include <gtest/gtest.h>

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

/*
// A randomly-generated key pair that's only used for this test file
// Unused here, but preserved in case you need to encrypt some more test data with it
// pragma: allowlist nextline secret
static const char* RSA_PUBLIC = R"(-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAll6WsBPm0SQUOMlRqZLh7IL75KMh2ve0pZrl
qN2gPsqislJ+VmAM95sd2CAk6iJPV9E9g4bQytyHp2KtnfZx6CjCwHUbLLUdS7KFkZ5Z6/N9Gj9MEJnf
X16WBfOUBrCXwABjZV7vgmMUqz1Wt6NS9G+WRypToEj6QRGp6PUm+4HzsY2k7qWe2i4zp9gzYobL6J+M
iqR7/+Jz+zjyas2MOa7lz7glc0lLaV33/hCNWFTTrqQt8U82gv0RiOOf0ozJiMoLgYolxPYQAcBlw1Z/
8ik1hOShhyAt7zui/7HSS89hgttPPQdNfyWYPVZnCfj3qVSZdX4wY5IUf1lhsYRFpwIDAQAB
-----END PUBLIC KEY-----)";
*/
// pragma: allowlist nextline secret
static const char* RSA_PRIVATE = R"(-----BEGIN PRIVATE KEY-----
MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCWXpawE+bRJBQ4yVGpkuHsgvvkoyHa
97SlmuWo3aA+yqKyUn5WYAz3mx3YICTqIk9X0T2DhtDK3IenYq2d9nHoKMLAdRsstR1LsoWRnlnr830a
P0wQmd9fXpYF85QGsJfAAGNlXu+CYxSrPVa3o1L0b5ZHKlOgSPpBEano9Sb7gfOxjaTupZ7aLjOn2DNi
hsvon4yKpHv/4nP7OPJqzYw5ruXPuCVzSUtpXff+EI1YVNOupC3xTzaC/RGI45/SjMmIyguBiiXE9hAB
wGXDVn/yKTWE5KGHIC3vO6L/sdJLz2GC2089B01/JZg9VmcJ+PepVJl1fjBjkhR/WWGxhEWnAgMBAAEC
ggEAFHjTRiynd6DUWfjas94KATPCDJpDITcpMoS4sLlfuDzJUsLVbXgNO+az9PlHXVMHf1q+57nCkmPc
2iaeoYtsbaGjBiXLadZMo0IuWil3b57KUPj+J6JzXV4Yyb2kURbYyiyjW6lFrXhE1511wXGseRf6Gz1B
fmiBHbvEaLF7ubD9oJLd8WI0m7SGmcpa+QZ60OiEoZkKo94A6fy6+gtcHcIIKicmTSZM24QSKorD2ssl
jmG9A9/q1qKITDxYqBsflynnywIJolaLIEoZ09A3yLTrpF03Q60wtNyWeQgi+ZlsR4vXK9iVKpZ8n2U5
5o8yosaPF+dnUcWLcJAsysTsjQKBgQDL1dRoRAcBJNSS9FzGRG6Uw1K6HAJhE6HKc3gXHgC6dcY7krKB
twzCHyrH5HRBvB5gZWZGsaL1PErGHZxocWvd8DJZPOUfuh7Cu9ixvGIFW3pFAfYflKTH1e9vfnbozrpc
1QwLQhD8pGFmx1/ivqwMLu+2PmtNdsczfH5mdJ+0swKBgQC82fk7oFut0j0V0zsFRaWu9vQmVk/tBOFP
8PmuY9C7LoIF+WMafsa+Ox/m554Xbtn0Biq9hyyOq4WquXyzTKYvzUjeICdkzLRXnwOwPgqY45XICK5k
ip68W70fErFkuek5YtmAjp2hSUvIgrh92D6j5L94bShIrKRlxyx72WJtPQKBgQCO0ZkNITUDMSoceUkT
xljwtNae/gcQu6+t6S/oiqYZ/3FQxl16k6ZF0Y6pFkH62PMzuXhq6gXy7Da8D31KlMXucGDms8saas8o
xHN1PTg05r6J4XDw+bZnKlekeCiawFZFuyTAMp8yVX7Fg8aEWfK0aqKcv6lxUlsKkR0Dbo2EDwKBgDRs
DVMP4GMPyQUV7Xw5KRS4WG2L6APTJvgZ3DUcYASVlGI0J95i8qg7oU9nW+sFSlsCmzqhGy0/T4tkkcjz
re32/hMqucSxTh5EdbGmhyqJgjpUrpQaJpYCoSzO848STDsxxU56SCdNQUwXfT7xl/HGvZ+gLV5/DeNy
tlZqsXfBAoGBAMNfdpLdEnshMbygyb0/yy1iP8MbaMPnIP4n1NSfgLaQ3BVpaowWtceyU7CQk+xuTlXv
5N+1MJ3Ha3R3VbyWse504h9ysn3YqsLGkjZ2mywzpOoaFQAksa+3yG9OEyxbBeyKB74OvalZreZcx2DP
wmzjIvDnR/4XIgku9AVpR1Uk
-----END PRIVATE KEY-----)";

using pkey_ptr = std::unique_ptr<EVP_PKEY, decltype(&EVP_PKEY_free)>;

static pkey_ptr loadTestPrivateKey()
{
  std::unique_ptr<BIO, decltype(&BIO_free)> bio(BIO_new_mem_buf(RSA_PRIVATE, -1), BIO_free);
  if (!bio) {
    return pkey_ptr(nullptr, EVP_PKEY_free);
  }

  return pkey_ptr(PEM_read_bio_PrivateKey(bio.get(), nullptr, nullptr, nullptr), EVP_PKEY_free);
}

TEST(CryptoTest, AesEncryptDecrypt)
{
   // generate a random 128-bit key and IV
   std::vector<unsigned char> key;
   std::vector<unsigned char> iv;
   Error error = core::system::crypto::random(16, key);
   ASSERT_FALSE(error);
   error = core::system::crypto::random(16, iv);
   ASSERT_FALSE(error);

   // construct the data to encrypt
   std::string payload = "Hello, world! This is a secret.";
   std::vector<unsigned char> data;
   std::copy(payload.begin(), payload.end(), std::back_inserter(data));

   // encrypt the data
   std::vector<unsigned char> encryptedData;
   error = core::system::crypto::aesEncrypt(data, key, iv, encryptedData);
   ASSERT_FALSE(error);

   // decrypt the encrypted data
   std::vector<unsigned char> decryptedData;
   error = core::system::crypto::aesDecrypt(encryptedData, key, iv, decryptedData);
   ASSERT_FALSE(error);

   // verify that the decryption gives us back the original data
   std::string decryptedPayload;
   std::copy(decryptedData.begin(), decryptedData.end(), std::back_inserter(decryptedPayload));
   ASSERT_EQ(payload, decryptedPayload);
}

TEST(CryptoTest, GenerateRsaFileCert)
{
   FilePath certFilePath, keyFilePath;
   ASSERT_FALSE(FilePath::tempFilePath(".cert", certFilePath));
   ASSERT_FALSE(FilePath::tempFilePath(".key", keyFilePath));
   Error error = core::system::crypto::generateRsaCertAndKeyFiles("testCN", certFilePath, keyFilePath);
   ASSERT_FALSE(error);

   ASSERT_TRUE(certFilePath.exists());
   ASSERT_TRUE(keyFilePath.exists());

   BIO* certBIO = BIO_new_file(certFilePath.getAbsolutePath().c_str(), "r");
   BIO* keyBIO = BIO_new_file(keyFilePath.getAbsolutePath().c_str(), "r");
   X509* cert = PEM_read_bio_X509(certBIO, NULL, 0, NULL);
   EVP_PKEY* key = PEM_read_bio_PrivateKey(keyBIO, NULL, NULL, NULL);

   // check the common name we gave it matches
   ASSERT_EQ(1, X509_check_host(cert, (const char *) "testCN", 0, 0, NULL));
   // check that the private key matches the public key in the cert
   ASSERT_EQ(1, X509_check_private_key(cert, key));

   EVP_PKEY_free(key);
   X509_free(cert);
   BIO_free(certBIO);
   BIO_free(keyBIO);

   certFilePath.remove();
   keyFilePath.remove();
}

TEST(CryptoTest, GenerateRsaStringCert)
{
   std::string certStr, keyStr;
   Error error = core::system::crypto::generateRsaCertAndKeyPair("testCN", certStr, keyStr);
   ASSERT_FALSE(error);

   BIO* certBIO = BIO_new_mem_buf(const_cast<char*>(certStr.c_str()), gsl::narrow_cast<int>(certStr.size()));
   BIO* keyBIO = BIO_new_mem_buf(const_cast<char*>(keyStr.c_str()), gsl::narrow_cast<int>(keyStr.size()));
   X509* cert = PEM_read_bio_X509(certBIO, NULL, 0, NULL);
   EVP_PKEY* key = PEM_read_bio_PrivateKey(keyBIO, NULL, NULL, NULL);

   // check the common name we gave it matches
   ASSERT_EQ(1, X509_check_host(cert, (const char *) "testCN", 0, 0, NULL));
   // check that the private key matches the public key in the cert
   ASSERT_EQ(1, X509_check_private_key(cert, key));

   EVP_PKEY_free(key);
   X509_free(cert);
   BIO_free(certBIO);
   BIO_free(keyBIO);
}

TEST(CryptoTest, RsaSigningVerificationRoundtrip)
{
   std::string message = "message from the key holder";
   std::string pub, priv, sig;
   ASSERT_FALSE(core::system::crypto::generateRsaKeyPair(&pub, &priv));
   ASSERT_FALSE(core::system::crypto::rsaSign(message, priv, &sig));
   ASSERT_FALSE(core::system::crypto::rsaVerify(message, sig, pub));
   // Sanity checks.
   ASSERT_EQ(0u, pub.rfind("-----BEGIN PUBLIC KEY-----", 0));
   ASSERT_EQ(0u, priv.rfind("-----BEGIN PRIVATE KEY-----", 0));  // pragma: allowlist secret
   EXPECT_EQ(kRsaKeySizeBits / 8, sig.size());
}

TEST(CryptoTest, Sha256Hashing)
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
   ASSERT_FALSE(core::system::crypto::sha256(message, &hash));
   EXPECT_EQ(32u, hash.size());
   ASSERT_EQ(expected, hash);
}

TEST(CryptoTest, Sha256HexHashing)
{
   // Generated with openssl sha256 -hex.
   std::string message = "secret message";
   std::string expectedHex = "bb0b57005f01018b19c278c55273a60118ffdd3e5790ccc8a48cad03907fa521";  // pragma: allowlist secret
   std::string hexHash;
   ASSERT_FALSE(core::system::crypto::sha256Hex(message, &hexHash));
   EXPECT_EQ(64u, hexHash.size());
   ASSERT_EQ(expectedHex, hexHash);
}

TEST(CryptoTest, Sha256BinaryHexEquivalence)
{
   std::string message = "test consistency";
   std::string binaryHash, hexHash;
   
   ASSERT_FALSE(core::system::crypto::sha256(message, &binaryHash));
   ASSERT_FALSE(core::system::crypto::sha256Hex(message, &hexHash));
   
   // Convert binary hash to hex manually and compare
   std::string expectedHex;
   for (unsigned char byte : binaryHash)
   {
      char hexByte[3];
      snprintf(hexByte, sizeof(hexByte), "%02x", static_cast<unsigned int>(byte));
      expectedHex += hexByte;
   }
   
   ASSERT_EQ(expectedHex, hexHash);
   EXPECT_EQ(64u, hexHash.size());
   EXPECT_EQ(32u, binaryHash.size());
}

TEST(CryptoTest, Sha256HexOutputCharacters)
{
   std::string message = "validate hex format";
   std::string hexHash;
   
   ASSERT_FALSE(core::system::crypto::sha256Hex(message, &hexHash));
   
   // Check that all characters are valid hex (0-9, a-f)
   for (char c : hexHash)
   {
<<<<<<< HEAD
      ASSERT_TRUE((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'));
=======
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
      REQUIRE(pub.rfind("-----BEGIN PUBLIC KEY-----", 0) == 0);   // pragma: allowlist secret
      REQUIRE(priv.rfind("-----BEGIN PRIVATE KEY-----", 0) == 0); // pragma: allowlist secret
      REQUIRE(sig.size() == kRsaKeySizeBits / 8);
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

   test_that("SHA-256 hex hashing works correctly")
   {
      // Generated with openssl sha256 -hex.
      std::string message = "secret message";
      // pragma: allowlist nextline secret
      std::string expectedHex = "bb0b57005f01018b19c278c55273a60118ffdd3e5790ccc8a48cad03907fa521";
      std::string hexHash;
      REQUIRE_FALSE(core::system::crypto::sha256Hex(message, &hexHash));
      REQUIRE(hexHash.size() == 64);
      REQUIRE(hexHash == expectedHex);
   }

   test_that("SHA-256 binary and hex produce equivalent results")
   {
      std::string message = "test consistency";
      std::string binaryHash, hexHash;

      REQUIRE_FALSE(core::system::crypto::sha256(message, &binaryHash));
      REQUIRE_FALSE(core::system::crypto::sha256Hex(message, &hexHash));

      // Convert binary hash to hex manually and compare
      std::string expectedHex;
      for (unsigned char byte : binaryHash)
      {
         char hexByte[3];
         snprintf(hexByte, sizeof(hexByte), "%02x", static_cast<unsigned int>(byte));
         expectedHex += hexByte;
      }

      REQUIRE(hexHash == expectedHex);
      REQUIRE(hexHash.size() == 64);
      REQUIRE(binaryHash.size() == 32);
   }

   test_that("SHA-256 hex output contains only valid hexadecimal characters")
   {
      std::string message = "validate hex format";
      std::string hexHash;

      REQUIRE_FALSE(core::system::crypto::sha256Hex(message, &hexHash));

      // Check that all characters are valid hex (0-9, a-f)
      for (char c : hexHash)
      {
         REQUIRE(((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f')));
      }
   }

   test_that("SHA-256 hex handles empty string")
   {
      std::string emptyMessage = "";
      std::string hexHash;

      REQUIRE_FALSE(core::system::crypto::sha256Hex(emptyMessage, &hexHash));
      REQUIRE(hexHash.size() == 64);

      // The SHA256 of an empty string is a known value
      // pragma: allowlist nextline secret
      std::string expectedEmpty = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
      REQUIRE(hexHash == expectedEmpty);
>>>>>>> main
   }

   test_that("RSA decryption handles PKCS#1 1.5")
   {
     auto key(loadTestPrivateKey());
     // pragma: allowlist nextline secret
     static const std::string cipherText = "fAq9H+a+K/6fdeVR5u78c0LIG/eihceVsCM9y0V5M8Ied0DkZIl/mnyNMio"  // pragma: allowlist secret
       "BV33aodQm5rDDG1O1kbMOeOgYkHTitQTR+zb6lge4fFBExuV6Ivhitspxkq0TiieyuK4hSMY1xKkNdHwTq+3Le0sTgqTQuA"  // pragma: allowlist secret
       "daR6KGPz8T9FPK0oyqk3KAKmpH6vOSzojA41iLIlnPQiCuz9crixJiD43LGBH0ja7z0jhOCWThLltU5PZj9i/OutQRAofsS"  // pragma: allowlist secret
       "wSrLdqrXpPufHIe003Pgg7mGyqkL/DMIqXlxKDmsfBOvWLygr8+4Ff2rdnmtR/tsx51Y0KHUPT9kb5CLFWHNMzyubrZRw=="; // pragma: allowlist secret
     static const std::string expected = "test";

     std::string plainText;
     Error error = core::system::crypto::rsaPrivateDecrypt(cipherText, &plainText, key.get());
     REQUIRE_FALSE(error);
     REQUIRE(plainText == expected);
   }

   test_that("RSA decryption handles OAEP")
   {
     auto key(loadTestPrivateKey());
     static const std::string cipherText = "$RSA-OAEP$UfkheH3D5+zcpbuUDDX/BIyQ1+EqasZLjsCIE4sIabRFZcd5T"  // pragma: allowlist secret
       "tgXXDw+B9aqZOWArOtey8YnO1i7yal2BGFmXhgb2k43HeoQenEf2pYnbqps26YBc267ZOTvUqL2OjuccBTR/eeQwNxdfldD"  // pragma: allowlist secret
       "yEY8h0g0mOOgc+nbH2TWhPXRf3aF3YA+j4nIsshpppY3CgBfuCGKUcIs9qaxYW5A0ycJCkmxVPCm/xeCEYwkppE1Ntdk0bL"  // pragma: allowlist secret
       "NHW0tD3N/Kb449VbH9rGigV3A+AXEtExcWyTEvU3y1y9cDRtEXP6ygwxCCptPcR4AMQmuWtzQXlY+mIix3XtXZEjWYUvOE7"  // pragma: allowlist secret
       "W8VUb12A==";  // pragma: allowlist secret
     static const std::string expected = "test";

     std::string plainText;
     Error error = core::system::crypto::rsaPrivateDecrypt(cipherText, &plainText, key.get());
     REQUIRE_FALSE(error);
     REQUIRE(plainText == expected);
   }
}

TEST(CryptoTest, Sha256HexEmptyString)
{
   std::string emptyMessage = "";
   std::string hexHash;

   ASSERT_FALSE(core::system::crypto::sha256Hex(emptyMessage, &hexHash));
   EXPECT_EQ(64u, hexHash.size());

   // The SHA256 of an empty string is a known value
   std::string expectedEmpty = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";  // pragma: allowlist secret
   ASSERT_EQ(hexHash, expectedEmpty);
}

TEST(CryptoTest, RsaDecryptionPKCS1_5)
{
   auto key(loadTestPrivateKey());
   // pragma: allowlist nextline secret
   static const std::string cipherText = "fAq9H+a+K/6fdeVR5u78c0LIG/eihceVsCM9y0V5M8Ied0DkZIl/mnyNMio"  // pragma: allowlist secret
      "BV33aodQm5rDDG1O1kbMOeOgYkHTitQTR+zb6lge4fFBExuV6Ivhitspxkq0TiieyuK4hSMY1xKNdHwTq+3Le0sTgqTQuA"  // pragma: allowlist secret
      "daR6KGPz8T9FPK0oyqk3KAKmpH6vOSzojA41iLIlnPQiCuz9crixJiD43LGBH0ja7z0jhOCWThLltU5PZj9i/OutQRAofsS"  // pragma: allowlist secret
      "wSrLdqrXpPufHIe003Pgg7mGyqkL/DMIqXlxKDmsfBOvWLygr8+4Ff2rdnmtR/tsx51Y0KHUPT9kb5CLFWHNMzyubrZRw=="; // pragma: allowlist secret
   static const std::string expected = "test";

   std::string plainText;
   Error error = core::system::crypto::rsaPrivateDecrypt(cipherText, &plainText, key.get());
   ASSERT_FALSE(error);
   ASSERT_EQ(plainText, expected);
}

TEST(CryptoTest, RsaDecryptionOAEP)
{
   auto key(loadTestPrivateKey());
   static const std::string cipherText = "$RSA-OAEP$UfkheH3D5+zcpbuUDDX/BIyQ1+EqasZLjsCIE4sIabRFZcd5T"  // pragma: allowlist secret
      "tgXXDw+B9aqZOWArOtey8YnO1i7yal2BGFmXhgb2k43HeoQenEf2pYnbqps26YBc267ZOTvUqL2OjuccBTR/eeQwNxdfldD"  // pragma: allowlist secret
      "yEY8h0g0mOOgc+nbH2TWhPXRf3aF3YA+j4nIsshpppY3CgBfuCGKUcIs9qaxYW5A0ycJCkmxVPCm/xeCEYwkppE1Ntdk0bL"  // pragma: allowlist secret
      "NHW0tD3N/Kb449VbH9rGigV3A+AXEtExcWyTEvU3y1y9cDRtEXP6ygwxCCptPcR4AMQmuWtzQXlY+mIix3XtXZEjWYUvOE7"  // pragma: allowlist secret
      "W8VUb12A==";  // pragma: allowlist secret
   static const std::string expected = "test";

   std::string plainText;
   Error error = core::system::crypto::rsaPrivateDecrypt(cipherText, &plainText, key.get());
   ASSERT_FALSE(error);
   ASSERT_EQ(plainText, expected);
}

} // namespace tests
} // namespace system
} // namespace core
} // namespace rstudio

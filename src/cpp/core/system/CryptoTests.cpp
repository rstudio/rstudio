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
   ASSERT_EQ(0u, priv.rfind("-----BEGIN PRIVATE KEY-----", 0));
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
   std::string expectedHex = "bb0b57005f01018b19c278c55273a60118ffdd3e5790ccc8a48cad03907fa521";
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
      ASSERT_TRUE((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'));
   }
}

TEST(CryptoTest, Sha256HexEmptyString)
{
   std::string emptyMessage = "";
   std::string hexHash;
   
   ASSERT_FALSE(core::system::crypto::sha256Hex(emptyMessage, &hexHash));
   EXPECT_EQ(64u, hexHash.size());
   
   // The SHA256 of an empty string is a known value
   std::string expectedEmpty = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
   ASSERT_EQ(hexHash, expectedEmpty);
}

} // namespace tests
} // namespace system
} // namespace core
} // namespace rstudio

/*
 * EncryptionVersionTests.cpp
 *
 * Copyright (C) 2024 by Posit Software, PBC
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

#include <shared_core/system/Crypto.hpp>
#include <shared_core/system/encryption/EncryptionConfiguration.hpp>
#include <shared_core/system/encryption/EncryptionVersion.hpp>

#include <tests/TestThat.hpp>

#include <fcntl.h>

#include <openssl/err.h>
#include <openssl/evp.h>
#include <openssl/rand.h>

#include <shared_core/Error.hpp>

namespace rstudio {
namespace core {
namespace system {
namespace tests {

using namespace core;

std::vector<unsigned char> g_key;
std::vector<unsigned char> g_iv;
std::string g_payload = "Hello, world! This is a secret.";
std::vector<unsigned char> g_data;

// Simplified random function mirrored after core::system::crypto::random()
bool random(uint32_t in_length, std::vector<unsigned char>& out_randomData)
{
   out_randomData.resize(in_length);

   if (!RAND_bytes(&out_randomData[0], in_length))
      return false;

   return true;
}

bool generateKeys(unsigned char version)
{
   crypto::setMinimumEncryptionVersion(0);
   crypto::setMaximumEncryptionVersion(2);

   // construct the data to encrypt in tests
   if (g_data.empty())
      std::copy(g_payload.begin(), g_payload.end(), std::back_inserter(g_data));

   if (version == crypto::v1::VERSION_BYTE || version == 0)
      // generate a random 128-bit key and IV
      return random(16, g_key) && random(16, g_iv);
   else if (version == crypto::v2::VERSION_BYTE)
      // generate a random 256-bit key and 128-bit IV
      return random(32, g_key) && random(16, g_iv);

   return false;
}

bool decryptedPayloadMatches(std::vector<unsigned char>& decryptedData)
{
   std::string decryptedPayload;
   std::copy(decryptedData.begin(), decryptedData.end(), std::back_inserter(decryptedPayload));
   return g_payload == decryptedPayload;
}

test_context("EncryptionVersionTests")
{
   test_that("v0: Can AES encrypt/decrypt")
   {
      // setup
      REQUIRE(generateKeys(crypto::v0::VERSION_BYTE));

      // encrypt the data
      std::vector<unsigned char> encryptedData;
      Error error = core::system::crypto::v0::aesEncrypt(g_data, g_key, g_iv, encryptedData);
      REQUIRE_FALSE(error);

      // decrypt the encrypted data
      std::vector<unsigned char> decryptedData;
      error = core::system::crypto::v0::aesDecrypt(encryptedData, g_key, g_iv, decryptedData);
      REQUIRE_FALSE(error);

      // verify that the decryption gives us back the original data
      REQUIRE(decryptedPayloadMatches(decryptedData));
   }

   test_that("v1: Can AES encrypt/decrypt")
   {
      // setup
      REQUIRE(generateKeys(crypto::v1::VERSION_BYTE));

      // encrypt the data
      std::vector<unsigned char> encryptedData;
      Error error = core::system::crypto::v1::aesEncrypt(g_data, g_key, g_iv, encryptedData);
      REQUIRE_FALSE(error);

      // decrypt the encrypted data
      std::vector<unsigned char> decryptedData;
      error = core::system::crypto::v1::aesDecrypt(encryptedData, g_key, g_iv, decryptedData);
      REQUIRE_FALSE(error);

      // verify that the decryption gives us back the original data
      REQUIRE(decryptedPayloadMatches(decryptedData));
   }

   test_that("v2: Can AES encrypt/decrypt")
   {
      // setup
      REQUIRE(generateKeys(crypto::v2::VERSION_BYTE));

      // encrypt the data
      std::vector<unsigned char> encryptedData;
      Error error = core::system::crypto::v2::aesEncrypt(g_data, g_key, g_iv, encryptedData);
      REQUIRE_FALSE(error);

      // decrypt the encrypted data
      std::vector<unsigned char> decryptedData;
      error = core::system::crypto::v2::aesDecrypt(encryptedData, g_key, g_iv, decryptedData);
      REQUIRE_FALSE(error);

      // verify that the decryption gives us back the original data
      REQUIRE(decryptedPayloadMatches(decryptedData));
   }

   test_that("v2: Can AES encrypt/decrypt detect tampering/corruption")
   {
      // setup
      REQUIRE(generateKeys(crypto::v2::VERSION_BYTE));

      // encrypt the data
      std::vector<unsigned char> encryptedData;
      Error error = core::system::crypto::v2::aesEncrypt(g_data, g_key, g_iv, encryptedData);
      REQUIRE_FALSE(error);

      // decrypt the encrypted data with wrong AAD
      std::vector<unsigned char> decryptedData;
      std::vector<unsigned char> bad_aad = encryptedData;
      bad_aad[crypto::VERSION_BYTE_INDEX] += 1;
      error = core::system::crypto::v2::aesDecrypt(bad_aad, g_key, g_iv, decryptedData);
      REQUIRE(decryptedData.size() == 0);
      REQUIRE(error != Success());

      // decrypt the encrypted data with changed data
      std::vector<unsigned char> bad_encryptedData = encryptedData;
      bad_encryptedData[crypto::ENCRYPTION_VERSION_SIZE_BYTES] += 1;
      error = core::system::crypto::v2::aesDecrypt(bad_encryptedData, g_key, g_iv, decryptedData);
      REQUIRE(decryptedData.size() == 0);
      REQUIRE(error != Success());

      // decrypt the encrypted data with wrong MAC
      std::vector<unsigned char> bad_mac = encryptedData;
      bad_mac.back() += 1;
      error = core::system::crypto::v2::aesDecrypt(bad_mac, g_key, g_iv, decryptedData);
      REQUIRE(decryptedData.size() == 0);
      REQUIRE(error != Success());

      // Finally, decrypt the encrypted data correctly
      error = core::system::crypto::v2::aesDecrypt(encryptedData, g_key, g_iv, decryptedData);
      REQUIRE(decryptedData.size() > 0);
      REQUIRE_FALSE(error);

      // verify that the decryption gives us back the original data
      REQUIRE(decryptedPayloadMatches(decryptedData));
   }

   test_that("Key size mismatches aren't allowed")
   {
      std::vector<unsigned char> iv(16);
      std::vector<unsigned char> encryptedData;

      // Encrypting v0/v1 with too small key size throws
      std::vector<unsigned char> key = {};
      REQUIRE_THROWS(core::system::crypto::v0::aesEncrypt(g_data, key, iv, encryptedData));
      REQUIRE_THROWS(core::system::crypto::v1::aesEncrypt(g_data, key, iv, encryptedData));

      // Encrypting v2 with v1 key size throws
      key.resize(crypto::v1::KEY_LENGTH_BYTES);
      REQUIRE_THROWS(core::system::crypto::v2::aesEncrypt(g_data, key, iv, encryptedData));
   }
}

test_context("Versioned Crypto Calls")
{
   test_that("Crypto can decrypt v0 data")
   {
      // setup
      REQUIRE(generateKeys(crypto::v0::VERSION_BYTE));

      // encrypt the data
      std::vector<unsigned char> encryptedData;
      Error error = core::system::crypto::v0::aesEncrypt(g_data, g_key, g_iv, encryptedData);
      REQUIRE_FALSE(error);

      // decrypt the encrypted data
      std::vector<unsigned char> decryptedData;
      error = core::system::crypto::aesDecrypt(encryptedData, g_key, g_iv, decryptedData);
      REQUIRE_FALSE(error);

      // verify that the decryption gives us back the original data
      REQUIRE(decryptedPayloadMatches(decryptedData));
   }

   test_that("Crypto can decrypt v1 data")
   {
      // setup
      REQUIRE(generateKeys(crypto::v1::VERSION_BYTE));

      // encrypt the data
      std::vector<unsigned char> encryptedData;
      Error error = core::system::crypto::v1::aesEncrypt(g_data, g_key, g_iv, encryptedData);
      REQUIRE_FALSE(error);

      // decrypt the encrypted data
      std::vector<unsigned char> decryptedData;
      error = core::system::crypto::aesDecrypt(encryptedData, g_key, g_iv, decryptedData);
      REQUIRE_FALSE(error);

      // verify that the decryption gives us back the original data
      REQUIRE(decryptedPayloadMatches(decryptedData));
   }

   test_that("Crypto can decrypt v2 data")
   {
      // setup
      REQUIRE(generateKeys(crypto::v2::VERSION_BYTE));

      // encrypt the data
      std::vector<unsigned char> encryptedData;
      Error error = core::system::crypto::v2::aesEncrypt(g_data, g_key, g_iv, encryptedData);
      REQUIRE_FALSE(error);

      // decrypt the encrypted data
      std::vector<unsigned char> decryptedData;
      error = core::system::crypto::aesDecrypt(encryptedData, g_key, g_iv, decryptedData);
      REQUIRE_FALSE(error);

      // verify that the decryption gives us back the original data
      REQUIRE(decryptedPayloadMatches(decryptedData));
   }

   test_that("Crypto can decrypt v0 data with v1 version byte")
   {
      // setup
      REQUIRE(generateKeys(crypto::v0::VERSION_BYTE));

      // Use a key/iv combo that will generate a v0 encrypted buffer that starts with a v1 version byte
      std::vector<unsigned char> encryptedData;
      std::vector<unsigned char> key = {0x95, 0x34, 0xef, 0xab, 0x94, 0xbb, 0xb2, 0xf2, 0x49, 0x3a, 0xbc, 0xe3, 0x69, 0x71, 0x59, 0x06};
      std::vector<unsigned char> iv = {0xbc, 0x8f, 0x17, 0x56, 0x50, 0xca, 0xeb, 0x4a, 0xcb, 0xcb, 0x63, 0x53, 0xff, 0xba, 0x58, 0x51};
      Error error = core::system::crypto::v0::aesEncrypt(g_data, key, iv, encryptedData);
      REQUIRE_FALSE(error);

      REQUIRE(encryptedData[0] == crypto::v1::VERSION_BYTE);

      // decrypt the encrypted data
      std::vector<unsigned char> decryptedData;
      error = core::system::crypto::aesDecrypt(encryptedData, key, iv, decryptedData);
      REQUIRE_FALSE(error);

      // verify that the decryption gives us back the original data
      REQUIRE(decryptedPayloadMatches(decryptedData));
   }

   test_that("Crypto can decrypt v0 data with v2 version byte")
   {
      // setup
      REQUIRE(generateKeys(crypto::v0::VERSION_BYTE));

      // Use a key/iv combo that will generate a v0 encrypted buffer that starts with a v2 version byte
      std::vector<unsigned char> encryptedData;
      std::vector<unsigned char> key = {0x78, 0x84, 0x9b, 0x4c, 0x27, 0x6a, 0x07, 0x17, 0xb1, 0xbb, 0x1d, 0xd0, 0x9e, 0xc5, 0x39, 0x55};
      std::vector<unsigned char> iv = {0x22, 0xaf, 0xca, 0x38, 0x2d, 0xeb, 0xf8, 0xaa, 0xa0, 0xfb, 0x97, 0x40, 0xaa, 0xbb, 0x97, 0x13};
      Error error = core::system::crypto::v0::aesEncrypt(g_data, key, iv, encryptedData);
      REQUIRE_FALSE(error);

      REQUIRE(encryptedData[0] == crypto::v2::VERSION_BYTE);

      // decrypt the encrypted data
      std::vector<unsigned char> decryptedData;
      error = core::system::crypto::aesDecrypt(encryptedData, key, iv, decryptedData);
      REQUIRE_FALSE(error);

      // verify that the decryption gives us back the original data
      REQUIRE(decryptedPayloadMatches(decryptedData));
   }
}

} // end namespace tests
} // end namespace system
} // end namespace core
} // end namespace rstudio

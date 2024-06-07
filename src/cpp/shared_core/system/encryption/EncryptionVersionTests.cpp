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

#include <gsl/gsl>

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

bool generateKeys()
{
   // construct the data to encrypt in tests
   if (g_data.empty())
      std::copy(g_payload.begin(), g_payload.end(), std::back_inserter(g_data));

   // generate a random 128-bit key and IV
   return random(16, g_key) && random(16, g_iv);
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
      REQUIRE(generateKeys());

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
      REQUIRE(generateKeys());

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

   test_that("Are v0/v1 AES encrypt/decrypt enterchangable")
   {
      // setup
      REQUIRE(generateKeys());

      // encrypt with v0, decrypt with v1
      std::vector<unsigned char> encryptedData;
      Error error = core::system::crypto::v0::aesEncrypt(g_data, g_key, g_iv, encryptedData);
      REQUIRE_FALSE(error);
      std::vector<unsigned char> decryptedData;
      error = core::system::crypto::v1::aesDecrypt(encryptedData, g_key, g_iv, decryptedData);
      REQUIRE_FALSE(error);
      REQUIRE(decryptedPayloadMatches(decryptedData));

      // re-setup
      REQUIRE(generateKeys());
      encryptedData = {};
      decryptedData = {};

      // encrypt with v1, decrypt with v0
      error = core::system::crypto::v1::aesEncrypt(g_data, g_key, g_iv, encryptedData);
      REQUIRE_FALSE(error);
      error = core::system::crypto::v0::aesDecrypt(encryptedData, g_key, g_iv, decryptedData);
      REQUIRE_FALSE(error);
      REQUIRE(decryptedPayloadMatches(decryptedData));
   }

   test_that("v2: Can AES encrypt/decrypt")
   {
      // setup
      REQUIRE(generateKeys());

      // encrypt the data
      std::vector<unsigned char> encryptedData;
      std::vector<unsigned char> aad = {2};
      std::vector<unsigned char> mac;
      Error error = core::system::crypto::v2::aesEncrypt(g_data, g_key, g_iv, aad, mac, encryptedData);
      REQUIRE_FALSE(error);

      // decrypt the encrypted data
      std::vector<unsigned char> decryptedData;
      error = core::system::crypto::v2::aesDecrypt(encryptedData, g_key, g_iv, aad, mac, decryptedData);
      REQUIRE_FALSE(error);

      // verify that the decryption gives us back the original data
      REQUIRE(decryptedPayloadMatches(decryptedData));
   }

   test_that("v2: Can AES encrypt/decrypt detect tampering/corruption")
   {
      // setup
      REQUIRE(generateKeys());

      // encrypt the data
      std::vector<unsigned char> encryptedData;
      std::vector<unsigned char> aad = {2};
      std::vector<unsigned char> mac;
      Error error = core::system::crypto::v2::aesEncrypt(g_data, g_key, g_iv, aad, mac, encryptedData);
      REQUIRE_FALSE(error);

      // decrypt the encrypted data with wrong AAD
      std::vector<unsigned char> decryptedData;
      std::vector<unsigned char> bad_aad = {0};
      error = core::system::crypto::v2::aesDecrypt(encryptedData, g_key, g_iv, bad_aad, mac, decryptedData);
      REQUIRE(decryptedData.size() == 0);
      REQUIRE(error != Success());

      // decrypt the encrypted data with changed data

      std::vector<unsigned char> bad_encryptedData = encryptedData;
      bad_encryptedData[0] += 1;
      error = core::system::crypto::v2::aesDecrypt(bad_encryptedData, g_key, g_iv, aad, mac, decryptedData);
      REQUIRE(decryptedData.size() == 0);
      REQUIRE(error != Success());

      // decrypt the encrypted data with wrong MAC
      std::vector<unsigned char> bad_mac(16);
      error = core::system::crypto::v2::aesDecrypt(encryptedData, g_key, g_iv, aad, bad_mac, decryptedData);
      REQUIRE(decryptedData.size() == 0);
      REQUIRE(error != Success());

      // Finally, decrypt the encrypted data correctly
      error = core::system::crypto::v2::aesDecrypt(encryptedData, g_key, g_iv, aad, mac, decryptedData);
      REQUIRE(decryptedData.size() > 0);
      REQUIRE_FALSE(error);

      // verify that the decryption gives us back the original data
      REQUIRE(decryptedPayloadMatches(decryptedData));
   }
}

} // end namespace tests
} // end namespace system
} // end namespace core
} // end namespace rstudio

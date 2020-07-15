/*
 * CryptoTests.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <iterator>

#include <core/system/Crypto.hpp>

#include <tests/TestThat.hpp>

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
}

} // end namespace tests
} // end namespace system
} // end namespace core
} // end namespace rstudio

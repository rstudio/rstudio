/*
 * CryptoTests.cpp
 *
 * Copyright (C) 2022 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant to the terms of a commercial license agreement
 * with RStudio, then this program is licensed to you under the following terms:
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

#include "shared_core/Error.hpp"
#include <gtest/gtest.h>

#include <shared_core/system/Crypto.hpp>

namespace rstudio {
namespace core {
namespace system {
namespace crypto {

TEST(SharedCoreTest, CryptoTests)
{
   // Detect embedded encryption key hash section
   {
      // valid
      EXPECT_TRUE(passwordContainsKeyHash("1234ABCDtheencryptedpassword1234ABCD"));

      // prefix and suffix don't match
      EXPECT_FALSE(passwordContainsKeyHash("1234ABCDtheencryptedpassword5678ABCD"));

      // nothing but key...
      EXPECT_FALSE(passwordContainsKeyHash("1234ABCD1234ABCD"));

      // plain text
      EXPECT_FALSE(passwordContainsKeyHash("MyPlainTextPassword!!"));
   }

   // Parse embedded encryption key hash section
   {
      auto result = splitPasswordKeyHash("1234ABCDtheencryptedpassword1234ABCD");
      EXPECT_EQ("1234ABCD", result.first);
      EXPECT_EQ("theencryptedpassword", result.second);
   }

   // Parse password without embedded key hash section
   {
      auto result = splitPasswordKeyHash("MyPlainTextPassword!!");
      EXPECT_TRUE(result.first.empty());
      EXPECT_EQ("MyPlainTextPassword!!", result.second);
   }

   // Base64 Encode/Decode section
   {
      // Encode
      // An empty string encoded is also empty
      std::vector<unsigned char> input = {};
      std::string result = "";
      base64Encode(input, result);
      EXPECT_EQ(input.size(), input.size());
      EXPECT_EQ("", result);

      // Output parameter should be empty when passed with an empty input vector
      // even if it's passed in while non-empty
      result = "hello";
      base64Encode(input, result);
      EXPECT_EQ(input.size(), input.size());
      EXPECT_EQ("", result);

      input.clear();
      std::string inStr = "t";
      std::copy(inStr.begin(), inStr.end(), std::back_inserter(input));
      base64Encode(input, result);
      EXPECT_EQ("dA", result);

      input.clear();
      inStr = "test";
      std::copy(inStr.begin(), inStr.end(), std::back_inserter(input));
      base64Encode(input, result);
      EXPECT_EQ("dGVzdA==", result);

      input.clear();
      inStr = "test string";
      std::copy(inStr.begin(), inStr.end(), std::back_inserter(input));
      base64Encode(input, result);
      EXPECT_EQ("dGVzdCBzdHJpbmc=", result);

      // Decode
      // An empty string decoded is also empty
      result = "";
      base64Decode("", result);
      EXPECT_EQ("", result);

      result = "";
      base64Decode("Zg==", result);
      EXPECT_EQ("f", result);

      result = "";
      base64Decode("Zm9vYmFy", result);
      EXPECT_EQ("foobar", result);

      result = "";
      base64Decode("aGVsbG8gd29ybGQ=", result);
      EXPECT_EQ("hello world", result);
   }
}

} // namespace crypto
} // namespace system
} // namespace core
} // namespace rstudio

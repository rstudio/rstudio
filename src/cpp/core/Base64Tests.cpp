/*
 * Base64Tests.cpp
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

#include <gtest/gtest.h>

#include <shared_core/Error.hpp>
#include <core/Base64.hpp>
#include <core/StringUtils.hpp>

namespace rstudio {
namespace core {
namespace base64 {

TEST(Base64Test, VariousSmallStringsEncodeCorrectly)
{
   std::string encoded;
   Error err;
   err = encode("a", &encoded);
   EXPECT_FALSE(err);
   EXPECT_EQ("YQ==", encoded);
   
   err = encode("ab", &encoded);
   EXPECT_FALSE(err);
   EXPECT_EQ("YWI=", encoded);
   
   err = encode("abc", &encoded);
   EXPECT_FALSE(err);
   EXPECT_EQ("YWJj", encoded);
   
   err = encode("abcd", &encoded);
   EXPECT_FALSE(err);
   EXPECT_EQ("YWJjZA==", encoded);
   
   err = encode("abcde", &encoded);
   EXPECT_FALSE(err);
   EXPECT_EQ("YWJjZGU=", encoded);
   
   err = encode("abcdef", &encoded);
   EXPECT_FALSE(err);
   EXPECT_EQ("YWJjZGVm", encoded);
}

TEST(Base64Test, VariousSmallStringsDecodeCorrectly)
{
   std::string decoded;
   Error err;
   err = decode("YQ==", &decoded);
   EXPECT_FALSE(err);
   EXPECT_EQ("a", decoded);
   
   err = decode("YWI=", &decoded);
   EXPECT_FALSE(err);
   EXPECT_EQ("ab", decoded);
   
   err = decode("YWJj", &decoded);
   EXPECT_FALSE(err);
   EXPECT_EQ("abc", decoded);
   
   err = decode("YWJjZA==", &decoded);
   EXPECT_FALSE(err);
   EXPECT_EQ("abcd", decoded);
   
   err = decode("YWJjZGU=", &decoded);
   EXPECT_FALSE(err);
   EXPECT_EQ("abcde", decoded);
   
   err = decode("YWJjZGVm", &decoded);
   EXPECT_FALSE(err);
   EXPECT_EQ("abcdef", decoded);
}

TEST(Base64Test, ContentsArePreservedInEncodeDecodeProcess)
{
   Error error;
   ::srand(1);
   for (std::size_t i = 0; i < 100; ++i)
   {
      std::string random =
            string_utils::makeRandomByteString(::rand() % 1024);
      
      std::string encoded;
      error = encode(random, &encoded);
      EXPECT_FALSE(error);
      
      std::string decoded;
      error = decode(encoded, &decoded);
      EXPECT_FALSE(error);
      
      EXPECT_EQ(random, decoded);
   }
}
} // end namespace base64
} // end namespace core
} // end namespace rstudio

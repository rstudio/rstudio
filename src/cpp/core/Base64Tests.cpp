/*
 * Base64Tests.cpp
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

#include <tests/TestThat.hpp>

#include <shared_core/Error.hpp>
#include <core/Base64.hpp>
#include <core/StringUtils.hpp>

namespace rstudio {
namespace core {
namespace base64 {

test_context("Base64 Encoding")
{
   std::string encoded;
   test_that("Various small strings encode correctly")
   {
      encode("a", &encoded);
      expect_true(encoded == "YQ==");
      
      encode("ab", &encoded);
      expect_true(encoded == "YWI=");
      
      encode("abc", &encoded);
      expect_true(encoded == "YWJj");
      
      encode("abcd", &encoded);
      expect_true(encoded == "YWJjZA==");
      
      encode("abcde", &encoded);
      expect_true(encoded == "YWJjZGU=");
      
      encode("abcdef", &encoded);
      expect_true(encoded == "YWJjZGVm");
   }
   
   std::string decoded;
   test_that("Various small strings decode correctly")
   {
      decode("YQ==", &decoded);
      expect_true(decoded == "a");
      
      decode("YWI=", &decoded);
      expect_true(decoded ==  "ab");
      
      decode("YWJj", &decoded);
      expect_true(decoded == "abc");
      
      decode("YWJjZA==", &decoded);
      expect_true(decoded == "abcd");
      
      decode("YWJjZGU=", &decoded);
      expect_true(decoded == "abcde");
      
      decode("YWJjZGVm", &decoded);
      expect_true(decoded == "abcdef");
   }
   
   test_that("Contents are preserved in encode / decode process")
   {
      Error error;
      ::srand(1);
      for (std::size_t i = 0; i < 100; ++i)
      {
         std::string random =
               string_utils::makeRandomByteString(::rand() % 1024);
         
         std::string encoded;
         error = encode(random, &encoded);
         expect_true(!error);
         
         std::string decoded;
         error = decode(encoded, &decoded);
         expect_true(!error);
         
         expect_true(random == decoded);
      }
   }
}

} // end namespace base64
} // end namespace core
} // end namespace rstudio

/*
 * StringUtils.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#include <core/StringUtils.hpp>

namespace rstudio {
namespace core {
namespace string_utils {

context("isSubsequence")
{
   test_that("isSubsequence works")
   {
      expect_true(isSubsequence("", ""));
      expect_true(isSubsequence("annnbnnnc", "abc"));
      expect_false(isSubsequence("abcdef", "abdcef"));
      expect_true(isSubsequence("abcdef", "AeF", true));
      expect_true(isSubsequence("a1d2", "12"));
   }
   
   test_that("strippedOfBackQuotes works")
   {
      std::string string = "`abc`";
      std::string stripped = strippedOfBackQuotes(string);
      expect_true(stripped == "abc");
      
      expect_true(strippedOfBackQuotes("abc") == "abc");
      
   }
   
   test_that("substring works")
   {
      std::string string("  abc  ");
      expect_true(substring(string, 2, 5) == "abc");
   }
   
   test_that("trimWhitespace works")
   {
      std::string string("   abc   ");
      expect_true(trimWhitespace(string) == "abc");
      expect_true(trimWhitespace("abc") == "abc");
      expect_true(trimWhitespace("") == "");
   }
}

} // end namespace string_utils
} // end namespace core
} // end namespace rstudio

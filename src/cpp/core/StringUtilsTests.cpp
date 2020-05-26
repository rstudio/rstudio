/*
 * StringUtils.cpp
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

#include <core/Algorithm.hpp>
#include <core/StringUtils.hpp>

#define kLatexStyleLineCommentRegex ("^%+\\s*")

namespace rstudio {
namespace core {
namespace string_utils {

using namespace algorithm;

test_context("String splitting")
{
   test_that("Strings can be split on NUL bytes")
   {
      std::string text("a\0b\0c", 5);
      std::string delim("\0", 1);
      std::vector<std::string> splat = split(text, delim);
      expect_true(splat.size() == 3);
      expect_true(splat[0] == "a");
      expect_true(splat[1] == "b");
      expect_true(splat[2] == "c");
   }
   
   test_that("raw delimiters containing null bytes can be split")
   {
      std::string text("a\0b\0c", 5);
      std::vector<std::string> splat = split(text, "\0");
      expect_true(splat.size() == 3);
      expect_true(splat[0] == "a");
      expect_true(splat[1] == "b");
      expect_true(splat[2] == "c");
   }
}

test_context("isSubsequence")
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

   test_that("countNewLines works with empty string")
   {
      std::string str("");
      expect_true(countNewlines(str) == 0);
   }

   test_that("countNewLines works with line feed")
   {
      std::string str("\n\n\n");
      expect_true(countNewlines(str) == 3);
   }

   test_that("countNewLines works with carriage return/line feed")
   {
      std::string str("\r\n\r\n");
      expect_true(countNewlines(str) == 2);
   }
}

test_context("Comment extraction")
{
   test_that("Comment headers can be extracted")
   {
      std::string text(
               "% This is a header.\n"
               "% Let's hope the text is extracted.\n"
               "\n"
               "This should be ignored.");
      
      std::string extracted;
      bool success = extractCommentHeader(text, kLatexStyleLineCommentRegex, &extracted);
      std::string expected(
               "This is a header.\n"
               "Let's hope the text is extracted.\n");
      
      expect_true(success);
      expect_true(extracted == expected);
   }
   
   test_that("Comment headers with no trailing newline are handled")
   {
      std::string text("% Hello\n% World");
      
      std::string extracted;
      bool success = extractCommentHeader(text, kLatexStyleLineCommentRegex, &extracted);
      std::string expected("Hello\nWorld\n");
      
      expect_true(success);
      expect_true(extracted == expected);
   }
   
   test_that("Edge cases are handled")
   {
      std::string extracted;
      expect_false(extractCommentHeader("", kLatexStyleLineCommentRegex, &extracted));
      
      std::string text(
               "There is a comment\n"
               "% but not at the start of the document.\n");
      expect_false(extractCommentHeader(text, kLatexStyleLineCommentRegex, &extracted));
   }

   test_that("JavaScript literals are escaped")
   {
      expect_true(jsLiteralEscape("\"hello\"") == "\\\"hello\\\"");
      expect_true(jsLiteralEscape("'goodbye'") == "\\'goodbye\\'");
      expect_true(jsLiteralEscape("</script>") == "\\074/script>");
   }
}

} // end namespace string_utils
} // end namespace core
} // end namespace rstudio

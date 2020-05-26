/*
 * AnsiCodeParserTests.cpp
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

#include <core/text/AnsiCodeParser.hpp>

#include <tests/TestThat.hpp>

namespace rstudio {
namespace core {
namespace text {
namespace tests {

test_context("Ansi Code Parsing")
{
   test_that("Ansi stripping doesn't modify plain text")
   {
      std::string expect("abcd12345\nderkjdf\r\n234");
      std::string noAnsi = expect;
      stripAnsiCodes(&noAnsi);

      expect_true(expect == noAnsi);
   }

   test_that("Ansi stripping gets rid of Ansi escapes")
   {
      std::string hasAnsi("abc\x1b[31mHello\x1b[39m World\nBye.");
      std::string expect("abcHello World\nBye.");
      stripAnsiCodes(&hasAnsi);

      expect_true(expect == hasAnsi);
   }
}

} // end namespace tests
} // end namespace text
} // end namespace core
} // end namespace rstudio

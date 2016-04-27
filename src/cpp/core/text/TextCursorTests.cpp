/*
 * TextCursorTests.cpp
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

#include <vector>
#include <string>
#include <iostream>

#include <core/text/TextCursor.hpp>

namespace rstudio {
namespace core {
namespace text {
namespace unit_tests {

context("TextCursor")
{
   test_that("various search commands function correctly")
   {
      std::string contents("hello (world)");
      TextCursor cursor(contents);
      
      expect_true(cursor.findFwd('('));
      expect_true(cursor.offset() == 6);
      expect_true(cursor.findFwd(')'));
      expect_true(cursor.offset() == 12);
   }
   
   test_that("peek() functions don't cause OOB read")
   {
      std::string contents("hello");
      TextCursor cursor(contents);
      
      expect_true(cursor.peekFwd(1000) == '\0');
      expect_true(cursor.peekBwd(1000) == '\0');
   }
   
   test_that("TextCursor can be used to extract escaped quoted fields")
   {
      std::string contents("foo=\"bar + \\\"baz\"");
      TextCursor cursor(contents);
      
      expect_true(cursor.findFwd('"'));
      cursor.advance();
      const char* begin = cursor;
      
      expect_true(cursor.findFwd('"'));
      expect_true(cursor.peekBwd() == '\\');
      cursor.advance();
      
      expect_true(cursor.findFwd('"'));
      const char* end = cursor;
      
      std::string quoted(begin, end);
      expect_true(quoted == "bar + \\\"baz");
   }
}

} // namespace unit_tests
} // namespace text
} // namespace core
} // namespace rstudio

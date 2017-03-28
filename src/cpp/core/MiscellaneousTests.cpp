/*
 * MiscellaneousTests.cpp
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

#include <core/Algorithm.hpp>
#include <core/collection/Position.hpp>
#include <core/RegexUtils.hpp>

namespace rstudio {
namespace core {
namespace unit_tests {

using namespace core::collection;

context("Position")
{
   test_that("Positions are compared correctly")
   {
      expect_true(Position(0, 0) == Position(0, 0));
      expect_true(Position(0, 0) <  Position(0, 1));
      expect_true(Position(0, 0) <  Position(1, 0));
      expect_true(Position(2, 2) <  Position(2, 4));
   }
   
}

context("Splitting")
{
   test_that("core::algorithm::split handles multi-character delimiters")
   {
      std::string contents = "foo::bar::baz";
      std::vector<std::string> splat = core::algorithm::split(contents, "::");
      expect_true(splat.size() == 3);
      if (splat.size() == 3)
      {
         expect_true(splat[0] == "foo");
         expect_true(splat[1] == "bar");
         expect_true(splat[2] == "baz");
      }
   }
}

context("Regular Expressions")
{
   test_that("regex_replace() successfully escapes special characters")
   {
      expect_equal(
               regex_utils::regexEscape("A.B"),
               "A\\.B");
      
      expect_equal(
               regex_utils::regexEscape("X[1]"),
               "X\\[1\\]");
      
      expect_equal(
               regex_utils::regexEscape(".+?"),
               "\\.\\+\\?");
   }
}

} // namespace unit_tests
} // namespace core
} // namespace rstudio

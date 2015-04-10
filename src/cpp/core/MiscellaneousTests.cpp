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

#include <iostream>

#include <core/collection/Position.hpp>

namespace rstudio {
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

} // namespace unit_tests
} // namespace rstudio

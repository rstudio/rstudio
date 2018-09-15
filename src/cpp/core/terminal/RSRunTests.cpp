/*
 * RSRunTests.cpp
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
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

#include <core/terminal/RSRun.hpp>

#include <tests/TestThat.hpp>

#include <core/BoostThread.hpp>

namespace rstudio {
namespace core {
namespace terminal {
namespace tests {

namespace {

} // anonymous namespace

context("RSRun Terminal Handling")
{
   test_that("tests compile")
   {
      RSRun rsrun;

      expect_true(true);
   }
   
   // TODO (gary) more unit tests for RSRun class
}

} // end namespace tests
} // end namespace terminal
} // end namespace core
} // end namespace rstudio

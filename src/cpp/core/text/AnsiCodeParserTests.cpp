/*
 * AnsiCodeParserTests.cpp
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

context("Ansi Code Parsing")
{
   test_that("placeholder compiles")
   {
      expect_true(ansiFuncPlaceholder());
   }
}

} // end namespace tests
} // end namespace text
} // end namespace core
} // end namespace rstudio

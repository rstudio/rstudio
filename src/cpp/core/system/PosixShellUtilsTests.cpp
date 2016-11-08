/*
 * PosixShellUtilsTests.cpp
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

#ifndef _WIN32

#include <tests/TestThat.hpp>

#include <iostream>

#include <core/Error.hpp>

#include <core/system/ShellUtils.hpp>

namespace rstudio {
namespace core {
namespace shell_utils {
namespace tests {

context("Shell Escaping")
{
   test_that("Commands with special characters are escaped")
   {
      std::string dollars = "$$$";
      std::string escaped  = escape(dollars);
      std::string expected = "\"\\$\\$\\$\"";
      expect_true(escaped == expected);
   }
   
   test_that("Commands with backslashes are escaped")
   {
      std::string backslashes = "\\\\\\";
      std::string escaped = escape(backslashes);
      std::string expected = "\"\\\\\\\\\\\\\"";
      expect_true(escaped == expected);
   }
}

} // end namespace tests
} // end namespace shell_utils
} // end namespace core
} // end namespace rstudio

#endif // _WIN32

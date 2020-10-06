/*
 * SessionRTests.cpp
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

#include <r/RExec.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace tests {

test_context("R")
{
   test_that("RFunction execution errors don't set result to nullptr")
   {
      SEXP result = R_NilValue;
      r::sexp::Protect protect;
      
      Error error = r::exec::RFunction("NoSuchFunction")
            .call(&result, &protect);
      
      expect_true(error != Success());
      expect_true(result != nullptr);
      expect_true(result == R_NilValue);
   }
}

} // namespace tests
} // namespace session
} // namespace rstudio

/*
 * SessionRTests.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
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
   
   test_that("we can detect R active bindings")
   {
      // create an active binding
      Error error = r::exec::executeString(R"EOF({
         make_counter <- function() {
           count <- 0L
           function() {
             count <<- count + 1L
             count
           }
         }
         makeActiveBinding("counter", make_counter(), env = globalenv())
      })EOF");
      
      if (error)
      {
         LOG_ERROR(error);
         expect_true(false);
      }
      
      // check that it behaves the way we expect
      int v1 = 0;
      error = r::exec::evaluateString("counter", &v1);
      expect_equal(v1, 1);
      
      int v2 = 0;
      error = r::exec::evaluateString("counter", &v2);
      expect_equal(v2, 2);
      
      // check that regular bindings are not considered active
      expect_false(r::sexp::isActiveBinding("data.frame", R_GlobalEnv));
      
      // check that we can detect it
      expect_true(r::sexp::isActiveBinding("counter", R_GlobalEnv));
     
      // check that undefined variables aren't an error
      expect_false(r::sexp::isActiveBinding("noBindingWithThisNameExists", R_GlobalEnv));
      
      // remove it when we're done
      error = r::exec::executeString(R"EOF(
         rm(counter, make_counter)
      )EOF");
      expect_true(error == Success());
     
   }
}

} // namespace tests
} // namespace session
} // namespace rstudio

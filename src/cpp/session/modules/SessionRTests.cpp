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

#include <gtest/gtest.h>

#include <r/RExec.hpp>
#include <r/RSexp.hpp>
#include <r/RErrorCategory.hpp>
#include <r/RInterface.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace tests {

TEST(SessionRTest, RFunctionErrorsDontSetResultToNull) {
   SEXP result = R_NilValue;
   r::sexp::Protect protect;
   
   Error error = r::exec::RFunction("NoSuchFunction")
         .call(&result, &protect);
   
   EXPECT_NE(Success(), error);
   EXPECT_NE(nullptr, result);
   EXPECT_EQ(R_NilValue, result);
}

TEST(SessionRTest, RActiveBindingDetection) {
   // Create an active binding in R
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
   
   EXPECT_FALSE(error) << "Failed to create active binding";
   
   // Check that it behaves as expected
   int v1 = 0;
   error = r::exec::evaluateString("counter", &v1);
   EXPECT_FALSE(error);
   EXPECT_EQ(1, v1);
   
   int v2 = 0;
   error = r::exec::evaluateString("counter", &v2);
   EXPECT_FALSE(error);
   EXPECT_EQ(2, v2);
   
   // Check that regular bindings are not considered active
   EXPECT_FALSE(r::sexp::isActiveBinding("data.frame", R_GlobalEnv));
   
   // Check that we can detect it
   EXPECT_TRUE(r::sexp::isActiveBinding("counter", R_GlobalEnv));
  
   // Check that undefined variables aren't an error
   EXPECT_FALSE(r::sexp::isActiveBinding("noBindingWithThisNameExists", R_GlobalEnv));
   
   // Remove it when we're done
   error = r::exec::executeString(R"EOF(
      rm(counter, make_counter)
   )EOF");
   EXPECT_FALSE(error);
}

} // namespace tests
} // namespace session
} // namespace rstudio
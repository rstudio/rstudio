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

#include <memory>
#include <thread>

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

TEST(SessionRTest, RFunctionRefusesNonMainThread) {
   // the R runtime is single-threaded; RFunction must refuse to touch it
   // from other threads rather than race code running on the main thread

   // a full construct-and-call chain off the main thread errors cleanly
   Error error;
   std::thread([&error]()
   {
      error = r::exec::RFunction("identity")
            .addParam("x")
            .call();
   }).join();
   EXPECT_TRUE(error == r::errc::CodeExecutionError);

   // a function constructed off the main thread is poisoned, so it stays
   // unusable even when called from the main thread
   std::unique_ptr<r::exec::RFunction> pFunction;
   std::thread([&pFunction]()
   {
      pFunction.reset(new r::exec::RFunction("identity"));
   }).join();
   error = pFunction->call();
   EXPECT_TRUE(error == r::errc::CodeExecutionError);

   // the same holds for a function wrapping an already-resolved SEXP
   SEXP functionSEXP = R_NilValue;
   r::sexp::Protect protect;
   error = r::exec::evaluateString("base::identity", &functionSEXP, &protect);
   EXPECT_FALSE(error);

   std::unique_ptr<r::exec::RFunction> pSexpFunction;
   std::thread([&pSexpFunction, functionSEXP]()
   {
      pSexpFunction.reset(new r::exec::RFunction(functionSEXP));
   }).join();
   error = pSexpFunction->call();
   EXPECT_TRUE(error == r::errc::CodeExecutionError);

   // parameters added from a background thread are refused and poison the
   // function: a later main-thread call must fail rather than execute with
   // missing parameters (list() would otherwise succeed with no arguments)
   r::exec::RFunction listFunction("list");
   std::thread([&listFunction]()
   {
      listFunction.addParam("x");
      listFunction.addUtf8Param("y");
   }).join();
   error = listFunction.call();
   EXPECT_TRUE(error == r::errc::CodeExecutionError);

   // the same call made entirely on the main thread succeeds
   std::string result;
   error = r::exec::RFunction("identity")
         .addParam("x")
         .call(&result);
   EXPECT_FALSE(error);
   EXPECT_EQ("x", result);
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
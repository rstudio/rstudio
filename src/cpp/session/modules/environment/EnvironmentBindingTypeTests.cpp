/*
 * EnvironmentBindingTypeTests.cpp
 *
 * Copyright (C) 2026 by Posit Software, PBC
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

#include "EnvironmentUtils.hpp"

namespace rstudio {
namespace session {
namespace modules {
namespace environment {
namespace {

using r::sexp::BindingType;
using r::sexp::getBindingType;

// Bindings cannot be removed from the base environment, so this one uses a
// unique name and is left in place for the rest of the test run.
const char kActiveName[] = ".rsBindingTypeTestActive";
const char kUnboundName[] = ".rsBindingTypeTestUnbound";

} // anonymous namespace

// Base bindings live on the symbol itself rather than in a frame cell, so
// they take a different path through getBindingType on R < 4.6.
TEST(EnvironmentBindingType, ClassifiesBaseEnvironmentBindings)
{
   // The base frame is locked, so delayedAssign() cannot add a binding to
   // it; reuse .Last.value instead, as the environment listing tests do.
   // Active bindings can be added to base regardless of the frame lock.
   // Base package objects such as pi are lazy-load promises until first
   // use, so force one to cover the forced-promise case; .Machine is set
   // from C and is never a promise.
   core::Error error = r::exec::executeString(R"(
      assign(".rsBindingTypeTestLastValue", .Last.value, envir = globalenv())
      unlockBinding(".Last.value", baseenv())
      delayedAssign(".Last.value", 42L, assign.env = baseenv())
      makeActiveBinding(".rsBindingTypeTestActive", function() 42L, baseenv())
      invisible(pi)
   )");
   ASSERT_FALSE(error) << error.asString();

   for (SEXP env : { R_BaseEnv, R_BaseNamespace })
   {
      EXPECT_EQ(getBindingType(".Last.value", env), BindingType::Promise);
      EXPECT_EQ(getBindingType(kActiveName, env), BindingType::ActiveBinding);
      EXPECT_EQ(getBindingType("pi", env), BindingType::Normal);
      EXPECT_EQ(getBindingType(".Machine", env), BindingType::Normal);
      EXPECT_EQ(getBindingType(kUnboundName, env), BindingType::Unbound);
   }

   // the Environment pane's promise check follows the same path
   EXPECT_TRUE(isUnevaluatedPromise(".Last.value", R_BaseEnv));
   EXPECT_FALSE(isUnevaluatedPromise("pi", R_BaseEnv));
   EXPECT_FALSE(isUnevaluatedPromise(".Machine", R_BaseEnv));

   // none of the checks above may have forced the promise
   EXPECT_EQ(getBindingType(".Last.value", R_BaseEnv), BindingType::Promise);

   error = r::exec::executeString(R"(
      assign(".Last.value", .rsBindingTypeTestLastValue, envir = baseenv())
      lockBinding(".Last.value", baseenv())
      rm(".rsBindingTypeTestLastValue", envir = globalenv())
   )");
   EXPECT_FALSE(error) << error.asString();
}

} // namespace environment
} // namespace modules
} // namespace session
} // namespace rstudio

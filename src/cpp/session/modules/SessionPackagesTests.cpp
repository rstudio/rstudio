/*
 * SessionPackagesTests.cpp
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

#include <boost/bind/bind.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/json/Json.hpp>

#include "SessionPackages.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace packages {

// containsCallSyntax() is a cheap, deliberately loose pre-filter: it only asks
// "does this input contain a function call?", which gates the precise,
// namespace-resolving check in '.rs.isPackageManagementCall' (covered by
// test-packages.R). It does NOT decide whether a call mutates the library --
// e.g. 'remove(x)' and 'c(1, 2)' are accepted here but rejected there.

TEST(SessionPackagesTest, ContainsCallSyntax_DetectsCalls) {
   EXPECT_TRUE(containsCallSyntax("install.packages(\"dplyr\")"));
   EXPECT_TRUE(containsCallSyntax("remove(x)"));
   EXPECT_TRUE(containsCallSyntax("update(model)"));
   EXPECT_TRUE(containsCallSyntax("renv::update()"));
   EXPECT_TRUE(containsCallSyntax("devtools::install_github(\"r-lib/cli\")"));
   EXPECT_TRUE(containsCallSyntax("str_remove_all(x)"));
   EXPECT_TRUE(containsCallSyntax("c(1, 2, 3)"));
   EXPECT_TRUE(containsCallSyntax("library(dplyr)"));
   EXPECT_TRUE(containsCallSyntax("x <- update.packages()"));
}

TEST(SessionPackagesTest, ContainsCallSyntax_AllowsWhitespaceBeforeParen) {
   EXPECT_TRUE(containsCallSyntax("install.packages (\"dplyr\")"));
   EXPECT_TRUE(containsCallSyntax("install.packages\t(\"dplyr\")"));
}

TEST(SessionPackagesTest, ContainsCallSyntax_IgnoresCallFreeInput) {
   EXPECT_FALSE(containsCallSyntax(""));
   EXPECT_FALSE(containsCallSyntax("# install foo"));
   EXPECT_FALSE(containsCallSyntax("# TODO: remove"));
   EXPECT_FALSE(containsCallSyntax("x <- 42"));
   EXPECT_FALSE(containsCallSyntax("install <- 42"));
   EXPECT_FALSE(containsCallSyntax("x <- update"));
   EXPECT_FALSE(containsCallSyntax("1 + 2"));
   EXPECT_FALSE(containsCallSyntax("mtcars"));
}

// PackageStateBuilder serializes package-state builds that nest (a request
// arriving while the DESCRIPTION scan services polled events). The scan itself
// needs R, so these tests script it: each pass stamps its sequence number into
// the list, and a test can have a given pass re-enter the builder -- a nested
// request -- or fail.

class PackageStateBuilderTest : public ::testing::Test
{
protected:
   PackageStateBuilderTest()
      : builder_(boost::bind(&PackageStateBuilderTest::runPass, this, boost::placeholders::_1))
   {
   }

   Error runPass(json::Object* pJson)
   {
      int pass = ++passes_;
      (*pJson)["pass"] = pass;
      if (onPass_)
         return onPass_(pass);
      return Success();
   }

   static Error failure()
   {
      return systemError(boost::system::errc::io_error, ERROR_LOCATION);
   }

   int passes_ = 0;
   boost::function<Error(int)> onPass_;
   PackageStateBuilder builder_;
};

TEST_F(PackageStateBuilderTest, Build_ReturnsTheListWithoutAnEvent) {
   json::Object result;
   bool deliverEvent = true;
   Error error = builder_.build(&result, &deliverEvent);

   EXPECT_FALSE(error);
   EXPECT_EQ(1, passes_);
   EXPECT_EQ(1, result["pass"].getInt());
   EXPECT_FALSE(deliverEvent);
}

TEST_F(PackageStateBuilderTest, BuildForEvent_DeliversTheEvent) {
   json::Object result;
   bool deliverEvent = false;
   Error error = builder_.buildForEvent(&result, &deliverEvent);

   EXPECT_FALSE(error);
   EXPECT_EQ(1, passes_);
   EXPECT_EQ(1, result["pass"].getInt());
   EXPECT_TRUE(deliverEvent);
}

// The RPC is scanning when the PPM batch completion requests an event: the
// nested request doesn't scan, and the RPC runs again and delivers the event
// on its behalf, so event-only listeners still see the change.
TEST_F(PackageStateBuilderTest, NestedEventRequest_FoldsIntoTheOuterBuild) {
   json::Object nestedResult;
   bool nestedDeliverEvent = true;
   Error nestedError;
   onPass_ = [&](int pass) -> Error {
      if (pass == 1)
         nestedError = builder_.buildForEvent(&nestedResult, &nestedDeliverEvent);
      return Success();
   };

   json::Object result;
   bool deliverEvent = false;
   Error error = builder_.build(&result, &deliverEvent);

   EXPECT_FALSE(nestedError);
   EXPECT_FALSE(nestedDeliverEvent);
   EXPECT_FALSE(nestedResult.hasMember("pass"));

   EXPECT_FALSE(error);
   EXPECT_EQ(2, passes_);
   EXPECT_EQ(2, result["pass"].getInt());
   EXPECT_TRUE(deliverEvent);
}

// An event build is scanning when a get_package_state RPC is dequeued: the RPC
// needs its own result, so it scans, and the outer build runs again so that
// the event it delivers is no older than the RPC's list.
TEST_F(PackageStateBuilderTest, NestedResultRequest_RerunsTheOuterBuild) {
   json::Object nestedResult;
   bool nestedDeliverEvent = true;
   Error nestedError;
   onPass_ = [&](int pass) -> Error {
      if (pass == 1)
         nestedError = builder_.build(&nestedResult, &nestedDeliverEvent);
      return Success();
   };

   json::Object result;
   bool deliverEvent = false;
   Error error = builder_.buildForEvent(&result, &deliverEvent);

   EXPECT_FALSE(nestedError);
   EXPECT_FALSE(nestedDeliverEvent);
   EXPECT_EQ(2, nestedResult["pass"].getInt());

   EXPECT_FALSE(error);
   EXPECT_EQ(3, passes_);
   EXPECT_EQ(3, result["pass"].getInt());
   EXPECT_TRUE(deliverEvent);
}

TEST_F(PackageStateBuilderTest, NestedBuildFailure_DoesNotDropTheOuterList) {
   json::Object nestedResult;
   bool nestedDeliverEvent = false;
   Error nestedError;
   onPass_ = [&](int pass) -> Error {
      if (pass == 1)
         nestedError = builder_.build(&nestedResult, &nestedDeliverEvent);
      else if (pass == 2)
         return failure();
      return Success();
   };

   json::Object result;
   bool deliverEvent = false;
   Error error = builder_.buildForEvent(&result, &deliverEvent);

   EXPECT_TRUE(nestedError);
   EXPECT_FALSE(nestedDeliverEvent);

   EXPECT_FALSE(error);
   EXPECT_EQ(3, passes_);
   EXPECT_EQ(3, result["pass"].getInt());
   EXPECT_TRUE(deliverEvent);
}

// A failed build keeps the event it owed -- its own, or one folded into it --
// until the next successful build from any trigger delivers it. A failed RPC
// that owed nothing stays that way.
TEST_F(PackageStateBuilderTest, FailedBuild_KeepsTheEventOwedUntilTheNextSuccess) {
   onPass_ = [&](int pass) -> Error {
      return pass <= 2 ? failure() : Success();
   };

   json::Object result;
   bool deliverEvent = true;
   EXPECT_TRUE(builder_.buildForEvent(&result, &deliverEvent));
   EXPECT_FALSE(deliverEvent);
   EXPECT_TRUE(builder_.eventPending());

   // a later failed build keeps it owed
   EXPECT_TRUE(builder_.build(&result, &deliverEvent));
   EXPECT_FALSE(deliverEvent);
   EXPECT_TRUE(builder_.eventPending());

   // the next successful build, from any trigger, delivers it
   EXPECT_FALSE(builder_.build(&result, &deliverEvent));
   EXPECT_EQ(3, result["pass"].getInt());
   EXPECT_TRUE(deliverEvent);
   EXPECT_FALSE(builder_.eventPending());

   // nothing was owed, so nothing is retained
   onPass_ = [&](int) -> Error { return failure(); };
   EXPECT_TRUE(builder_.build(&result, &deliverEvent));
   EXPECT_FALSE(deliverEvent);
   EXPECT_FALSE(builder_.eventPending());
}

TEST_F(PackageStateBuilderTest, RebuildPasses_AreCapped) {
   json::Object nestedResult;
   bool nestedDeliverEvent = false;
   onPass_ = [&](int) -> Error {
      builder_.buildForEvent(&nestedResult, &nestedDeliverEvent);
      return Success();
   };

   json::Object result;
   bool deliverEvent = false;
   Error error = builder_.build(&result, &deliverEvent);

   EXPECT_FALSE(error);
   EXPECT_EQ(PackageStateBuilder::kMaxPasses, passes_);
   EXPECT_EQ(PackageStateBuilder::kMaxPasses, result["pass"].getInt());
   EXPECT_TRUE(deliverEvent);
   EXPECT_FALSE(builder_.eventPending());
}

} // namespace packages
} // namespace modules
} // namespace session
} // namespace rstudio

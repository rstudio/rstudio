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

#include "SessionPackages.hpp"

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

} // namespace packages
} // namespace modules
} // namespace session
} // namespace rstudio

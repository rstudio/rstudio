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

TEST(SessionPackagesTest, IsPackageManagementCall_DetectsBaseAndDevtools) {
   EXPECT_TRUE(isPackageManagementCall("install.packages(\"dplyr\")"));
   EXPECT_TRUE(isPackageManagementCall("update.packages()"));
   EXPECT_TRUE(isPackageManagementCall("remove.packages(\"dplyr\")"));
   EXPECT_TRUE(isPackageManagementCall("install_github(\"r-lib/cli\")"));
   EXPECT_TRUE(isPackageManagementCall("devtools::install_github(\"r-lib/cli\")"));
   EXPECT_TRUE(isPackageManagementCall("devtools::install(\".\")"));
   EXPECT_TRUE(isPackageManagementCall("devtools::load_all(\".\")"));
   EXPECT_TRUE(isPackageManagementCall("remotes::install_github(\"r-lib/cli\")"));
}

TEST(SessionPackagesTest, IsPackageManagementCall_DetectsPak) {
   EXPECT_TRUE(isPackageManagementCall("pak::pkg_install(\"dplyr\")"));
   EXPECT_TRUE(isPackageManagementCall("pak::pkg_remove(\"dplyr\")"));
   EXPECT_TRUE(isPackageManagementCall("pak::pkg_update(\"dplyr\")"));
   EXPECT_TRUE(isPackageManagementCall("pak::local_install(\".\")"));
   EXPECT_TRUE(isPackageManagementCall("pkg_install(\"dplyr\")"));
}

TEST(SessionPackagesTest, IsPackageManagementCall_DetectsPacman) {
   EXPECT_TRUE(isPackageManagementCall("pacman::p_install(\"dplyr\")"));
   EXPECT_TRUE(isPackageManagementCall("pacman::p_load(dplyr)"));
   EXPECT_TRUE(isPackageManagementCall("p_load(dplyr)"));
   EXPECT_TRUE(isPackageManagementCall("p_unload(dplyr)"));
}

TEST(SessionPackagesTest, IsPackageManagementCall_DetectsRenv) {
   EXPECT_TRUE(isPackageManagementCall("renv::install(\"dplyr\")"));
   EXPECT_TRUE(isPackageManagementCall("renv::update()"));
   EXPECT_TRUE(isPackageManagementCall("renv::restore()"));
   EXPECT_TRUE(isPackageManagementCall("renv::rebuild(\"dplyr\")"));
   EXPECT_TRUE(isPackageManagementCall("renv::remove(\"dplyr\")"));
}

TEST(SessionPackagesTest, IsPackageManagementCall_AllowsWhitespaceBeforeParen) {
   EXPECT_TRUE(isPackageManagementCall("install.packages (\"dplyr\")"));
   EXPECT_TRUE(isPackageManagementCall("install.packages\t(\"dplyr\")"));
}

TEST(SessionPackagesTest, IsPackageManagementCall_RejectsReadOnlyLookalikes) {
   // 'install' is a prefix of 'installed', but the suffix '[._]' requirement
   // prevents the match from extending into the longer identifier.
   EXPECT_FALSE(isPackageManagementCall("installed.packages()"));
   EXPECT_FALSE(isPackageManagementCall("x <- installed.packages()"));
   EXPECT_FALSE(isPackageManagementCall("y <- old.packages()"));
}

TEST(SessionPackagesTest, IsPackageManagementCall_RejectsAssignmentTargets) {
   // The trailing '\(' requirement prevents these from matching: there's
   // no '(' immediately after the keyword.
   EXPECT_FALSE(isPackageManagementCall("updates <- c(1, 2)"));
   EXPECT_FALSE(isPackageManagementCall("install <- 42"));
   EXPECT_FALSE(isPackageManagementCall("x <- update"));
   EXPECT_FALSE(isPackageManagementCall("removed_rows <- nrow(df)"));
}

TEST(SessionPackagesTest, IsPackageManagementCall_RejectsCommentsWithoutParens) {
   EXPECT_FALSE(isPackageManagementCall("# install foo"));
   EXPECT_FALSE(isPackageManagementCall("# update later"));
   EXPECT_FALSE(isPackageManagementCall("# TODO: remove"));
}

TEST(SessionPackagesTest, IsPackageManagementCall_RejectsUnrelatedCalls) {
   EXPECT_FALSE(isPackageManagementCall("library(dplyr)"));
   EXPECT_FALSE(isPackageManagementCall("require(dplyr)"));
   EXPECT_FALSE(isPackageManagementCall("data(mtcars)"));
   EXPECT_FALSE(isPackageManagementCall("c(1, 2, 3)"));
   EXPECT_FALSE(isPackageManagementCall(""));
}

} // namespace packages
} // namespace modules
} // namespace session
} // namespace rstudio

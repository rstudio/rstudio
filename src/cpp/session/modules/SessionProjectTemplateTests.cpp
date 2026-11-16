/*
 * SessionProjectTemplateTests.cpp
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

#include <session/SessionProjectTemplate.hpp>

namespace rstudio {
namespace session {
namespace modules {
namespace projects {
namespace templates {
namespace {

bool createsPackageJson(const ProjectTemplateDescription& description)
{
   core::json::Object object = description.toJson().getObject();
   EXPECT_TRUE(object["creates_package"].isBool());
   return object["creates_package"].getBool();
}

} // anonymous namespace

// the client validates the directory name as a package name only when the
// session marks the template this way
TEST(SessionProjectTemplateTest, ToJsonIncludesCreatesPackage)
{
   ProjectTemplateDescription description;
   description.package = "Rcpp";
   description.binding = "Rcpp.package.skeleton";
   description.createsPackage = true;
   EXPECT_TRUE(createsPackageJson(description));
}

TEST(SessionProjectTemplateTest, CreatesPackageDefaultsToFalse)
{
   ProjectTemplateDescription description;
   description.package = "bookdown";
   description.binding = "bookdown_skeleton";
   EXPECT_FALSE(createsPackageJson(description));
}

} // namespace templates
} // namespace projects
} // namespace modules
} // namespace session
} // namespace rstudio

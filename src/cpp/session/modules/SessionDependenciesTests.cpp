/*
 * SessionDependenciesTests.cpp
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

#include <core/system/Environment.hpp>

#include <r/RSexp.hpp>

#include "SessionDependencies.hpp"

namespace rstudio {
namespace session {
namespace modules {
namespace dependencies {
namespace {

Dependency cranDependency(const std::string& name, bool source = false)
{
   Dependency dep;
   dep.name = name;
   dep.source = source;
   return dep;
}

// Scoped override of the RENV_PROJECT environment variable, which controls
// whether install scripts use renv::install() or utils::install.packages().
class RenvProjectScope
{
public:
   explicit RenvProjectScope(const std::string& value) :
      saved_(core::system::getenv("RENV_PROJECT"))
   {
      core::system::setenv("RENV_PROJECT", value);
   }

   ~RenvProjectScope()
   {
      core::system::setenv("RENV_PROJECT", saved_);
   }

private:
   std::string saved_;
};

TEST(SessionDependenciesTest, SexpConstructorDefaultsOptionalFields)
{
   // the runtime dependency walker returns records containing only 'name'
   // and 'version'; all other fields must fall back to sensible defaults
   // (in particular, versionSatisfied must not be left uninitialized)
   r::sexp::Protect protect;
   r::sexp::ListBuilder builder(&protect);
   builder.add("name", std::string("foo"));
   builder.add("version", std::string("1.0"));

   Dependency dep(r::sexp::create(builder, &protect));

   EXPECT_EQ(dep.name, "foo");
   EXPECT_EQ(dep.version, "1.0");
   EXPECT_EQ(dep.location, kCRANPackageDependency);
   EXPECT_FALSE(dep.source);
   EXPECT_TRUE(dep.availableVersion.empty());
   EXPECT_TRUE(dep.versionSatisfied);
}

TEST(SessionDependenciesTest, SexpConstructorReadsOptionalFields)
{
   r::sexp::Protect protect;
   Dependency original = cranDependency("foo", true);
   original.version = "1.0";
   original.availableVersion = "0.9";
   original.versionSatisfied = false;

   Dependency dep(original.asSEXP(&protect));

   EXPECT_EQ(dep.name, "foo");
   EXPECT_EQ(dep.version, "1.0");
   EXPECT_EQ(dep.location, kCRANPackageDependency);
   EXPECT_TRUE(dep.source);
   EXPECT_EQ(dep.availableVersion, "0.9");
   EXPECT_FALSE(dep.versionSatisfied);
}

TEST(SessionDependenciesTest, CombinedInstallScriptIsWellFormed)
{
   RenvProjectScope scope("");
   std::vector<Dependency> deps = {
      cranDependency("first"),
      cranDependency("second")
   };

   EXPECT_EQ(buildCombinedInstallScript(deps),
             "utils::install.packages(c('first','second'))\n\n");
}

TEST(SessionDependenciesTest, CombinedInstallScriptInstallsSourcePackagesSeparately)
{
   RenvProjectScope scope("");
   std::vector<Dependency> deps = {
      cranDependency("binary"),
      cranDependency("compiled", true)
   };

   EXPECT_EQ(buildCombinedInstallScript(deps),
             "utils::install.packages(c('binary'))\n\n"
             "utils::install.packages(c('compiled'), type = 'source')\n\n");
}

TEST(SessionDependenciesTest, CombinedInstallScriptUsesRenvWhenActive)
{
   RenvProjectScope scope("/tmp/project");
   std::vector<Dependency> deps = {
      cranDependency("binary"),
      cranDependency("compiled", true)
   };

   EXPECT_EQ(buildCombinedInstallScript(deps),
             "renv::install(c('binary'))\n\n"
             "options(pkgType = 'source'); renv::install(c('compiled'))\n\n");
}

} // anonymous namespace
} // namespace dependencies
} // namespace modules
} // namespace session
} // namespace rstudio

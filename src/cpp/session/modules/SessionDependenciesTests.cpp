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

#include <r/RExec.hpp>
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

Dependency versionedDependency(const std::string& name, const std::string& version)
{
   Dependency dep = cranDependency(name);
   dep.version = version;
   return dep;
}

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
   std::vector<Dependency> deps = {
      cranDependency("first"),
      cranDependency("second")
   };

   EXPECT_EQ(buildCombinedInstallScript(deps, false),
             "utils::install.packages(c('first','second'))\n\n");
}

// Source packages go first: a binary package depending on one of them would
// otherwise pull in a binary build, which renv then keeps.
TEST(SessionDependenciesTest, CombinedInstallScriptInstallsSourcePackagesFirst)
{
   std::vector<Dependency> deps = {
      cranDependency("binary"),
      cranDependency("compiled", true)
   };

   EXPECT_EQ(buildCombinedInstallScript(deps, false),
             "utils::install.packages(c('compiled'), type = 'source')\n\n"
             "utils::install.packages(c('binary'))\n\n");
}

TEST(SessionDependenciesTest, CombinedInstallScriptUsesRenvWhenActive)
{
   std::vector<Dependency> deps = {
      cranDependency("binary"),
      cranDependency("compiled", true)
   };

   EXPECT_EQ(buildCombinedInstallScript(deps, true),
             "options(pkgType = 'source'); renv::install(c('compiled'))\n\n"
             "renv::install(c('binary'))\n\n");
}

// renv projects install everything together even when the individual-install
// preference is set, so that renv can resolve dependencies and install in
// parallel. (The individual path itself queries available.packages(), so it is
// not exercised here.)
TEST(SessionDependenciesTest, InstallScriptIgnoresIndividualPreferenceForRenv)
{
   std::vector<Dependency> deps = {
      cranDependency("binary"),
      cranDependency("compiled", true)
   };

   std::string expected = buildCombinedInstallScript(deps, true);
   EXPECT_EQ(buildInstallScript(deps, true, true), expected);
   EXPECT_EQ(buildInstallScript(deps, true, false), expected);
}

TEST(SessionDependenciesTest, InstallScriptUsesCombinedInstallWhenPreferenceIsOff)
{
   std::vector<Dependency> deps = {
      cranDependency("binary"),
      cranDependency("compiled", true)
   };

   EXPECT_EQ(buildInstallScript(deps, false, false), buildCombinedInstallScript(deps, false));
}

TEST(SessionDependenciesTest, VerifyInstallScriptAcceptsInstalledPackages)
{
   // 'utils' is always installed; an empty version means any version will do
   std::vector<Dependency> deps = {
      cranDependency("utils"),
      versionedDependency("stats", "1.0")
   };

   core::Error error = r::exec::executeString(buildVerifyInstallScript(deps));
   EXPECT_FALSE(error) << error.asString();
}

TEST(SessionDependenciesTest, VerifyInstallScriptRejectsMissingPackages)
{
   // install.packages() only warns when an installation fails, so this check
   // is what turns a failed install into a failed job (#18882)
   std::vector<Dependency> deps = {
      cranDependency("utils"),
      cranDependency("rstudioNoSuchPackage")
   };

   core::Error error = r::exec::executeString(buildVerifyInstallScript(deps));
   ASSERT_TRUE(error);
   EXPECT_NE(error.asString().find("Failed to install 'rstudioNoSuchPackage'"), std::string::npos)
      << error.asString();
}

TEST(SessionDependenciesTest, VerifyInstallScriptRejectsOutdatedPackages)
{
   // a failed update leaves the old version in place, which must not pass
   std::vector<Dependency> deps = {
      versionedDependency("utils", "9999.0")
   };

   core::Error error = r::exec::executeString(buildVerifyInstallScript(deps));
   ASSERT_TRUE(error);
   EXPECT_NE(error.asString().find("Failed to install 'utils'"), std::string::npos)
      << error.asString();
}

TEST(SessionDependenciesTest, VerifyInstallScriptChecksEachDuplicateEntry)
{
   // the unversioned entry must not answer for the versioned one
   std::vector<Dependency> deps = {
      cranDependency("utils"),
      versionedDependency("utils", "9999.0")
   };

   core::Error error = r::exec::executeString(buildVerifyInstallScript(deps));
   ASSERT_TRUE(error);
   EXPECT_NE(error.asString().find("Failed to install 'utils'"), std::string::npos)
      << error.asString();
}

TEST(SessionDependenciesTest, VerifyInstallScriptRejectsMissingRecursiveDependencies)
{
   // a binary package installs without its dependencies being present, so the
   // requested package can be in place while one it pulled in failed to install
   constexpr auto setup = R"EOF(
local({
   lib <- tempfile("rstudio-deps-lib-")
   dir.create(file.path(lib, "rstudioFakeParent"), recursive = TRUE)
   writeLines(
      c("Package: rstudioFakeParent", "Version: 1.0", "Imports: utils, rstudioNoSuchPackage"),
      file.path(lib, "rstudioFakeParent", "DESCRIPTION")
   )
   options(rstudio.tests.libPaths = .libPaths())
   .libPaths(c(lib, .libPaths()))
})
)EOF";

   core::Error error = r::exec::executeString(setup);
   ASSERT_FALSE(error) << error.asString();

   std::vector<Dependency> deps = {
      cranDependency("rstudioFakeParent")
   };
   error = r::exec::executeString(buildVerifyInstallScript(deps));

   core::Error restoreError = r::exec::executeString(
         ".libPaths(getOption('rstudio.tests.libPaths')); options(rstudio.tests.libPaths = NULL)");
   EXPECT_FALSE(restoreError) << restoreError.asString();

   ASSERT_TRUE(error);
   EXPECT_NE(error.asString().find("Failed to install 'rstudioNoSuchPackage'"), std::string::npos)
      << error.asString();
}

} // anonymous namespace
} // namespace dependencies
} // namespace modules
} // namespace session
} // namespace rstudio

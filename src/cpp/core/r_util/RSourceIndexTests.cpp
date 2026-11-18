/*
 * RSourceIndexTests.cpp
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
#include <core/r_util/RSourceIndex.hpp>

namespace rstudio {
namespace core {
namespace r_util {
namespace {

// A malformed name, as a NAMESPACE or DESCRIPTION file could contain.
const char* const kMalformedName = "utils', 'stats";

// Checked without isValidRPackageName() so the test does not depend on the
// function it covers.
bool containsQuote(const std::set<std::string>& names)
{
   for (const std::string& name : names)
   {
      if (name.find_first_of("'\"") != std::string::npos)
         return true;
   }
   return false;
}

class RSourceIndexPackageNameTests : public ::testing::Test
{
protected:
   void SetUp() override
   {
      importedPackages_ = RSourceIndex::getImportedPackages();
      importFromDirectives_ = RSourceIndex::getImportFromDirectives();
   }

   void TearDown() override
   {
      RSourceIndex::setImportedPackages(importedPackages_);
      RSourceIndex::setImportFromDirectives(importFromDirectives_);
   }

   std::set<std::string> importedPackages_;
   RSourceIndex::ImportFromMap importFromDirectives_;
};

} // anonymous namespace

TEST(RSourceIndexTests, AcceptsValidPackageNames)
{
   for (const char* name : {"utils", "data.table", "R6", "Rcpp", "a1", "A.b.C2"})
   {
      SCOPED_TRACE(name);
      EXPECT_TRUE(isValidRPackageName(name));
   }
}

TEST(RSourceIndexTests, RejectsInvalidPackageNames)
{
   for (const char* name : {
           "", "a", "1pkg", ".pkg", "pkg.", "pkg_name", "pkg-name", "pkg name",
           "pkg'", "pkg\"", "pkg\\", "pkg\n", "pkg;", "pkg()", "caf\xc3\xa9",
           kMalformedName})
   {
      SCOPED_TRACE(name);
      EXPECT_FALSE(isValidRPackageName(name));
   }
}

TEST_F(RSourceIndexPackageNameTests, ImportedPackagesKeepOnlyValidNames)
{
   RSourceIndex::setImportedPackages({"splines", kMalformedName});

   EXPECT_EQ(std::set<std::string>{"splines"}, RSourceIndex::getImportedPackages());
   EXPECT_EQ(0u, RSourceIndex::getAllInferredPackages().count(kMalformedName));
}

TEST_F(RSourceIndexPackageNameTests, ImportFromDirectivesKeepOnlyValidPackages)
{
   RSourceIndex::setImportFromDirectives({
      {"stats", {"median"}},
      {kMalformedName, {"head"}}
   });

   const RSourceIndex::ImportFromMap& directives = RSourceIndex::getImportFromDirectives();
   ASSERT_EQ(1u, directives.size());
   EXPECT_EQ(1u, directives.at("stats").count("median"));
   EXPECT_EQ(0u, RSourceIndex::getAllInferredPackages().count(kMalformedName));
}

TEST_F(RSourceIndexPackageNameTests, SettersAcceptTheirOwnContents)
{
   RSourceIndex::setImportedPackages({"splines"});
   RSourceIndex::setImportFromDirectives({{"stats", {"median"}}});

   RSourceIndex::setImportedPackages(RSourceIndex::getImportedPackages());
   RSourceIndex::setImportFromDirectives(RSourceIndex::getImportFromDirectives());

   EXPECT_EQ(std::set<std::string>{"splines"}, RSourceIndex::getImportedPackages());
   EXPECT_EQ(1u, RSourceIndex::getImportFromDirectives().count("stats"));
}

TEST(RSourceIndexTests, GloballyInferredPackagesKeepOnlyValidNames)
{
   RSourceIndex::addGloballyInferredPackage(kMalformedName);
   RSourceIndex::addGloballyInferredPackage("tools");

   EXPECT_EQ(0u, RSourceIndex::getAllInferredPackages().count(kMalformedName));
   EXPECT_EQ(1u, RSourceIndex::getAllInferredPackages().count("tools"));
}

TEST(RSourceIndexTests, LibraryCallsInferOnlyValidNames)
{
   RSourceIndex index("test.R", R"code(library("utils', 'stats")
library(compiler)
)code");

   EXPECT_EQ(std::vector<std::string>{"compiler"}, index.getInferredPackages());

   index.addInferredPackage(kMalformedName);
   EXPECT_EQ(std::vector<std::string>{"compiler"}, index.getInferredPackages());
}

TEST_F(RSourceIndexPackageNameTests, UnindexedPackagesHoldOnlyValidNames)
{
   RSourceIndex::setImportedPackages({kMalformedName});
   RSourceIndex::setImportFromDirectives({{kMalformedName, {"head"}}});
   RSourceIndex::addGloballyInferredPackage(kMalformedName);

   std::vector<std::string> unindexed = RSourceIndex::getAllUnindexedPackages();
   EXPECT_FALSE(containsQuote({unindexed.begin(), unindexed.end()}));
   EXPECT_FALSE(containsQuote(RSourceIndex::getAllInferredPackages()));
}

} // namespace r_util
} // namespace core
} // namespace rstudio

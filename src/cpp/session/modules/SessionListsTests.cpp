/*
 * SessionListsTests.cpp
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

#include <string>

#include <shared_core/FilePath.hpp>
#include <session/SessionConstants.hpp>

#include "SessionLists.hpp"

namespace rstudio {
namespace session {
namespace modules {
namespace lists {
namespace {

using core::FilePath;

const char kSep = kProjectNameSepChar;

std::string withName(const std::string& path, const std::string& name)
{
   return path + kSep + name;
}

#ifdef _WIN32
FilePath fakeHome()
{
   return FilePath("C:/Users/test");
}
#else
FilePath fakeHome()
{
   return FilePath("/home/test");
}
#endif

TEST(SessionListsTest, SplitEntry_PathOnly)
{
   auto parts = detail::splitProjectMruEntry("/home/test/foo/foo.Rproj");
   EXPECT_EQ("/home/test/foo/foo.Rproj", parts.first);
   EXPECT_EQ("", parts.second);
}

TEST(SessionListsTest, SplitEntry_PathWithName)
{
   auto parts = detail::splitProjectMruEntry(withName("/home/test/foo/foo.Rproj", "My Project"));
   EXPECT_EQ("/home/test/foo/foo.Rproj", parts.first);
   EXPECT_EQ("My Project", parts.second);
}

TEST(SessionListsTest, SplitEntry_Empty)
{
   auto parts = detail::splitProjectMruEntry("");
   EXPECT_EQ("", parts.first);
   EXPECT_EQ("", parts.second);
}

TEST(SessionListsTest, JoinEntry_NoName)
{
   EXPECT_EQ("/home/test/foo/foo.Rproj",
             detail::joinProjectMruEntry("/home/test/foo/foo.Rproj", ""));
}

TEST(SessionListsTest, JoinEntry_WithName)
{
   EXPECT_EQ(withName("/home/test/foo/foo.Rproj", "My Project"),
             detail::joinProjectMruEntry("/home/test/foo/foo.Rproj", "My Project"));
}

TEST(SessionListsTest, SplitJoin_RoundTrips)
{
   const std::string entries[] = {
      "/home/test/foo/foo.Rproj",
      withName("/home/test/foo/foo.Rproj", "Display Name"),
      "",
   };

   for (const std::string& entry : entries)
   {
      auto parts = detail::splitProjectMruEntry(entry);
      EXPECT_EQ(entry, detail::joinProjectMruEntry(parts.first, parts.second));
   }
}

TEST(SessionListsTest, Canonicalize_ResolvesTildePath)
{
   FilePath home = fakeHome();
   std::string aliased = "~/foo/foo.Rproj";
   std::string canonical = detail::canonicalizeProjectMruEntry(aliased, home);

   // After canonicalize, the path is no longer aliased.
   EXPECT_FALSE(FilePath::isAliasedPath(detail::splitProjectMruEntry(canonical).first));

   // Aliasing the canonical form against the same home returns the
   // original aliased entry, so the transformation is reversible.
   EXPECT_EQ(aliased, detail::aliasProjectMruEntry(canonical, home));
}

TEST(SessionListsTest, Canonicalize_PreservesProjectName)
{
   FilePath home = fakeHome();
   std::string entry = withName("~/foo/foo.Rproj", "My Project");
   std::string canonical = detail::canonicalizeProjectMruEntry(entry, home);

   auto parts = detail::splitProjectMruEntry(canonical);
   EXPECT_FALSE(FilePath::isAliasedPath(parts.first));
   EXPECT_EQ("My Project", parts.second);
}

TEST(SessionListsTest, Canonicalize_PreservesPathsOutsideHome)
{
#ifdef _WIN32
   const std::string outside = "D:/projects/foo/foo.Rproj";
#else
   const std::string outside = "/opt/projects/foo/foo.Rproj";
#endif
   FilePath home = fakeHome();
   std::string canonical = detail::canonicalizeProjectMruEntry(outside, home);
   EXPECT_EQ(outside, detail::splitProjectMruEntry(canonical).first);
}

TEST(SessionListsTest, Canonicalize_PassesThroughEmpty)
{
   FilePath home = fakeHome();
   EXPECT_EQ("", detail::canonicalizeProjectMruEntry("", home));
}

TEST(SessionListsTest, Canonicalize_AliasedAndAbsoluteMatch)
{
   // Same project recorded both ways should produce the same canonical form
   // so the dedup pass in normalizeProjectMru collapses them.
   FilePath home = fakeHome();
   std::string aliased = "~/foo/foo.Rproj";
   std::string absolute = home.getAbsolutePath() + "/foo/foo.Rproj";

   EXPECT_EQ(detail::canonicalizeProjectMruEntry(aliased, home),
             detail::canonicalizeProjectMruEntry(absolute, home));
}

#ifdef _WIN32
TEST(SessionListsTest, Canonicalize_NormalizesWindowsSeparators)
{
   // The same project with native vs. generic separators must canonicalize
   // identically -- this is what the dedup pass relies on for the duplicate
   // entries reported in rstudio/rstudio#17225 on Windows.
   FilePath home = fakeHome();
   std::string native = "C:\\Users\\test\\foo\\foo.Rproj";
   std::string generic = "C:/Users/test/foo/foo.Rproj";

   EXPECT_EQ(detail::canonicalizeProjectMruEntry(native, home),
             detail::canonicalizeProjectMruEntry(generic, home));
}
#endif

TEST(SessionListsTest, Alias_RewritesPathsInsideHome)
{
   FilePath home = fakeHome();
   std::string absolute = home.getAbsolutePath() + "/foo/foo.Rproj";
   std::string aliased = detail::aliasProjectMruEntry(absolute, home);
   EXPECT_EQ("~/foo/foo.Rproj", aliased);
}

TEST(SessionListsTest, Alias_PreservesProjectName)
{
   FilePath home = fakeHome();
   std::string entry = withName(home.getAbsolutePath() + "/foo/foo.Rproj", "My Project");
   std::string aliased = detail::aliasProjectMruEntry(entry, home);
   EXPECT_EQ(withName("~/foo/foo.Rproj", "My Project"), aliased);
}

TEST(SessionListsTest, Alias_PassesThroughExternalPaths)
{
#ifdef _WIN32
   const std::string outside = "D:/projects/foo/foo.Rproj";
#else
   const std::string outside = "/opt/projects/foo/foo.Rproj";
#endif
   FilePath home = fakeHome();
   EXPECT_EQ(outside, detail::aliasProjectMruEntry(outside, home));
}

TEST(SessionListsTest, Alias_PassesThroughAlreadyAliased)
{
   FilePath home = fakeHome();
   EXPECT_EQ("~/foo", detail::aliasProjectMruEntry("~/foo", home));
}

} // anonymous namespace
} // namespace lists
} // namespace modules
} // namespace session
} // namespace rstudio

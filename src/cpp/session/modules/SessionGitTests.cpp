/*
 * SessionGitTests.cpp
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

#include <boost/algorithm/string.hpp>

#include "SessionGit.hpp"

namespace rstudio {
namespace session {
namespace modules {
namespace git {
namespace {

using namespace core;

TEST(SessionGitTests, KeepsTheFilePairForAStagedCopy)
{
   // a copy's source, with staged changes of its own, comes first
   std::string source =
         "diff --git a/sub/orig.R b/sub/orig.R\n"
         "index dbe1d57..c3fa9e1 100644\n"
         "--- a/sub/orig.R\n"
         "+++ b/sub/orig.R\n"
         "@@ -5,0 +6 @@ e <- 5\n"
         "+f <- 6\n";
   std::string copy =
         "diff --git a/sub/orig.R b/sub/zcopy.R\n"
         "similarity index 77%\n"
         "copy from sub/orig.R\n"
         "copy to sub/zcopy.R\n"
         "index dbe1d57..3daa188 100644\n"
         "--- a/sub/orig.R\n"
         "+++ b/sub/zcopy.R\n"
         "@@ -2 +2 @@ a <- 1\n"
         "-b <- 2\n"
         "+b <- 20\n";

   EXPECT_EQ(filePairFor(source + copy, "sub/zcopy.R"), copy);
   EXPECT_EQ(filePairFor(source + copy, "sub/orig.R"), source);

   // with diff.noprefix, and with a custom prefix
   std::string noPrefix = boost::algorithm::replace_all_copy(
         boost::algorithm::replace_all_copy(source + copy, " a/", " "), " b/", " ");
   std::string noPrefixCopy = boost::algorithm::replace_all_copy(
         boost::algorithm::replace_all_copy(copy, " a/", " "), " b/", " ");
   EXPECT_EQ(filePairFor(noPrefix, "sub/zcopy.R"), noPrefixCopy);
   std::string wPrefix = boost::algorithm::replace_all_copy(source + copy, "+++ b/", "+++ w/");
   EXPECT_EQ(filePairFor(wPrefix, "sub/zcopy.R"),
             boost::algorithm::replace_all_copy(copy, "+++ b/", "+++ w/"));

   // a path that only ends the same is not the one
   EXPECT_EQ(filePairFor(source + copy, "orig.R"), source + copy);

   // one pair, or none for the path, stays whole
   EXPECT_EQ(filePairFor(copy, "sub/orig.R"), copy);
   EXPECT_EQ(filePairFor(source + copy, "other.R"), source + copy);
   EXPECT_EQ(filePairFor("", "sub/zcopy.R"), "");
}

TEST(SessionGitTests, ParsesWorktreeListPorcelain)
{
   std::string output =
         "worktree /home/user/repo\n"
         "HEAD 0123456789abcdef0123456789abcdef01234567\n"
         "branch refs/heads/main\n"
         "\n"
         "worktree /home/user/repo/.worktrees/feature\n"
         "HEAD fedcba9876543210fedcba9876543210fedcba98\n"
         "branch refs/heads/feature/x\n"
         "locked reason with spaces\n"
         "\n"
         "worktree /home/user/detached\n"
         "HEAD 1111111111111111111111111111111111111111\n"
         "detached\n"
         "prunable gitdir file points to non-existent location\n"
         "\n";

   json::Array worktrees = parseWorktreeList(output);
   ASSERT_EQ(worktrees.getSize(), 3u);

   json::Object main = worktrees[0].getObject();
   EXPECT_EQ(main["path"].getString(), "/home/user/repo");
   EXPECT_EQ(main["head"].getString(), "01234567");
   EXPECT_EQ(main["branch"].getString(), "main");
   EXPECT_TRUE(main["is_main"].getBool());
   EXPECT_FALSE(main["detached"].getBool());
   EXPECT_FALSE(main["locked"].getBool());

   json::Object feature = worktrees[1].getObject();
   EXPECT_EQ(feature["path"].getString(), "/home/user/repo/.worktrees/feature");
   EXPECT_EQ(feature["branch"].getString(), "feature/x");
   EXPECT_FALSE(feature["is_main"].getBool());
   EXPECT_TRUE(feature["locked"].getBool());
   EXPECT_FALSE(feature["prunable"].getBool());

   json::Object detached = worktrees[2].getObject();
   EXPECT_EQ(detached["branch"].getString(), "");
   EXPECT_EQ(detached["head"].getString(), "11111111");
   EXPECT_TRUE(detached["detached"].getBool());
   EXPECT_TRUE(detached["prunable"].getBool());
}

TEST(SessionGitTests, ParsesBareAndCrlfWorktreeList)
{
   // a bare repository lists itself first, and Windows git may emit CRLF
   std::string output =
         "worktree C:/repo.git\r\n"
         "bare\r\n"
         "\r\n"
         "worktree C:/repo\r\n"
         "HEAD 0123456789abcdef0123456789abcdef01234567\r\n"
         "branch refs/heads/main\r\n";

   json::Array worktrees = parseWorktreeList(output);
   ASSERT_EQ(worktrees.getSize(), 2u);
   EXPECT_TRUE(worktrees[0].getObject()["bare"].getBool());
   EXPECT_EQ(worktrees[1].getObject()["path"].getString(), "C:/repo");
   EXPECT_EQ(worktrees[1].getObject()["branch"].getString(), "main");
}

TEST(SessionGitTests, ParsesEmptyWorktreeList)
{
   EXPECT_TRUE(parseWorktreeList("").isEmpty());
   EXPECT_TRUE(parseWorktreeList("\n\n").isEmpty());
}

} // anonymous namespace
} // namespace git
} // namespace modules
} // namespace session
} // namespace rstudio

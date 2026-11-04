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

#include "SessionGit.hpp"

namespace rstudio {
namespace session {
namespace modules {
namespace git {
namespace {

using namespace core;

TEST(SessionGitTests, KeepsTheRenamedPairForItsNewPath)
{
   // two files staged in place of the one they came from (the directory
   // of its name is in the pathspec for it), the one without hunks first
   std::string whole =
         "diff --git a/foo b/foo/a.R\n"
         "similarity index 100%\n"
         "copy from foo\n"
         "copy to foo/a.R\n";
   std::string edited =
         "diff --git a/foo b/foo/z.R\n"
         "similarity index 77%\n"
         "rename from foo\n"
         "rename to foo/z.R\n"
         "index dbe1d57..3daa188 100644\n"
         "--- a/foo\n"
         "+++ b/foo/z.R\n"
         "@@ -2 +2 @@ a <- 1\n"
         "-b <- 2\n"
         "+b <- 20\n";

   EXPECT_EQ(renamedPairFor(whole + edited, "foo/z.R"), edited);
   EXPECT_EQ(renamedPairFor(whole + edited, "foo/a.R"), whole);
   EXPECT_EQ(renamedPairFor(edited + whole, "foo/z.R"), edited);

   // the path whole, not one ending the same
   EXPECT_EQ(renamedPairFor(whole + edited, "z.R"), whole + edited);

   // one pair, or none for the path, stays as it is
   EXPECT_EQ(renamedPairFor(edited, "foo/a.R"), edited);
   EXPECT_EQ(renamedPairFor(whole + edited, "other.R"), whole + edited);
   EXPECT_EQ(renamedPairFor("", "foo/z.R"), "");
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

/*
 * SessionLibGit2Tests.cpp
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

#include "SessionLibGit2.hpp"

#include <git2.h>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>
#include <core/FileSerializer.hpp>

#include <gtest/gtest.h>

namespace rstudio {
namespace session {
namespace modules {
namespace libgit2 {
namespace tests {

using namespace rstudio::core;

class LibGit2Test : public ::testing::Test
{
protected:
   static void SetUpTestSuite()
   {
      git_libgit2_init();
   }

   void SetUp() override
   {
      // Create a temp directory with a git repo and .gitignore
      FilePath::tempFilePath(repoDir_);
      repoDir_.ensureDirectory();

      git_repository* repo = nullptr;
      git_repository_init(&repo, repoDir_.getAbsolutePath().c_str(), 0);
      git_repository_free(repo);

      FilePath gitignore = repoDir_.completeChildPath(".gitignore");
      writeStringToFile(gitignore, "build/\n*.log\n");
   }

   void TearDown() override
   {
      repoDir_.removeIfExists();
   }

   FilePath repoDir_;
};

TEST_F(LibGit2Test, RelativePathIgnored)
{
   Git git(repoDir_);
   ASSERT_TRUE(git.isOpen());
   EXPECT_TRUE(git.isIgnored("build/"));
   EXPECT_FALSE(git.isIgnored("src/"));
}

TEST_F(LibGit2Test, AbsolutePathIgnored)
{
   Git git(repoDir_);
   ASSERT_TRUE(git.isOpen());

   std::string absIgnored =
      repoDir_.completeChildPath("build").getAbsolutePath() + "/";
   std::string absNotIgnored =
      repoDir_.completeChildPath("src").getAbsolutePath() + "/";

   EXPECT_TRUE(git.isIgnored(absIgnored));
   EXPECT_FALSE(git.isIgnored(absNotIgnored));
}

TEST_F(LibGit2Test, NestedProjectIsNoOp)
{
   // git_repository_open() uses GIT_REPOSITORY_OPEN_NO_SEARCH, so it does
   // NOT perform parent-directory discovery. When a project lives inside a
   // subdirectory of a larger git repo (monorepo), the Git object simply
   // won't open and gitignore filtering is a graceful no-op.
   FilePath projectDir = repoDir_.completeChildPath("projects/myproject");
   projectDir.ensureDirectory();

   Git git(projectDir);
   EXPECT_FALSE(git.isOpen());
   EXPECT_FALSE(git.isIgnored("anything"));
}

TEST_F(LibGit2Test, NonRepoNotOpen)
{
   FilePath noRepoDir;
   FilePath::tempFilePath(noRepoDir);
   noRepoDir.ensureDirectory();

   Git git(noRepoDir);
   EXPECT_FALSE(git.isOpen());
   EXPECT_FALSE(git.isIgnored("anything"));

   noRepoDir.removeIfExists();
}

} // namespace tests
} // namespace libgit2
} // namespace modules
} // namespace session
} // namespace rstudio

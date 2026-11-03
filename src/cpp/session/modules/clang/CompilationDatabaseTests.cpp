/*
 * CompilationDatabaseTests.cpp
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

#include <shared_core/FilePath.hpp>
#include <core/FileSerializer.hpp>

#include "CompilationDatabase.hpp"

namespace rstudio {
namespace session {
namespace modules {
namespace clang {
namespace {

using namespace core;

class CompileFlagsTest : public ::testing::Test
{
protected:
   void SetUp() override
   {
      // <root>/pkg/src, with 'pkg' standing in for the project directory
      FilePath::tempFilePath(root_);
      pkg_ = root_.completeChildPath("pkg");
      src_ = pkg_.completeChildPath("src");
      src_.ensureDirectory();
   }

   void TearDown() override
   {
      root_.removeIfExists();
   }

   FilePath flagsFile(const FilePath& dir, const std::string& contents = "-DFOO\n")
   {
      FilePath file = dir.completeChildPath("compile_flags.txt");
      writeStringToFile(file, contents);
      return file;
   }

   FilePath root_;
   FilePath pkg_;
   FilePath src_;
};

TEST_F(CompileFlagsTest, FindsFileInSourceDirectory)
{
   FilePath file = flagsFile(src_);
   EXPECT_EQ(findCompileFlagsFile(src_, src_), file);
   EXPECT_EQ(findCompileFlagsFile(src_, pkg_), file);
}

TEST_F(CompileFlagsTest, FindsFileInParentUpToStopDirectory)
{
   FilePath file = flagsFile(pkg_);
   EXPECT_EQ(findCompileFlagsFile(src_, pkg_), file);
}

TEST_F(CompileFlagsTest, NearestFileWins)
{
   flagsFile(pkg_);
   FilePath nearest = flagsFile(src_);
   EXPECT_EQ(findCompileFlagsFile(src_, pkg_), nearest);
}

TEST_F(CompileFlagsTest, DoesNotLookAboveStopDirectory)
{
   flagsFile(root_);
   EXPECT_TRUE(findCompileFlagsFile(src_, pkg_).isEmpty());

   // and not beyond the source directory when that is the stop directory
   flagsFile(pkg_);
   EXPECT_TRUE(findCompileFlagsFile(src_, src_).isEmpty());
}

TEST_F(CompileFlagsTest, StopDirectoryMayHaveTrailingSeparator)
{
   flagsFile(root_);
   FilePath stopDir(pkg_.getAbsolutePath() + "/");
   EXPECT_TRUE(findCompileFlagsFile(src_, stopDir).isEmpty());
}

TEST_F(CompileFlagsTest, ReturnsEmptyWhenThereIsNoFile)
{
   EXPECT_TRUE(findCompileFlagsFile(src_, pkg_).isEmpty());
}

TEST_F(CompileFlagsTest, ReadsOneFlagPerLine)
{
   FilePath file = flagsFile(src_, "\r\n-ferror-limit=5\r\n\r\n  -Wall  \r\n-DFOO=1");
   std::vector<std::string> expected = {"-ferror-limit=5", "-Wall", "-DFOO=1"};
   EXPECT_EQ(readCompileFlags(file), expected);
}

TEST_F(CompileFlagsTest, ReadsNothingFromBlankFile)
{
   EXPECT_TRUE(readCompileFlags(flagsFile(src_, "")).empty());
   EXPECT_TRUE(readCompileFlags(flagsFile(src_, "\n  \n\r\n")).empty());
}

TEST_F(CompileFlagsTest, ResolvesRelativePathsAgainstDirectory)
{
   std::string inc = pkg_.completePath("inc").getAbsolutePath();
   std::string header = pkg_.completePath("../pre.h").getAbsolutePath();

   // joined and separate operands both come out joined
   std::vector<std::string> flags = {"-Iinc", "-I", "inc", "-isystem", "inc", "-include", "../pre.h"};
   std::vector<std::string> expected = {"-I" + inc, "-I" + inc, "-isystem" + inc, "-include" + header};
   EXPECT_EQ(resolveCompileFlagPaths(flags, pkg_), expected);
}

TEST_F(CompileFlagsTest, LeavesAbsolutePathsAndOtherFlagsAlone)
{
   std::string abs = root_.getAbsolutePath();
   std::vector<std::string> flags = {"-DFOO", "-I" + abs, "-ferror-limit=5", "-fPIC", "-Wall"};
   EXPECT_EQ(resolveCompileFlagPaths(flags, pkg_), flags);
}

TEST_F(CompileFlagsTest, KeepsIncludePchSeparateAndDanglingOptions)
{
   std::string pch = pkg_.completePath("x.pch").getAbsolutePath();
   std::vector<std::string> flags = {"-include-pch", "x.pch", "-I"};
   std::vector<std::string> expected = {"-include-pch", pch, "-I"};
   EXPECT_EQ(resolveCompileFlagPaths(flags, pkg_), expected);
}

} // anonymous namespace
} // namespace clang
} // namespace modules
} // namespace session
} // namespace rstudio

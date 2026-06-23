/*
 * ServerCheckConfigTests.cpp
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

#include "ServerCheckConfig.hpp"

#include <sstream>

#include <shared_core/FilePath.hpp>
#include <core/FileSerializer.hpp>

#include <gtest/gtest.h>

using namespace rstudio::core;

namespace rstudio {
namespace server {

// ---------------------------------------------------------------------------
// checkConfigFilePath -- string overload
// ---------------------------------------------------------------------------

TEST(CheckConfigFilePathTest, EmptyPathIsSkippedAndReturnsTrue)
{
   std::ostringstream out;
   bool result = checkConfigFilePath("some-option", std::string(), out);
   EXPECT_TRUE(result);
   EXPECT_EQ("", out.str()); // nothing printed for empty paths
}

TEST(CheckConfigFilePathTest, ExistingPathReturnsPassAndTrue)
{
   // Use /tmp, which is guaranteed to exist on POSIX systems
   std::ostringstream out;
   bool result = checkConfigFilePath("test-option", std::string("/tmp"), out);
   EXPECT_TRUE(result);
   EXPECT_NE(std::string::npos, out.str().find("[PASS]"));
   EXPECT_NE(std::string::npos, out.str().find("test-option"));
}

TEST(CheckConfigFilePathTest, MissingPathReturnsFailAndFalse)
{
   std::ostringstream out;
   bool result = checkConfigFilePath("test-option",
                                     std::string("/nonexistent/path/that/does/not/exist"),
                                     out);
   EXPECT_FALSE(result);
   EXPECT_NE(std::string::npos, out.str().find("[FAIL]"));
   EXPECT_NE(std::string::npos, out.str().find("test-option"));
}

TEST(CheckConfigFilePathTest, MissingPathInformationalReturnsPassAndTrue)
{
   std::ostringstream out;
   bool result = checkConfigFilePath("server-data-dir",
                                     std::string("/nonexistent/path/that/does/not/exist"),
                                     out,
                                     true /* informational */);
   EXPECT_TRUE(result);
   EXPECT_NE(std::string::npos, out.str().find("[PASS]"));
   EXPECT_NE(std::string::npos, out.str().find("will be created on startup"));
}

// ---------------------------------------------------------------------------
// checkConfigFilePath -- FilePath overload
// ---------------------------------------------------------------------------

TEST(CheckConfigFilePathTest, EmptyFilePathIsSkippedAndReturnsTrue)
{
   std::ostringstream out;
   bool result = checkConfigFilePath("some-option", FilePath(), out);
   EXPECT_TRUE(result);
   EXPECT_EQ("", out.str());
}

TEST(CheckConfigFilePathTest, ExistingFilePathReturnsPassAndTrue)
{
   std::ostringstream out;
   bool result = checkConfigFilePath("test-option", FilePath("/tmp"), out);
   EXPECT_TRUE(result);
   EXPECT_NE(std::string::npos, out.str().find("[PASS]"));
}

TEST(CheckConfigFilePathTest, MissingFilePathReturnsFailAndFalse)
{
   std::ostringstream out;
   bool result = checkConfigFilePath("test-option",
                                     FilePath("/nonexistent/path/that/does/not/exist"),
                                     out);
   EXPECT_FALSE(result);
   EXPECT_NE(std::string::npos, out.str().find("[FAIL]"));
}

TEST(CheckConfigFilePathTest, MissingFilePathInformationalReturnsPassAndTrue)
{
   std::ostringstream out;
   bool result = checkConfigFilePath("server-data-dir",
                                     FilePath("/nonexistent/path/that/does/not/exist"),
                                     out,
                                     true /* informational */);
   EXPECT_TRUE(result);
   EXPECT_NE(std::string::npos, out.str().find("[PASS]"));
   EXPECT_NE(std::string::npos, out.str().find("will be created on startup"));
}

// ---------------------------------------------------------------------------
// checkConfigFilePath -- fileOnly flag
// ---------------------------------------------------------------------------

TEST(CheckConfigFilePathTest, FileOnlyRejectsDirectoryWithFail)
{
   // /tmp always exists and is a directory; fileOnly=true must reject it
   std::ostringstream out;
   bool result = checkConfigFilePath("test-option", FilePath("/tmp"), out,
                                     false /* informational */, true /* fileOnly */);
   EXPECT_FALSE(result);
   EXPECT_NE(std::string::npos, out.str().find("[FAIL]"));
   EXPECT_NE(std::string::npos, out.str().find("directory"));
}

TEST(CheckConfigFilePathTest, FileOnlyAcceptsRegularFile)
{
   // Write a real temp file and verify fileOnly=true accepts it
   FilePath path;
   Error error = FilePath::tempFilePath(path);
   ASSERT_FALSE(error);
   error = writeStringToFile(path, "x");
   ASSERT_FALSE(error);

   std::ostringstream out;
   bool result = checkConfigFilePath("test-option", path, out,
                                     false /* informational */, true /* fileOnly */);
   EXPECT_TRUE(result);
   EXPECT_NE(std::string::npos, out.str().find("[PASS]"));
   path.removeIfExists();
}

} // namespace server
} // namespace rstudio

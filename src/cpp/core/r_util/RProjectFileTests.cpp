/*
 * RProjectFileTests.cpp
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

#include <core/r_util/RProjectFile.hpp>

#include <core/FileSerializer.hpp>
#include <shared_core/FilePath.hpp>

#include <gtest/gtest.h>

namespace rstudio {
namespace core {
namespace r_util {
namespace {

TEST(RProjectFileTests, EditorThemeRoundTrips)
{
   FilePath projPath;
   ASSERT_FALSE(FilePath::tempFilePath(".Rproj", projPath));

   RProjectConfig config;
   config.editorTheme = "Cobalt";

   ASSERT_FALSE(writeProjectFile(projPath, RProjectBuildDefaults(), config));

   RProjectConfig readConfig;
   std::string errMsg;
   ASSERT_FALSE(readProjectFile(projPath, &readConfig, &errMsg));

   EXPECT_EQ(readConfig.editorTheme, "Cobalt");

   projPath.removeIfExists();
}

TEST(RProjectFileTests, EmptyEditorThemeOmitted)
{
   FilePath projPath;
   ASSERT_FALSE(FilePath::tempFilePath(".Rproj", projPath));

   RProjectConfig config; // editorTheme defaults to ""
   ASSERT_FALSE(writeProjectFile(projPath, RProjectBuildDefaults(), config));

   std::string contents;
   ASSERT_FALSE(readStringFromFile(projPath, &contents));
   EXPECT_EQ(contents.find("EditorTheme"), std::string::npos);

   RProjectConfig readConfig;
   std::string errMsg;
   ASSERT_FALSE(readProjectFile(projPath, &readConfig, &errMsg));
   EXPECT_EQ(readConfig.editorTheme, "");

   projPath.removeIfExists();
}

TEST(RProjectFileTests, UnknownSortedFieldPreservedAlongsideEditorTheme)
{
   FilePath projPath;
   ASSERT_FALSE(FilePath::tempFilePath(".Rproj", projPath));

   RProjectConfig config;
   config.editorTheme = "Cobalt";
   config.sortedFields["FutureUnknownField"] = "keepme";

   ASSERT_FALSE(writeProjectFile(projPath, RProjectBuildDefaults(), config));

   RProjectConfig readConfig;
   std::string errMsg;
   ASSERT_FALSE(readProjectFile(projPath, &readConfig, &errMsg));

   EXPECT_EQ(readConfig.editorTheme, "Cobalt");
   ASSERT_TRUE(readConfig.sortedFields.count("FutureUnknownField") == 1);
   EXPECT_EQ(readConfig.sortedFields["FutureUnknownField"], "keepme");

   projPath.removeIfExists();
}

} // anonymous namespace
} // namespace r_util
} // namespace core
} // namespace rstudio

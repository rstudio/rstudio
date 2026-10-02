/*
 * FileUtilsTests.cpp
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

#include <core/FileUtils.hpp>

#include <gtest/gtest.h>

#include <shared_core/FilePath.hpp>

namespace rstudio {
namespace core {
namespace file_utils {
namespace tests {

TEST(FileUtilsTest, FirstUnusedPath)
{
   FilePath root;
   ASSERT_FALSE(FilePath::tempFilePath(root));
   ASSERT_FALSE(root.ensureDirectory());

   // an unused path is returned as is
   FilePath path = root.completeChildPath("state.json");
   EXPECT_EQ(path.getAbsolutePath(), firstUnusedPath(path).getAbsolutePath());

   // the number goes after the whole file name, and skips anything in the way
   ASSERT_FALSE(path.ensureFile());
   EXPECT_EQ("state.json-2", firstUnusedPath(path).getFilename());
   ASSERT_FALSE(root.completeChildPath("state.json-2").ensureDirectory());
   EXPECT_EQ("state.json-3", firstUnusedPath(path).getFilename());

   EXPECT_FALSE(root.removeIfExists());
}

} // namespace tests
} // namespace file_utils
} // namespace core
} // namespace rstudio

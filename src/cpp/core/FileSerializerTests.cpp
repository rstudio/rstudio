/*
 * FileSerializerTests.cpp
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

#include <core/FileSerializer.hpp>
#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

namespace rstudio {
namespace core {

TEST(FileSerializerTest, WriteStringRoundTrips)
{
   FilePath filePath;
   ASSERT_FALSE(FilePath::tempFilePath(filePath));

   std::string contents = "hello\nworld\n";
   Error error = writeStringToFile(filePath, contents);
   EXPECT_FALSE(error);

   std::string readback;
   error = readStringFromFile(filePath, &readback);
   EXPECT_FALSE(error);
   EXPECT_EQ(contents, readback);

   filePath.removeIfExists();
}

// Regression test for #17833: a write that fails (e.g. a full disk or an
// exceeded disk quota) must be reported as an error rather than silently
// appearing to succeed. We use /dev/full, which always fails writes with
// ENOSPC, to simulate the condition. The device is only available on Linux,
// so the test is skipped elsewhere.
TEST(FileSerializerTest, WriteStringReportsFailedWrite)
{
   FilePath devFull("/dev/full");
   if (!devFull.exists())
      GTEST_SKIP() << "/dev/full not available on this platform";

   Error error = writeStringToFile(devFull, "this write should fail");
   EXPECT_TRUE(error);
}

} // namespace core
} // namespace rstudio

/*
 * FileScannerTests.cpp
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

#include <algorithm>

#ifndef _WIN32
#include <sys/stat.h>
#include <unistd.h>
#endif

#include <gtest/gtest.h>

#include <core/FileSerializer.hpp>
#include <core/system/FileScanner.hpp>

namespace rstudio {
namespace core {
namespace {

TEST(FileScannerTests, RecordsFailedRegistrationAndClearsFailureAfterRetry)
{
   for (bool empty : {true, false})
   {
      SCOPED_TRACE(empty);
      FilePath root;
      ASSERT_FALSE(FilePath::tempFilePath(root));
      ASSERT_FALSE(root.ensureDirectory());
      RemoveOnExitScope removeScope(root, ERROR_LOCATION);
      FilePath blocked = root.completeChildPath("blocked");
      ASSERT_FALSE(blocked.ensureDirectory());
      if (!empty)
         ASSERT_FALSE(writeStringToFile(blocked.completeChildPath("source.R"), "x <- 1\n"));
      ASSERT_FALSE(writeStringToFile(root.completeChildPath("healthy.R"), "y <- 2\n"));

      tree<FileInfo> files;
      system::FileScannerOptions options;
      options.recursive = true;
      options.onBeforeScanDir = [blocked](const FileInfo& info) -> Error
      {
         if (FilePath(info.absolutePath()) == blocked)
            return systemError(boost::system::errc::no_space_on_device, ERROR_LOCATION);
         return Success();
      };
      ASSERT_FALSE(system::scanFiles(FileInfo(root), options, &files));
      auto failed = std::find_if(files.begin(), files.end(), [blocked](const FileInfo& info)
      {
         return FilePath(info.absolutePath()) == blocked;
      });
      ASSERT_NE(files.end(), failed);
      ASSERT_TRUE(failed->scanFailed());
      EXPECT_NE(files.end(), std::find_if(files.begin(), files.end(), [](const FileInfo& info)
      {
         return FilePath(info.absolutePath()).getFilename() == "healthy.R";
      }));
      FileInfo previous = *failed;

      options.onBeforeScanDir.clear();
      ASSERT_FALSE(system::scanFiles(failed, options, &files));
      EXPECT_FALSE(failed->scanFailed());
      EXPECT_NE(previous, *failed);
      if (!empty)
      {
         EXPECT_NE(files.end(), std::find_if(files.begin(), files.end(), [](const FileInfo& info)
         {
            return FilePath(info.absolutePath()).getFilename() == "source.R";
         }));
      }
   }
}

#ifndef _WIN32
TEST(FileScannerTests, RecordsUnreadableChildMetadata)
{
   if (::geteuid() == 0)
      GTEST_SKIP() << "Root can search directories without execute permission";

   FilePath root;
   ASSERT_FALSE(FilePath::tempFilePath(root));
   ASSERT_FALSE(root.ensureDirectory());
   RemoveOnExitScope removeScope(root, ERROR_LOCATION);
   FilePath blocked = root.completeChildPath("blocked");
   ASSERT_FALSE(blocked.ensureDirectory());
   FilePath source = blocked.completeChildPath("source.R");
   ASSERT_FALSE(writeStringToFile(source, "x <- 1\n"));
   ASSERT_EQ(0, ::chmod(blocked.getAbsolutePath().c_str(), 0400));

   tree<FileInfo> files;
   system::FileScannerOptions options;
   options.recursive = true;
   Error error = system::scanFiles(FileInfo(root), options, &files);
   // Restore access before any assertions so cleanup also works on failure.
   ASSERT_EQ(0, ::chmod(blocked.getAbsolutePath().c_str(), 0700));
   ASSERT_FALSE(error);
   auto failed = std::find_if(files.begin(), files.end(), [blocked](const FileInfo& info)
   {
      return FilePath(info.absolutePath()) == blocked;
   });
   ASSERT_NE(files.end(), failed);
   EXPECT_TRUE(failed->scanFailed());
   EXPECT_EQ(0, files.number_of_children(failed));

   ASSERT_FALSE(system::scanFiles(failed, options, &files));
   EXPECT_FALSE(failed->scanFailed());
   ASSERT_EQ(1, files.number_of_children(failed));
   EXPECT_EQ(source.getAbsolutePath(), files.begin(failed)->absolutePath());
}
#endif

} // anonymous namespace
} // namespace core
} // namespace rstudio

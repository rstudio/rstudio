/*
 * FileMonitorReconciliationTests.cpp
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
#include <core/system/FileScanner.hpp>

#include "system/file_monitor/FileMonitorImpl.hpp"

namespace rstudio {
namespace core {
namespace {

namespace monitor = system::file_monitor::impl;
using system::FileChangeEvent;

class FileMonitorReconciliationTests : public ::testing::Test
{
protected:
   void SetUp() override
   {
      ASSERT_FALSE(FilePath::tempFilePath(root_));
      ASSERT_FALSE(root_.ensureDirectory());
      blocked_ = root_.completeChildPath("blocked");
      ASSERT_FALSE(blocked_.ensureDirectory());
      source_ = blocked_.completeChildPath("source.R");
      options_.recursive = true;
      options_.onBeforeScanDir = [this](const FileInfo& info) -> Error
      {
         if (failScan_ && FilePath(info.absolutePath()) == blocked_)
            return systemError(boost::system::errc::no_space_on_device, ERROR_LOCATION);
         return Success();
      };
   }

   void TearDown() override
   {
      EXPECT_FALSE(root_.removeIfExists());
   }

   Error reconcile(const FilePath& path, bool recursive = true)
   {
      events_.clear();
      return monitor::discoverAndProcessFileChanges(
         FileInfo(path.getAbsolutePath(), true),
         recursive,
         options_.filter,
         options_.onBeforeScanDir,
         &files_,
         [this](const std::vector<FileChangeEvent>& events) { events_ = events; });
   }

   void expectScanStatus(bool failed)
   {
      auto dir = monitor::findFile(files_.begin(), files_.end(), blocked_.getAbsolutePath());
      ASSERT_NE(files_.end(), dir);
      EXPECT_EQ(failed, dir->scanFailed());
      auto event = std::find_if(events_.begin(), events_.end(), [this](const FileChangeEvent& event)
      {
         return event.fileInfo().absolutePath() == blocked_.getAbsolutePath();
      });
      ASSERT_NE(events_.end(), event);
      EXPECT_EQ(FileChangeEvent::FileModified, event->type());
      EXPECT_EQ(failed, event->fileInfo().scanFailed());
   }

   FilePath root_, blocked_, source_;
   bool failScan_ = false;
   system::FileScannerOptions options_;
   tree<FileInfo> files_;
   std::vector<FileChangeEvent> events_;
};

TEST_F(FileMonitorReconciliationTests, RecursiveRescanReportsFailureAndRecovery)
{
   // Empty directories also need events: there are no file changes from which
   // consumers could otherwise infer that coverage was lost or restored.
   for (bool empty : {true, false})
   {
      SCOPED_TRACE(empty);
      if (!empty)
         ASSERT_FALSE(writeStringToFile(source_, "x <- 1\n"));
      files_.clear();
      ASSERT_FALSE(system::scanFiles(FileInfo(root_), options_, &files_));

      failScan_ = true;
      ASSERT_FALSE(reconcile(root_));
      expectScanStatus(true);
      ASSERT_FALSE(reconcile(root_));
      EXPECT_TRUE(events_.empty());

      failScan_ = false;
      ASSERT_FALSE(reconcile(root_));
      expectScanStatus(false);
      ASSERT_FALSE(reconcile(root_));
      EXPECT_TRUE(events_.empty());
   }
}

TEST_F(FileMonitorReconciliationTests, DirectoryEventRetriesPreviouslyFailedPath)
{
   ASSERT_FALSE(writeStringToFile(source_, "x <- 1\n"));
   for (bool recursive : {true, false})
   {
      SCOPED_TRACE(recursive);
      failScan_ = true;
      files_.clear();
      ASSERT_FALSE(system::scanFiles(FileInfo(root_), options_, &files_));

      failScan_ = false;
      ASSERT_FALSE(reconcile(blocked_, recursive));
      expectScanStatus(false);
      EXPECT_NE(files_.end(), monitor::findFile(files_.begin(), files_.end(), source_.getAbsolutePath()));
      EXPECT_NE(events_.end(), std::find_if(events_.begin(), events_.end(), [this](const FileChangeEvent& event)
      {
         return event.type() == FileChangeEvent::FileAdded &&
                event.fileInfo().absolutePath() == source_.getAbsolutePath();
      }));
   }
}

TEST_F(FileMonitorReconciliationTests, DirectScanFailureReportsLostCoverage)
{
   ASSERT_FALSE(writeStringToFile(source_, "x <- 1\n"));
   for (bool recursive : {true, false})
   {
      SCOPED_TRACE(recursive);
      files_.clear();
      ASSERT_FALSE(system::scanFiles(FileInfo(root_), options_, &files_));
      failScan_ = true;
      EXPECT_TRUE(reconcile(blocked_, recursive));
      expectScanStatus(true);
      EXPECT_EQ(files_.end(), monitor::findFile(files_.begin(), files_.end(), source_.getAbsolutePath()));

      failScan_ = false;
      ASSERT_FALSE(reconcile(blocked_, recursive));
      expectScanStatus(false);
      EXPECT_NE(files_.end(), monitor::findFile(files_.begin(), files_.end(), source_.getAbsolutePath()));
   }
}

TEST_F(FileMonitorReconciliationTests, ShallowRescanPreservesUnscannedChildFailure)
{
   failScan_ = true;
   ASSERT_FALSE(system::scanFiles(FileInfo(root_), options_, &files_));
   failScan_ = false;
   ASSERT_FALSE(reconcile(root_, false));
   auto dir = monitor::findFile(files_.begin(), files_.end(), blocked_.getAbsolutePath());
   ASSERT_NE(files_.end(), dir);
   EXPECT_TRUE(dir->scanFailed());
   EXPECT_TRUE(events_.empty());
}

} // anonymous namespace
} // namespace core
} // namespace rstudio

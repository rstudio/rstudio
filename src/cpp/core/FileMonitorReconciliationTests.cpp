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

#ifndef _WIN32
#include <unistd.h>
#endif

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

   Error reconcile(const FilePath& path, bool recursive = true, bool recursiveMonitor = false)
   {
      events_.clear();
      return monitor::discoverAndProcessFileChanges(
         FileInfo(path.getAbsolutePath(), true),
         recursive,
         options_.filter,
         options_.onBeforeScanDir,
         &files_,
         [this](const std::vector<FileChangeEvent>& events) { events_ = events; },
         recursiveMonitor);
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

TEST_F(FileMonitorReconciliationTests, ShallowRecoveryRebuildsNestedSourcesForRecursiveMonitor)
{
   FilePath nested = blocked_.completeChildPath("R/sub/helper.R");
   ASSERT_FALSE(nested.getParent().ensureDirectory());
   ASSERT_FALSE(writeStringToFile(nested, "helper <- function() TRUE\n"));
   failScan_ = true;
   ASSERT_FALSE(system::scanFiles(FileInfo(root_), options_, &files_));

   failScan_ = false;
   ASSERT_FALSE(reconcile(blocked_, false, true));
   expectScanStatus(false);
   EXPECT_NE(files_.end(), monitor::findFile(files_.begin(), files_.end(), nested.getAbsolutePath()));
   EXPECT_NE(events_.end(), std::find_if(events_.begin(), events_.end(), [nested](const FileChangeEvent& event)
   {
      return event.type() == FileChangeEvent::FileAdded &&
             event.fileInfo().absolutePath() == nested.getAbsolutePath();
   }));
}

TEST_F(FileMonitorReconciliationTests, IgnoresStaleDirectoryEventAfterFileReplacement)
{
   ASSERT_FALSE(system::scanFiles(FileInfo(root_), options_, &files_));
   ASSERT_FALSE(blocked_.remove());
   ASSERT_FALSE(writeStringToFile(blocked_, "now a file\n"));
   ASSERT_FALSE(reconcile(root_));
   ASSERT_FALSE(reconcile(blocked_));
   EXPECT_TRUE(events_.empty());
   auto file = monitor::findFile(files_.begin(), files_.end(), blocked_.getAbsolutePath());
   ASSERT_NE(files_.end(), file);
   EXPECT_FALSE(file->isDirectory());
}

#ifndef _WIN32
TEST_F(FileMonitorReconciliationTests, IgnoresStaleDirectoryEventAfterSymlinkReplacement)
{
   FilePath target = root_.completeChildPath("target");
   ASSERT_FALSE(target.ensureDirectory());
   ASSERT_FALSE(writeStringToFile(target.completeChildPath("external.R"), "x <- 1\n"));
   ASSERT_FALSE(system::scanFiles(FileInfo(root_), options_, &files_));
   ASSERT_FALSE(blocked_.remove());
   ASSERT_EQ(0, ::symlink(target.getAbsolutePath().c_str(), blocked_.getAbsolutePath().c_str()));
   ASSERT_FALSE(reconcile(root_));
   ASSERT_FALSE(reconcile(blocked_));
   EXPECT_TRUE(events_.empty());
   EXPECT_EQ(files_.end(), monitor::findFile(
         files_.begin(), files_.end(), blocked_.completeChildPath("external.R").getAbsolutePath()));
   auto link = monitor::findFile(files_.begin(), files_.end(), blocked_.getAbsolutePath());
   ASSERT_NE(files_.end(), link);
   EXPECT_TRUE(link->isSymlink());
   EXPECT_EQ(0, files_.number_of_children(link));
}

TEST_F(FileMonitorReconciliationTests, AllowsSymlinkAtRegisteredMonitorRoot)
{
   FilePath link = root_.completeChildPath("root-link");
   ASSERT_EQ(0, ::symlink(blocked_.getAbsolutePath().c_str(), link.getAbsolutePath().c_str()));
   ASSERT_FALSE(system::scanFiles(FileInfo(link, true), options_, &files_));
   ASSERT_FALSE(writeStringToFile(source_, "x <- 1\n"));
   ASSERT_FALSE(reconcile(link));
   EXPECT_NE(files_.end(), monitor::findFile(
         files_.begin(), files_.end(), link.completeChildPath("source.R").getAbsolutePath()));
}
#endif

} // anonymous namespace
} // namespace core
} // namespace rstudio

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

#include <algorithm>

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

   Error reconcile(const FilePath& path,
                   bool recursive = true,
                   bool recursiveMonitor = false,
                   bool rescanPending = false)
   {
      events_.clear();
      return monitor::discoverAndProcessFileChanges(
         FileInfo(path.getAbsolutePath(), true),
         recursive,
         options_.filter,
         options_.onBeforeScanDir,
         &files_,
         [this](const std::vector<FileChangeEvent>& events) { events_ = events; },
         recursiveMonitor,
         rescanPending);
   }

   bool hasEvent(FileChangeEvent::Type type, const FilePath& path) const
   {
      return std::any_of(events_.begin(), events_.end(), [&](const FileChangeEvent& event)
      {
         return event.type() == type && event.fileInfo().absolutePath() == path.getAbsolutePath();
      });
   }

   void expectScanStatus(bool failed,
                         FileChangeEvent::Type type = FileChangeEvent::FileModified)
   {
      auto dir = monitor::findFile(files_.begin(), files_.end(), blocked_.getAbsolutePath());
      ASSERT_NE(files_.end(), dir);
      EXPECT_EQ(failed, dir->scanFailed());
      auto event = std::find_if(events_.begin(), events_.end(), [this](const FileChangeEvent& event)
      {
         return event.fileInfo().absolutePath() == blocked_.getAbsolutePath();
      });
      ASSERT_NE(events_.end(), event);
      EXPECT_EQ(type, event->type());
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

TEST_F(FileMonitorReconciliationTests, ShallowRescanIndexesNewlyIncludedDirectories)
{
   FilePath nested = blocked_.completeChildPath("sub/helper.R");
   ASSERT_FALSE(nested.getParent().ensureDirectory());
   ASSERT_FALSE(writeStringToFile(nested, "helper <- function() TRUE\n"));
   options_.filter = [this](const FileInfo& info)
   {
      return FilePath(info.absolutePath()) != blocked_;
   };
   ASSERT_FALSE(system::scanFiles(FileInfo(root_), options_, &files_));
   ASSERT_EQ(files_.end(), monitor::findFile(files_.begin(), files_.end(), blocked_.getAbsolutePath()));

   // Removing an ignore rule can first generate an ordinary shallow event
   // for the parent of an existing, populated directory.
   options_.filter.clear();
   ASSERT_FALSE(reconcile(root_, false, true));
   EXPECT_NE(files_.end(), monitor::findFile(files_.begin(), files_.end(), nested.getAbsolutePath()));
   EXPECT_NE(events_.end(), std::find_if(events_.begin(), events_.end(), [nested](const FileChangeEvent& event)
   {
      return event.type() == FileChangeEvent::FileAdded &&
             event.fileInfo().absolutePath() == nested.getAbsolutePath();
   }));
}

TEST_F(FileMonitorReconciliationTests, FindsPathsByDescendingFromRoot)
{
   FilePath nested = blocked_.completeChildPath("sub/helper.R");
   ASSERT_FALSE(nested.getParent().ensureDirectory());
   ASSERT_FALSE(writeStringToFile(nested, "helper <- function() TRUE\n"));
   ASSERT_FALSE(system::scanFiles(FileInfo(root_), options_, &files_));

   EXPECT_EQ(files_.begin(), monitor::findPath(&files_, root_.getAbsolutePath()));
   auto found = monitor::findPath(&files_, nested.getAbsolutePath());
   ASSERT_NE(files_.end(), found);
   EXPECT_EQ(nested.getAbsolutePath(), found->absolutePath());

   // Lookups must respect component boundaries and tolerate absent subtrees.
   EXPECT_EQ(files_.end(), monitor::findPath(&files_, blocked_.getAbsolutePath() + "2/sub"));
   EXPECT_EQ(files_.end(), monitor::findPath(&files_, root_.completeChildPath("missing/x").getAbsolutePath()));
   EXPECT_EQ(files_.end(), monitor::findPath(&files_, root_.getParent().getAbsolutePath()));
}

TEST_F(FileMonitorReconciliationTests, PendingRescanDefersAddedDirectoryScan)
{
   FilePath nested = blocked_.completeChildPath("sub/helper.R");
   ASSERT_FALSE(nested.getParent().ensureDirectory());
   ASSERT_FALSE(writeStringToFile(nested, "helper <- function() TRUE\n"));
   options_.filter = [this](const FileInfo& info)
   {
      return FilePath(info.absolutePath()) != blocked_;
   };
   ASSERT_FALSE(system::scanFiles(FileInfo(root_), options_, &files_));
   options_.filter.clear();

   // The directory itself is announced, but its contents wait for the rescan
   // instead of being scanned twice during an overload burst.
   ASSERT_FALSE(reconcile(root_, false, true, true));
   EXPECT_TRUE(hasEvent(FileChangeEvent::FileAdded, blocked_));
   EXPECT_FALSE(hasEvent(FileChangeEvent::FileAdded, nested));
   EXPECT_EQ(files_.end(), monitor::findFile(files_.begin(), files_.end(), nested.getAbsolutePath()));

   ASSERT_FALSE(reconcile(root_, true, true));
   EXPECT_TRUE(hasEvent(FileChangeEvent::FileAdded, nested));
   EXPECT_NE(files_.end(), monitor::findFile(files_.begin(), files_.end(), nested.getAbsolutePath()));
}

TEST_F(FileMonitorReconciliationTests, PendingRescanDefersFailedSubtreeRecovery)
{
   ASSERT_FALSE(writeStringToFile(source_, "x <- 1\n"));
   failScan_ = true;
   ASSERT_FALSE(system::scanFiles(FileInfo(root_), options_, &files_));
   failScan_ = false;

   // Recovery must not be advertised from a shallow scan; the pending rescan
   // rebuilds the subtree and publishes it in one pass.
   ASSERT_FALSE(reconcile(blocked_, false, true, true));
   EXPECT_TRUE(events_.empty());
   auto dir = monitor::findFile(files_.begin(), files_.end(), blocked_.getAbsolutePath());
   ASSERT_NE(files_.end(), dir);
   EXPECT_TRUE(dir->scanFailed());
   EXPECT_EQ(files_.end(), monitor::findFile(files_.begin(), files_.end(), source_.getAbsolutePath()));

   ASSERT_FALSE(reconcile(root_, true, true));
   expectScanStatus(false);
   EXPECT_TRUE(hasEvent(FileChangeEvent::FileAdded, source_));
}

TEST_F(FileMonitorReconciliationTests, FilteredDirectoryRecoversCompleteSubtree)
{
   FilePath nested = blocked_.completeChildPath("sub/helper.R");
   ASSERT_FALSE(nested.getParent().ensureDirectory());
   ASSERT_FALSE(writeStringToFile(nested, "helper <- function() TRUE\n"));
   ASSERT_FALSE(system::scanFiles(FileInfo(root_), options_, &files_));
   auto invalidate = [this]()
   {
      events_.clear();
      monitor::invalidateDirectory(
            FileInfo(blocked_.getAbsolutePath(), true),
            &files_,
            [this](const std::vector<FileChangeEvent>& events) { events_ = events; });
   };
   invalidate();
   auto dir = monitor::findFile(files_.begin(), files_.end(), blocked_.getAbsolutePath());
   ASSERT_NE(files_.end(), dir);
   EXPECT_FALSE(dir->scanFailed());
   EXPECT_TRUE(dir->scanFiltered());
   ASSERT_FALSE(events_.empty());
   EXPECT_EQ(FileChangeEvent::FileRemoved, events_.front().type());
   EXPECT_EQ(blocked_.getAbsolutePath(), events_.front().fileInfo().absolutePath());
   EXPECT_EQ(files_.end(), monitor::findFile(files_.begin(), files_.end(), nested.getAbsolutePath()));
   EXPECT_NE(events_.end(), std::find_if(events_.begin(), events_.end(), [nested](const FileChangeEvent& event)
   {
      return event.type() == FileChangeEvent::FileRemoved &&
             event.fileInfo().absolutePath() == nested.getAbsolutePath();
   }));
   invalidate();
   EXPECT_TRUE(events_.empty());

   // No file timestamps changed: the failed directory must still trigger a
   // recursive recovery and republish every discarded descendant.
   ASSERT_FALSE(reconcile(blocked_, false, true));
   expectScanStatus(false, FileChangeEvent::FileAdded);
   EXPECT_NE(files_.end(), monitor::findFile(files_.begin(), files_.end(), nested.getAbsolutePath()));
   EXPECT_NE(events_.end(), std::find_if(events_.begin(), events_.end(), [nested](const FileChangeEvent& event)
   {
      return event.type() == FileChangeEvent::FileAdded &&
             event.fileInfo().absolutePath() == nested.getAbsolutePath();
   }));
}

TEST_F(FileMonitorReconciliationTests, FilteredDirectoryPublishesFailedRecovery)
{
   ASSERT_FALSE(system::scanFiles(FileInfo(root_), options_, &files_));
   monitor::invalidateDirectory(
         FileInfo(blocked_),
         &files_,
         [this](const std::vector<FileChangeEvent>& events) { events_ = events; });
   failScan_ = true;
   EXPECT_TRUE(reconcile(blocked_, false, true));
   expectScanStatus(true, FileChangeEvent::FileAdded);
}

TEST_F(FileMonitorReconciliationTests, DeletedFilteredDirectoryIsNotRepublished)
{
   ASSERT_FALSE(system::scanFiles(FileInfo(root_), options_, &files_));
   monitor::invalidateDirectory(
         FileInfo(blocked_),
         &files_,
         [this](const std::vector<FileChangeEvent>& events) { events_ = events; });
   ASSERT_FALSE(blocked_.remove());
   EXPECT_TRUE(reconcile(blocked_, false, true));
   EXPECT_TRUE(events_.empty());
   EXPECT_EQ(files_.end(), monitor::findFile(files_.begin(), files_.end(), blocked_.getAbsolutePath()));
}

TEST_F(FileMonitorReconciliationTests, ParentScanRepublishesFilteredDirectory)
{
   ASSERT_FALSE(writeStringToFile(source_, "x <- 1\n"));
   for (bool recursive : {false, true})
   {
      SCOPED_TRACE(recursive);
      files_.clear();
      ASSERT_FALSE(system::scanFiles(FileInfo(root_), options_, &files_));
      monitor::invalidateDirectory(
            FileInfo(blocked_),
            &files_,
            [this](const std::vector<FileChangeEvent>& events) { events_ = events; });
      ASSERT_FALSE(reconcile(root_, recursive, true));
      expectScanStatus(false, FileChangeEvent::FileAdded);
      EXPECT_NE(files_.end(), monitor::findFile(files_.begin(), files_.end(), source_.getAbsolutePath()));
   }
}

TEST_F(FileMonitorReconciliationTests, DuplicateDirectoryAdditionPreservesScanFailure)
{
   ASSERT_FALSE(writeStringToFile(source_, "x <- 1\n"));
   failScan_ = true;
   ASSERT_FALSE(system::scanFiles(FileInfo(root_), options_, &files_));
   ASSERT_FALSE(monitor::processFileAdded(
         files_.begin(),
         FileChangeEvent(FileChangeEvent::FileAdded, FileInfo(blocked_)),
         true,
         options_.filter,
         options_.onBeforeScanDir,
         &files_,
         &events_));
   auto dir = monitor::findFile(files_.begin(), files_.end(), blocked_.getAbsolutePath());
   ASSERT_NE(files_.end(), dir);
   EXPECT_TRUE(dir->scanFailed());
   EXPECT_TRUE(events_.empty());
   EXPECT_EQ(files_.end(), monitor::findFile(files_.begin(), files_.end(), source_.getAbsolutePath()));
}

TEST_F(FileMonitorReconciliationTests, FailedDirectoryAdditionCanRecoverFromItsOwnEvent)
{
   FilePath nested = blocked_.completeChildPath("sub/helper.R");
   ASSERT_FALSE(nested.getParent().ensureDirectory());
   ASSERT_FALSE(writeStringToFile(nested, "helper <- function() TRUE\n"));
   options_.filter = [this](const FileInfo& info)
   {
      return FilePath(info.absolutePath()) != blocked_;
   };
   ASSERT_FALSE(system::scanFiles(FileInfo(root_), options_, &files_));
   options_.filter.clear();
   failScan_ = true;
   ASSERT_FALSE(reconcile(root_, false, true));
   auto dir = monitor::findFile(files_.begin(), files_.end(), blocked_.getAbsolutePath());
   ASSERT_NE(files_.end(), dir);
   EXPECT_TRUE(dir->scanFailed());
   ASSERT_EQ(1, events_.size());
   EXPECT_EQ(FileChangeEvent::FileAdded, events_.front().type());
   EXPECT_TRUE(events_.front().fileInfo().scanFailed());

   failScan_ = false;
   ASSERT_FALSE(reconcile(blocked_, false, true));
   expectScanStatus(false);
   EXPECT_NE(files_.end(), monitor::findFile(files_.begin(), files_.end(), nested.getAbsolutePath()));
}

TEST_F(FileMonitorReconciliationTests, ShallowFileReplacementIndexesNewDirectory)
{
   ASSERT_FALSE(blocked_.remove());
   ASSERT_FALSE(writeStringToFile(blocked_, "initial file\n"));
   ASSERT_FALSE(system::scanFiles(FileInfo(root_), options_, &files_));
   ASSERT_FALSE(blocked_.remove());
   FilePath nested = blocked_.completeChildPath("sub/helper.R");
   ASSERT_FALSE(nested.getParent().ensureDirectory());
   ASSERT_FALSE(writeStringToFile(nested, "helper <- function() TRUE\n"));
   ASSERT_FALSE(reconcile(root_, false, true));
   auto dir = monitor::findFile(files_.begin(), files_.end(), blocked_.getAbsolutePath());
   ASSERT_NE(files_.end(), dir);
   EXPECT_TRUE(dir->isDirectory());
   EXPECT_NE(files_.end(), monitor::findFile(files_.begin(), files_.end(), nested.getAbsolutePath()));
   ASSERT_GE(events_.size(), 2);
   EXPECT_EQ(FileChangeEvent::FileRemoved, events_[0].type());
   EXPECT_FALSE(events_[0].fileInfo().isDirectory());
   EXPECT_EQ(FileChangeEvent::FileAdded, events_[1].type());
   EXPECT_TRUE(events_[1].fileInfo().isDirectory());
}

TEST_F(FileMonitorReconciliationTests, ShallowDirectoryReplacementRemovesDescendants)
{
   ASSERT_FALSE(writeStringToFile(source_, "x <- 1\n"));
   ASSERT_FALSE(system::scanFiles(FileInfo(root_), options_, &files_));
   ASSERT_FALSE(blocked_.remove());
   ASSERT_FALSE(writeStringToFile(blocked_, "replacement file\n"));
   ASSERT_FALSE(reconcile(root_, false, true));
   auto file = monitor::findFile(files_.begin(), files_.end(), blocked_.getAbsolutePath());
   ASSERT_NE(files_.end(), file);
   EXPECT_FALSE(file->isDirectory());
   EXPECT_EQ(0, files_.number_of_children(file));
   EXPECT_NE(events_.end(), std::find_if(events_.begin(), events_.end(), [this](const FileChangeEvent& event)
   {
      return event.type() == FileChangeEvent::FileRemoved &&
             event.fileInfo().absolutePath() == source_.getAbsolutePath();
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

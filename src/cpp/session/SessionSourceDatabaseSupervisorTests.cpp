/*
 * SessionSourceDatabaseSupervisorTests.cpp
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

#include "SessionSourceDatabaseSupervisor.hpp"

#include <ctime>
#include <vector>

#include <gtest/gtest.h>

#include <core/FileLock.hpp>
#include <core/FileSerializer.hpp>

using namespace rstudio::core;
namespace supervisor = rstudio::session::source_database::supervisor;

namespace {

class SourceDatabaseSupervisorTest : public testing::Test
{
protected:
   void SetUp() override
   {
      FileLock::initialize();
      oldUseSymlinks_ = FileLock::useSymlinks();
      oldLoadBalanced_ = FileLock::isLoadBalanced();
      FileLock::setUseSymlinksForTesting(false);
      FileLock::setLoadBalancedForTesting(false);

      ASSERT_FALSE(FilePath::tempFilePath(temporary_));
      sourceRoot_ = temporary_.completePath("sources");
      originalDir_ = sourceRoot_.completePath("session-original");
      targetDir_ = sourceRoot_.completePath("session-new");
      ASSERT_FALSE(originalDir_.ensureDirectory());
      ASSERT_FALSE(writeStringToFile(originalDir_.completePath("document"), "metadata"));
      ASSERT_FALSE(writeStringToFile(originalDir_.completePath("document-contents"), "unsaved work"));
   }

   void TearDown() override
   {
      FileLock::setUseSymlinksForTesting(oldUseSymlinks_);
      FileLock::setLoadBalancedForTesting(oldLoadBalanced_);
      EXPECT_FALSE(temporary_.removeIfExists());
   }

   void expectDocuments(const FilePath& directory)
   {
      std::string contents;
      ASSERT_FALSE(readStringFromFile(directory.completePath("document"), &contents));
      EXPECT_EQ("metadata", contents);
      ASSERT_FALSE(readStringFromFile(directory.completePath("document-contents"), &contents));
      EXPECT_EQ("unsaved work", contents);
   }

   FilePath temporary_;
   FilePath sourceRoot_;
   FilePath originalDir_;
   FilePath targetDir_;
   bool oldUseSymlinks_;
   bool oldLoadBalanced_;
};

struct LockTypes
{
   FileLock::LockType owner;
   FileLock::LockType contender;
   bool symlinks;
};

std::vector<LockTypes> lockTypes()
{
   std::vector<LockTypes> types = {
      {FileLock::LOCKTYPE_ADVISORY, FileLock::LOCKTYPE_ADVISORY, false}
   };
#ifndef _WIN32
   types.push_back({FileLock::LOCKTYPE_ADVISORY, FileLock::LOCKTYPE_LINKBASED, false});
   types.push_back({FileLock::LOCKTYPE_LINKBASED, FileLock::LOCKTYPE_ADVISORY, false});
   types.push_back({FileLock::LOCKTYPE_LINKBASED, FileLock::LOCKTYPE_LINKBASED, false});
   types.push_back({FileLock::LOCKTYPE_LINKBASED, FileLock::LOCKTYPE_ADVISORY, true});
   types.push_back({FileLock::LOCKTYPE_LINKBASED, FileLock::LOCKTYPE_LINKBASED, true});
#endif
   return types;
}

class SourceDatabaseOwnershipTest : public SourceDatabaseSupervisorTest,
                                   public testing::WithParamInterface<LockTypes>
{
protected:
   void SetUp() override
   {
      SourceDatabaseSupervisorTest::SetUp();
      FileLock::setUseSymlinksForTesting(GetParam().symlinks);
      owner_ = FileLock::create(GetParam().owner);
      contender_ = FileLock::create(GetParam().contender);

      // A live advisory lock can have an old, empty file. A link-only probe
      // would consider it stale despite its owner still holding the lock.
      FilePath lockFile = originalDir_.completePath("lock_file");
      if (GetParam().owner == FileLock::LOCKTYPE_ADVISORY)
      {
         ASSERT_FALSE(lockFile.ensureFile());
         lockFile.setLastWriteTime(std::time(nullptr) - 3600);
      }
      ASSERT_FALSE(owner_->acquire(lockFile));
   }

   void TearDown() override
   {
      contender_.reset();
      owner_.reset();
      SourceDatabaseSupervisorTest::TearDown();
   }

   boost::shared_ptr<FileLock> owner_;
   boost::shared_ptr<FileLock> contender_;
};

TEST_P(SourceDatabaseOwnershipTest, LeavesLiveSessionsInPlace)
{
   bool reclaimed = true;
   ASSERT_FALSE(supervisor::detail::reclaimOrphanedSession(
      sourceRoot_,
      targetDir_,
      *contender_,
      &reclaimed));
   EXPECT_FALSE(reclaimed);
   EXPECT_FALSE(targetDir_.exists());
   expectDocuments(originalDir_);
   EXPECT_TRUE(owner_->isLocked(originalDir_.completePath("lock_file")));
   EXPECT_FALSE(writeStringToFile(originalDir_.completePath("new-document-contents"), "more work"));
}

TEST_P(SourceDatabaseOwnershipTest, RefusesToShareAnOwnedDirectory)
{
   Error error = supervisor::detail::acquireSessionDirLock(originalDir_, *contender_);
   EXPECT_TRUE(FileLock::isNoLockAvailable(error));
   EXPECT_NE(std::string::npos, error.getProperty("description").find("recovery data"));
   EXPECT_TRUE(contender_->lockFilePath().isEmpty());
   EXPECT_TRUE(owner_->isLocked(originalDir_.completePath("lock_file")));
   expectDocuments(originalDir_);
}

TEST_P(SourceDatabaseOwnershipTest, PreservesAbandonedDocuments)
{
   ASSERT_FALSE(owner_->release());
   bool reclaimed = false;
   Error error = supervisor::detail::reclaimOrphanedSession(
      sourceRoot_,
      targetDir_,
      *contender_,
      &reclaimed);
   EXPECT_FALSE(originalDir_.exists());
   expectDocuments(targetDir_);

   if (GetParam().symlinks && GetParam().contender == FileLock::LOCKTYPE_ADVISORY)
   {
      // A released link lock retains an absolute symlink into the original
      // directory. Moving it leaves a dangling link that advisory locking
      // cannot acquire. Preserve the documents and stop, rather than running
      // without a lock; a subsequent link-based session can recover them.
      EXPECT_TRUE(isFileNotFoundError(error));
      EXPECT_FALSE(reclaimed);
      EXPECT_TRUE(contender_->lockFilePath().isEmpty());

      auto recoveryLock = FileLock::create(FileLock::LOCKTYPE_LINKBASED);
      FilePath recoveryDir = sourceRoot_.completePath("session-recovery");
      ASSERT_FALSE(supervisor::detail::reclaimOrphanedSession(
         sourceRoot_,
         recoveryDir,
         *recoveryLock,
         &reclaimed));
      EXPECT_TRUE(reclaimed);
      expectDocuments(recoveryDir);
   }
   else
   {
      EXPECT_FALSE(error);
      EXPECT_TRUE(reclaimed);
      EXPECT_EQ(targetDir_.completePath("lock_file"), contender_->lockFilePath());
   }
}

TEST_P(SourceDatabaseOwnershipTest, AllowsAnIndependentSessionDirectory)
{
   ASSERT_FALSE(targetDir_.ensureDirectory());
   EXPECT_FALSE(supervisor::detail::acquireSessionDirLock(targetDir_, *contender_));
   EXPECT_EQ(targetDir_.completePath("lock_file"), contender_->lockFilePath());
   expectDocuments(originalDir_);
}

INSTANTIATE_TEST_SUITE_P(
   LockConfigurations,
   SourceDatabaseOwnershipTest,
   testing::ValuesIn(lockTypes()));

// Model an acquisition failure after the ownership probe, without changing
// filesystem permissions or relying on a scheduling race.
class FailingFileLock : public FileLock
{
public:
   explicit FailingFileLock(const Error& error) : error_(error), acquisitions(0) {}

   Error acquire(const FilePath&) override
   {
      ++acquisitions;
      return error_;
   }
   Error release() override { return Success(); }
   FilePath lockFilePath() const override { return FilePath(); }
   Error isLocked(const FilePath&, bool* pLocked) const override
   {
      *pLocked = false;
      return Success();
   }

   Error error_;
   int acquisitions;
};

TEST_F(SourceDatabaseSupervisorTest, PropagatesContentionAfterTheOwnershipProbe)
{
   FailingFileLock lock(FileLock::noLockAvailableError(originalDir_.completePath("lock_file")));
   EXPECT_TRUE(FileLock::isNoLockAvailable(supervisor::detail::acquireSessionDirLock(originalDir_, lock)));
   EXPECT_EQ(1, lock.acquisitions);
   expectDocuments(originalDir_);
}

TEST_F(SourceDatabaseSupervisorTest, PreservesAdoptedDocumentsOnAcquisitionFailure)
{
   FailingFileLock lock(FileLock::noLockAvailableError(targetDir_.completePath("lock_file")));
   bool reclaimed = false;
   Error error = supervisor::detail::reclaimOrphanedSession(sourceRoot_, targetDir_, lock, &reclaimed);
   EXPECT_TRUE(FileLock::isNoLockAvailable(error));
   EXPECT_FALSE(reclaimed);
   EXPECT_EQ(1, lock.acquisitions);
   expectDocuments(targetDir_);
}

TEST_F(SourceDatabaseSupervisorTest, AllowsUnsupportedLockingForAnUnownedDirectory)
{
   FailingFileLock lock(systemError(boost::system::errc::operation_not_supported, ERROR_LOCATION));
   EXPECT_FALSE(supervisor::detail::acquireSessionDirLock(originalDir_, lock));
   EXPECT_EQ(1, lock.acquisitions);
   expectDocuments(originalDir_);
}

TEST_F(SourceDatabaseSupervisorTest, PropagatesOtherAcquisitionErrors)
{
   Error error = systemError(boost::system::errc::io_error, ERROR_LOCATION);
   FailingFileLock lock(error);
   EXPECT_EQ(error, supervisor::detail::acquireSessionDirLock(originalDir_, lock));
   EXPECT_EQ(1, lock.acquisitions);
   expectDocuments(originalDir_);
}

TEST_F(SourceDatabaseSupervisorTest, LeavesUninspectableDirectoriesAlone)
{
   ASSERT_FALSE(originalDir_.completePath("lock_file").ensureDirectory());
   FailingFileLock lock{Success()};
   bool reclaimed = true;
   EXPECT_FALSE(supervisor::detail::reclaimOrphanedSession(
      sourceRoot_,
      targetDir_,
      lock,
      &reclaimed));
   EXPECT_FALSE(reclaimed);
   EXPECT_TRUE(supervisor::detail::acquireSessionDirLock(originalDir_, lock));
   EXPECT_EQ(0, lock.acquisitions);
   EXPECT_FALSE(targetDir_.exists());
   expectDocuments(originalDir_);
}

TEST_F(SourceDatabaseSupervisorTest, LeavesSuspendedSessionsAlone)
{
   ASSERT_FALSE(originalDir_.completePath("suspend_file").ensureFile());
   FailingFileLock lock{Success()};
   bool reclaimed = true;
   EXPECT_FALSE(supervisor::detail::reclaimOrphanedSession(
      sourceRoot_,
      targetDir_,
      lock,
      &reclaimed));
   EXPECT_FALSE(reclaimed);
   EXPECT_EQ(0, lock.acquisitions);
   expectDocuments(originalDir_);
}

TEST_F(SourceDatabaseSupervisorTest, LeavesRestartingSessionsAlone)
{
   ASSERT_FALSE(originalDir_.completePath("restart_file").ensureFile());
   FailingFileLock lock{Success()};
   bool reclaimed = true;
   EXPECT_FALSE(supervisor::detail::reclaimOrphanedSession(
      sourceRoot_,
      targetDir_,
      lock,
      &reclaimed));
   EXPECT_FALSE(reclaimed);
   EXPECT_EQ(0, lock.acquisitions);
   expectDocuments(originalDir_);
}

#ifndef _WIN32
TEST_F(SourceDatabaseSupervisorTest, LeavesAnEmptyLockPublicationAlone)
{
   ASSERT_FALSE(originalDir_.completePath("lock_file").ensureFile());
   FailingFileLock lock{Success()};
   bool reclaimed = true;
   EXPECT_FALSE(supervisor::detail::reclaimOrphanedSession(
      sourceRoot_,
      targetDir_,
      lock,
      &reclaimed));
   EXPECT_FALSE(reclaimed);
   EXPECT_TRUE(FileLock::isNoLockAvailable(supervisor::detail::acquireSessionDirLock(originalDir_, lock)));
   EXPECT_EQ(0, lock.acquisitions);
   expectDocuments(originalDir_);
}
#endif

} // anonymous namespace

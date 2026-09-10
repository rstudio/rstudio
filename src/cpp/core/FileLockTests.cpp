/*
 * FileLockTests.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
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

#include <core/FileLock.hpp>

#include <pthread.h>
#include <sys/stat.h>
#include <sys/wait.h>
#include <unistd.h>

#ifdef __linux__
# include <sys/syscall.h>
#endif

#include <atomic>
#include <ctime>
#include <string>
#include <vector>

#include <boost/function.hpp>
#include <boost/thread.hpp>
#include <boost/thread/barrier.hpp>

#include <gtest/gtest.h>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

#include <core/DateTime.hpp>
#include <core/FileSerializer.hpp>
#include <core/system/PosixSystem.hpp>
#include <core/system/System.hpp>

namespace rstudio {
namespace core {
namespace tests {

namespace {

FilePath claimPathFor(const FilePath& lockFilePath)
{
   return LinkBasedFileLock::claimPathForTesting(lockFilePath);
}

std::vector<FilePath> childrenOf(const FilePath& directory)
{
   std::vector<FilePath> children;
   Error error = directory.getChildren(children);
   if (error)
      LOG_ERROR(error);
   return children;
}

int countChildrenWithPrefix(const FilePath& directory, const std::string& prefix)
{
   int count = 0;
   for (const FilePath& child : childrenOf(directory))
   {
      if (child.getFilename().compare(0, prefix.size(), prefix) == 0)
         ++count;
   }
   return count;
}

class FileLockingTest : public ::testing::Test
{
protected:
   FileLockingTest()
      : oldTimeout_(0),
        oldGraceMultiplier_(0)
   {
   }

   void SetUp() override
   {
      FileLock::initialize();
      ASSERT_FALSE(FilePath::tempFilePath(root_));
      ASSERT_FALSE(root_.remove());
      ASSERT_FALSE(root_.ensureDirectory());
      lockFilePath_ = root_.completePath("lock");
      oldTimeout_ = FileLock::getTimeoutInterval();
      oldGraceMultiplier_ = FileLock::getLiveOwnerGraceMultiplier();
      FileLock::setLoadBalancedForTesting(false);
      FileLock::setUseSymlinksForTesting(false);
   }

   // The wall-clock start time of this test process, which the staleness
   // check compares against lock mtimes. Skips the test where unavailable.
   bool ownStartTime(std::time_t* pStartTime)
   {
      system::ProcessInfo info;
      info.pid = ::getpid();
      boost::posix_time::ptime created;
      Error error = info.creationTime(&created);
      if (error)
         return false;

      *pStartTime = static_cast<std::time_t>(
         date_time::secondsSinceEpoch(created));
      return true;
   }

   // Waits until this process has been alive for at least 'seconds', so a
   // test can back-date a lock mtime past a short timeout without also
   // pre-dating the owner's start (which reads as PID reuse).
   void waitUntilAliveFor(std::time_t startTime, std::time_t seconds)
   {
      while (::time(nullptr) - startTime < seconds)
         boost::this_thread::sleep_for(boost::chrono::milliseconds(100));
   }

   // Forks repeatedly while threads keep a lock registry busy. Each child
   // must complete 'childWork' rather than block on a registry mutex it
   // inherited in the locked state; alarm() turns such a deadlock into a
   // signal exit. The child work must not log (the logger's own locks are
   // not this test's concern).
   void expectChildrenSurviveForkDuringActivity(
      const boost::function<void(int)>& activity,
      const boost::function<bool()>& childWork)
   {
      const int threadCount = 4;
      std::atomic<bool> stop(false);
      boost::thread_group threads;
      for (int i = 0; i < threadCount; ++i)
      {
         threads.create_thread([&, i]()
         {
            while (!stop.load())
               activity(i);
         });
      }

      for (int i = 0; i < 40; ++i)
      {
         pid_t child = ::fork();
         EXPECT_NE(-1, child);
         if (child == -1)
            break;
         if (child == 0)
         {
            ::alarm(10);
            ::_exit(childWork() ? 0 : 1);
         }

         int status;
         EXPECT_EQ(child, ::waitpid(child, &status, 0));
         EXPECT_TRUE(WIFEXITED(status)) << "child " << i << " was killed";
         if (WIFEXITED(status))
            EXPECT_EQ(0, WEXITSTATUS(status)) << "child " << i;
      }

      stop.store(true);
      threads.join_all();
   }

   void TearDown() override
   {
      FileLock::cleanUp();
      FileLock::setTimeoutInterval(oldTimeout_);
      FileLock::setLiveOwnerGraceMultiplierForTesting(oldGraceMultiplier_);
      FileLock::setLoadBalancedForTesting(false);
      FileLock::setUseSymlinksForTesting(false);
      Error error = root_.removeIfExists();
      if (error)
         LOG_ERROR(error);
   }

   void expectChildSeesAdvisoryLock()
   {
      pid_t child = ::fork();
      ASSERT_NE(-1, child);
      if (child == 0)
      {
         AdvisoryFileLock childLock;
         if (!childLock.isLocked(lockFilePath_))
            ::_exit(1);
         if (!childLock.acquire(lockFilePath_))
            ::_exit(2);
         if (!childLock.isLocked(lockFilePath_))
            ::_exit(3);
         ::_exit(0);
      }

      int status;
      ASSERT_EQ(child, ::waitpid(child, &status, 0));
      ASSERT_TRUE(WIFEXITED(status));
      ASSERT_EQ(0, WEXITSTATUS(status));
   }

   FilePath root_;
   FilePath lockFilePath_;
   boost::posix_time::seconds oldTimeout_;
   int oldGraceMultiplier_;
};

// One byte over a pipe, for handing a turn between a test and its child.
void waitForByte(int descriptor)
{
   char byte = 0;
   while (::read(descriptor, &byte, 1) == -1 && errno == EINTR)
   {
   }
}

void sendByte(int descriptor)
{
   char byte = 1;
   while (::write(descriptor, &byte, 1) == -1 && errno == EINTR)
   {
   }
}

} // anonymous namespace

TEST_F(FileLockingTest, LinkBasedLockCanOnlyBeAcquiredOnce)
{
   LinkBasedFileLock first;
   LinkBasedFileLock second;

   ASSERT_FALSE(first.acquire(lockFilePath_));
   EXPECT_TRUE(FileLock::isNoLockAvailable(second.acquire(lockFilePath_)));

   ASSERT_FALSE(first.release());
   EXPECT_FALSE(second.isLocked(lockFilePath_));
   ASSERT_FALSE(second.acquire(lockFilePath_));
   EXPECT_TRUE(second.isLocked(lockFilePath_));
   EXPECT_FALSE(second.release());
}

TEST_F(FileLockingTest, OnlyOneThreadAcquiresLinkBasedFileLockAtATime)
{
   const std::size_t threadCount = 100;
   boost::barrier attempted(threadCount);
   std::atomic<int> lockCount(0);
   boost::thread_group threads;

   for (std::size_t i = 0; i < threadCount; ++i)
   {
      threads.create_thread([&]()
      {
         LinkBasedFileLock lock;
         Error error = lock.acquire(lockFilePath_);
         if (!error)
            ++lockCount;
         attempted.wait();
      });
   }

   threads.join_all();
   EXPECT_EQ(1, lockCount.load());
}

TEST_F(FileLockingTest, OnlyOneThreadReplacesReleasedLinkLockAtATime)
{
   LinkBasedFileLock released;
   ASSERT_FALSE(released.acquire(lockFilePath_));
   ASSERT_FALSE(released.release());

   const std::size_t threadCount = 100;
   boost::barrier attempted(threadCount);
   std::atomic<int> lockCount(0);
   boost::thread_group threads;

   for (std::size_t i = 0; i < threadCount; ++i)
   {
      threads.create_thread([&]()
      {
         LinkBasedFileLock lock;
         Error error = lock.acquire(lockFilePath_);
         if (!error)
            ++lockCount;
         attempted.wait();
      });
   }

   threads.join_all();
   EXPECT_EQ(1, lockCount.load());
}

TEST_F(FileLockingTest, OnlyOneProcessReplacesLegacyStaleLockAtATime)
{
   ASSERT_FALSE(writeStringToFile(lockFilePath_, "-1\n"));

   int startPipe[2];
   int resultPipe[2];
   int releasePipe[2];
   ASSERT_EQ(0, ::pipe(startPipe));
   ASSERT_EQ(0, ::pipe(resultPipe));
   ASSERT_EQ(0, ::pipe(releasePipe));

   const std::size_t processCount = 20;
   std::vector<pid_t> children;
   for (std::size_t i = 0; i < processCount; ++i)
   {
      pid_t child = ::fork();
      ASSERT_NE(-1, child);
      if (child == 0)
      {
         ::close(startPipe[1]);
         ::close(resultPipe[0]);
         ::close(releasePipe[1]);

         char signal;
         if (::read(startPipe[0], &signal, 1) != 1)
            ::_exit(1);

         LinkBasedFileLock lock;
         bool acquired = !lock.acquire(lockFilePath_);
         char result = acquired ? 1 : 0;
         if (::write(resultPipe[1], &result, 1) != 1)
            ::_exit(2);

         if (acquired)
         {
            if (::read(releasePipe[0], &signal, 1) != 1)
               ::_exit(3);
            if (lock.release())
               ::_exit(4);
         }
         ::_exit(0);
      }
      children.push_back(child);
   }

   ::close(startPipe[0]);
   ::close(resultPipe[1]);
   ::close(releasePipe[0]);

   int lockCount = 0;
   char signal = 1;
   for (std::size_t i = 0; i < processCount; ++i)
      ASSERT_EQ(1, ::write(startPipe[1], &signal, 1));
   ::close(startPipe[1]);

   for (std::size_t i = 0; i < processCount; ++i)
   {
      char result = 0;
      ASSERT_EQ(1, ::read(resultPipe[0], &result, 1));
      lockCount += result;
   }
   ::close(resultPipe[0]);

   for (int i = 0; i < lockCount; ++i)
      ASSERT_EQ(1, ::write(releasePipe[1], &signal, 1));
   ::close(releasePipe[1]);

   for (pid_t child : children)
   {
      int status;
      ASSERT_EQ(child, ::waitpid(child, &status, 0));
      ASSERT_TRUE(WIFEXITED(status));
      EXPECT_EQ(0, WEXITSTATUS(status));
   }
   EXPECT_EQ(1, lockCount);
}

TEST_F(FileLockingTest, ChildCleanupDoesNotClearParentLinkLock)
{
   LinkBasedFileLock lock;
   ASSERT_FALSE(lock.acquire(lockFilePath_));
   ASSERT_TRUE(lock.isLocked(lockFilePath_));

   pid_t child = ::fork();
   ASSERT_NE(-1, child);
   if (child == 0)
   {
      FileLock::cleanUp();
      LinkBasedFileLock probe;
      ::_exit(probe.isLocked(lockFilePath_) ? 0 : 1);
   }

   int status;
   ASSERT_EQ(child, ::waitpid(child, &status, 0));
   ASSERT_TRUE(WIFEXITED(status));
   ASSERT_EQ(0, WEXITSTATUS(status));
   EXPECT_TRUE(lock.isLocked(lockFilePath_));
   EXPECT_FALSE(lock.release());
}

TEST_F(FileLockingTest, AdvisoryLockSurvivesSameProcessOperations)
{
   AdvisoryFileLock lock;
   AdvisoryFileLock other;

   ASSERT_FALSE(lock.acquire(lockFilePath_));
   expectChildSeesAdvisoryLock();

   EXPECT_TRUE(FileLock::isNoLockAvailable(other.acquire(lockFilePath_)));
   expectChildSeesAdvisoryLock();

   EXPECT_TRUE(lock.isLocked(lockFilePath_));
   expectChildSeesAdvisoryLock();

   bool isLocked = false;
   EXPECT_FALSE(other.isLocked(lockFilePath_, &isLocked));
   EXPECT_TRUE(isLocked);
   expectChildSeesAdvisoryLock();

   EXPECT_TRUE(FileLock::isNoLockAvailable(lock.acquire(lockFilePath_)));
   expectChildSeesAdvisoryLock();

   ASSERT_FALSE(lock.release());
   EXPECT_TRUE(lockFilePath_.exists());
   EXPECT_FALSE(other.isLocked(lockFilePath_));
   ASSERT_FALSE(other.acquire(lockFilePath_));
   EXPECT_FALSE(other.release());
}

TEST_F(FileLockingTest, AdvisoryMissingLockFileIsNotLocked)
{
   // Nothing can hold a lock file that does not exist. The probe must say so
   // rather than report an error, which the bool overload reads as "locked"
   // and callers then refuse to proceed on.
   AdvisoryFileLock lock;
   bool isLocked = true;
   EXPECT_FALSE(lock.isLocked(lockFilePath_, &isLocked));
   EXPECT_FALSE(isLocked);
   EXPECT_FALSE(lock.isLocked(lockFilePath_));
}

TEST_F(FileLockingTest, AdvisoryReadOnlyLockFileCanBeProbed)
{
   if (::geteuid() == 0)
      GTEST_SKIP() << "root bypasses file permissions";

   // A lock file this process may not write (root-owned after a sudo run,
   // say) is still probed rather than reported as permanently held.
   ASSERT_FALSE(lockFilePath_.ensureFile());
   ASSERT_EQ(0, ::chmod(lockFilePath_.getAbsolutePath().c_str(), 0444));

   AdvisoryFileLock lock;
   bool isLocked = true;
   EXPECT_FALSE(lock.isLocked(lockFilePath_, &isLocked));
   EXPECT_FALSE(isLocked);

   // ... and it does see a holder: the child locks the (briefly writable)
   // file, makes it read-only again, then holds it until told to exit.
   int lockReady[2];
   int parentDone[2];
   ASSERT_EQ(0, ::pipe(lockReady));
   ASSERT_EQ(0, ::pipe(parentDone));

   pid_t child = ::fork();
   ASSERT_NE(-1, child);
   if (child == 0)
   {
      ::alarm(10);
      AdvisoryFileLock childLock;
      if (::chmod(lockFilePath_.getAbsolutePath().c_str(), 0644) == -1)
         ::_exit(1);
      if (childLock.acquire(lockFilePath_))
         ::_exit(2);
      if (::chmod(lockFilePath_.getAbsolutePath().c_str(), 0444) == -1)
         ::_exit(3);
      sendByte(lockReady[1]);
      waitForByte(parentDone[0]);
      ::_exit(0);
   }

   waitForByte(lockReady[0]);
   isLocked = false;
   EXPECT_FALSE(lock.isLocked(lockFilePath_, &isLocked));
   EXPECT_TRUE(isLocked);
   sendByte(parentDone[1]);

   int status;
   ASSERT_EQ(child, ::waitpid(child, &status, 0));
   ASSERT_TRUE(WIFEXITED(status));
   EXPECT_EQ(0, WEXITSTATUS(status));
   ::close(lockReady[0]);
   ::close(lockReady[1]);
   ::close(parentDone[0]);
   ::close(parentDone[1]);

   isLocked = true;
   EXPECT_FALSE(lock.isLocked(lockFilePath_, &isLocked));
   EXPECT_FALSE(isLocked);
}

TEST_F(FileLockingTest, OnlyOneThreadAcquiresAdvisoryFileLockAtATime)
{
   const std::size_t threadCount = 100;
   boost::barrier attempted(threadCount + 1);
   boost::barrier releaseLocks(threadCount + 1);
   std::atomic<int> lockCount(0);
   boost::thread_group threads;

   for (std::size_t i = 0; i < threadCount; ++i)
   {
      threads.create_thread([&]()
      {
         AdvisoryFileLock lock;
         Error error = lock.acquire(lockFilePath_);
         if (!error)
            ++lockCount;
         attempted.wait();
         releaseLocks.wait();
      });
   }

   attempted.wait();
   EXPECT_EQ(1, lockCount.load());
   expectChildSeesAdvisoryLock();
   releaseLocks.wait();
   threads.join_all();
}

TEST_F(FileLockingTest, AdvisoryReleaseKeepsStableInode)
{
   AdvisoryFileLock first;
   AdvisoryFileLock second;
   ASSERT_FALSE(first.acquire(lockFilePath_));

   struct stat before;
   ASSERT_EQ(0, ::stat(lockFilePath_.getAbsolutePath().c_str(), &before));
   ASSERT_FALSE(first.release());
   ASSERT_FALSE(second.acquire(lockFilePath_));

   struct stat after;
   ASSERT_EQ(0, ::stat(lockFilePath_.getAbsolutePath().c_str(), &after));
   EXPECT_EQ(before.st_dev, after.st_dev);
   EXPECT_EQ(before.st_ino, after.st_ino);
   EXPECT_FALSE(second.release());
}

TEST_F(FileLockingTest, AdvisoryLockSurvivesSymlinkAliasOperations)
{
   ASSERT_FALSE(lockFilePath_.ensureFile());
   FilePath aliasPath = root_.completePath("alias-lock");
   ASSERT_EQ(
      0,
      ::symlink(
         lockFilePath_.getAbsolutePath().c_str(),
         aliasPath.getAbsolutePath().c_str()));

   AdvisoryFileLock lock;
   AdvisoryFileLock other;
   ASSERT_FALSE(lock.acquire(lockFilePath_));

   bool isLocked = false;
   EXPECT_FALSE(other.isLocked(aliasPath, &isLocked));
   EXPECT_TRUE(isLocked);
   expectChildSeesAdvisoryLock();

   EXPECT_TRUE(FileLock::isNoLockAvailable(other.acquire(aliasPath)));
   expectChildSeesAdvisoryLock();
   EXPECT_FALSE(lock.release());
}

TEST_F(FileLockingTest, LiveLinkOwnerDoesNotExpire)
{
   // A stalled owner's lock ages past the timeout without being refreshed,
   // but its mtime still post-dates the owner's start: not stale.
   std::time_t startTime = 0;
   if (!ownStartTime(&startTime))
      GTEST_SKIP() << "process start time unavailable on this platform";

   FileLock::setTimeoutInterval(boost::posix_time::seconds(1));
   waitUntilAliveFor(startTime, 3);

   LinkBasedFileLock first;
   LinkBasedFileLock second;
   ASSERT_FALSE(first.acquire(lockFilePath_));
   lockFilePath_.setLastWriteTime(::time(nullptr) - 2);

   EXPECT_TRUE(first.isLocked(lockFilePath_));
   EXPECT_TRUE(FileLock::isNoLockAvailable(second.acquire(lockFilePath_)));
   EXPECT_FALSE(first.release());
}

TEST_F(FileLockingTest, StalledLiveOwnerLosesLockAfterGrace)
{
   // A live local owner that has stopped refreshing keeps its lock past the
   // timeout, but only for the configured number of timeout intervals: the
   // PID check cannot tell it from an unrelated process in another PID
   // namespace, which must not pin an orphaned lock forever.
   std::time_t startTime = 0;
   if (!ownStartTime(&startTime))
      GTEST_SKIP() << "process start time unavailable on this platform";

   FileLock::setTimeoutInterval(boost::posix_time::seconds(1));
   FileLock::setLiveOwnerGraceMultiplierForTesting(3);

   LinkBasedFileLock first;
   LinkBasedFileLock second;
   ASSERT_FALSE(first.acquire(lockFilePath_));

   lockFilePath_.setLastWriteTime(::time(nullptr) - 2);
   EXPECT_TRUE(first.isLocked(lockFilePath_));
   EXPECT_TRUE(FileLock::isNoLockAvailable(second.acquire(lockFilePath_)));

   lockFilePath_.setLastWriteTime(::time(nullptr) - 4);
   EXPECT_FALSE(first.isLocked(lockFilePath_));
   EXPECT_FALSE(second.acquire(lockFilePath_));
   EXPECT_FALSE(second.release());
   EXPECT_FALSE(first.release());
}

TEST_F(FileLockingTest, ReusedPidDoesNotPinOrphanedLock)
{
   // The lock names a live PID (ours), but was last written before that
   // process started: the original owner is gone and the PID was reused.
   std::time_t startTime = 0;
   if (!ownStartTime(&startTime))
      GTEST_SKIP() << "process start time unavailable on this platform";

   ASSERT_FALSE(writeStringToFile(lockFilePath_, std::to_string(::getpid()) + "\n"));
   lockFilePath_.setLastWriteTime(
      startTime - FileLock::getTimeoutInterval().total_seconds() - 5);

   EXPECT_TRUE(LinkBasedFileLock::isLockFileStale(lockFilePath_));

   LinkBasedFileLock lock;
   EXPECT_FALSE(lock.acquire(lockFilePath_));
   EXPECT_TRUE(lock.isLocked(lockFilePath_));
   EXPECT_FALSE(lock.release());
}

TEST_F(FileLockingTest, ReusedPidVerdictWaitsForTimeout)
{
   // The lock names a live PID that started after the last write, but the
   // lock has not aged out yet: a young owner is still refreshing, so the
   // start-time comparison (which a wall-clock step can upset) is not
   // consulted until the age check has already failed.
   std::time_t startTime = 0;
   if (!ownStartTime(&startTime))
      GTEST_SKIP() << "process start time unavailable on this platform";

   FileLock::setTimeoutInterval(boost::posix_time::seconds(3600));
   ASSERT_FALSE(writeStringToFile(lockFilePath_, std::to_string(::getpid()) + "\n"));
   lockFilePath_.setLastWriteTime(startTime - 100);

   EXPECT_FALSE(LinkBasedFileLock::isLockFileStale(lockFilePath_));
   LinkBasedFileLock lock;
   EXPECT_TRUE(FileLock::isNoLockAvailable(lock.acquire(lockFilePath_)));
   EXPECT_TRUE(lock.isLocked(lockFilePath_));
}

TEST_F(FileLockingTest, ExpiredLinkOwnerCannotReleaseReplacement)
{
   FileLock::setTimeoutInterval(boost::posix_time::seconds(1));
   FileLock::setLoadBalancedForTesting(true);

   LinkBasedFileLock expired;
   LinkBasedFileLock replacement;
   LinkBasedFileLock contender;
   ASSERT_FALSE(expired.acquire(lockFilePath_));
   lockFilePath_.setLastWriteTime(::time(nullptr) - 10);
   ASSERT_FALSE(replacement.acquire(lockFilePath_));

   // The expired owner's release must leave the replacement's files alone.
   EXPECT_FALSE(expired.release());
   EXPECT_TRUE(lockFilePath_.exists());
   EXPECT_EQ(1, countChildrenWithPrefix(root_, ".rstudio-lock-owner-41c29-"));
   EXPECT_TRUE(replacement.isLocked(lockFilePath_));
   EXPECT_TRUE(FileLock::isNoLockAvailable(contender.acquire(lockFilePath_)));
   EXPECT_FALSE(replacement.release());
}

TEST_F(FileLockingTest, LinkMetadataRemainsBackwardCompatible)
{
   LinkBasedFileLock lock;
   ASSERT_FALSE(lock.acquire(lockFilePath_));

   std::string contents;
   ASSERT_FALSE(readStringFromFile(lockFilePath_, &contents));
   EXPECT_EQ(std::to_string(::getpid()) + "\n", contents);

   // Release removes the lock's own entries; a link an older RStudio (or
   // anything else) still holds to the inode must read as released.
   FilePath keptLink = root_.completePath("kept-link");
   ASSERT_EQ(
      0,
      ::link(
         lockFilePath_.getAbsolutePath().c_str(),
         keptLink.getAbsolutePath().c_str()));

   ASSERT_FALSE(lock.release());
   EXPECT_FALSE(lockFilePath_.exists());
   ASSERT_FALSE(readStringFromFile(keptLink, &contents));
   EXPECT_EQ("-1\n", contents);
}

TEST_F(FileLockingTest, ReleaseRemovesLockAndOwnerFiles)
{
   LinkBasedFileLock lock;
   ASSERT_FALSE(lock.acquire(lockFilePath_));
   EXPECT_TRUE(lockFilePath_.exists());
   EXPECT_EQ(1, countChildrenWithPrefix(root_, ".rstudio-lock-owner-41c29-"));

   ASSERT_FALSE(lock.release());
   EXPECT_FALSE(lockFilePath_.exists());
   EXPECT_TRUE(childrenOf(root_).empty());
}

TEST_F(FileLockingTest, SymlinkReleaseRemovesLockAndOwnerFiles)
{
   FileLock::setUseSymlinksForTesting(true);

   LinkBasedFileLock lock;
   ASSERT_FALSE(lock.acquire(lockFilePath_));
   EXPECT_TRUE(lockFilePath_.isSymlink());

   ASSERT_FALSE(lock.release());
   EXPECT_TRUE(childrenOf(root_).empty());
}

TEST_F(FileLockingTest, CleanUpRemovesRegisteredLockFiles)
{
   LinkBasedFileLock lock;
   ASSERT_FALSE(lock.acquire(lockFilePath_));

   FileLock::cleanUp();
   EXPECT_TRUE(childrenOf(root_).empty());

   // the object's own release afterwards is harmless
   EXPECT_FALSE(lock.release());
}

TEST_F(FileLockingTest, OrphanedOwnerFilesAreSwept)
{
   // Owners left behind when something else deleted their public lock path
   // (a released owner and a legacy proxy of a dead process) are reclaimed by
   // the next acquisition in the directory; a live claim is not.
   FilePath releasedOwner =
      root_.completePath(".rstudio-lock-owner-41c29-released");
   FilePath deadLegacyOwner =
      root_.completePath(".rstudio-lock-41c29-host-99999999-thread");
   FilePath liveClaim =
      root_.completePath(".rstudio-lock-claim-41c29-live");
   ASSERT_FALSE(writeStringToFile(releasedOwner, "-1\n"));
   ASSERT_FALSE(writeStringToFile(deadLegacyOwner, "99999999\n"));
   ASSERT_FALSE(writeStringToFile(liveClaim, std::to_string(::getpid()) + "\n"));

   LinkBasedFileLock lock;
   ASSERT_FALSE(lock.acquire(lockFilePath_));
   EXPECT_FALSE(releasedOwner.exists());
   EXPECT_FALSE(deadLegacyOwner.exists());
   EXPECT_TRUE(liveClaim.exists());
   EXPECT_TRUE(lockFilePath_.exists());
   EXPECT_EQ(1, countChildrenWithPrefix(root_, ".rstudio-lock-owner-41c29-"));

   ASSERT_FALSE(lock.release());
   ASSERT_FALSE(liveClaim.remove());
   EXPECT_TRUE(childrenOf(root_).empty());
}

TEST_F(FileLockingTest, AbandonedTempFilesAreSwept)
{
   // A contender killed between renaming a stale entry aside and unlinking
   // it leaves the temp file behind. It is swept once that contender is
   // gone; one whose contender is still alive may be mid-removal and stays.
   FilePath abandoned = root_.completePath(
      ".rstudio-lock-tmp-41c29-99999999-abandoned");
   FilePath inFlight = root_.completePath(
      ".rstudio-lock-tmp-41c29-" + std::to_string(::getpid()) + "-inflight");
   ASSERT_FALSE(writeStringToFile(abandoned, "12345\n"));
   ASSERT_FALSE(writeStringToFile(inFlight, "12345\n"));

   LinkBasedFileLock lock;
   ASSERT_FALSE(lock.acquire(lockFilePath_));
   EXPECT_FALSE(abandoned.exists());
   EXPECT_TRUE(inFlight.exists());
   EXPECT_FALSE(lock.release());
}

TEST_F(FileLockingTest, ExternallyDeletedLockPathLeavesNoPermanentLitter)
{
   // A crashed owner leaves its lock and owner file behind. A caller that
   // then removes the public path (as the notebook cache does) orphans the
   // owner; the next acquisition in the directory sweeps it.
   pid_t child = ::fork();
   ASSERT_NE(-1, child);
   if (child == 0)
   {
      LinkBasedFileLock lock;
      ::_exit(lock.acquire(lockFilePath_) ? 1 : 0);
   }

   int status;
   ASSERT_EQ(child, ::waitpid(child, &status, 0));
   ASSERT_TRUE(WIFEXITED(status));
   ASSERT_EQ(0, WEXITSTATUS(status));
   ASSERT_TRUE(lockFilePath_.exists());
   EXPECT_FALSE(LinkBasedFileLock().isLocked(lockFilePath_));

   ASSERT_FALSE(lockFilePath_.remove());
   EXPECT_EQ(1, countChildrenWithPrefix(root_, ".rstudio-lock-owner-41c29-"));

   LinkBasedFileLock next;
   ASSERT_FALSE(next.acquire(root_.completePath("other-lock")));
   EXPECT_EQ(1, countChildrenWithPrefix(root_, ".rstudio-lock-owner-41c29-"));
   ASSERT_FALSE(next.release());
   EXPECT_TRUE(childrenOf(root_).empty());
}

TEST_F(FileLockingTest, LiveClaimBlocksStaleLockTakeover)
{
   ASSERT_FALSE(writeStringToFile(lockFilePath_, "-1\n"));
   FilePath claim = claimPathFor(lockFilePath_);
   ASSERT_FALSE(writeStringToFile(claim, std::to_string(::getpid()) + "\n"));

   LinkBasedFileLock lock;
   EXPECT_TRUE(FileLock::isNoLockAvailable(lock.acquire(lockFilePath_)));
   EXPECT_TRUE(claim.exists());
   EXPECT_TRUE(lockFilePath_.exists());

   ASSERT_FALSE(claim.remove());
   EXPECT_FALSE(lock.acquire(lockFilePath_));
   EXPECT_FALSE(claim.exists());
   EXPECT_FALSE(lock.release());
}

TEST_F(FileLockingTest, ReleaseLeavesFilesWhileContenderHoldsClaim)
{
   // A contender holding the claim is mid-takeover of this (expired) lock
   // and owns the files; release must not move or remove anything, only
   // mark the inode released.
   LinkBasedFileLock lock;
   ASSERT_FALSE(lock.acquire(lockFilePath_));
   FilePath claim = claimPathFor(lockFilePath_);
   ASSERT_FALSE(writeStringToFile(claim, std::to_string(::getpid()) + "\n"));

   ASSERT_FALSE(lock.release());
   EXPECT_TRUE(lockFilePath_.exists());
   EXPECT_TRUE(claim.exists());
   EXPECT_EQ(1, countChildrenWithPrefix(root_, ".rstudio-lock-owner-41c29-"));

   std::string contents;
   ASSERT_FALSE(readStringFromFile(lockFilePath_, &contents));
   EXPECT_EQ("-1\n", contents);

   // once the claim is gone, the released lock is reclaimed and cleaned up
   ASSERT_FALSE(claim.remove());
   LinkBasedFileLock next;
   ASSERT_FALSE(next.acquire(lockFilePath_));
   ASSERT_FALSE(next.release());
   EXPECT_TRUE(childrenOf(root_).empty());
}

TEST_F(FileLockingTest, AdvisoryRegistryUsableInChildAfterFork)
{
   FilePath probedPath = root_.completePath("probed");
   ASSERT_FALSE(probedPath.ensureFile());

   expectChildrenSurviveForkDuringActivity(
      [&](int)
      {
         AdvisoryFileLock probe;
         bool isLocked = false;
         probe.isLocked(probedPath, &isLocked);
      },
      [&]()
      {
         // only the probe's completion matters: a parent thread's probe may
         // momentarily hold the fcntl lock, so the answer itself may vary
         AdvisoryFileLock probe;
         bool isLocked = true;
         return !probe.isLocked(probedPath, &isLocked);
      });
}

TEST_F(FileLockingTest, InheritedAdvisoryLockObjectIsInertInChild)
{
   // A lock object copied into a child by fork() never held anything there.
   // Releasing it must not unlock or close the inode (which would drop a
   // lock the child has since taken) nor disturb the child's registry. The
   // parent verifies the child's kernel lock from outside the child's
   // registry: its own acquisition must fail while the child holds the lock.
   AdvisoryFileLock inherited;
   ASSERT_FALSE(inherited.acquire(lockFilePath_));

   int toChild[2];
   int toParent[2];
   ASSERT_EQ(0, ::pipe(toChild));
   ASSERT_EQ(0, ::pipe(toParent));

   pid_t child = ::fork();
   ASSERT_NE(-1, child);
   if (child == 0)
   {
      ::close(toChild[1]);
      ::close(toParent[0]);
      char signal;
      if (::read(toChild[0], &signal, 1) != 1)
         ::_exit(1);

      AdvisoryFileLock held;
      if (held.acquire(lockFilePath_))
         ::_exit(2);
      if (inherited.release())
         ::_exit(3);

      // still held in this process, per the registry
      AdvisoryFileLock probe;
      if (!FileLock::isNoLockAvailable(probe.acquire(lockFilePath_)))
         ::_exit(4);

      // let the parent verify the kernel lock, then release
      if (::write(toParent[1], "h", 1) != 1)
         ::_exit(5);
      if (::read(toChild[0], &signal, 1) != 1)
         ::_exit(6);
      if (held.release())
         ::_exit(7);
      if (::write(toParent[1], "r", 1) != 1)
         ::_exit(8);
      ::_exit(0);
   }

   ::close(toChild[0]);
   ::close(toParent[1]);

   ASSERT_FALSE(inherited.release());
   ASSERT_EQ(1, ::write(toChild[1], "x", 1));

   char signal = 0;
   ASSERT_EQ(1, ::read(toParent[0], &signal, 1));
   ASSERT_EQ('h', signal);
   AdvisoryFileLock parentLock;
   EXPECT_TRUE(FileLock::isNoLockAvailable(parentLock.acquire(lockFilePath_)));

   ASSERT_EQ(1, ::write(toChild[1], "x", 1));
   ASSERT_EQ(1, ::read(toParent[0], &signal, 1));
   ASSERT_EQ('r', signal);
   EXPECT_FALSE(parentLock.acquire(lockFilePath_));
   EXPECT_FALSE(parentLock.release());

   ::close(toChild[1]);
   ::close(toParent[0]);

   int status;
   ASSERT_EQ(child, ::waitpid(child, &status, 0));
   ASSERT_TRUE(WIFEXITED(status));
   EXPECT_EQ(0, WEXITSTATUS(status));
}

TEST_F(FileLockingTest, LinkRegistryUsableInChildAfterFork)
{
   expectChildrenSurviveForkDuringActivity(
      [&](int index)
      {
         LinkBasedFileLock lock;
         if (!lock.acquire(root_.completePath("lock-" + std::to_string(index))))
            lock.release();
      },
      [&]()
      {
         // registry access without logging: the child's registry is empty
         FileLock::refresh();
         return true;
      });
}

TEST_F(FileLockingTest, StaleClaimIsReplacedDuringTakeover)
{
   ASSERT_FALSE(writeStringToFile(lockFilePath_, "-1\n"));
   FilePath claim = claimPathFor(lockFilePath_);
   ASSERT_FALSE(writeStringToFile(claim, "99999999\n"));

   LinkBasedFileLock lock;
   EXPECT_FALSE(lock.acquire(lockFilePath_));
   EXPECT_FALSE(claim.exists());
   EXPECT_TRUE(lock.isLocked(lockFilePath_));
   EXPECT_FALSE(lock.release());
}

TEST_F(FileLockingTest, LockFilesAreReadableByOtherUsers)
{
   mode_t mask = ::umask(0);
   ::umask(mask);
   mode_t expected = 0644 & ~mask;

   LinkBasedFileLock lock;
   ASSERT_FALSE(lock.acquire(lockFilePath_));

   for (const FilePath& child : childrenOf(root_))
   {
      struct stat info;
      ASSERT_EQ(0, ::stat(child.getAbsolutePath().c_str(), &info));
      EXPECT_EQ(expected, info.st_mode & 0777) << child.getAbsolutePath();
   }
   EXPECT_FALSE(lock.release());
}

TEST_F(FileLockingTest, UnreadableLinkLockExpiresByTimeout)
{
   if (::geteuid() == 0)
      GTEST_SKIP() << "root bypasses file permissions";

   // Another user's lock (unreadable to us) is held until it ages out, then
   // may be taken over; the read failure is not an inspection error.
   ASSERT_FALSE(writeStringToFile(lockFilePath_, "99999999\n"));
   ASSERT_EQ(0, ::chmod(lockFilePath_.getAbsolutePath().c_str(), 0));

   LinkBasedFileLock lock;
   bool isLocked = false;
   EXPECT_FALSE(lock.isLocked(lockFilePath_, &isLocked));
   EXPECT_TRUE(isLocked);
   EXPECT_TRUE(FileLock::isNoLockAvailable(lock.acquire(lockFilePath_)));

   lockFilePath_.setLastWriteTime(
      ::time(nullptr) - FileLock::getTimeoutInterval().total_seconds() - 1);
   EXPECT_FALSE(lock.isLocked(lockFilePath_, &isLocked));
   EXPECT_FALSE(isLocked);
   EXPECT_FALSE(lock.acquire(lockFilePath_));
   EXPECT_FALSE(lock.release());
}

TEST_F(FileLockingTest, BoolIsLockedFailsClosedOnInspectionError)
{
   // A path that cannot be inspected reads as locked through both overloads:
   // callers of the bool overload remove or adopt what is "not locked", so a
   // transient error must not hand them another session's live lock. The
   // Error overload additionally reports the failure.
   FilePath directoryAsLock = root_.completePath("dir-lock");
   ASSERT_FALSE(directoryAsLock.ensureDirectory());

   AdvisoryFileLock advisory;
   bool isLocked = false;
   EXPECT_TRUE(advisory.isLocked(directoryAsLock, &isLocked));
   EXPECT_TRUE(isLocked);
   EXPECT_TRUE(advisory.isLocked(directoryAsLock));

   FilePath invalidPath = root_.completePath(std::string(300, 'x'));
   LinkBasedFileLock linkBased;
   isLocked = false;
   EXPECT_TRUE(linkBased.isLocked(invalidPath, &isLocked));
   EXPECT_TRUE(isLocked);
   EXPECT_TRUE(linkBased.isLocked(invalidPath));
}

TEST_F(FileLockingTest, AdvisoryLockThroughDanglingSymlinkSurvivesProbe)
{
   // The same-process registry key must not depend on whether the lock path
   // resolves yet: acquiring through a dangling symlink creates the target,
   // and a later probe through the same path must find the registration
   // rather than open the file and drop this process's lock.
   FilePath target = root_.completePath("real-lock");
   ASSERT_EQ(
      0,
      ::symlink(
         target.getAbsolutePath().c_str(),
         lockFilePath_.getAbsolutePath().c_str()));
   ASSERT_FALSE(target.exists());

   AdvisoryFileLock lock;
   AdvisoryFileLock other;
   ASSERT_FALSE(lock.acquire(lockFilePath_));
   ASSERT_TRUE(target.exists());

   bool isLocked = false;
   EXPECT_FALSE(other.isLocked(lockFilePath_, &isLocked));
   EXPECT_TRUE(isLocked);
   EXPECT_FALSE(other.isLocked(target, &isLocked));
   EXPECT_TRUE(isLocked);
   expectChildSeesAdvisoryLock();

   EXPECT_TRUE(FileLock::isNoLockAvailable(other.acquire(target)));
   expectChildSeesAdvisoryLock();
   EXPECT_FALSE(lock.release());
}

TEST_F(FileLockingTest, AdvisoryLockSurvivesHardLinkAliasProbe)
{
   // fcntl locks belong to the inode, so a hard link is the same lock; the
   // same-process registry must recognize it under the alias rather than
   // open and close the inode (which would drop this process's lock).
   AdvisoryFileLock lock;
   AdvisoryFileLock other;
   ASSERT_FALSE(lock.acquire(lockFilePath_));

   FilePath alias = root_.completePath("alias-link");
   ASSERT_EQ(
      0,
      ::link(
         lockFilePath_.getAbsolutePath().c_str(),
         alias.getAbsolutePath().c_str()));

   bool isLocked = false;
   EXPECT_FALSE(other.isLocked(alias, &isLocked));
   EXPECT_TRUE(isLocked);
   expectChildSeesAdvisoryLock();

   EXPECT_TRUE(FileLock::isNoLockAvailable(other.acquire(alias)));
   expectChildSeesAdvisoryLock();

   EXPECT_FALSE(lock.release());
   EXPECT_FALSE(other.acquire(alias));
   EXPECT_TRUE(FileLock::isNoLockAvailable(lock.acquire(lockFilePath_)));
   EXPECT_FALSE(other.release());
}

TEST_F(FileLockingTest, ZombieOwnerLinkLockIsStale)
{
   // An owner that has exited but not been reaped still answers kill(0);
   // its lock must nonetheless be reclaimable without waiting for a reaper.
   pid_t child = ::fork();
   ASSERT_NE(-1, child);
   if (child == 0)
   {
      LinkBasedFileLock lock;
      ::_exit(lock.acquire(lockFilePath_) ? 1 : 0);
   }

   // wait for the child to exit without reaping it
   for (int attempt = 0; attempt < 100 && !system::isProcessZombie(child); ++attempt)
      boost::this_thread::sleep_for(boost::chrono::milliseconds(50));
   ASSERT_TRUE(system::isProcessZombie(child));
   ASSERT_TRUE(system::isProcessRunning(child));

   EXPECT_TRUE(LinkBasedFileLock::isLockFileStale(lockFilePath_));
   LinkBasedFileLock lock;
   EXPECT_FALSE(lock.acquire(lockFilePath_));
   EXPECT_FALSE(lock.release());

   int status;
   ASSERT_EQ(child, ::waitpid(child, &status, 0));
   ASSERT_TRUE(WIFEXITED(status));
   EXPECT_EQ(0, WEXITSTATUS(status));
}

TEST_F(FileLockingTest, LockHeldByWorkerAfterMainThreadExitIsLive)
{
   // On Linux the thread group leader reads as a zombie once the main thread
   // has exited, while worker threads run on; a lock such a worker holds must
   // not be reclaimed. Once the worker exits too, the whole process is gone.
   int acquired[2];
   int release[2];
   ASSERT_EQ(0, ::pipe(acquired));
   ASSERT_EQ(0, ::pipe(release));

   pid_t child = ::fork();
   ASSERT_NE(-1, child);
   if (child == 0)
   {
      ::close(acquired[0]);
      ::close(release[1]);

      // detached: the leader exit below may unwind this frame, and a
      // joinable boost::thread destructor would terminate the process
      boost::thread worker([&]()
      {
         LinkBasedFileLock lock;
         char ok = lock.acquire(lockFilePath_) ? 0 : 1;
         if (::write(acquired[1], &ok, 1) != 1)
            ::_exit(1);

         char signal;
         if (::read(release[0], &signal, 1) != 1)
            ::_exit(2);
         ::_exit(lock.release() ? 3 : 0);
      });
      worker.detach();

      // exit only the leader thread. glibc implements pthread_exit() as a
      // forced unwind; gtest's catch-all frames are still on this forked
      // stack, would swallow it, and glibc then aborts the whole child
      // ("FATAL: exception not rethrown") -- taking the worker with it
#ifdef __linux__
      ::syscall(SYS_exit, 0);
#else
      ::pthread_exit(nullptr);
#endif
   }

   ::close(acquired[1]);
   ::close(release[0]);

   char ok = 0;
   ASSERT_EQ(1, ::read(acquired[0], &ok, 1));
   ASSERT_EQ(1, ok);

#ifdef __linux__
   // the worker signals before the main thread has necessarily exited; wait
   // until the leader actually reads as a zombie so the check below is real
   FilePath childStat("/proc/" + std::to_string(child) + "/stat");
   bool leaderIsZombie = false;
   for (int attempt = 0; attempt < 100 && !leaderIsZombie; ++attempt)
   {
      std::string contents;
      if (!readStringFromFile(childStat, &contents))
      {
         std::size_t end = contents.rfind(')');
         leaderIsZombie = end != std::string::npos &&
                          end + 2 < contents.size() &&
                          contents[end + 2] == 'Z';
      }
      if (!leaderIsZombie)
         boost::this_thread::sleep_for(boost::chrono::milliseconds(50));
   }
   ASSERT_TRUE(leaderIsZombie);
#endif

   EXPECT_FALSE(system::isProcessZombie(child));
   EXPECT_FALSE(LinkBasedFileLock::isLockFileStale(lockFilePath_));
   LinkBasedFileLock contender;
   EXPECT_TRUE(FileLock::isNoLockAvailable(contender.acquire(lockFilePath_)));

   ASSERT_EQ(1, ::write(release[1], "x", 1));
   int status;
   ASSERT_EQ(child, ::waitpid(child, &status, 0));
   ASSERT_TRUE(WIFEXITED(status));
   EXPECT_EQ(0, WEXITSTATUS(status));
   EXPECT_FALSE(contender.acquire(lockFilePath_));
   EXPECT_FALSE(contender.release());
}

TEST_F(FileLockingTest, ConcurrentAdvisoryProbesDoNotBlockAcquire)
{
   ASSERT_FALSE(lockFilePath_.ensureFile());

   const std::size_t threadCount = 8;
   std::atomic<bool> stop(false);
   std::atomic<int> probeErrors(0);
   boost::barrier started(threadCount + 1);
   boost::thread_group threads;

   for (std::size_t i = 0; i < threadCount; ++i)
   {
      threads.create_thread([&]()
      {
         started.wait();
         while (!stop.load())
         {
            AdvisoryFileLock probe;
            bool isLocked = false;
            if (probe.isLocked(lockFilePath_, &isLocked))
               ++probeErrors;
         }
      });
   }

   started.wait();

   // An in-flight probe is not contention: acquire waits it out.
   AdvisoryFileLock lock;
   Error error = lock.acquire(lockFilePath_);
   ASSERT_FALSE(error) << error.asString();

   // Probes see the held lock without touching the descriptor.
   AdvisoryFileLock observer;
   bool isLocked = false;
   EXPECT_FALSE(observer.isLocked(lockFilePath_, &isLocked));
   EXPECT_TRUE(isLocked);
   expectChildSeesAdvisoryLock();

   stop.store(true);
   threads.join_all();
   EXPECT_EQ(0, probeErrors.load());
   EXPECT_FALSE(lock.release());
}

TEST_F(FileLockingTest, LegacySymlinkOwnerCanBeReplaced)
{
   FilePath legacyOwner =
      root_.completePath(".rstudio-lock-41c29-legacy-owner");
   ASSERT_FALSE(writeStringToFile(legacyOwner, "-1\n"));
   ASSERT_EQ(
      0,
      ::symlink(
         legacyOwner.getAbsolutePath().c_str(),
         lockFilePath_.getAbsolutePath().c_str()));

   LinkBasedFileLock lock;
   EXPECT_FALSE(lock.acquire(lockFilePath_));
   EXPECT_FALSE(legacyOwner.exists());
   EXPECT_FALSE(lock.release());
}

TEST_F(FileLockingTest, EmptyLinkLockIsHeldUntilTimeout)
{
   ASSERT_FALSE(writeStringToFile(lockFilePath_, ""));
   EXPECT_FALSE(LinkBasedFileLock::isLockFileStale(lockFilePath_));

   LinkBasedFileLock lock;
   EXPECT_TRUE(FileLock::isNoLockAvailable(lock.acquire(lockFilePath_)));

   lockFilePath_.setLastWriteTime(
      ::time(nullptr) - FileLock::getTimeoutInterval().total_seconds() - 1);
   EXPECT_TRUE(LinkBasedFileLock::isLockFileStale(lockFilePath_));
   EXPECT_FALSE(lock.acquire(lockFilePath_));
   EXPECT_FALSE(lock.release());
}

TEST_F(FileLockingTest, LinkLockPidIsNeverInterpretedByShell)
{
   FilePath marker = root_.completePath("marker");
   std::string contents =
      std::to_string(::getpid()) + "; touch " + marker.getAbsolutePath();
   ASSERT_FALSE(writeStringToFile(lockFilePath_, contents));

   EXPECT_FALSE(LinkBasedFileLock::isLockFileStale(lockFilePath_));
   EXPECT_FALSE(marker.exists());
}

TEST_F(FileLockingTest, SymlinkLocksUseUniqueOwnerFiles)
{
   FileLock::setUseSymlinksForTesting(true);

   FilePath secondPath = root_.completePath("second-lock");
   LinkBasedFileLock first;
   LinkBasedFileLock second;
   LinkBasedFileLock replacement;
   ASSERT_FALSE(first.acquire(lockFilePath_));
   ASSERT_FALSE(second.acquire(secondPath));

   EXPECT_NE(
      lockFilePath_.resolveSymlink().getAbsolutePath(),
      secondPath.resolveSymlink().getAbsolutePath());

   EXPECT_FALSE(first.release());
   EXPECT_TRUE(second.isLocked(secondPath));
   EXPECT_FALSE(second.release());
   EXPECT_FALSE(replacement.acquire(lockFilePath_));
   EXPECT_FALSE(replacement.release());
}

TEST_F(FileLockingTest, SymlinkLockSupportsAliasedParent)
{
   FileLock::setUseSymlinksForTesting(true);

   FilePath realDirectory = root_.completePath("real");
   FilePath alias = root_.completePath("alias");
   ASSERT_FALSE(realDirectory.ensureDirectory());
   ASSERT_EQ(
      0,
      ::symlink(
         realDirectory.getAbsolutePath().c_str(),
         alias.getAbsolutePath().c_str()));

   LinkBasedFileLock lock;
   EXPECT_FALSE(lock.acquire(alias.completePath("lock")));
   EXPECT_FALSE(lock.release());
}

TEST_F(FileLockingTest, LinkLockDestructorReleasesLock)
{
   {
      LinkBasedFileLock lock;
      ASSERT_FALSE(lock.acquire(lockFilePath_));
   }

   LinkBasedFileLock replacement;
   EXPECT_FALSE(replacement.acquire(lockFilePath_));
   EXPECT_FALSE(replacement.release());
}

TEST_F(FileLockingTest, LinkLockReleaseWithoutAcquireSucceeds)
{
   // Callers that tolerate a failed acquire still release unconditionally at
   // shutdown; that must not read as an error.
   LinkBasedFileLock lock;
   EXPECT_FALSE(lock.release());

   ASSERT_FALSE(lock.acquire(lockFilePath_));
   EXPECT_FALSE(lock.release());
   EXPECT_FALSE(lock.release());
}

TEST_F(FileLockingTest, LinkCreationErrorsAreNotReportedAsContention)
{
   FilePath invalidPath = root_.completePath(std::string(300, 'x'));
   LinkBasedFileLock lock;

   bool isLocked = false;
   Error probeError = lock.isLocked(invalidPath, &isLocked);
   EXPECT_TRUE(probeError);
   EXPECT_TRUE(isLocked);

   Error error = lock.acquire(invalidPath);
   EXPECT_TRUE(error);
   EXPECT_FALSE(FileLock::isNoLockAvailable(error));
}

} // namespace tests
} // namespace core
} // namespace rstudio

#endif // ifndef _WIN32

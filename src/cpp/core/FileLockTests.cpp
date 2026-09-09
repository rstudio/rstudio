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

#include <sys/stat.h>
#include <sys/wait.h>
#include <unistd.h>

#include <atomic>
#include <string>
#include <vector>

#include <boost/thread.hpp>
#include <boost/thread/barrier.hpp>

#include <gtest/gtest.h>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

#include <core/FileSerializer.hpp>
#include <core/system/System.hpp>

namespace rstudio {
namespace core {
namespace tests {

namespace {

class FileLockingTest : public ::testing::Test
{
protected:
   FileLockingTest()
      : oldTimeout_(0)
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
      FileLock::setLoadBalancedForTesting(false);
      FileLock::setUseSymlinksForTesting(false);
   }

   void TearDown() override
   {
      FileLock::cleanUp();
      FileLock::setTimeoutInterval(oldTimeout_);
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
};

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
   LinkBasedFileLock first;
   LinkBasedFileLock second;
   ASSERT_FALSE(first.acquire(lockFilePath_));
   lockFilePath_.setLastWriteTime(::time(nullptr) - 3600);

   EXPECT_TRUE(first.isLocked(lockFilePath_));
   EXPECT_TRUE(FileLock::isNoLockAvailable(second.acquire(lockFilePath_)));
   EXPECT_FALSE(first.release());
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

   EXPECT_FALSE(expired.release());
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

   ASSERT_FALSE(lock.release());
   ASSERT_FALSE(readStringFromFile(lockFilePath_, &contents));
   EXPECT_EQ("-1\n", contents);
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

/*
 * FileLocktests.cpp
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

#include <sys/wait.h>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

#include <boost/thread.hpp>
#include <boost/bind/bind.hpp>

#include <gtest/gtest.h>

using namespace boost::placeholders;

namespace rstudio {
namespace core {
namespace tests {

int s_lockCount = 0;
boost::thread_group s_threads;
boost::mutex s_mutex;
FilePath s_lockFilePath("/tmp/rstudio-test-lock");
   
void acquireLock(std::size_t threadNumber)
{
   LinkBasedFileLock lock;
   Error error = lock.acquire(s_lockFilePath);
   if (!error)
   {
      s_mutex.lock();
      ++s_lockCount;
      s_mutex.unlock();
   }
}

void forkAndCheckLock()
{
   // fork process
   pid_t child = ::fork();
   if (child == 0)
   {
      // the child process should see that the file is locked
      AdvisoryFileLock childLock;
      EXPECT_TRUE(childLock.isLocked(s_lockFilePath));

      // if locked, the child process should not be able to acquire the lock
      Error error = childLock.acquire(s_lockFilePath);
   EXPECT_TRUE(error);

      // the lock should remain after this attempt to lock
      EXPECT_TRUE(childLock.isLocked(s_lockFilePath));

      // exit successfully
      ::_exit(EXIT_SUCCESS);
   }
   
   // parent waits for child process to exit
   int status;
   ::waitpid(child, &status, 0);
   ASSERT_TRUE(WIFEXITED(status) && WEXITSTATUS(status) == 0) << "Child process did not exit cleanly";
}

void checkCrossTypeLockFromChild(bool expected)
{
   pid_t child = ::fork();
   ASSERT_NE(-1, child);
   if (child == 0)
   {
      bool locked = FileLock::isLockedByAnyType(s_lockFilePath);
      ::_exit(locked == expected ? EXIT_SUCCESS : EXIT_FAILURE);
   }

   int status;
   ASSERT_EQ(child, ::waitpid(child, &status, 0));
   ASSERT_TRUE(WIFEXITED(status));
   EXPECT_EQ(EXIT_SUCCESS, WEXITSTATUS(status));
}

TEST(FileLockingTest, CrossTypeCheckDetectsAdvisoryOwner)
{
   FileLock::initialize();
   ScopedFileLock lock(FileLock::create(FileLock::LOCKTYPE_ADVISORY), s_lockFilePath);
   ASSERT_FALSE(lock.error());

   checkCrossTypeLockFromChild(true);
   // Probing in another process must leave the owner's lock intact.
   checkCrossTypeLockFromChild(true);
}

TEST(FileLockingTest, CrossTypeCheckDetectsLinkBasedOwner)
{
   FileLock::initialize();
   ScopedFileLock lock(FileLock::create(FileLock::LOCKTYPE_LINKBASED), s_lockFilePath);
   ASSERT_FALSE(lock.error());

   checkCrossTypeLockFromChild(true);
   checkCrossTypeLockFromChild(true);
}

TEST(FileLockingTest, CrossTypeCheckAllowsExpiredLinkBasedLock)
{
   FileLock::initialize();
   ScopedFileLock lock(FileLock::create(FileLock::LOCKTYPE_LINKBASED), s_lockFilePath);
   ASSERT_FALSE(lock.error());

   s_lockFilePath.setLastWriteTime(
      std::time(nullptr) - FileLock::getTimeoutInterval().total_seconds() - 1);
   checkCrossTypeLockFromChild(false);
}

TEST(FileLockingTest, CrossTypeCheckAllowsMissingAndUnlockedAdvisoryFiles)
{
   FileLock::initialize();
   ASSERT_FALSE(s_lockFilePath.removeIfExists());
   EXPECT_FALSE(FileLock::isLockedByAnyType(s_lockFilePath));
   EXPECT_FALSE(s_lockFilePath.exists());

   ASSERT_FALSE(s_lockFilePath.ensureFile());
   RemoveOnExitScope cleanup(s_lockFilePath, ERROR_LOCATION);
   EXPECT_FALSE(FileLock::isLockedByAnyType(s_lockFilePath));
}

TEST(FileLockingTest, CrossTypeCheckDoesNotTreatInspectionErrorsAsUnlocked)
{
   FileLock::initialize();
   FilePath directory(s_lockFilePath.getAbsolutePath() + "-directory");
   ASSERT_FALSE(directory.ensureDirectory());
   RemoveOnExitScope cleanup(directory, ERROR_LOCATION);

   // A directory cannot be opened as an advisory lock file. An inspection
   // failure must not authorize reclaiming another session's source data.
   EXPECT_TRUE(FileLock::isLockedByAnyType(directory));
}

TEST(FileLockingTest, LinkBasedLockCanOnlyBeAcquiredOnce)
{
   Error error;
   
   FileLock::initialize();

   LinkBasedFileLock lock1;
   LinkBasedFileLock lock2;
   
   error = lock1.acquire(s_lockFilePath);
   if (error)
      LOG_ERROR(error);
   
   EXPECT_FALSE(error);
   
   error = lock2.acquire(s_lockFilePath);
   if (error)
      LOG_ERROR(error);
   
   EXPECT_TRUE(error);
   
   // release and re-acquire
   error = lock1.release();
   if (error)
      LOG_ERROR(error);
   
   EXPECT_FALSE(error);
   
   error = lock2.acquire(s_lockFilePath);
   if (error)
      LOG_ERROR(error);
   
   EXPECT_FALSE(error);
   
   // clean up the lockfile when we're done
   s_lockFilePath.removeIfExists();

   // clean up
   FileLock::cleanUp();
}

TEST(FileLockingTest, OnlyOneThreadSuccessfullyAcquiresLinkBasedFileLock)
{
   FileLock::initialize();

   // this test can potentially take a long time due to the large amount of threads
   // there have been cases where the default timeout (30 sec) would cause the test to fail
   // as later threads would see the lockfile as stale
   // increase the timeout for this test and restore it afterwards
   boost::posix_time::seconds timeout = FileLock::getTimeoutInterval();
   FileLock::setTimeoutInterval(boost::posix_time::seconds(600)); // 10 minutes

   for (std::size_t i = 0; i < 100; ++i)
      s_threads.create_thread(boost::bind(acquireLock, i));
   
   s_threads.join_all();
   EXPECT_EQ(1, s_lockCount);

   // clean up
   s_lockFilePath.removeIfExists();
   FileLock::setTimeoutInterval(timeout);
   FileLock::cleanUp();
}



TEST(FileLockingTest, ChildProcessesGeneratedByForkDontClearRegistry)
{
   FileLock::initialize();

   LinkBasedFileLock lock;
   Error error = lock.acquire(s_lockFilePath);
   if (error)
      LOG_ERROR(error);
   
   EXPECT_TRUE(s_lockFilePath.exists());
   EXPECT_TRUE(lock.isLocked(s_lockFilePath));
   
   pid_t child = ::fork();
   if (child == 0)
   {
      // tell the child process to exit (running any destructors and so on)
      ::_exit(0);
   }
   else
   {
      // wait for child process to exit
      int status;
      ::waitpid(child, &status, 0);
   ASSERT_TRUE(WIFEXITED(status) && WEXITSTATUS(status) == 0) << "Child process did not exit cleanly";
      
      // ensure that the child process didn't take our locks down with it
      EXPECT_TRUE(s_lockFilePath.exists());
      EXPECT_TRUE(lock.isLocked(s_lockFilePath));
   }
   
   // clean up
   s_lockFilePath.removeIfExists();
   FileLock::cleanUp();
}



TEST(FileLockingTest, BasicAdvisoryFileLockAssumptionsHoldTrue)
{
   Error error;
   FileLock::initialize();

   // create lock file
   error = s_lockFilePath.ensureFile();
   ASSERT_FALSE(error);
   
   // ensure file is not locked
   AdvisoryFileLock lock;
   ASSERT_TRUE(s_lockFilePath.exists());
   ASSERT_FALSE(lock.isLocked(s_lockFilePath));
   
   // attempt to acquire that lock
   error = lock.acquire(s_lockFilePath);
   ASSERT_FALSE(error);
   
   // check lock in child process
   forkAndCheckLock();

   // ensure lockfile still exists
   ASSERT_TRUE(s_lockFilePath.exists());
   
   // check whether other locks kill existing locks
   {
      AdvisoryFileLock transientLock;
      error = transientLock.acquire(s_lockFilePath);
      ASSERT_FALSE(error);
      forkAndCheckLock();
   }
   
   // TODO: this fails (the destructor above kills lock)
   // forkAndCheckLock();
   
   // ensure lock acquired
   error = lock.acquire(s_lockFilePath);
   ASSERT_FALSE(error);
   
   // TODO: checking if a file is locked from same process will release lock
   // lock.isLocked(s_lockFilePath);
   // forkAndCheckLock();

   // clean up lockfile
   s_lockFilePath.removeIfExists();
   FileLock::cleanUp();
}

} // namespace tests
} // namespace core
} // namespace rstudio

#endif // ifndef _WIN32
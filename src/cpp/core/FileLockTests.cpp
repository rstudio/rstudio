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

#include <fcntl.h>
#include <pthread.h>
#include <sys/stat.h>
#include <sys/wait.h>
#include <unistd.h>

#ifdef __APPLE__
# include <membership.h>
# include <sys/acl.h>
#endif

#ifdef __linux__
# include <acl/libacl.h>
# include <grp.h>
# include <sys/acl.h>
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

#include "file_lock/ForkAwareRegistry.hpp"

namespace rstudio {
namespace core {
namespace tests {

namespace {

FilePath claimPathFor(const FilePath& lockFilePath)
{
   return LinkBasedFileLock::claimPathForTesting(lockFilePath);
}

FilePath legacyClaimPathFor(const FilePath& lockFilePath)
{
   return LinkBasedFileLock::legacyClaimPathForTesting(lockFilePath);
}

Error writeClaimContents(const FilePath& claimPath, const std::string& contents)
{
   Error error = claimPath.getParent().ensureDirectory();
   if (error)
      return error;
   return writeStringToFile(claimPath, contents);
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

bool descriptorRefersTo(int descriptor, const FilePath& path)
{
   struct stat opened;
   struct stat named;
   return ::fstat(descriptor, &opened) == 0 &&
          ::stat(path.getAbsolutePath().c_str(), &named) == 0 &&
          opened.st_dev == named.st_dev && opened.st_ino == named.st_ino;
}

#ifdef __linux__

bool setAccessAcl(const FilePath& path, const std::string& text, int* pError)
{
   acl_t acl = ::acl_from_text(text.c_str());
   if (!acl)
   {
      *pError = errno;
      return false;
   }

   int status = ::acl_set_file(
      path.getAbsolutePath().c_str(),
      ACL_TYPE_ACCESS,
      acl);
   *pError = status == -1 ? errno : 0;
   ::acl_free(acl);
   return status == 0;
}

bool aclHasNamedPrincipal(const FilePath& path,
                          acl_tag_t expectedTag,
                          unsigned int expectedId,
                          mode_t expectedPermissions)
{
   acl_t acl = ::acl_get_file(
      path.getAbsolutePath().c_str(),
      ACL_TYPE_ACCESS);
   if (!acl)
      return false;

   bool found = false;
   acl_entry_t entry;
   int entryId = ACL_FIRST_ENTRY;
   while (::acl_get_entry(acl, entryId, &entry) == 1)
   {
      entryId = ACL_NEXT_ENTRY;
      acl_tag_t tag;
      if (::acl_get_tag_type(entry, &tag) == -1 || tag != expectedTag)
         continue;

      void* pQualifier = ::acl_get_qualifier(entry);
      if (!pQualifier)
         continue;
      unsigned int id = *static_cast<unsigned int*>(pQualifier);
      ::acl_free(pQualifier);
      if (id != expectedId)
         continue;

      acl_permset_t permissions;
      if (::acl_get_permset(entry, &permissions) == -1)
         break;
      mode_t actualPermissions = 0;
      if (::acl_get_perm(permissions, ACL_READ) == 1)
         actualPermissions |= 04;
      if (::acl_get_perm(permissions, ACL_WRITE) == 1)
         actualPermissions |= 02;
      if (::acl_get_perm(permissions, ACL_EXECUTE) == 1)
         actualPermissions |= 01;
      found = actualPermissions == expectedPermissions;
      break;
   }

   ::acl_free(acl);
   return found;
}

#endif

#ifdef __APPLE__

bool setExtendedAclForUser(const FilePath& path,
                           uid_t user,
                           bool allowWrites,
                           int* pError)
{
   acl_t acl = ::acl_init(1);
   if (!acl)
   {
      *pError = errno;
      return false;
   }

   acl_entry_t entry;
   uuid_t identity;
   int status = ::mbr_uid_to_uuid(user, identity);
   if (status == 0)
      status = ::acl_create_entry(&acl, &entry);
   if (status == 0)
      status = ::acl_set_tag_type(entry, ACL_EXTENDED_ALLOW);
   if (status == 0)
      status = ::acl_set_qualifier(entry, identity);

   acl_permset_t permissions;
   if (status == 0)
      status = ::acl_get_permset(entry, &permissions);
   if (status == 0)
      status = ::acl_clear_perms(permissions);
   if (status == 0)
      status = ::acl_add_perm(permissions, ACL_READ_DATA);
   if (status == 0)
      status = ::acl_add_perm(permissions, ACL_SEARCH);
   if (status == 0 && allowWrites)
      status = ::acl_add_perm(permissions, ACL_ADD_FILE);
   if (status == 0 && allowWrites)
      status = ::acl_add_perm(permissions, ACL_ADD_SUBDIRECTORY);
   if (status == 0 && allowWrites)
      status = ::acl_add_perm(permissions, ACL_DELETE_CHILD);

   acl_flagset_t flags;
   if (status == 0)
      status = ::acl_get_flagset_np(entry, &flags);
   if (status == 0)
      status = ::acl_clear_flags_np(flags);
   if (status == 0)
      status = ::acl_add_flag_np(flags, ACL_ENTRY_FILE_INHERIT);
   if (status == 0)
   {
      status = ::acl_set_file(
         path.getAbsolutePath().c_str(),
         ACL_TYPE_EXTENDED,
         acl);
   }

   *pError = status == -1 ? errno : status;
   ::acl_free(acl);
   return status == 0;
}

bool setExtendedAcl(const FilePath& path, int* pError)
{
   return setExtendedAclForUser(
      path,
      ::geteuid(),
      false,
      pError);
}

bool denyExtendedAclPermission(const FilePath& path,
                               acl_perm_t deniedPermission,
                               int* pError)
{
   acl_t acl = ::acl_init(1);
   if (!acl)
   {
      *pError = errno;
      return false;
   }

   acl_entry_t entry;
   uuid_t identity;
   int status = ::mbr_uid_to_uuid(::geteuid(), identity);
   if (status == 0)
      status = ::acl_create_entry(&acl, &entry);
   if (status == 0)
      status = ::acl_set_tag_type(entry, ACL_EXTENDED_DENY);
   if (status == 0)
      status = ::acl_set_qualifier(entry, identity);

   acl_permset_t permissions;
   if (status == 0)
      status = ::acl_get_permset(entry, &permissions);
   if (status == 0)
      status = ::acl_clear_perms(permissions);
   if (status == 0)
      status = ::acl_add_perm(permissions, deniedPermission);

   acl_flagset_t flags;
   if (status == 0)
      status = ::acl_get_flagset_np(entry, &flags);
   if (status == 0)
      status = ::acl_clear_flags_np(flags);
   if (status == 0)
   {
      status = ::acl_set_file(
         path.getAbsolutePath().c_str(),
         ACL_TYPE_EXTENDED,
         acl);
   }

   *pError = status == -1 ? errno : status;
   ::acl_free(acl);
   return status == 0;
}

std::string extendedAclText(const FilePath& path)
{
   acl_t acl = ::acl_get_file(
      path.getAbsolutePath().c_str(),
      ACL_TYPE_EXTENDED);
   if (!acl)
      return std::string();

   ssize_t length = 0;
   char* pText = ::acl_to_text(acl, &length);
   std::string text;
   if (pText)
   {
      text.assign(pText, static_cast<std::size_t>(length));
      ::acl_free(pText);
   }
   ::acl_free(acl);
   return text;
}

#endif

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
      LinkBasedFileLock::setBeforeReleaseForTesting({});
      LinkBasedFileLock::setBeforeRefreshForTesting({});
      LinkBasedFileLock::setBeforeWriteForTesting({});
      LinkBasedFileLock::setBeforeClaimForTesting({});
      LinkBasedFileLock::setAfterRenameForTesting({});
      LinkBasedFileLock::setForceClaimDirectoryChownFailureForTesting(false);
      LinkBasedFileLock::setForceFallbackForTesting(false);
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
      LinkBasedFileLock::setBeforeReleaseForTesting({});
      LinkBasedFileLock::setBeforeRefreshForTesting({});
      LinkBasedFileLock::setBeforeWriteForTesting({});
      LinkBasedFileLock::setBeforeClaimForTesting({});
      LinkBasedFileLock::setAfterRenameForTesting({});
      LinkBasedFileLock::setForceClaimDirectoryChownFailureForTesting(false);
      LinkBasedFileLock::setForceFallbackForTesting(false);
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

class ForkPublicationTestRegistry : public file_lock::ForkAwareRegistry
{
public:
   static boost::function<void()> afterPublish;

   bool wasResetInChild() const { return reset_; }
   void touch() { Guard guard(*this); }

private:
   void resetInChild() override { reset_ = true; }
   void afterPublishForTesting() override { afterPublish(); }

   bool reset_ = false;
};

boost::function<void()> ForkPublicationTestRegistry::afterPublish;

} // anonymous namespace

TEST_F(FileLockingTest, RegistryPublicationFinishesBeforeFork)
{
   int published[2];
   int resume[2];
   ASSERT_EQ(0, ::pipe(published));
   ASSERT_EQ(0, ::pipe(resume));

   ForkPublicationTestRegistry* pRegistry = nullptr;
   ForkPublicationTestRegistry::afterPublish = [&]()
   {
      sendByte(published[1]);
      waitForByte(resume[0]);
   };
   boost::thread initializer([&]()
   {
      file_lock::ForkAwareRegistry::instance(pRegistry).touch();
   });

   // The first instance() call is still on another thread's stack. fork()
   // must see the published pointer without inheriting a busy C++ guard.
   waitForByte(published[0]);
   pid_t child = ::fork();
   if (child == 0)
   {
      ::alarm(10);
      auto& registry = file_lock::ForkAwareRegistry::instance(pRegistry);
      registry.touch();
      ::_exit(registry.wasResetInChild() ? 0 : 1);
   }

   sendByte(resume[1]);
   initializer.join();
   ForkPublicationTestRegistry::afterPublish.clear();
   for (int descriptor : {published[0], published[1], resume[0], resume[1]})
      ::close(descriptor);

   ASSERT_NE(-1, child);
   int status;
   ASSERT_EQ(child, ::waitpid(child, &status, 0));
   ASSERT_TRUE(WIFEXITED(status));
   EXPECT_EQ(0, WEXITSTATUS(status));
}

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
   EXPECT_TRUE(lock.acquire(lockFilePath_));
   struct stat info;
   ASSERT_EQ(0, ::stat(lockFilePath_.getAbsolutePath().c_str(), &info));
   EXPECT_EQ(0444, info.st_mode & 0777);

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

TEST_F(FileLockingTest, AdvisoryNewLockIsSharedDespiteRestrictiveUmask)
{
   mode_t previousMask = ::umask(0077);
   AdvisoryFileLock first;
   Error error = first.acquire(lockFilePath_);
   ::umask(previousMask);
   ASSERT_FALSE(error);

   struct stat before;
   ASSERT_EQ(0, ::stat(lockFilePath_.getAbsolutePath().c_str(), &before));
   EXPECT_EQ(0666, before.st_mode & 0777);
   ASSERT_FALSE(first.release());

   pid_t child = ::fork();
   ASSERT_NE(-1, child);
   if (child == 0)
   {
      ::alarm(10);
      AdvisoryFileLock second;
      if (second.acquire(lockFilePath_))
         ::_exit(1);
      struct stat after;
      if (::stat(lockFilePath_.getAbsolutePath().c_str(), &after))
         ::_exit(2);
      if (before.st_dev != after.st_dev || before.st_ino != after.st_ino)
         ::_exit(3);
      if ((after.st_mode & 0777) != 0666)
         ::_exit(4);
      if (second.release())
         ::_exit(5);
      ::_exit(0);
   }

   int status;
   ASSERT_EQ(child, ::waitpid(child, &status, 0));
   ASSERT_TRUE(WIFEXITED(status));
   EXPECT_EQ(0, WEXITSTATUS(status));
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

TEST_F(FileLockingTest, StalledReleasePreservesSuccessorInAnotherProcess)
{
   FileLock::setTimeoutInterval(boost::posix_time::seconds(1));
   FileLock::setLoadBalancedForTesting(true);

   for (bool useSymlinks : {false, true})
   {
      SCOPED_TRACE(useSymlinks ? "symlink" : "hard link");
      FileLock::setUseSymlinksForTesting(useSymlinks);
      LinkBasedFileLock first;
      ASSERT_FALSE(first.acquire(lockFilePath_));

      int takeover[2];
      int ready[2];
      int done[2];
      ASSERT_EQ(0, ::pipe(takeover));
      ASSERT_EQ(0, ::pipe(ready));
      ASSERT_EQ(0, ::pipe(done));
      pid_t child = ::fork();
      ASSERT_NE(-1, child);
      if (child == 0)
      {
         ::alarm(10);
         ::close(takeover[1]);
         ::close(ready[0]);
         ::close(done[1]);
         waitForByte(takeover[0]);
         LinkBasedFileLock replacement;
         if (replacement.acquire(lockFilePath_))
            ::_exit(1);
         sendByte(ready[1]);
         waitForByte(done[0]);
         ::_exit(replacement.release() ? 2 : 0);
      }

      ::close(takeover[0]);
      ::close(ready[1]);
      ::close(done[0]);

      LinkBasedFileLock::setBeforeReleaseForTesting([&]()
      {
         // Pause immediately before release's filesystem operation. Model
         // expiry during that pause, including an abandoned cleanup claim,
         // and let the child complete its takeover before release resumes.
         lockFilePath_.setLastWriteTime(::time(nullptr) - 10);
         FilePath claim = claimPathFor(lockFilePath_);
         EXPECT_FALSE(writeClaimContents(claim, std::to_string(::getpid()) + "\n"));
         claim.setLastWriteTime(::time(nullptr) - 10);
         sendByte(takeover[1]);
         waitForByte(ready[0]);
      });

      EXPECT_FALSE(first.release());
      LinkBasedFileLock::setBeforeReleaseForTesting({});
      EXPECT_TRUE(lockFilePath_.exists());
      LinkBasedFileLock contender;
      EXPECT_TRUE(contender.isLocked(lockFilePath_));
      EXPECT_TRUE(FileLock::isNoLockAvailable(contender.acquire(lockFilePath_)));
      EXPECT_EQ(1, countChildrenWithPrefix(root_, ".rstudio-lock-owner-41c29-"));

      sendByte(done[1]);
      int status;
      EXPECT_EQ(child, ::waitpid(child, &status, 0));
      EXPECT_TRUE(WIFEXITED(status));
      if (WIFEXITED(status))
         EXPECT_EQ(0, WEXITSTATUS(status));
      ::close(takeover[1]);
      ::close(ready[0]);
      ::close(done[1]);
   }
}

TEST_F(FileLockingTest, RefreshSnapshotDoesNotTouchReleasedInode)
{
   LinkBasedFileLock lock;
   ASSERT_FALSE(lock.acquire(lockFilePath_));
   const std::time_t releasedTime = ::time(nullptr) - 60;

   LinkBasedFileLock::setBeforeRefreshForTesting([&]()
   {
      // The refresh already copied its descriptor out of the registry.
      // Release and back-date the retained entry before that snapshot runs.
      EXPECT_FALSE(lock.release());
      lockFilePath_.setLastWriteTime(releasedTime);
   });
   LinkBasedFileLock::refresh();
   LinkBasedFileLock::setBeforeRefreshForTesting({});

   std::string contents;
   ASSERT_FALSE(readStringFromFile(lockFilePath_, &contents));
   EXPECT_EQ("-1\n", contents);
   EXPECT_EQ(releasedTime, lockFilePath_.getLastWriteTime());
   EXPECT_FALSE(lock.isLocked(lockFilePath_));
}

TEST_F(FileLockingTest, StalledFallbackPublicationPreservesSuccessor)
{
   FileLock::setLoadBalancedForTesting(true);
   FileLock::setTimeoutInterval(boost::posix_time::seconds(1));
   LinkBasedFileLock::setForceFallbackForTesting(true);

   for (bool failWrite : {true, false})
   {
      SCOPED_TRACE(failWrite ? "failed write" : "successful write");
      LinkBasedFileLock first;
      LinkBasedFileLock successor;
      bool replaced = false;
      LinkBasedFileLock::setBeforeWriteForTesting([&](int descriptor) -> Error
      {
         if (replaced || !descriptorRefersTo(descriptor, lockFilePath_))
            return Success();

         // Model a write that stalls beyond publication's timeout. A second
         // contender takes over before the original write returns.
         replaced = true;
         lockFilePath_.setLastWriteTime(::time(nullptr) - 10);
         claimPathFor(lockFilePath_).setLastWriteTime(::time(nullptr) - 10);
         legacyClaimPathFor(lockFilePath_).setLastWriteTime(::time(nullptr) - 10);
         EXPECT_FALSE(successor.acquire(lockFilePath_));
         return failWrite ? systemError(ENOSPC, ERROR_LOCATION) : Success();
      });

      Error error = first.acquire(lockFilePath_);
      LinkBasedFileLock::setBeforeWriteForTesting({});
      EXPECT_TRUE(replaced);
      if (failWrite)
         EXPECT_EQ(systemError(ENOSPC, ErrorLocation()), error);
      else
         EXPECT_TRUE(FileLock::isNoLockAvailable(error));

      EXPECT_FALSE(first.release());
      EXPECT_TRUE(lockFilePath_.exists());
      LinkBasedFileLock third;
      EXPECT_TRUE(third.isLocked(lockFilePath_));
      EXPECT_TRUE(FileLock::isNoLockAvailable(third.acquire(lockFilePath_)));
      EXPECT_FALSE(successor.release());
   }
}

TEST_F(FileLockingTest, FailedFallbackPublicationCanExpire)
{
   FileLock::setLoadBalancedForTesting(true);
   FileLock::setTimeoutInterval(boost::posix_time::seconds(1));
   LinkBasedFileLock::setForceFallbackForTesting(true);
   LinkBasedFileLock::setBeforeWriteForTesting([&](int descriptor) -> Error
   {
      return descriptorRefersTo(descriptor, lockFilePath_)
         ? systemError(ENOSPC, ERROR_LOCATION)
         : Success();
   });

   LinkBasedFileLock first;
   EXPECT_EQ(systemError(ENOSPC, ErrorLocation()), first.acquire(lockFilePath_));
   LinkBasedFileLock::setBeforeWriteForTesting({});

   // The failed publication stays held until expiry, then normal takeover
   // reclaims it without any unsafe pathname cleanup by the failed writer.
   EXPECT_TRUE(first.isLocked(lockFilePath_));
   lockFilePath_.setLastWriteTime(::time(nullptr) - 10);
   LinkBasedFileLock next;
   EXPECT_FALSE(next.acquire(lockFilePath_));
   EXPECT_FALSE(next.release());
}

TEST_F(FileLockingTest, FailedClaimPublicationPreservesSuccessorClaim)
{
   ASSERT_FALSE(writeStringToFile(lockFilePath_, "-1\n"));
   FilePath claimPath = claimPathFor(lockFilePath_);
   bool replaced = false;
   LinkBasedFileLock::setBeforeWriteForTesting([&](int descriptor) -> Error
   {
      if (replaced || !descriptorRefersTo(descriptor, claimPath))
         return Success();

      // Simulate another contender replacing an expired, incomplete claim
      // while its creator is still waiting for the initial write.
      replaced = true;
      EXPECT_FALSE(claimPath.remove());
      EXPECT_FALSE(writeClaimContents(claimPath, std::to_string(::getpid()) + "\n"));
      return systemError(ENOSPC, ERROR_LOCATION);
   });

   LinkBasedFileLock lock;
   EXPECT_EQ(systemError(ENOSPC, ErrorLocation()), lock.acquire(lockFilePath_));
   LinkBasedFileLock::setBeforeWriteForTesting({});
   EXPECT_TRUE(replaced);
   EXPECT_TRUE(claimPath.exists());
   EXPECT_TRUE(FileLock::isNoLockAvailable(lock.acquire(lockFilePath_)));
}

TEST_F(FileLockingTest, ReleasedLinkInodeExpiresForLegacyLoadBalancedReaders)
{
   FileLock::setLoadBalancedForTesting(true);
   for (int mode = 0; mode < 3; ++mode)
   {
      SCOPED_TRACE(mode);
      FileLock::setUseSymlinksForTesting(mode == 1);
      LinkBasedFileLock::setForceFallbackForTesting(mode == 2);
      for (bool cleanUp : {false, true})
      {
         SCOPED_TRACE(cleanUp ? "cleanup" : "release");
         LinkBasedFileLock lock;
         ASSERT_FALSE(lock.acquire(lockFilePath_));
         int descriptor = ::open(lockFilePath_.getAbsolutePath().c_str(), O_RDONLY);
         ASSERT_NE(-1, descriptor);
         struct stat info = {};
         EXPECT_EQ(0, ::fstat(descriptor, &info));
         EXPECT_LT(::difftime(::time(nullptr), info.st_mtime),
                   FileLock::getTimeoutInterval().total_seconds());

         if (cleanUp)
            FileLock::cleanUp();
         else
            EXPECT_FALSE(lock.release());

         EXPECT_EQ(0, ::fstat(descriptor, &info));
         // Older load-balanced versions ignore the release sentinel. Test
         // their age-only predicate against the retained inode directly.
         EXPECT_GE(::difftime(::time(nullptr), info.st_mtime),
                   FileLock::getTimeoutInterval().total_seconds());
         ::close(descriptor);
         EXPECT_FALSE(lock.isLocked(lockFilePath_));
      }
   }
}

TEST_F(FileLockingTest, LinkMetadataRemainsBackwardCompatible)
{
   LinkBasedFileLock lock;
   ASSERT_FALSE(lock.acquire(lockFilePath_));

   std::string contents;
   ASSERT_FALSE(readStringFromFile(lockFilePath_, &contents));
   EXPECT_EQ(std::to_string(::getpid()) + "\n", contents);

   // Every link to the held inode, including the retained public entry,
   // must read as released in the PID-only format older versions understand.
   FilePath keptLink = root_.completePath("kept-link");
   ASSERT_EQ(
      0,
      ::link(
         lockFilePath_.getAbsolutePath().c_str(),
         keptLink.getAbsolutePath().c_str()));

   ASSERT_FALSE(lock.release());
   EXPECT_TRUE(lockFilePath_.exists());
   ASSERT_FALSE(readStringFromFile(keptLink, &contents));
   EXPECT_EQ("-1\n", contents);
}

TEST_F(FileLockingTest, ReleaseRetiresPublicEntryAndRemovesOwnerFile)
{
   LinkBasedFileLock lock;
   ASSERT_FALSE(lock.acquire(lockFilePath_));
   EXPECT_TRUE(lockFilePath_.exists());
   EXPECT_EQ(1, countChildrenWithPrefix(root_, ".rstudio-lock-owner-41c29-"));

   ASSERT_FALSE(lock.release());
   EXPECT_TRUE(lockFilePath_.exists());
   EXPECT_FALSE(lock.isLocked(lockFilePath_));
   EXPECT_EQ(2, childrenOf(root_).size());
   EXPECT_EQ(0, countChildrenWithPrefix(root_, ".rstudio-lock-owner-41c29-"));

   LinkBasedFileLock next;
   ASSERT_FALSE(next.acquire(lockFilePath_));
   EXPECT_TRUE(next.isLocked(lockFilePath_));
   EXPECT_FALSE(next.release());
}

TEST_F(FileLockingTest, SymlinkReleaseRetiresPublicEntryAndRemovesOwnerFile)
{
   FileLock::setUseSymlinksForTesting(true);

   LinkBasedFileLock lock;
   ASSERT_FALSE(lock.acquire(lockFilePath_));
   EXPECT_TRUE(lockFilePath_.isSymlink());

   ASSERT_FALSE(lock.release());
   EXPECT_TRUE(lockFilePath_.isSymlink());
   EXPECT_FALSE(lockFilePath_.exists());
   EXPECT_FALSE(lock.isLocked(lockFilePath_));
   EXPECT_EQ(2, childrenOf(root_).size());
   EXPECT_EQ(0, countChildrenWithPrefix(root_, ".rstudio-lock-owner-41c29-"));

   LinkBasedFileLock next;
   ASSERT_FALSE(next.acquire(lockFilePath_));
   EXPECT_TRUE(next.isLocked(lockFilePath_));
   EXPECT_FALSE(next.release());
}

TEST_F(FileLockingTest, CleanUpRetiresRegisteredLockFiles)
{
   LinkBasedFileLock lock;
   ASSERT_FALSE(lock.acquire(lockFilePath_));

   FileLock::cleanUp();
   EXPECT_TRUE(lockFilePath_.exists());
   EXPECT_FALSE(lock.isLocked(lockFilePath_));
   EXPECT_EQ(2, childrenOf(root_).size());
   EXPECT_EQ(0, countChildrenWithPrefix(root_, ".rstudio-lock-owner-41c29-"));

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
   EXPECT_EQ(2, childrenOf(root_).size());
   EXPECT_FALSE(lock.isLocked(lockFilePath_));
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

TEST_F(FileLockingTest, LiveContenderTempFileIsSweptOnceAged)
{
   // A rename-aside temp file whose namer is alive but unrelated (a container
   // sharing the directory, or a reused PID) must not linger forever. Once it
   // is older than the timeout it is swept by age, like an owner file, rather
   // than pinned indefinitely by the live PID. The name uses our own live PID
   // so the process check cannot classify it as abandoned.
   FileLock::setTimeoutInterval(boost::posix_time::seconds(1));

   FilePath liveContender = root_.completePath(
      ".rstudio-lock-tmp-41c29-" + std::to_string(::getpid()) + "-aged");
   ASSERT_FALSE(writeStringToFile(liveContender, "12345\n"));

   // Its ctime is now; let the wall clock pass the one-second timeout.
   boost::this_thread::sleep_for(boost::chrono::milliseconds(1200));

   LinkBasedFileLock lock;
   ASSERT_FALSE(lock.acquire(lockFilePath_));
   EXPECT_FALSE(liveContender.exists());
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
   EXPECT_EQ(2, childrenOf(root_).size());
   EXPECT_FALSE(next.isLocked(root_.completePath("other-lock")));
}

TEST_F(FileLockingTest, LiveClaimBlocksStaleLockTakeover)
{
   ASSERT_FALSE(writeStringToFile(lockFilePath_, "-1\n"));
   FilePath claim = claimPathFor(lockFilePath_);
   ASSERT_FALSE(writeClaimContents(claim, std::to_string(::getpid()) + "\n"));

   LinkBasedFileLock lock;
   EXPECT_TRUE(FileLock::isNoLockAvailable(lock.acquire(lockFilePath_)));
   EXPECT_TRUE(claim.exists());
   EXPECT_TRUE(lockFilePath_.exists());

   ASSERT_FALSE(claim.remove());
   EXPECT_FALSE(lock.acquire(lockFilePath_));
   EXPECT_FALSE(claim.exists());
   EXPECT_FALSE(lock.release());
}

TEST_F(FileLockingTest, LiveClaimBlocksPublicationIntoAbsentPath)
{
   FilePath claim = claimPathFor(lockFilePath_);
   ASSERT_FALSE(writeClaimContents(claim, std::to_string(::getpid()) + "\n"));

   for (int mode = 0; mode < 3; ++mode)
   {
      SCOPED_TRACE(mode);
      FileLock::setUseSymlinksForTesting(mode == 1);
      LinkBasedFileLock::setForceFallbackForTesting(mode == 2);

      LinkBasedFileLock lock;
      EXPECT_TRUE(FileLock::isNoLockAvailable(lock.acquire(lockFilePath_)));
      EXPECT_FALSE(lockFilePath_.exists());
      EXPECT_TRUE(claim.exists());
   }
}

TEST_F(FileLockingTest, LiveLegacyClaimBlocksPublication)
{
   FilePath legacyClaim = legacyClaimPathFor(lockFilePath_);
   ASSERT_FALSE(writeClaimContents(
      legacyClaim,
      std::to_string(::getpid()) + "\n"));

   LinkBasedFileLock lock;
   EXPECT_TRUE(FileLock::isNoLockAvailable(lock.acquire(lockFilePath_)));
   EXPECT_TRUE(legacyClaim.exists());
   EXPECT_FALSE(lockFilePath_.exists());
   EXPECT_FALSE(claimPathFor(lockFilePath_).getParent().exists());
   EXPECT_FALSE(legacyClaim.remove());
}

TEST_F(FileLockingTest, ClaimDirectoryReplacementDuringOpenIsRetried)
{
   FilePath claim = claimPathFor(lockFilePath_);
   bool removed = false;
   LinkBasedFileLock::setBeforeClaimForTesting([&](const FilePath& path)
   {
      if (removed || path != claim)
         return;

      removed = true;
      EXPECT_EQ(
         0,
         ::rmdir(path.getParent().getAbsolutePathNative().c_str()));
   });

   LinkBasedFileLock lock;
   EXPECT_FALSE(lock.acquire(lockFilePath_));
   LinkBasedFileLock::setBeforeClaimForTesting({});
   EXPECT_TRUE(removed);
   EXPECT_TRUE(lock.isLocked(lockFilePath_));
   EXPECT_FALSE(lock.release());
}

TEST_F(FileLockingTest, ClaimDirectorySymlinkIsRejected)
{
   FilePath target = root_.completePath("claim-target");
   ASSERT_FALSE(target.ensureDirectory());
   ASSERT_EQ(0, ::chmod(target.getAbsolutePath().c_str(), 0700));

   FilePath claimDirectory = claimPathFor(lockFilePath_).getParent();
   ASSERT_EQ(
      0,
      ::symlink(
         target.getAbsolutePath().c_str(),
         claimDirectory.getAbsolutePath().c_str()));

   LinkBasedFileLock lock;
   Error error = lock.acquire(lockFilePath_);
   EXPECT_TRUE(error);
   EXPECT_FALSE(FileLock::isNoLockAvailable(error));
   EXPECT_FALSE(lockFilePath_.exists());

   struct stat info;
   ASSERT_EQ(0, ::stat(target.getAbsolutePath().c_str(), &info));
   EXPECT_EQ(0700, info.st_mode & 0777);
   ASSERT_EQ(0, ::unlink(claimDirectory.getAbsolutePath().c_str()));
}

TEST_F(FileLockingTest, ReplacedClaimDirectoryCannotRedirectRemoval)
{
   FilePath claim = claimPathFor(lockFilePath_);
   ASSERT_FALSE(writeClaimContents(claim, "-1\n"));

   FilePath claimDirectory = claim.getParent();
   FilePath displacedDirectory = root_.completePath("displaced-claims");
   FilePath targetDirectory = root_.completePath("claim-target");
   ASSERT_FALSE(targetDirectory.ensureDirectory());
   FilePath victim = targetDirectory.completePath(claim.getFilename());
   ASSERT_FALSE(writeStringToFile(victim, "-1\n"));

   bool replaced = false;
   LinkBasedFileLock::setBeforeClaimForTesting([&](const FilePath& path)
   {
      if (replaced || path != claim)
         return;

      replaced = true;
      ASSERT_EQ(
         0,
         ::rename(
            claimDirectory.getAbsolutePath().c_str(),
            displacedDirectory.getAbsolutePath().c_str()));
      ASSERT_EQ(
         0,
         ::symlink(
            targetDirectory.getAbsolutePath().c_str(),
            claimDirectory.getAbsolutePath().c_str()));
   });

   LinkBasedFileLock lock;
   EXPECT_TRUE(FileLock::isNoLockAvailable(lock.acquire(lockFilePath_)));
   LinkBasedFileLock::setBeforeClaimForTesting({});
   EXPECT_TRUE(replaced);
   EXPECT_TRUE(victim.exists());
   std::string contents;
   ASSERT_FALSE(readStringFromFile(victim, &contents));
   EXPECT_EQ("-1\n", contents);
   EXPECT_FALSE(lockFilePath_.exists());

   ASSERT_EQ(0, ::unlink(claimDirectory.getAbsolutePath().c_str()));
   ASSERT_FALSE(victim.remove());
   ASSERT_FALSE(targetDirectory.remove());
   ASSERT_FALSE(displacedDirectory.remove());
}

TEST_F(FileLockingTest, ClaimDirectoryUsesParentPermissionsDespiteUmask)
{
   ASSERT_EQ(0, ::chmod(root_.getAbsolutePath().c_str(), 01777));
   mode_t oldMask = ::umask(0077);
   LinkBasedFileLock lock;
   Error error = lock.acquire(lockFilePath_);
   ::umask(oldMask);
   ASSERT_FALSE(error);

   struct stat parentInfo;
   struct stat claimInfo;
   ASSERT_EQ(0, ::stat(root_.getAbsolutePath().c_str(), &parentInfo));
   ASSERT_EQ(
      0,
      ::stat(
         claimPathFor(lockFilePath_).getParent().getAbsolutePath().c_str(),
         &claimInfo));
   mode_t permissionBits = S_ISGID | 0777;
   EXPECT_EQ(
      parentInfo.st_mode & permissionBits,
      claimInfo.st_mode & permissionBits);
   EXPECT_EQ(0, claimInfo.st_mode & S_ISVTX);
   EXPECT_EQ(parentInfo.st_gid, claimInfo.st_gid);
   EXPECT_FALSE(lock.release());
}

TEST_F(FileLockingTest, ClaimDirectoryUsesParentGroupWithoutSetgid)
{
   int groupCount = ::getgroups(0, nullptr);
   ASSERT_NE(-1, groupCount);
   std::vector<gid_t> groups(static_cast<std::size_t>(groupCount));
   ASSERT_EQ(groupCount, ::getgroups(groupCount, groups.data()));

   gid_t parentGroup = ::getegid();
   for (gid_t group : groups)
   {
      if (group != ::getegid())
      {
         parentGroup = group;
         break;
      }
   }
   if (parentGroup == ::getegid())
      GTEST_SKIP() << "process has no alternate supplementary group";

   ASSERT_EQ(
      0,
      ::chown(
         root_.getAbsolutePath().c_str(),
         static_cast<uid_t>(-1),
         parentGroup));
   ASSERT_EQ(0, ::chmod(root_.getAbsolutePath().c_str(), 0770));

   LinkBasedFileLock lock;
   ASSERT_FALSE(lock.acquire(lockFilePath_));

   struct stat claimInfo;
   ASSERT_EQ(
      0,
      ::stat(
         claimPathFor(lockFilePath_).getParent().getAbsolutePath().c_str(),
         &claimInfo));
   EXPECT_EQ(parentGroup, claimInfo.st_gid);
   EXPECT_EQ(0770, claimInfo.st_mode & 0777);
   EXPECT_EQ(0, claimInfo.st_mode & S_ISGID);
   EXPECT_FALSE(lock.release());
}

#ifdef __APPLE__

TEST_F(FileLockingTest, ClaimDirectoryCopiesExtendedAcl)
{
   int aclError = 0;
   if (!setExtendedAcl(root_, &aclError))
   {
      if (aclError == ENOTSUP)
         GTEST_SKIP() << "filesystem has no extended ACL support";
      FAIL() << "could not set parent ACL: " << aclError;
   }
   std::string expectedAcl = extendedAclText(root_);
   ASSERT_FALSE(expectedAcl.empty());

   LinkBasedFileLock lock;
   ASSERT_FALSE(lock.acquire(lockFilePath_));
   EXPECT_EQ(
      expectedAcl,
      extendedAclText(claimPathFor(lockFilePath_).getParent()));
   EXPECT_FALSE(lock.release());
}

TEST_F(FileLockingTest, DarwinAclPreservesParentOwnerAcrossCreators)
{
   if (::geteuid() != 0)
      GTEST_SKIP() << "test needs root to run children under separate uids";

   const uid_t parentOwner = 12345;
   const uid_t creator = 12346;
   ASSERT_EQ(
      0,
      ::chown(root_.getAbsolutePath().c_str(), parentOwner, parentOwner));
   ASSERT_EQ(0, ::chmod(root_.getAbsolutePath().c_str(), 0700));

   int aclError = 0;
   if (!setExtendedAclForUser(root_, creator, true, &aclError))
   {
      if (aclError == ENOTSUP || aclError == EOPNOTSUPP)
         GTEST_SKIP() << "filesystem has no extended ACL support";
      FAIL() << "could not set parent ACL: " << aclError;
   }

   int ready[2];
   int done[2];
   ASSERT_EQ(0, ::pipe(ready));
   ASSERT_EQ(0, ::pipe(done));
   pid_t creatorChild = ::fork();
   ASSERT_NE(-1, creatorChild);
   if (creatorChild == 0)
   {
      ::close(ready[0]);
      ::close(done[1]);
      if (::setgroups(0, nullptr) == -1 ||
          ::setgid(creator) == -1 ||
          ::setuid(creator) == -1)
      {
         ::_exit(10);
      }

      LinkBasedFileLock lock;
      if (lock.acquire(root_.completePath("creator-lock")))
         ::_exit(1);
      FilePath liveClaim = claimPathFor(root_.completePath("live-claim"));
      if (writeClaimContents(liveClaim, std::to_string(::getpid()) + "\n"))
         ::_exit(2);
      sendByte(ready[1]);
      waitForByte(done[0]);
      if (liveClaim.remove())
         ::_exit(3);
      ::_exit(lock.release() ? 4 : 0);
   }

   ::close(ready[1]);
   ::close(done[0]);
   waitForByte(ready[0]);

   pid_t ownerChild = ::fork();
   ASSERT_NE(-1, ownerChild);
   if (ownerChild == 0)
   {
      if (::setgroups(0, nullptr) == -1 ||
          ::setgid(parentOwner) == -1 ||
          ::setuid(parentOwner) == -1)
      {
         ::_exit(10);
      }

      LinkBasedFileLock lock;
      Error error = lock.acquire(root_.completePath("owner-lock"));
      if (error)
         ::_exit(1);
      ::_exit(lock.release() ? 2 : 0);
   }

   int status;
   ASSERT_EQ(ownerChild, ::waitpid(ownerChild, &status, 0));
   ASSERT_TRUE(WIFEXITED(status));
   EXPECT_EQ(0, WEXITSTATUS(status));

   sendByte(done[1]);
   ASSERT_EQ(creatorChild, ::waitpid(creatorChild, &status, 0));
   ASSERT_TRUE(WIFEXITED(status));
   EXPECT_EQ(0, WEXITSTATUS(status));
   ::close(ready[0]);
   ::close(done[1]);
}

TEST_F(FileLockingTest, DarwinAddFileAclFailsClosedWithoutExactClaim)
{
   int aclError = 0;
   if (!denyExtendedAclPermission(
          root_,
          ACL_ADD_SUBDIRECTORY,
          &aclError))
   {
      if (aclError == ENOTSUP || aclError == EOPNOTSUPP)
         GTEST_SKIP() << "filesystem has no extended ACL support";
      FAIL() << "could not set parent ACL: " << aclError;
   }

   LinkBasedFileLock lock;
   Error error = lock.acquire(lockFilePath_);
   EXPECT_TRUE(error);
   EXPECT_FALSE(FileLock::isNoLockAvailable(error));
   EXPECT_FALSE(lockFilePath_.exists());
   EXPECT_FALSE(claimPathFor(lockFilePath_).getParent().exists());
}

#endif

#ifdef __linux__

TEST_F(FileLockingTest, ClaimDirectoryCopiesAccessAclWithoutDefaultAcl)
{
   uid_t namedUser = ::geteuid() == 12345 ? 12346 : 12345;
   int aclError = 0;
   if (!setAccessAcl(
          root_,
          "u::rwx,u:" + std::to_string(namedUser) +
             ":rwx,g::---,m::rwx,o::---",
          &aclError))
   {
      if (aclError == ENOTSUP)
         GTEST_SKIP() << "filesystem has no POSIX ACL support";
      FAIL() << "could not set parent ACL: " << aclError;
   }

   LinkBasedFileLock lock;
   ASSERT_FALSE(lock.acquire(lockFilePath_));
   EXPECT_TRUE(aclHasNamedPrincipal(
      claimPathFor(lockFilePath_).getParent(),
      ACL_USER,
      namedUser,
      07));
   EXPECT_FALSE(lock.release());
}

TEST_F(FileLockingTest, AclAuthorizedUserCanCreateClaimDirectory)
{
   if (::geteuid() != 0)
      GTEST_SKIP() << "test needs root to run a child under another uid";

   const uid_t childUser = 12345;
   LinkBasedFileLock originalOwner;
   ASSERT_FALSE(originalOwner.acquire(root_.completePath("owner-lock")));

   int aclError = 0;
   if (!setAccessAcl(
          root_,
          "u::rwx,u:" + std::to_string(childUser) +
             ":rwx,g::---,m::rwx,o::---",
          &aclError))
   {
      if (aclError == ENOTSUP)
         GTEST_SKIP() << "filesystem has no POSIX ACL support";
      FAIL() << "could not set parent ACL: " << aclError;
   }

   pid_t child = ::fork();
   ASSERT_NE(-1, child);
   if (child == 0)
   {
      if (::setgroups(0, nullptr) == -1 ||
          ::setgid(childUser) == -1 ||
          ::setuid(childUser) == -1)
      {
         ::_exit(10);
      }

      LinkBasedFileLock lock;
      Error error = lock.acquire(lockFilePath_);
      if (error)
         ::_exit(1);
      error = lock.release();
      ::_exit(error ? 2 : 0);
   }

   int status;
   ASSERT_EQ(child, ::waitpid(child, &status, 0));
   ASSERT_TRUE(WIFEXITED(status));
   EXPECT_EQ(0, WEXITSTATUS(status));
   EXPECT_TRUE(aclHasNamedPrincipal(
      claimPathFor(lockFilePath_).getParent(),
      ACL_USER,
      ::geteuid(),
      07));
   EXPECT_FALSE(originalOwner.release());
}

TEST_F(FileLockingTest, ClaimDirectoryMapsParentGroupWhenChownIsDenied)
{
   gid_t parentGroup = ::getegid();
   if (::geteuid() == 0)
   {
      ++parentGroup;
   }
   else
   {
      int groupCount = ::getgroups(0, nullptr);
      ASSERT_NE(-1, groupCount);
      std::vector<gid_t> groups(static_cast<std::size_t>(groupCount));
      ASSERT_EQ(groupCount, ::getgroups(groupCount, groups.data()));
      for (gid_t group : groups)
      {
         if (group != ::getegid())
         {
            parentGroup = group;
            break;
         }
      }
   }
   if (parentGroup == ::getegid())
      GTEST_SKIP() << "process cannot assign an alternate parent group";

   ASSERT_EQ(
      0,
      ::chown(
         root_.getAbsolutePath().c_str(),
         static_cast<uid_t>(-1),
         parentGroup));
   ASSERT_EQ(0, ::chmod(root_.getAbsolutePath().c_str(), 0770));
   LinkBasedFileLock::setForceClaimDirectoryChownFailureForTesting(true);

   LinkBasedFileLock lock;
   ASSERT_FALSE(lock.acquire(lockFilePath_));
   FilePath claimDirectory = claimPathFor(lockFilePath_).getParent();
   struct stat claimInfo;
   ASSERT_EQ(0, ::stat(claimDirectory.getAbsolutePath().c_str(), &claimInfo));
   EXPECT_EQ(::getegid(), claimInfo.st_gid);
   EXPECT_TRUE(aclHasNamedPrincipal(
      claimDirectory,
      ACL_GROUP,
      parentGroup,
      07));
   EXPECT_FALSE(lock.release());
}

#endif

TEST_F(FileLockingTest, WriteAndSearchPermissionsAllowClaimCreation)
{
#if defined(O_SEARCH) || defined(O_PATH)
   ASSERT_EQ(0, ::chmod(root_.getAbsolutePath().c_str(), 0300));
   LinkBasedFileLock lock;
   Error error = lock.acquire(lockFilePath_);
   ASSERT_EQ(0, ::chmod(root_.getAbsolutePath().c_str(), 0700));
   ASSERT_EQ(
      0,
      ::chmod(
         claimPathFor(lockFilePath_).getParent().getAbsolutePath().c_str(),
         0700));
   ASSERT_FALSE(error);
   EXPECT_FALSE(lock.release());
#else
   GTEST_SKIP() << "platform has no search-only directory descriptor";
#endif
}

TEST_F(FileLockingTest, StaleClaimsForOtherLocksAreSwept)
{
   FilePath staleClaim = claimPathFor(root_.completePath("stale-lock"));
   FilePath liveClaim = claimPathFor(root_.completePath("live-lock"));
   ASSERT_FALSE(writeClaimContents(staleClaim, "-1\n"));
   ASSERT_FALSE(writeClaimContents(
      liveClaim,
      std::to_string(::getpid()) + "\n"));

   LinkBasedFileLock lock;
   ASSERT_FALSE(lock.acquire(lockFilePath_));
   EXPECT_FALSE(staleClaim.exists());
   EXPECT_TRUE(liveClaim.exists());
   EXPECT_FALSE(lock.release());
   EXPECT_FALSE(liveClaim.remove());
}

TEST_F(FileLockingTest, LiveNestedRemovalTempIsNotSwept)
{
   FilePath staleClaim = claimPathFor(lockFilePath_);
   ASSERT_FALSE(writeClaimContents(staleClaim, "-1\n"));

   FilePath otherLockPath = root_.completePath("other-lock");
   LinkBasedFileLock other;
   bool interleaved = false;
   LinkBasedFileLock::setAfterRenameForTesting([&](const FilePath& path)
   {
      if (interleaved || path != staleClaim)
         return;

      interleaved = true;
      EXPECT_FALSE(other.acquire(otherLockPath));
   });

   LinkBasedFileLock lock;
   EXPECT_FALSE(lock.acquire(lockFilePath_));
   LinkBasedFileLock::setAfterRenameForTesting({});
   EXPECT_TRUE(interleaved);
   EXPECT_TRUE(lock.isLocked(lockFilePath_));
   EXPECT_TRUE(other.isLocked(otherLockPath));
   EXPECT_FALSE(lock.release());
   EXPECT_FALSE(other.release());
}

TEST_F(FileLockingTest, AbandonedPreparedClaimDirectoriesAreSwept)
{
   FilePath abandoned = root_.completePath(
      ".rstudio-lock-claims-tmp-41c29-99999999-abandoned");
   FilePath live = root_.completePath(
      ".rstudio-lock-claims-tmp-41c29-" +
      std::to_string(::getpid()) + "-live");
   ASSERT_FALSE(abandoned.ensureDirectory());
   ASSERT_FALSE(live.ensureDirectory());

   LinkBasedFileLock lock;
   ASSERT_FALSE(lock.acquire(lockFilePath_));
   EXPECT_FALSE(abandoned.exists());
   EXPECT_TRUE(live.exists());
   EXPECT_FALSE(lock.release());
   EXPECT_FALSE(live.remove());
}

TEST_F(FileLockingTest, TempShapedBasenameKeepsItsLiveClaim)
{
   FilePath lockPath = root_.completePath(
      ".rstudio-lock-tmp-41c29-99999999-public-name");
   LinkBasedFileLock lock;
   ASSERT_FALSE(lock.acquire(lockPath));
   EXPECT_TRUE(lockPath.exists());
   EXPECT_TRUE(lock.isLocked(lockPath));
   LinkBasedFileLock contender;
   EXPECT_TRUE(FileLock::isNoLockAvailable(contender.acquire(lockPath)));
   EXPECT_FALSE(lock.release());
}

TEST_F(FileLockingTest, ClaimNamespaceBasenameIsRejected)
{
   FilePath lockPath = root_.completePath(".rstudio-lock-claims-41c29");
   LinkBasedFileLock lock;
   Error error = lock.acquire(lockPath);
   EXPECT_EQ(systemError(EINVAL, ErrorLocation()), error);
   EXPECT_FALSE(lockPath.exists());
}

TEST_F(FileLockingTest, StaleRemovalBlocksPublicationThroughCaseAliases)
{
   FilePath aliasPath = root_.completePath("LOCK");
   ASSERT_FALSE(writeStringToFile(lockFilePath_, "-1\n"));
   if (!lockFilePath_.isEquivalentTo(aliasPath))
   {
      ASSERT_FALSE(lockFilePath_.remove());
      GTEST_SKIP() << "filesystem is case-sensitive";
   }
   ASSERT_FALSE(lockFilePath_.remove());

   for (int mode = 0; mode < 3; ++mode)
   {
      SCOPED_TRACE(mode);
      FileLock::setUseSymlinksForTesting(mode == 1);
      LinkBasedFileLock::setForceFallbackForTesting(mode == 2);
      ASSERT_FALSE(writeStringToFile(lockFilePath_, "-1\n"));

      bool renamed = false;
      LinkBasedFileLock contender;
      LinkBasedFileLock aliasContender;
      LinkBasedFileLock::setAfterRenameForTesting([&](const FilePath& path)
      {
         if (path != lockFilePath_)
            return;

         renamed = true;
         // The public name is absent between rename and identity validation
         // (and possible restoration). Neither spelling may publish into it.
         EXPECT_FALSE(lockFilePath_.exists());
         EXPECT_TRUE(claimPathFor(lockFilePath_).isEquivalentTo(
            claimPathFor(aliasPath)));
         EXPECT_TRUE(FileLock::isNoLockAvailable(contender.acquire(lockFilePath_)));
         EXPECT_TRUE(FileLock::isNoLockAvailable(aliasContender.acquire(aliasPath)));
      });

      LinkBasedFileLock owner;
      EXPECT_FALSE(owner.acquire(lockFilePath_));
      LinkBasedFileLock::setAfterRenameForTesting({});
      EXPECT_TRUE(renamed);
      EXPECT_TRUE(owner.isLocked(lockFilePath_));
      EXPECT_FALSE(claimPathFor(lockFilePath_).exists());
      ASSERT_FALSE(owner.release());
      ASSERT_FALSE(lockFilePath_.remove());
   }
}

TEST_F(FileLockingTest, CaseSensitiveNamesUseIndependentClaims)
{
   FilePath aliasPath = root_.completePath("LOCK");
   ASSERT_FALSE(writeStringToFile(lockFilePath_, "probe\n"));
   if (lockFilePath_.isEquivalentTo(aliasPath))
   {
      ASSERT_FALSE(lockFilePath_.remove());
      GTEST_SKIP() << "filesystem is case-insensitive";
   }
   ASSERT_FALSE(lockFilePath_.remove());

   FilePath claim = claimPathFor(lockFilePath_);
   ASSERT_FALSE(writeClaimContents(claim, std::to_string(::getpid()) + "\n"));

   LinkBasedFileLock alias;
   EXPECT_FALSE(alias.acquire(aliasPath));
   EXPECT_TRUE(claim.exists());
   ASSERT_FALSE(alias.release());
   ASSERT_FALSE(claim.remove());
}

TEST_F(FileLockingTest, StaleRemovalBlocksPublicationThroughUnicodeAliases)
{
   FilePath primaryPath = root_.completePath("\xC3\x84" "LOCK");
   std::vector<FilePath> aliases = {
      root_.completePath("\xC3\xA4" "lock"),
      root_.completePath("A\xCC\x88" "LOCK")
   };

   ASSERT_FALSE(writeStringToFile(primaryPath, "-1\n"));
   for (const FilePath& aliasPath : aliases)
   {
      if (!primaryPath.isEquivalentTo(aliasPath))
      {
         ASSERT_FALSE(primaryPath.remove());
         GTEST_SKIP() << "filesystem does not use the tested Unicode aliases";
      }
   }
   ASSERT_FALSE(primaryPath.remove());

   for (const FilePath& aliasPath : aliases)
   {
      for (int mode = 0; mode < 3; ++mode)
      {
         SCOPED_TRACE(aliasPath.getFilename());
         SCOPED_TRACE(mode);
         FileLock::setUseSymlinksForTesting(mode == 1);
         LinkBasedFileLock::setForceFallbackForTesting(mode == 2);
         ASSERT_FALSE(writeStringToFile(primaryPath, "-1\n"));

         bool renamed = false;
         LinkBasedFileLock aliasContender;
         LinkBasedFileLock::setAfterRenameForTesting([&](const FilePath& path)
         {
            if (path != primaryPath)
               return;

            renamed = true;
            EXPECT_FALSE(primaryPath.exists());
            EXPECT_TRUE(claimPathFor(primaryPath).isEquivalentTo(
               claimPathFor(aliasPath)));
            EXPECT_TRUE(FileLock::isNoLockAvailable(
               aliasContender.acquire(aliasPath)));
         });

         LinkBasedFileLock owner;
         ASSERT_FALSE(owner.acquire(primaryPath));
         LinkBasedFileLock::setAfterRenameForTesting({});
         EXPECT_TRUE(renamed);
         EXPECT_TRUE(owner.isLocked(primaryPath));
         ASSERT_FALSE(owner.release());
         ASSERT_FALSE(primaryPath.remove());
      }
   }
}

TEST_F(FileLockingTest, LostClaimBeforePublicationLeavesPublicPathAbsent)
{
   FilePath claimPath = claimPathFor(lockFilePath_);
   FilePath legacyClaimPath = legacyClaimPathFor(lockFilePath_);
   bool replaced = false;
   LinkBasedFileLock::setBeforeWriteForTesting([&](int descriptor) -> Error
   {
      if (replaced ||
          descriptorRefersTo(descriptor, claimPath) ||
          descriptorRefersTo(descriptor, legacyClaimPath))
         return Success();

      // Preparing the private owner inode stalled long enough for another
      // contender to replace our claim. We must not publish after resuming.
      replaced = true;
      EXPECT_FALSE(claimPath.remove());
      EXPECT_FALSE(writeClaimContents(claimPath, std::to_string(::getpid()) + "\n"));
      return Success();
   });

   LinkBasedFileLock lock;
   EXPECT_TRUE(FileLock::isNoLockAvailable(lock.acquire(lockFilePath_)));
   LinkBasedFileLock::setBeforeWriteForTesting({});
   EXPECT_TRUE(replaced);
   EXPECT_FALSE(lockFilePath_.exists());
   EXPECT_TRUE(claimPath.exists());
   EXPECT_EQ(0, countChildrenWithPrefix(root_, ".rstudio-lock-owner-41c29-"));
}

TEST_F(FileLockingTest, LostClaimAfterPublicationRetiresPublishedInode)
{
   LinkBasedFileLock::setForceFallbackForTesting(true);
   FilePath claimPath = claimPathFor(lockFilePath_);
   bool replaced = false;
   LinkBasedFileLock::setBeforeWriteForTesting([&](int descriptor) -> Error
   {
      if (replaced || !descriptorRefersTo(descriptor, lockFilePath_))
         return Success();

      // The public inode remains ours, but the publication outlasted the
      // claim. Retire that inode without removing the successor's claim.
      replaced = true;
      EXPECT_FALSE(claimPath.remove());
      EXPECT_FALSE(writeClaimContents(claimPath, std::to_string(::getpid()) + "\n"));
      return Success();
   });

   LinkBasedFileLock lock;
   EXPECT_TRUE(FileLock::isNoLockAvailable(lock.acquire(lockFilePath_)));
   LinkBasedFileLock::setBeforeWriteForTesting({});
   EXPECT_TRUE(replaced);
   EXPECT_TRUE(claimPath.exists());
   std::string contents;
   ASSERT_FALSE(readStringFromFile(lockFilePath_, &contents));
   EXPECT_EQ("-1\n", contents);
   EXPECT_FALSE(lock.isLocked(lockFilePath_));
}

TEST_F(FileLockingTest, ReleaseLeavesFilesWhileContenderHoldsClaim)
{
   // A contender holding the claim is mid-takeover of this (expired) lock
   // and owns the public path; release must leave that path and claim alone.
   LinkBasedFileLock lock;
   ASSERT_FALSE(lock.acquire(lockFilePath_));
   FilePath claim = claimPathFor(lockFilePath_);
   ASSERT_FALSE(writeClaimContents(claim, std::to_string(::getpid()) + "\n"));

   ASSERT_FALSE(lock.release());
   EXPECT_TRUE(lockFilePath_.exists());
   EXPECT_TRUE(claim.exists());
   EXPECT_EQ(0, countChildrenWithPrefix(root_, ".rstudio-lock-owner-41c29-"));

   std::string contents;
   ASSERT_FALSE(readStringFromFile(lockFilePath_, &contents));
   EXPECT_EQ("-1\n", contents);

   // once the claim is gone, the released lock is reclaimed and cleaned up
   ASSERT_FALSE(claim.remove());
   LinkBasedFileLock next;
   ASSERT_FALSE(next.acquire(lockFilePath_));
   ASSERT_FALSE(next.release());
   EXPECT_EQ(2, childrenOf(root_).size());
   EXPECT_FALSE(next.isLocked(lockFilePath_));
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
   ASSERT_FALSE(writeClaimContents(claim, "99999999\n"));

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

   struct stat parentInfo;
   ASSERT_EQ(0, ::stat(root_.getAbsolutePath().c_str(), &parentInfo));

   LinkBasedFileLock lock;
   ASSERT_FALSE(lock.acquire(lockFilePath_));

   for (const FilePath& child : childrenOf(root_))
   {
      struct stat info;
      ASSERT_EQ(0, ::stat(child.getAbsolutePath().c_str(), &info));
      mode_t expectedMode = S_ISDIR(info.st_mode) ?
         parentInfo.st_mode & 0777 : expected;
      EXPECT_EQ(expectedMode, info.st_mode & 0777) << child.getAbsolutePath();
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
         target.getFilename().c_str(),
         lockFilePath_.getAbsolutePath().c_str()));
   ASSERT_FALSE(target.exists());

   AdvisoryFileLock lock;
   AdvisoryFileLock other;
   ASSERT_FALSE(lock.acquire(lockFilePath_));
   ASSERT_TRUE(target.exists());
   struct stat info;
   ASSERT_EQ(0, ::stat(target.getAbsolutePath().c_str(), &info));
   EXPECT_EQ(0666, info.st_mode & 0777);

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

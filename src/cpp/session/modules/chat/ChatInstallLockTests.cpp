/*
 * ChatInstallLockTests.cpp
 *
 * Copyright (C) 2026 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

// Link-based locks are unsupported on Windows (FileLock forces advisory
// there), and these tests rely on link-based semantics for cross-instance
// exclusion within one process (POSIX fcntl advisory locks never conflict
// in-process). The lock primitive itself is covered on Windows by
// Win32FileLockTests.cpp; InstallLock's token/component state machine is
// platform-independent and exercised here on POSIX.
#ifndef _WIN32

#include "ChatInstallLock.hpp"

#include <gtest/gtest.h>

#include <core/FileLock.hpp>
#include <core/FileSerializer.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

using namespace rstudio::core;
using namespace rstudio::session::modules::chat::install_lock;

namespace {

// Two InstallLock instances over the same locks directory model two
// concurrent rsession processes.
class ChatInstallLock : public testing::Test
{
protected:
   void SetUp() override
   {
      FileLock::initialize();

      FilePath tempPath;
      ASSERT_FALSE(FilePath::tempFilePath(tempPath));
      locksDir_ = tempPath.completePath("locks");

      sessionA_.reset(new InstallLock(
         locksDir_, "session-a", FileLock::LOCKTYPE_LINKBASED));
      sessionB_.reset(new InstallLock(
         locksDir_, "session-b", FileLock::LOCKTYPE_LINKBASED));
   }

   void TearDown() override
   {
      sessionA_.reset();
      sessionB_.reset();
      locksDir_.getParent().removeIfExists();
   }

   FilePath locksDir_;
   std::unique_ptr<InstallLock> sessionA_;
   std::unique_ptr<InstallLock> sessionB_;
};

TEST_F(ChatInstallLock, MutationExcludesOtherMutation)
{
   std::string message;
   ASSERT_FALSE(sessionA_->tryBeginMutation(&message));
   EXPECT_TRUE(sessionA_->mutationInProgress());

   Error error = sessionB_->tryBeginMutation(&message);
   EXPECT_TRUE(error);
   EXPECT_NE(message.find("installing or updating"), std::string::npos);
   EXPECT_FALSE(sessionB_->mutationInProgress());

   sessionA_->endMutation();
   EXPECT_FALSE(sessionA_->mutationInProgress());

   EXPECT_FALSE(sessionB_->tryBeginMutation(&message));
   sessionB_->endMutation();
}

TEST_F(ChatInstallLock, LiveInUseBlocksMutationUntilReleased)
{
   uint64_t token = 0;
   ASSERT_FALSE(
      sessionA_->acquireInUse(InstallLock::Component::ChatBackend, &token));
   EXPECT_TRUE(sessionA_->inUseHeld());

   std::string message;
   Error error = sessionB_->tryBeginMutation(&message);
   EXPECT_TRUE(error);
   EXPECT_NE(message.find("in use by another"), std::string::npos);

   sessionA_->releaseInUse(InstallLock::Component::ChatBackend, token);
   EXPECT_FALSE(sessionA_->inUseHeld());

   EXPECT_FALSE(sessionB_->tryBeginMutation(&message));
   sessionB_->endMutation();
}

TEST_F(ChatInstallLock, OwnComponentsDoNotBlockOwnMutation)
{
   uint64_t token = 0;
   ASSERT_FALSE(
      sessionA_->acquireInUse(InstallLock::Component::ChatBackend, &token));

   // Update/uninstall legitimately begin while this session's own backend is
   // running; they stop it right after taking the lock.
   std::string message;
   EXPECT_FALSE(sessionA_->tryBeginMutation(&message));

   sessionA_->endMutation();
   sessionA_->releaseInUse(InstallLock::Component::ChatBackend, token);
}

TEST_F(ChatInstallLock, NestedMutationFails)
{
   std::string message;
   ASSERT_FALSE(sessionA_->tryBeginMutation(&message));
   EXPECT_TRUE(sessionA_->tryBeginMutation(&message));

   sessionA_->endMutation();
   EXPECT_FALSE(sessionA_->tryBeginMutation(&message));
   sessionA_->endMutation();
}

TEST_F(ChatInstallLock, StaleSessionFileDoesNotBlockAndIsDeleted)
{
   // A leftover lock file from a crashed session: present on disk, but its
   // owner (a PID beyond pid_max) is gone. File existence alone must never
   // mean "in use".
   FilePath staleFile =
      sessionA_->sessionLocksDir().completePath("session-dead.lock");
   ASSERT_FALSE(staleFile.getParent().ensureDirectory());
   ASSERT_FALSE(writeStringToFile(staleFile, "99999999"));

   std::string message;
   EXPECT_FALSE(sessionB_->tryBeginMutation(&message));
   EXPECT_FALSE(staleFile.exists());
   sessionB_->endMutation();
}

TEST_F(ChatInstallLock, ComponentsShareOneLockAndReleaseOnLast)
{
   uint64_t backendToken = 0;
   uint64_t agentToken = 0;
   ASSERT_FALSE(sessionA_->acquireInUse(
      InstallLock::Component::ChatBackend, &backendToken));
   ASSERT_FALSE(sessionA_->acquireInUse(
      InstallLock::Component::NesAgent, &agentToken));
   EXPECT_TRUE(sessionA_->inUseHeld());

   // Releasing one component keeps the session lock held.
   sessionA_->releaseInUse(InstallLock::Component::ChatBackend, backendToken);
   EXPECT_TRUE(sessionA_->inUseHeld());

   std::string message;
   EXPECT_TRUE(sessionB_->tryBeginMutation(&message));

   // Releasing the last component releases the file lock.
   sessionA_->releaseInUse(InstallLock::Component::NesAgent, agentToken);
   EXPECT_FALSE(sessionA_->inUseHeld());

   EXPECT_FALSE(sessionB_->tryBeginMutation(&message));
   sessionB_->endMutation();
}

TEST_F(ChatInstallLock, AcquireInUseFailsDuringOwnMutation)
{
   std::string message;
   ASSERT_FALSE(sessionA_->tryBeginMutation(&message));

   // A re-entrant start dispatched while this process mutates the install
   // must not launch from the directory being swapped.
   uint64_t token = 0;
   EXPECT_TRUE(
      sessionA_->acquireInUse(InstallLock::Component::ChatBackend, &token));

   sessionA_->endMutation();
   EXPECT_FALSE(
      sessionA_->acquireInUse(InstallLock::Component::ChatBackend, &token));
   sessionA_->releaseInUse(InstallLock::Component::ChatBackend, token);
}

TEST_F(ChatInstallLock, StaleTokenReleaseIsNoOp)
{
   uint64_t firstToken = 0;
   ASSERT_FALSE(sessionA_->acquireInUse(
      InstallLock::Component::ChatBackend, &firstToken));
   sessionA_->releaseInUse(InstallLock::Component::ChatBackend, firstToken);
   EXPECT_FALSE(sessionA_->inUseHeld());

   // Restarted backend: new generation.
   uint64_t secondToken = 0;
   ASSERT_FALSE(sessionA_->acquireInUse(
      InstallLock::Component::ChatBackend, &secondToken));
   EXPECT_NE(firstToken, secondToken);

   // A late exit callback from the previous process must not release the
   // lock the restarted process now holds.
   sessionA_->releaseInUse(InstallLock::Component::ChatBackend, firstToken);
   EXPECT_TRUE(sessionA_->inUseHeld());

   sessionA_->releaseInUse(InstallLock::Component::ChatBackend, secondToken);
   EXPECT_FALSE(sessionA_->inUseHeld());
}

TEST_F(ChatInstallLock, ProbesDoNotReleaseOwnLocks)
{
   uint64_t token = 0;
   ASSERT_FALSE(
      sessionA_->acquireInUse(InstallLock::Component::ChatBackend, &token));

   // Probing must not drop the lock we hold (POSIX fcntl hazard).
   EXPECT_FALSE(sessionA_->updateInProgressElsewhere());

   std::string message;
   EXPECT_TRUE(sessionB_->tryBeginMutation(&message));

   sessionA_->releaseInUse(InstallLock::Component::ChatBackend, token);
}

TEST_F(ChatInstallLock, LocksDirAutoCreated)
{
   EXPECT_FALSE(locksDir_.exists());

   uint64_t token = 0;
   ASSERT_FALSE(
      sessionA_->acquireInUse(InstallLock::Component::ChatBackend, &token));
   EXPECT_TRUE(sessionA_->sessionLocksDir().exists());
   sessionA_->releaseInUse(InstallLock::Component::ChatBackend, token);

   std::string message;
   ASSERT_FALSE(sessionB_->tryBeginMutation(&message));
   sessionB_->endMutation();
}

TEST_F(ChatInstallLock, UpdateInProgressElsewhereReflectsOtherMutator)
{
   std::string message;
   ASSERT_FALSE(sessionA_->tryBeginMutation(&message));

   EXPECT_TRUE(sessionB_->updateInProgressElsewhere());

   // Never self-probes: we hold it, so it is not in progress "elsewhere".
   EXPECT_FALSE(sessionA_->updateInProgressElsewhere());

   sessionA_->endMutation();
   EXPECT_FALSE(sessionB_->updateInProgressElsewhere());
}

TEST_F(ChatInstallLock, MutationScopeReleasesOnDestruction)
{
   {
      MutationScope scope(*sessionA_);
      ASSERT_FALSE(scope.error());
      EXPECT_TRUE(sessionA_->mutationInProgress());

      std::string message;
      EXPECT_TRUE(sessionB_->tryBeginMutation(&message));
   }

   EXPECT_FALSE(sessionA_->mutationInProgress());
   std::string message;
   EXPECT_FALSE(sessionB_->tryBeginMutation(&message));
   sessionB_->endMutation();
}

} // anonymous namespace

#endif // !_WIN32

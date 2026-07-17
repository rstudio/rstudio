/*
 * ChatInstallLockTests.cpp
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

// Link-based locks are unsupported on Windows (FileLock forces advisory
// there), and these tests rely on link-based semantics for cross-instance
// exclusion within one process (POSIX fcntl advisory locks never conflict
// in-process). The lock primitive itself is covered on Windows by
// Win32FileLockTests.cpp; InstallLock's token/component state machine is
// platform-independent and exercised here on POSIX.
#ifndef _WIN32

#include "ChatInstallLock.hpp"

#include <sys/wait.h>
#include <unistd.h>

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
   // A nested begin reports this session's own install-in-progress refusal.
   EXPECT_NE(message.find("installing or updating"), std::string::npos);

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
   EXPECT_NE(message.find("in use by another"), std::string::npos);

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
   Error error =
      sessionA_->acquireInUse(InstallLock::Component::ChatBackend, &token);
   EXPECT_TRUE(error);
   // The refusal names this session's own mutation as the blocker (the
   // description carries the custom text; getMessage() is the errno string).
   EXPECT_NE(error.getProperty("description").find("being modified by this session"),
             std::string::npos);

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

TEST_F(ChatInstallLock, OverlappingGenerationsKeepLockHeldUntilAllReaped)
{
   // A force-terminated backend may still be alive (unreaped) when a new one
   // starts: both generations are outstanding, and the session lock must not
   // release until BOTH exit callbacks have run — otherwise the new process
   // exiting first would unlock while the old one still runs from pai/bin.
   uint64_t oldToken = 0;
   uint64_t newToken = 0;
   ASSERT_FALSE(sessionA_->acquireInUse(
      InstallLock::Component::ChatBackend, &oldToken));
   ASSERT_FALSE(sessionA_->acquireInUse(
      InstallLock::Component::ChatBackend, &newToken));

   sessionA_->releaseInUse(InstallLock::Component::ChatBackend, newToken);
   EXPECT_TRUE(sessionA_->inUseHeld());

   std::string message;
   EXPECT_TRUE(sessionB_->tryBeginMutation(&message));
   EXPECT_NE(message.find("in use by another"), std::string::npos);

   sessionA_->releaseInUse(InstallLock::Component::ChatBackend, oldToken);
   EXPECT_FALSE(sessionA_->inUseHeld());

   EXPECT_FALSE(sessionB_->tryBeginMutation(&message));
   sessionB_->endMutation();
}

TEST_F(ChatInstallLock, StartProbeDoesNotReleaseOwnSessionLock)
{
   // acquireInUseForStart probes install.lock right after taking the session
   // lock; the probe must not drop the lock just acquired (POSIX fcntl
   // hazard), or the in-use protection would silently vanish.
   uint64_t token = 0;
   std::string userMessage;
   ASSERT_FALSE(sessionA_->acquireInUseForStart(
      InstallLock::Component::ChatBackend, &token, &userMessage));

   std::string message;
   EXPECT_TRUE(sessionB_->tryBeginMutation(&message));
   EXPECT_NE(message.find("in use by another"), std::string::npos);

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

TEST_F(ChatInstallLock, AdvisoryLockAcrossProcessesBlocksMutationAndClearsOnExit)
{
   // The production desktop path: advisory locks conflict across processes
   // and are released by the kernel the moment the holder exits — even when
   // the holder never cleans up its lock file.
   InstallLock advisoryLocal(
      locksDir_, "session-local", FileLock::LOCKTYPE_ADVISORY);

   FilePath otherLockFile =
      advisoryLocal.sessionLocksDir().completePath("session-remote.lock");
   ASSERT_FALSE(otherLockFile.getParent().ensureDirectory());

   int lockReady[2];
   int parentDone[2];
   ASSERT_EQ(::pipe(lockReady), 0);
   ASSERT_EQ(::pipe(parentDone), 0);

   pid_t child = ::fork();
   ASSERT_NE(child, -1);
   if (child == 0)
   {
      // Child: hold an advisory lock on another session's file until the
      // parent has observed the blocked mutation, then exit WITHOUT
      // releasing — process exit must free the lock.
      ::close(lockReady[0]);
      ::close(parentDone[1]);

      AdvisoryFileLock remoteLock;
      Error error = remoteLock.acquire(otherLockFile);
      char ok = error ? 0 : 1;
      (void)::write(lockReady[1], &ok, 1);
      ::close(lockReady[1]);

      char buf;
      (void)::read(parentDone[0], &buf, 1);
      ::close(parentDone[0]);
      ::_exit(0);
   }

   ::close(lockReady[1]);
   ::close(parentDone[0]);

   char childOk = 0;
   ASSERT_EQ(::read(lockReady[0], &childOk, 1), 1);
   ::close(lockReady[0]);
   ASSERT_EQ(childOk, 1);

   std::string message;
   Error error = advisoryLocal.tryBeginMutation(&message);
   EXPECT_TRUE(error);
   EXPECT_NE(message.find("in use by another"), std::string::npos);

   ASSERT_EQ(::write(parentDone[1], "x", 1), 1);
   ::close(parentDone[1]);
   int status = 0;
   ASSERT_EQ(::waitpid(child, &status, 0), child);

   // The leftover lock file must not read as "in use" once its holder is
   // gone; the mutation proceeds and cleans it up.
   EXPECT_FALSE(advisoryLocal.tryBeginMutation(&message));
   EXPECT_FALSE(otherLockFile.exists());
   advisoryLocal.endMutation();
}

TEST_F(ChatInstallLock, MutationScopeReleasesOnDestruction)
{
   {
      MutationScope scope(*sessionA_);
      ASSERT_FALSE(scope.error());
      EXPECT_TRUE(sessionA_->mutationInProgress());

      std::string message;
      EXPECT_TRUE(sessionB_->tryBeginMutation(&message));
      // Blocked by the outer session's mutation, not an in-use component.
      EXPECT_NE(message.find("installing or updating"), std::string::npos);
   }

   EXPECT_FALSE(sessionA_->mutationInProgress());
   std::string message;
   EXPECT_FALSE(sessionB_->tryBeginMutation(&message));
   sessionB_->endMutation();
}

TEST_F(ChatInstallLock, ReleaseIgnoresZeroTokenAndWrongComponent)
{
   uint64_t token = 0;
   ASSERT_FALSE(
      sessionA_->acquireInUse(InstallLock::Component::ChatBackend, &token));
   EXPECT_TRUE(sessionA_->inUseHeld());

   // Token 0 is the "never acquired" sentinel (SessionAssistant's Copilot
   // path passes it unconditionally); releasing it must free nothing.
   sessionA_->releaseInUse(InstallLock::Component::ChatBackend, 0);
   EXPECT_TRUE(sessionA_->inUseHeld());

   // A live token released against the wrong component must not free the lock.
   sessionA_->releaseInUse(InstallLock::Component::NesAgent, token);
   EXPECT_TRUE(sessionA_->inUseHeld());

   sessionA_->releaseInUse(InstallLock::Component::ChatBackend, token);
   EXPECT_FALSE(sessionA_->inUseHeld());
}

TEST_F(ChatInstallLock, TwoSessionsBothInUseIsTheSteadyState)
{
   // Two sessions running the backend at once is the normal case. Each names
   // its own lock file by ownerId; were ownerId ignored, the second
   // link-based acquire would collide on one file and fail here.
   uint64_t tokenA = 0;
   uint64_t tokenB = 0;
   ASSERT_FALSE(
      sessionA_->acquireInUse(InstallLock::Component::ChatBackend, &tokenA));
   ASSERT_FALSE(
      sessionB_->acquireInUse(InstallLock::Component::ChatBackend, &tokenB));
   EXPECT_TRUE(sessionA_->inUseHeld());
   EXPECT_TRUE(sessionB_->inUseHeld());

   // B cannot mutate while A is in use: B skips only its own lock file and
   // sees A's.
   std::string message;
   Error error = sessionB_->tryBeginMutation(&message);
   EXPECT_TRUE(error);
   EXPECT_NE(message.find("in use by another"), std::string::npos);

   // Once A releases, B may mutate even while still holding its own component.
   sessionA_->releaseInUse(InstallLock::Component::ChatBackend, tokenA);
   EXPECT_FALSE(sessionB_->tryBeginMutation(&message));
   sessionB_->endMutation();

   sessionB_->releaseInUse(InstallLock::Component::ChatBackend, tokenB);
}

TEST_F(ChatInstallLock, MutationScopeFailureDoesNotEndOuterMutation)
{
   std::string message;
   ASSERT_FALSE(sessionA_->tryBeginMutation(&message));

   {
      // A nested scope fails to begin; its destructor must not release the
      // outer mutation's lock (MutationScope ends only a begin it owns).
      MutationScope nested(*sessionA_);
      EXPECT_TRUE(nested.error());
      EXPECT_NE(nested.userMessage().find("installing or updating"),
                std::string::npos);
   }

   EXPECT_TRUE(sessionA_->mutationInProgress());
   EXPECT_TRUE(sessionB_->tryBeginMutation(&message));

   sessionA_->endMutation();
   EXPECT_FALSE(sessionB_->tryBeginMutation(&message));
   sessionB_->endMutation();
}

TEST_F(ChatInstallLock, NonLockFilesInSessionsDirAreIgnored)
{
   // Link-based locking drops transient proxy files beside the lock files it
   // manages; the mutation probe must neither inspect nor delete a non-.lock
   // entry.
   FilePath proxyFile =
      sessionA_->sessionLocksDir().completePath("proxy.txt");
   ASSERT_FALSE(proxyFile.getParent().ensureDirectory());
   ASSERT_FALSE(writeStringToFile(proxyFile, "transient"));

   std::string message;
   EXPECT_FALSE(sessionA_->tryBeginMutation(&message));
   EXPECT_TRUE(proxyFile.exists());
   sessionA_->endMutation();
}

TEST_F(ChatInstallLock, FailClosedWhenSessionLockUninspectable)
{
   // A directory planted where a session lock file belongs cannot be probed
   // (advisory acquire on a directory throws). The mutation must fail closed
   // rather than assume the "session" is stale and delete it.
   InstallLock advisory(
      locksDir_, "session-advisory", FileLock::LOCKTYPE_ADVISORY);

   FilePath evilLock =
      advisory.sessionLocksDir().completePath("session-evil.lock");
   ASSERT_FALSE(evilLock.ensureDirectory());

   std::string message;
   Error error = advisory.tryBeginMutation(&message);
   EXPECT_TRUE(error);
   EXPECT_NE(message.find("Unable to check"), std::string::npos);
   // Fail-closed must not destroy the thing it could not inspect.
   EXPECT_TRUE(evilLock.exists());

   // Removing the obstruction lets a retry succeed, which also proves
   // install.lock was released on the error path.
   ASSERT_FALSE(evilLock.removeIfExists());
   EXPECT_FALSE(advisory.tryBeginMutation(&message));
   advisory.endMutation();
}

TEST_F(ChatInstallLock, AdvisoryStartProbeIsNonDestructive)
{
   // The start-side probe must observe an advisory install.lock held by
   // another process, yet never unlink the file: an acquire-and-release probe
   // would delete it, opening a takeover race across inodes of the same path.
   InstallLock advisory(
      locksDir_, "session-advisory", FileLock::LOCKTYPE_ADVISORY);
   FilePath installLockFile = advisory.installLockPath();
   ASSERT_FALSE(installLockFile.getParent().ensureDirectory());

   // An existing but unlocked install.lock (a crashed updater's leftover) is
   // not "in progress"; file existence alone must not block starts.
   ASSERT_FALSE(writeStringToFile(installLockFile, ""));
   uint64_t token = 0;
   std::string userMessage;
   ASSERT_FALSE(advisory.acquireInUseForStart(
      InstallLock::Component::ChatBackend, &token, &userMessage));
   advisory.releaseInUse(InstallLock::Component::ChatBackend, token);
   EXPECT_TRUE(installLockFile.exists());

   int lockReady[2];
   int parentDone[2];
   ASSERT_EQ(::pipe(lockReady), 0);
   ASSERT_EQ(::pipe(parentDone), 0);

   pid_t child = ::fork();
   ASSERT_NE(child, -1);
   if (child == 0)
   {
      // Child: hold an advisory lock on install.lock until signaled, then
      // exit WITHOUT releasing so the kernel is what frees the lock.
      ::close(lockReady[0]);
      ::close(parentDone[1]);

      AdvisoryFileLock installLockHeld;
      Error error = installLockHeld.acquire(installLockFile);
      char ok = error ? 0 : 1;
      (void)::write(lockReady[1], &ok, 1);
      ::close(lockReady[1]);

      char buf;
      (void)::read(parentDone[0], &buf, 1);
      ::close(parentDone[0]);
      ::_exit(0);
   }

   ::close(lockReady[1]);
   ::close(parentDone[0]);

   char childOk = 0;
   ASSERT_EQ(::read(lockReady[0], &childOk, 1), 1);
   ::close(lockReady[0]);
   ASSERT_EQ(childOk, 1);

   // Held elsewhere: the start is refused with the retryable message, its
   // session-lock acquisition rolled back, and the file left in place.
   token = 0;
   Error error = advisory.acquireInUseForStart(
      InstallLock::Component::ChatBackend, &token, &userMessage);
   EXPECT_TRUE(error);
   EXPECT_NE(userMessage.find("update is in progress"), std::string::npos);
   EXPECT_EQ(token, 0u);
   EXPECT_FALSE(advisory.inUseHeld());
   EXPECT_TRUE(installLockFile.exists());

   ASSERT_EQ(::write(parentDone[1], "x", 1), 1);
   ::close(parentDone[1]);
   int status = 0;
   ASSERT_EQ(::waitpid(child, &status, 0), child);

   // Holder gone: the start succeeds, and the leftover file the probe never
   // removes stays.
   ASSERT_FALSE(advisory.acquireInUseForStart(
      InstallLock::Component::ChatBackend, &token, &userMessage));
   EXPECT_NE(token, 0u);
   advisory.releaseInUse(InstallLock::Component::ChatBackend, token);
   EXPECT_TRUE(installLockFile.exists());
}

TEST_F(ChatInstallLock, AcquireInUseForStartSucceedsWhenIdle)
{
   uint64_t token = 0;
   std::string userMessage;
   ASSERT_FALSE(sessionA_->acquireInUseForStart(
      InstallLock::Component::ChatBackend, &token, &userMessage));
   EXPECT_NE(token, 0u);
   EXPECT_TRUE(sessionA_->inUseHeld());

   sessionA_->releaseInUse(InstallLock::Component::ChatBackend, token);
   EXPECT_FALSE(sessionA_->inUseHeld());
}

TEST_F(ChatInstallLock, AcquireInUseForStartRollsBackOnOtherMutation)
{
   std::string message;
   ASSERT_FALSE(sessionA_->tryBeginMutation(&message));

   // A start racing another session's mutation must acquire, observe the
   // mutation, then roll its own acquisition back -- leaving no in-use lock
   // behind. That leaked session lock is the regression this combined API
   // exists to prevent.
   uint64_t token = 0;
   std::string userMessage;
   Error error = sessionB_->acquireInUseForStart(
      InstallLock::Component::ChatBackend, &token, &userMessage);
   EXPECT_TRUE(error);
   EXPECT_NE(userMessage.find("update is in progress"), std::string::npos);
   EXPECT_EQ(token, 0u);
   EXPECT_FALSE(sessionB_->inUseHeld());

   sessionA_->endMutation();

   // The refusal is retryable: once the mutation ends, the start succeeds.
   ASSERT_FALSE(sessionB_->acquireInUseForStart(
      InstallLock::Component::ChatBackend, &token, &userMessage));
   EXPECT_NE(token, 0u);
   sessionB_->releaseInUse(InstallLock::Component::ChatBackend, token);
}

TEST_F(ChatInstallLock, AcquireInUseForStartFailsDuringOwnMutation)
{
   std::string message;
   ASSERT_FALSE(sessionA_->tryBeginMutation(&message));

   // Our own active mutation reads as an update in progress: a re-entrant
   // start must not launch from the directory being swapped.
   uint64_t token = 0;
   std::string userMessage;
   Error error = sessionA_->acquireInUseForStart(
      InstallLock::Component::ChatBackend, &token, &userMessage);
   EXPECT_TRUE(error);
   EXPECT_NE(userMessage.find("update is in progress"), std::string::npos);
   EXPECT_EQ(token, 0u);

   sessionA_->endMutation();
}

TEST_F(ChatInstallLock, AcquireInUseForStartSurfacesRealError)
{
   // A non-contention failure must surface the underlying error, not the
   // retryable "update in progress" text -- a disk fault must not masquerade
   // as an update. A regular file where the locks directory belongs makes the
   // session-lock directory uncreatable.
   ASSERT_FALSE(locksDir_.ensureDirectory());
   FilePath fileAsLocksDir = locksDir_.completePath("not-a-directory");
   ASSERT_FALSE(writeStringToFile(fileAsLocksDir, "x"));
   InstallLock broken(
      fileAsLocksDir, "session-broken", FileLock::LOCKTYPE_LINKBASED);

   uint64_t token = 0;
   std::string userMessage;
   Error error = broken.acquireInUseForStart(
      InstallLock::Component::ChatBackend, &token, &userMessage);
   EXPECT_TRUE(error);
   EXPECT_NE(userMessage.find("Unable to verify"), std::string::npos);
   EXPECT_EQ(token, 0u);
}

TEST_F(ChatInstallLock, AcquireInUseForStartFailsClosedWhenInstallLockUninspectable)
{
   // The start-side probe must distinguish an uninspectable install.lock from
   // real contention: an inspection failure (a directory planted in its place)
   // surfaces the honest "Unable to verify" error rather than the retryable
   // "update in progress" text, and must not leak the session lock it just
   // acquired.
   InstallLock advisory(
      locksDir_, "session-advisory", FileLock::LOCKTYPE_ADVISORY);
   ASSERT_FALSE(advisory.installLockPath().ensureDirectory());

   uint64_t token = 0;
   std::string userMessage;
   Error error = advisory.acquireInUseForStart(
      InstallLock::Component::ChatBackend, &token, &userMessage);
   EXPECT_TRUE(error);
   EXPECT_NE(userMessage.find("Unable to verify"), std::string::npos);
   EXPECT_EQ(userMessage.find("update is in progress"), std::string::npos);
   EXPECT_EQ(token, 0u);
   EXPECT_FALSE(advisory.inUseHeld());
}

} // anonymous namespace

#endif // !_WIN32

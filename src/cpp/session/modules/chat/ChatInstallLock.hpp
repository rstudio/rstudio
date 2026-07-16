/*
 * ChatInstallLock.hpp
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

#ifndef SESSION_CHAT_INSTALL_LOCK_HPP
#define SESSION_CHAT_INSTALL_LOCK_HPP

#include <cstdint>
#include <set>
#include <string>

#include <boost/noncopyable.hpp>
#include <boost/optional.hpp>
#include <boost/shared_ptr.hpp>

#include <core/FileLock.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

namespace rstudio {
namespace session {
namespace modules {
namespace chat {
namespace install_lock {

// Coordinates the shared per-user Posit Assistant installation across
// concurrent rsession processes:
//
// - An "in use" lock (locks/sessions/<ownerId>.lock) is held while this
//   session runs the chat backend and/or NES agent from the user-data
//   install. It is a *held* core::FileLock, never a marker file we clean up:
//   advisory locks (desktop) are released by the OS the moment the process
//   dies, and link-based locks (server) go stale via the existing PID/
//   heartbeat machinery — so a crashed session cannot cause a persistent
//   false "in use" report.
// - A mutation lock (locks/install.lock) serializes install/update/uninstall
//   across processes. Mutators additionally refuse to proceed while another
//   session's in-use lock is live.
//
// The two-flag protocol is TOCTOU-safe: mutators take install.lock and then
// probe the session locks; starters take their session lock and then probe
// install.lock. Simultaneous racers each see the other and back off.
//
// All methods must be called on the main thread (both subprocess lifecycles
// run their callbacks there via callbacksRequireMainThread); no internal
// locking is performed.
class InstallLock : boost::noncopyable
{
public:
   enum class Component
   {
      ChatBackend,
      NesAgent
   };

   // locksDir: directory for lock files (e.g. <userDataDir>/pai/locks); it
   //   must live outside the installation directory so it survives the
   //   mutation's rename/delete.
   // ownerId: unique per rsession process; names this session's lock file.
   // lockType: test seam; production omits it and uses the process default
   //   configured by FileLock::initialize() (advisory in desktop mode,
   //   link-based in server mode).
   InstallLock(const core::FilePath& locksDir,
               const std::string& ownerId,
               const boost::optional<core::FileLock::LockType>& lockType =
                  boost::none);

   // Acquires the in-use lock (first component takes the file lock; the
   // second just marks itself held). Fails while this process's own mutation
   // is active: a re-entrant start dispatched during the mutation must not
   // launch from the directory being swapped, and updateInProgressElsewhere()
   // intentionally ignores our own install.lock.
   //
   // On success *pToken receives a generation token identifying this holder.
   // Each acquisition adds an outstanding generation for the component: a
   // force-terminated process may still be alive (unreaped) when its
   // replacement starts, and both generations then legitimately pin the
   // lock at once.
   core::Error acquireInUse(Component component, uint64_t* pToken);

   // Releases one generation of a component. No-op unless the token is
   // outstanding: a late exit callback from a previous process must not
   // release anything twice or on behalf of a replacement. The file lock is
   // released only when no generation of any component remains outstanding.
   void releaseInUse(Component component, uint64_t token);

   // True while any component is held. Because components are released only
   // by reaped-exit callbacks, this is the authoritative "this session's
   // processes are really gone" signal mutators wait on.
   bool inUseHeld() const;

   // Begins a mutation: acquires install.lock (non-blocking), then probes
   // all other sessions' lock files with isLocked(), excluding our own file
   // by name (probing a lock this process holds would release it under POSIX
   // fcntl semantics). Files that probe as unlocked are stale leftovers and
   // are opportunistically deleted.
   //
   // On failure, *pUserMessage receives user-facing text describing why
   // (another mutator, or live sessions in use). This process's own held
   // components do NOT block the mutation — callers stop their own processes
   // right after acquiring the lock.
   core::Error tryBeginMutation(std::string* pUserMessage);

   // Releases install.lock. No-op when no mutation is active.
   void endMutation();

   // In-process flag only; never probes the file (self-probing an advisory
   // lock would release it).
   bool mutationInProgress() const;

   // True when ANOTHER process holds install.lock. Returns false without
   // probing while we hold it ourselves.
   bool updateInProgressElsewhere() const;

   core::FilePath installLockPath() const;
   core::FilePath sessionLocksDir() const;
   core::FilePath ownSessionLockPath() const;

private:
   boost::shared_ptr<core::FileLock> makeLock() const;
   core::FileLock::LockType effectiveLockType() const;
   bool anyComponentHeld() const;

   core::FilePath locksDir_;
   std::string ownerId_;
   boost::optional<core::FileLock::LockType> lockType_;
   boost::shared_ptr<core::FileLock> inUseLock_;
   boost::shared_ptr<core::FileLock> mutationLock_;
   uint64_t nextToken_;
   std::set<uint64_t> componentTokens_[2];
   bool mutationActive_;
};

// RAII wrapper: calls tryBeginMutation() on construction and endMutation()
// on destruction (when the begin succeeded). Check error() after
// construction; userMessage() carries the user-facing failure text.
class MutationScope : boost::noncopyable
{
public:
   explicit MutationScope(InstallLock& lock);
   ~MutationScope();

   const core::Error& error() const { return error_; }
   const std::string& userMessage() const { return userMessage_; }

private:
   InstallLock& lock_;
   core::Error error_;
   std::string userMessage_;
};

} // namespace install_lock
} // namespace chat
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_CHAT_INSTALL_LOCK_HPP

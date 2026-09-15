/*
 * ChatInstallLock.cpp
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

#include "ChatInstallLock.hpp"

#include <algorithm>
#include <cstddef>
#include <ctime>
#include <vector>

#include <fmt/format.h>

#include <core/Log.hpp>

namespace rstudio {
namespace session {
namespace modules {
namespace chat {
namespace install_lock {

using namespace rstudio::core;

namespace {

const char* const kInstallLockFileName = "install.lock";
const char* const kSessionLocksDirName = "sessions";
const char* const kSessionLockSuffix = ".lock";
const char* const kSessionLockEpochMarker = ".epoch-";

bool isEpochSessionLock(const FilePath& path)
{
   std::string stem = path.getStem();
   std::size_t marker = stem.rfind(kSessionLockEpochMarker);
   if (marker == std::string::npos || marker == 0)
      return false;

   std::string epoch = stem.substr(marker + std::string(kSessionLockEpochMarker).size());
   return !epoch.empty() && std::all_of(epoch.begin(), epoch.end(), [](char ch)
   {
      return ch >= '0' && ch <= '9';
   });
}

std::size_t componentIndex(InstallLock::Component component)
{
   return static_cast<std::size_t>(component);
}

std::string mutationContentionMessage()
{
   return "Another RStudio session is currently installing or updating "
          "Posit Assistant. Please wait for it to finish, then try again.";
}

std::string updateInProgressMessage()
{
   return "A Posit Assistant update is in progress. "
          "Please try again in a moment.";
}

std::string sessionsInUseMessage(FileLock::LockType lockType)
{
   std::string message =
      "Posit Assistant is currently in use by another RStudio session. "
      "Close Posit Assistant in your other sessions and try again.";

   // Advisory locks vanish with their process; only link-based locks
   // (macOS, Linux) can linger after a hard crash. A cleanly crashed session's
   // lock clears at the staleness timeout, but one whose process is still
   // alive yet unresponsive is held for the live-owner grace window (a
   // multiple of the timeout) so a briefly stalled session does not lose its
   // lock. Quote that upper bound rather than the bare timeout.
   if (lockType == FileLock::LOCKTYPE_LINKBASED)
   {
      long clearSeconds = FileLock::getTimeoutInterval().total_seconds() *
                          FileLock::getLiveOwnerGraceMultiplier();
      message += fmt::format(
         " If another session ended unexpectedly or stopped "
         "responding, this may take up to {} seconds to clear.",
         clearSeconds);
   }

   return message;
}

enum class LockProbe
{
   Free,
   Held,
   Error
};

// Non-destructive tri-state lock probe. Unlike the bool FileLock::isLocked()
// it distinguishes an inspection error from a held lock so callers can word
// their refusal honestly, and it does not alter the lock object's lifecycle
// state.
LockProbe probeLock(const FilePath& lockFilePath, FileLock::LockType lockType)
{
   bool isLocked = true;
   Error error = FileLock::create(lockType)->isLocked(lockFilePath, &isLocked);
   if (error)
   {
      LOG_ERROR(error);
      return LockProbe::Error;
   }

   return isLocked ? LockProbe::Held : LockProbe::Free;
}

} // anonymous namespace

InstallLock::InstallLock(
   const FilePath& locksDir,
   const std::string& ownerId,
   const boost::optional<FileLock::LockType>& lockType)
   : locksDir_(locksDir),
     ownerId_(ownerId),
     lockType_(lockType),
     sessionLockEpoch_(0),
     nextToken_(0),
     mutationActive_(false)
{
}

Error InstallLock::acquireInUse(Component component, uint64_t* pToken)
{
   *pToken = 0;

   Error error = checkOwnerId();
   if (error)
      return error;

   if (mutationActive_)
   {
      return systemError(
         boost::system::errc::operation_in_progress,
         "The Posit Assistant installation is being modified by this session",
         ERROR_LOCATION);
   }

   if (!anyComponentHeld())
   {
      // Every attempt gets a name this process will never publish again.
      // Released link locks retain their public path, so a mutator can only
      // safely remove that entry if no subsequent start can reuse its name.
      ++sessionLockEpoch_;

      error = sessionLocksDir().ensureDirectory();
      if (error)
         return error;

      boost::shared_ptr<FileLock> lock = makeLock();
      error = lock->acquire(ownSessionLockPath());
      if (error)
         return error;

      inUseLock_ = lock;
   }

   uint64_t token = ++nextToken_;
   componentTokens_[componentIndex(component)].insert(token);
   *pToken = token;
   return Success();
}

Error InstallLock::acquireInUseForStart(Component component,
                                        uint64_t* pToken,
                                        std::string* pUserMessage)
{
   Error error = acquireInUse(component, pToken);
   if (error)
   {
      // contention (our own active mutation, or -- if a caller reuses an
      // owner id across processes -- another live process holding the lock
      // file at our path, see #18571) reads as an update in progress;
      // anything else is a real failure whose message must reach the user
      // rather than masquerading as an update
      bool contention =
         error == systemError(boost::system::errc::operation_in_progress,
                              ErrorLocation()) ||
         FileLock::isNoLockAvailable(error);
      *pUserMessage = contention
         ? updateInProgressMessage()
         : "Unable to verify the Posit Assistant installation state: " +
              error.getMessage();
      return error;
   }

   // Both non-Free outcomes refuse the start — fail closed — but only real
   // contention claims an update is in progress; a genuine inspection
   // failure gets honest text. acquireInUse already failed above if our own
   // mutation is active, so no self-probe guard is needed here.
   switch (probeLock(installLockPath(), effectiveLockType()))
   {
      case LockProbe::Free:
         return Success();

      case LockProbe::Held:
         releaseInUse(component, *pToken);
         *pToken = 0;
         *pUserMessage = updateInProgressMessage();
         return systemError(
            boost::system::errc::device_or_resource_busy,
            "A Posit Assistant install operation is in progress in another "
            "session",
            ERROR_LOCATION);

      case LockProbe::Error:
      default:
         releaseInUse(component, *pToken);
         *pToken = 0;
         *pUserMessage =
            "Unable to verify the Posit Assistant installation state. "
            "Please try again; if the problem persists, restart RStudio.";
         return systemError(
            boost::system::errc::io_error,
            "Unable to inspect the Posit Assistant install lock",
            ERROR_LOCATION);
   }
}

void InstallLock::releaseInUse(Component component, uint64_t token)
{
   if (componentTokens_[componentIndex(component)].erase(token) == 0)
      return;

   if (!anyComponentHeld() && inUseLock_)
   {
      Error error = inUseLock_->release();
      if (error)
         LOG_ERROR(error);
      inUseLock_.reset();
   }
}

bool InstallLock::inUseHeld() const
{
   return anyComponentHeld();
}

Error InstallLock::tryBeginMutation(std::string* pUserMessage)
{
   if (mutationActive_)
   {
      *pUserMessage = mutationContentionMessage();
      return systemError(
         boost::system::errc::operation_in_progress,
         "A Posit Assistant install operation is already in progress "
         "in this session",
         ERROR_LOCATION);
   }

   Error error = checkOwnerId();
   if (error)
   {
      *pUserMessage =
         "Unable to verify the Posit Assistant installation state: " +
         error.getMessage();
      return error;
   }

   error = locksDir_.ensureDirectory();
   if (error)
   {
      *pUserMessage =
         "Unable to lock the Posit Assistant installation: " +
         error.getMessage();
      return error;
   }

   boost::shared_ptr<FileLock> lock = makeLock();
   error = lock->acquire(installLockPath());
   if (error)
   {
      *pUserMessage = FileLock::isNoLockAvailable(error)
         ? mutationContentionMessage()
         : "Unable to lock the Posit Assistant installation: " +
              error.getMessage();
      return error;
   }

   // Probe other sessions' in-use locks. Fail closed if we cannot enumerate:
   // proceeding without verifying would risk mutating under a live session.
   FilePath sessionsDir = sessionLocksDir();
   if (sessionsDir.exists())
   {
      std::vector<FilePath> children;
      Error childError = sessionsDir.getChildren(children);
      if (childError)
      {
         Error releaseError = lock->release();
         if (releaseError)
            LOG_ERROR(releaseError);
         *pUserMessage =
            "Unable to check for other RStudio sessions using "
            "Posit Assistant: " + childError.getMessage();
         return childError;
      }

      for (const FilePath& child : children)
      {
         // Skip our own lock file: this process already holds it. Skip
         // non-.lock entries: link-based locking keeps owner files and its
         // claim namespace beside the lock files it manages.
         if (anyComponentHeld() &&
             child.getFilename() == ownSessionLockPath().getFilename())
            continue;
         if (child.getExtensionLowerCase() != kSessionLockSuffix)
            continue;

         boost::shared_ptr<FileLock> probe = makeLock();
         Error probeError;
         if (effectiveLockType() == FileLock::LOCKTYPE_LINKBASED)
         {
            // Inspect without acquiring: acquisition would replace a stale
            // entry and renew its timestamp, leaving another released file
            // after the probe. Epoch names are never reused, and mutators
            // are serialized by install.lock, so an unlocked epoch entry
            // cannot be replaced by a later start while we remove it.
            bool locked = true;
            probeError = probe->isLocked(child, &locked);
            if (!probeError && locked)
               probeError = FileLock::noLockAvailableError(child);

            if (!probeError)
            {
               // Older clients reuse their per-process name. Keep those
               // entries: even a released marker can become another live
               // lock between this inspection and a pathname deletion.
               if (isEpochSessionLock(child))
               {
                  // removeIfExists() follows symlinks and would miss the
                  // dangling public link left after its owner was removed.
                  Error removeError = child.remove();
                  if (removeError)
                     LOG_ERROR(removeError);
               }
               continue;
            }
         }
         else
         {
            // Advisory files have no released marker. Hold the kernel lock
            // while removing old leftovers; preserve recent files whose
            // creator may still be between creating and locking them.
            probeError = probe->acquire(child);
            if (!probeError)
            {
               // Remove the stale leftover only if we can confirm it is older
               // than the timeout. The unchecked getLastWriteTime() returns 0 on
               // a stat failure (e.g. a transient ESTALE on network storage),
               // which would read as ancient and delete a file that may be
               // seconds old and about to be locked; the checked overload lets us
               // skip removal on such an error instead.
               std::time_t settled =
                  ::time(nullptr) - FileLock::getTimeoutInterval().total_seconds();
               std::time_t lastWrite = 0;
               Error timeError = child.getLastWriteTime(lastWrite);
               if (!timeError && lastWrite < settled)
               {
                  Error removeError = child.removeIfExists();
                  if (removeError)
                     LOG_ERROR(removeError);
               }

               Error releaseError = probe->release();
               if (releaseError)
                  LOG_ERROR(releaseError);
               continue;
            }
         }

         Error releaseError = lock->release();
         if (releaseError)
            LOG_ERROR(releaseError);

         if (FileLock::isNoLockAvailable(probeError))
         {
            *pUserMessage = sessionsInUseMessage(effectiveLockType());
            return systemError(
               boost::system::errc::device_or_resource_busy,
               "Posit Assistant is in use by another RStudio session",
               ERROR_LOCATION);
         }

         *pUserMessage =
            "Unable to check for other RStudio sessions using "
            "Posit Assistant: " + probeError.getMessage();
         return probeError;
      }
   }

   mutationLock_ = lock;
   mutationActive_ = true;
   return Success();
}

void InstallLock::endMutation()
{
   if (!mutationActive_)
      return;

   mutationActive_ = false;
   if (mutationLock_)
   {
      Error error = mutationLock_->release();
      if (error)
         LOG_ERROR(error);
      mutationLock_.reset();
   }
}

bool InstallLock::mutationInProgress() const
{
   return mutationActive_;
}

const std::string& InstallLock::ownerId() const
{
   return ownerId_;
}

FilePath InstallLock::installLockPath() const
{
   return locksDir_.completePath(kInstallLockFileName);
}

FilePath InstallLock::sessionLocksDir() const
{
   return locksDir_.completePath(kSessionLocksDirName);
}

FilePath InstallLock::ownSessionLockPath() const
{
   return sessionLocksDir().completePath(
      fmt::format(
         "{}{}{}{}",
         ownerId_,
         kSessionLockEpochMarker,
         sessionLockEpoch_,
         kSessionLockSuffix));
}

Error InstallLock::checkOwnerId() const
{
   // An empty owner id would name this session's lock file ".epoch-<n>.lock",
   // a path every other session with the same defect shares (#18787). A starter
   // would then contend on it and report an update in progress; a mutator
   // would take a foreign entry for its own, skip probing it, and modify
   // the installation under a live backend. Refuse both instead so the
   // defect surfaces as a real error.
   if (!ownerId_.empty())
      return Success();

   return systemError(
      boost::system::errc::invalid_argument,
      "The Posit Assistant install lock has no owner id; the session "
      "lock file cannot be named",
      ERROR_LOCATION);
}

boost::shared_ptr<FileLock> InstallLock::makeLock() const
{
   if (lockType_)
      return FileLock::create(*lockType_);
   return FileLock::createDefault();
}

FileLock::LockType InstallLock::effectiveLockType() const
{
   return lockType_ ? *lockType_ : FileLock::getDefaultType();
}

bool InstallLock::anyComponentHeld() const
{
   for (const std::set<uint64_t>& tokens : componentTokens_)
   {
      if (!tokens.empty())
         return true;
   }
   return false;
}

MutationScope::MutationScope(InstallLock& lock)
   : lock_(lock)
{
   error_ = lock_.tryBeginMutation(&userMessage_);
}

MutationScope::~MutationScope()
{
   if (!error_)
      lock_.endMutation();
}

} // namespace install_lock
} // namespace chat
} // namespace modules
} // namespace session
} // namespace rstudio

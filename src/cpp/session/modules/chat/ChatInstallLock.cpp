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

#include <cstddef>
#include <vector>

// boost/interprocess requires this undef under mingw64 (mirrors
// core/file_lock/AdvisoryFileLock.cpp)
#if defined(__GNUC__) && defined(_WIN64)
   #undef BOOST_USE_WINDOWS_H
#endif
#include <boost/interprocess/sync/file_lock.hpp>

#include <core/Log.hpp>
#include <core/StringUtils.hpp>

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
   // (server) can linger up to the staleness timeout after a hard crash.
   if (lockType == FileLock::LOCKTYPE_LINKBASED)
   {
      message += " If another session ended unexpectedly, this may take up "
                 "to " +
                 std::to_string(FileLock::getTimeoutInterval().total_seconds()) +
                 " seconds to clear.";
   }

   return message;
}

enum class LockProbe
{
   Free,
   Held,
   Error
};

// Non-destructive tri-state lock probe. Unlike FileLock::isLocked() it
// distinguishes an inspection error from a free lock (so callers can fail
// closed), and unlike an acquire-and-release probe it never unlinks the lock
// file (an advisory release unlocks and then deletes the file, opening a
// takeover race where two mutators end up holding locks on different inodes
// of the same path).
LockProbe probeLock(const FilePath& lockFilePath, FileLock::LockType lockType)
{
   if (!lockFilePath.exists())
      return LockProbe::Free;

   // Link-based locks never run on Windows (FileLock forces advisory there)
   if (lockType == FileLock::LOCKTYPE_LINKBASED)
   {
      return LinkBasedFileLock::isLockFileStale(lockFilePath)
         ? LockProbe::Free
         : LockProbe::Held;
   }

   try
   {
      boost::interprocess::file_lock lock(
         string_utils::utf8ToSystem(lockFilePath.getAbsolutePath()).c_str());
      if (lock.try_lock())
      {
         lock.unlock();
         return LockProbe::Free;
      }
      return LockProbe::Held;
   }
   catch (boost::interprocess::interprocess_exception& e)
   {
      LOG_WARNING_MESSAGE(
         "Unable to inspect lock file '" + lockFilePath.getAbsolutePath() +
         "': " + e.what());
      return LockProbe::Error;
   }
}

} // anonymous namespace

InstallLock::InstallLock(
   const FilePath& locksDir,
   const std::string& ownerId,
   const boost::optional<FileLock::LockType>& lockType)
   : locksDir_(locksDir),
     ownerId_(ownerId),
     lockType_(lockType),
     nextToken_(0),
     mutationActive_(false)
{
}

Error InstallLock::acquireInUse(Component component, uint64_t* pToken)
{
   *pToken = 0;

   if (mutationActive_)
   {
      return systemError(
         boost::system::errc::operation_in_progress,
         "The Posit Assistant installation is being modified by this session",
         ERROR_LOCATION);
   }

   if (!anyComponentHeld())
   {
      Error error = sessionLocksDir().ensureDirectory();
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
      // contention (our own active mutation, or a lock held elsewhere —
      // e.g. this session's own unreleased link-based file, which clears
      // with staleness) reads as an update in progress; anything else is a
      // real failure whose message must reach the user rather than
      // masquerading as an update
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

   if (updateInProgressElsewhere())
   {
      releaseInUse(component, *pToken);
      *pToken = 0;
      *pUserMessage = updateInProgressMessage();
      return systemError(
         boost::system::errc::device_or_resource_busy,
         "A Posit Assistant install operation is in progress in another "
         "session",
         ERROR_LOCATION);
   }

   return Success();
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

   Error error = locksDir_.ensureDirectory();
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
         // Skip our own lock file: probing a lock this process holds would
         // release it under POSIX fcntl semantics. Skip non-.lock entries:
         // link-based locking creates transient proxy files alongside the
         // lock files it manages.
         if (child.getFilename() == ownSessionLockPath().getFilename())
            continue;
         if (child.getExtensionLowerCase() != kSessionLockSuffix)
            continue;

         // Probe by acquisition: isLocked() reports false both for a free
         // lock and when inspection fails, which could delete a live lock.
         // Acquiring distinguishes the cases — success means the file was
         // stale (an advisory leftover from a crash, or a link-based file
         // whose owner is gone; file existence alone never means "in use"),
         // contention means a live session, and anything else fails closed
         // rather than risk mutating under a session we could not check.
         boost::shared_ptr<FileLock> probe = makeLock();
         Error probeError = probe->acquire(child);
         if (!probeError)
         {
            Error releaseError = probe->release();
            if (releaseError)
               LOG_ERROR(releaseError);
            Error removeError = child.removeIfExists();
            if (removeError)
            {
               LOG_WARNING_MESSAGE(
                  "Failed to remove stale Posit Assistant session lock "
                  "file '" + child.getAbsolutePath() + "': " +
                  removeError.getMessage());
            }
            continue;
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

bool InstallLock::updateInProgressElsewhere() const
{
   // Never probe a lock we hold ourselves (self-probing an advisory lock
   // releases it); while our own mutation is active it is not "elsewhere".
   if (mutationActive_)
      return false;

   switch (probeLock(installLockPath(), effectiveLockType()))
   {
      case LockProbe::Free:
         return false;
      case LockProbe::Held:
         return true;
      case LockProbe::Error:
      default:
         // Fail closed: refuse the start rather than launch a process from
         // a directory that may be mid-swap. The refusal is retryable.
         LOG_WARNING_MESSAGE(
            "Unable to inspect the Posit Assistant install lock; "
            "assuming an update is in progress");
         return true;
   }
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
   return sessionLocksDir().completePath(ownerId_ + kSessionLockSuffix);
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

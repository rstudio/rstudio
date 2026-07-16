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

std::size_t componentIndex(InstallLock::Component component)
{
   return static_cast<std::size_t>(component);
}

std::string mutationContentionMessage()
{
   return "Another RStudio session is currently installing or updating "
          "Posit Assistant. Please wait for it to finish, then try again.";
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
      message += " If another session ended unexpectedly, this may take "
                 "up to 30 seconds to clear.";
   }

   return message;
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
     componentTokens_{0, 0},
     mutationActive_(false)
{
}

Error InstallLock::acquireInUse(Component component, uint64_t* pToken)
{
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
   componentTokens_[componentIndex(component)] = token;
   *pToken = token;
   return Success();
}

void InstallLock::releaseInUse(Component component, uint64_t token)
{
   std::size_t index = componentIndex(component);
   if (componentTokens_[index] == 0 || componentTokens_[index] != token)
      return;

   componentTokens_[index] = 0;

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

         if (makeLock()->isLocked(child))
         {
            Error releaseError = lock->release();
            if (releaseError)
               LOG_ERROR(releaseError);
            *pUserMessage = sessionsInUseMessage(effectiveLockType());
            return systemError(
               boost::system::errc::device_or_resource_busy,
               "Posit Assistant is in use by another RStudio session",
               ERROR_LOCATION);
         }

         // Not locked means stale: an advisory leftover from a crash, or a
         // link-based file whose owner is gone. File existence alone never
         // means "in use" — delete opportunistically, log-only on failure.
         Error removeError = child.removeIfExists();
         if (removeError)
         {
            LOG_WARNING_MESSAGE(
               "Failed to remove stale Posit Assistant session lock file '" +
               child.getAbsolutePath() + "': " + removeError.getMessage());
         }
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

   return makeLock()->isLocked(installLockPath());
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
   return componentTokens_[0] != 0 || componentTokens_[1] != 0;
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

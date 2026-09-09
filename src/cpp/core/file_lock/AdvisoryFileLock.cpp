/*
 * AdvisoryFileLock.cpp
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

#include <map>
#include <sstream>
#include <string>

#include <core/FileLock.hpp>

#include <sys/types.h>
#include <sys/stat.h>

#ifndef _WIN32
# include <unistd.h>
#endif

#include <boost/filesystem/operations.hpp>
#include <boost/thread/condition_variable.hpp>
#include <boost/thread/mutex.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

#include <core/Log.hpp>
#include <core/StringUtils.hpp>
#include <core/system/System.hpp>

#include <core/BoostErrors.hpp>

#define LOG(__X__)                                                             \
   do                                                                          \
   {                                                                           \
      std::stringstream ss;                                                    \
      ss << "(PID " << ::getpid() << "): " << __X__ << std::endl;              \
      ::rstudio::core::FileLock::log(ss.str());                                \
   } while (0)

// we define BOOST_USE_WINDOWS_H on mingw64 to work around some
// incompatibilities. however, this prevents the interprocess headers
// from compiling so we undef it in this localized context
#if defined(__GNUC__) && defined(_WIN64)
   #undef BOOST_USE_WINDOWS_H
#endif
#include <boost/interprocess/sync/file_lock.hpp>

namespace rstudio {
namespace core {

namespace {
typedef boost::interprocess::file_lock BoostFileLock;

// Per-path bookkeeping for this process. POSIX fcntl locks are process-scoped:
// closing any descriptor for a file drops every lock this process holds on
// it, so nothing in this process may open a lock path while another object
// here holds (or is acquiring) a lock on it. Probes may overlap each other,
// since with no holder there is no lock for a close to drop.
struct PathState
{
   bool held = false;
   bool acquiring = false;
   int probes = 0;

   bool idle() const
   {
      return !held && !acquiring && probes == 0;
   }
};

class AdvisoryLockRegistration : boost::noncopyable
{
public:
   AdvisoryLockRegistration()
      : processId_(system::currentProcessId())
   {
   }

   // Reserves the key for acquisition, then waits out probes that still have
   // the file open. Taking the reservation first gives the acquirer priority
   // over a steady stream of probes. Returns false if this process already
   // holds the lock.
   bool beginAcquire(const std::string& key)
   {
      boost::mutex::scoped_lock lock(mutex_);
      for (;;)
      {
         resetAfterFork();
         PathState& state = states_[key];
         if (state.held)
         {
            prune(key);
            return false;
         }

         if (!state.acquiring)
         {
            state.acquiring = true;
            break;
         }

         condition_.wait(lock);
      }

      while (states_[key].probes > 0)
         condition_.wait(lock);
      return true;
   }

   void endAcquire(const std::string& key, bool held)
   {
      boost::mutex::scoped_lock lock(mutex_);
      resetAfterFork();
      auto it = states_.find(key);
      if (it != states_.end())
      {
         it->second.acquiring = false;
         it->second.held = held;
         prune(key);
      }
      condition_.notify_all();
   }

   void release(const std::string& key)
   {
      boost::mutex::scoped_lock lock(mutex_);
      resetAfterFork();
      auto it = states_.find(key);
      if (it != states_.end())
      {
         it->second.held = false;
         prune(key);
      }
      condition_.notify_all();
   }

   // Registers a probe unless this process holds the lock, in which case it
   // returns false and the caller can answer "locked" without opening the
   // file. An in-flight acquisition is waited out so the answer reflects it.
   bool beginProbe(const std::string& key)
   {
      boost::mutex::scoped_lock lock(mutex_);
      for (;;)
      {
         resetAfterFork();
         PathState& state = states_[key];
         if (state.held)
         {
            prune(key);
            return false;
         }

         if (!state.acquiring)
         {
            ++state.probes;
            return true;
         }

         condition_.wait(lock);
      }
   }

   void endProbe(const std::string& key)
   {
      boost::mutex::scoped_lock lock(mutex_);
      resetAfterFork();
      auto it = states_.find(key);
      if (it != states_.end() && it->second.probes > 0)
      {
         --it->second.probes;
         prune(key);
      }
      condition_.notify_all();
   }

private:
   void prune(const std::string& key)
   {
      auto it = states_.find(key);
      if (it != states_.end() && it->second.idle())
         states_.erase(it);
   }

   void resetAfterFork()
   {
      // fcntl locks are not inherited, so a child starts with nothing held
      PidType processId = system::currentProcessId();
      if (processId_ != processId)
      {
         states_.clear();
         processId_ = processId;
      }
   }

   boost::mutex mutex_;
   boost::condition_variable condition_;
   std::map<std::string, PathState> states_;
   PidType processId_;
};

AdvisoryLockRegistration& lockRegistration()
{
   // Lock objects can be destroyed during static shutdown. Keep the registry
   // alive for the lifetime of the process so those destructors remain safe.
   static AdvisoryLockRegistration* pInstance =
      new AdvisoryLockRegistration();
   return *pInstance;
}

// Same-process bookkeeping keys on the file the kernel will actually open.
// canonical() is only defined for existing paths, so the final component is
// resolved by hand: a lock path that is a symlink keys on its target whether
// or not that target exists yet, and a plain path keys the same way before and
// after the file is created.
std::string registrationKey(const FilePath& lockFilePath)
{
   FilePath path = lockFilePath;

#ifndef _WIN32
   for (int depth = 0; depth < 40; ++depth)
   {
      struct stat info;
      if (::lstat(path.getAbsolutePathNative().c_str(), &info) == -1)
         break;
      if (!S_ISLNK(info.st_mode))
         break;

      std::string target;
      Error error = path.readSymlink(target);
      if (error)
         break;

      FilePath targetPath(target);
      path = targetPath.isAbsolute()
         ? targetPath
         : path.getParent().completePath(target);
   }
#endif

   return path.getParent().getCanonicalPath() + "/" + path.getFilename();
}

class ProbeScope : boost::noncopyable
{
public:
   explicit ProbeScope(const std::string& key)
      : key_(key),
        active_(lockRegistration().beginProbe(key))
   {
   }

   ~ProbeScope()
   {
      if (active_)
         lockRegistration().endProbe(key_);
   }

   bool active() const
   {
      return active_;
   }

private:
   std::string key_;
   bool active_;
};

class AcquireScope : boost::noncopyable
{
public:
   explicit AcquireScope(const std::string& key)
      : key_(key),
        active_(lockRegistration().beginAcquire(key)),
        held_(false)
   {
   }

   ~AcquireScope()
   {
      if (active_)
         lockRegistration().endAcquire(key_, held_);
   }

   bool active() const
   {
      return active_;
   }

   void markHeld()
   {
      held_ = true;
   }

private:
   std::string key_;
   bool active_;
   bool held_;
};

Error noLockAvailableError(const FilePath& lockFilePath)
{
   Error error = systemError(
      boost::system::errc::no_lock_available,
      ERROR_LOCATION);
   error.addProperty("lock-file", lockFilePath);
   return error;
}

} // anonymous namespace

struct AdvisoryFileLock::Impl
{
   FilePath lockFilePath;
   std::string registrationKey;
   BoostFileLock lock;
};

bool AdvisoryFileLock::isLocked(const FilePath& lockFilePath) const
{
   bool isLocked = false;
   Error error = this->isLocked(lockFilePath, &isLocked);
   if (error)
   {
      LOG_ERROR(error);
      return false;
   }

   return isLocked;
}

Error AdvisoryFileLock::isLocked(const FilePath& lockFilePath,
                                 bool* pIsLocked) const
{
   *pIsLocked = true;

   std::string systemPath =
      string_utils::utf8ToSystem(lockFilePath.getAbsolutePath());
   boost::system::error_code existsError;
   bool exists = boost::filesystem::exists(systemPath, existsError);
   if (existsError)
   {
      Error error(existsError, ERROR_LOCATION);
      error.addProperty("lock-file", lockFilePath);
      return error;
   }

   if (!exists)
   {
      *pIsLocked = false;
      return Success();
   }

   // Held by this process: answer without opening the file (see PathState).
   ProbeScope probe(registrationKey(lockFilePath));
   if (!probe.active())
      return Success();

   // Check whether another process holds the lock. The BoostFileLock must be
   // destroyed (its descriptor closed) before the probe scope ends, or a
   // waiting acquirer could lock the file just before that close drops it.
   try
   {
      BoostFileLock lock(systemPath.c_str());
      if (lock.try_lock())
      {
         lock.unlock();
         *pIsLocked = false;
      }
      return Success();
   }
   catch (boost::interprocess::interprocess_exception& e)
   {
      Error error(boost::interprocess::ec_from_exception(e), ERROR_LOCATION);
      error.addProperty("lock-file", lockFilePath);
      return error;
   }
}

AdvisoryFileLock::AdvisoryFileLock()
   : pImpl_(new Impl())
{
}

AdvisoryFileLock::~AdvisoryFileLock()
{
   if (!pImpl_->registrationKey.empty())
   {
      Error error = release();
      if (error)
         LOG_ERROR(error);
   }
}

Error AdvisoryFileLock::acquire(const FilePath& lockFilePath)
{
   using namespace boost::interprocess;

   if (!pImpl_->registrationKey.empty())
      return noLockAvailableError(lockFilePath);

   Error error = lockFilePath.getParent().ensureDirectory();
   if (error)
      return error;

   // Reserve the path before opening the file, and wait out any probe that
   // still has it open: its close would drop the lock we are about to take.
   std::string key = registrationKey(lockFilePath);
   AcquireScope reservation(key);
   if (!reservation.active())
      return noLockAvailableError(lockFilePath);

   // Advisory lock files are intentionally persistent. Deleting one on
   // release allows contenders to lock different inodes at the same path.
   error = lockFilePath.ensureFile();
   if (error)
      return error;

   try
   {
      BoostFileLock lock(
         string_utils::utf8ToSystem(
            lockFilePath.getAbsolutePath()).c_str());

      if (!lock.try_lock())
      {
         LOG("Failed to acquire lock: " << lockFilePath.getAbsolutePath());
         return noLockAvailableError(lockFilePath);
      }

      LOG("Acquired lock: " << lockFilePath.getAbsolutePath());
      pImpl_->lockFilePath = lockFilePath;
      pImpl_->registrationKey = key;
      pImpl_->lock.swap(lock);
      reservation.markHeld();
      return Success();
   }
   catch (interprocess_exception& e)
   {
      Error error(ec_from_exception(e), ERROR_LOCATION);
      error.addProperty("lock-file", lockFilePath);
      return error;
   }
}

Error AdvisoryFileLock::release()
{
   using namespace boost::interprocess;

   if (pImpl_->registrationKey.empty())
      return noLockAvailableError(pImpl_->lockFilePath);

   Error error;
   try
   {
      pImpl_->lock.unlock();
      LOG("Released lock: " << pImpl_->lockFilePath.getAbsolutePath());
   }
   catch (interprocess_exception& e)
   {
      error = Error(ec_from_exception(e), ERROR_LOCATION);
      error.addProperty("lock-file", pImpl_->lockFilePath);
   }

   // Close the descriptor before allowing another file_lock in this process
   // to open the same path.
   pImpl_->lock = BoostFileLock();
   lockRegistration().release(pImpl_->registrationKey);
   pImpl_->registrationKey.clear();
   pImpl_->lockFilePath = FilePath();
   return error;
}

FilePath AdvisoryFileLock::lockFilePath() const
{
   return pImpl_->lockFilePath;
}

void AdvisoryFileLock::refresh()
{
}

void AdvisoryFileLock::cleanUp()
{
}

} // namespace core
} // namespace rstudio

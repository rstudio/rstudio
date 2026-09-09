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
#include <memory>
#include <sstream>
#include <string>
#include <vector>

#include <core/FileLock.hpp>

#include <sys/types.h>
#include <sys/stat.h>

#ifndef _WIN32
# include <unistd.h>
#endif

#include <boost/filesystem/operations.hpp>
#include <boost/scoped_ptr.hpp>

#include "ForkAwareRegistry.hpp"

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

class AdvisoryLockRegistration : public file_lock::ForkAwareRegistry
{
public:
   // Reserves the key for acquisition, then waits out probes that still have
   // the file open. Taking the reservation first gives the acquirer priority
   // over a steady stream of probes. Returns false if this process already
   // holds the lock.
   bool beginAcquire(const std::string& key)
   {
      Guard guard(*this);
      for (;;)
      {
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

         guard.wait();
      }

      while (states_[key].probes > 0)
         guard.wait();
      return true;
   }

   void endAcquire(const std::string& key, bool held)
   {
      Guard guard(*this);
      auto it = states_.find(key);
      if (it != states_.end())
      {
         it->second.acquiring = false;
         it->second.held = held;
         prune(key);
      }
      notifyAll();
   }

   void release(const std::string& key)
   {
      Guard guard(*this);
      auto it = states_.find(key);
      if (it != states_.end())
      {
         it->second.held = false;
         prune(key);
      }
      notifyAll();
   }

   // Registers a probe unless this process holds the lock, in which case it
   // returns false and the caller can answer "locked" without opening the
   // file. An in-flight acquisition is waited out so the answer reflects it.
   bool beginProbe(const std::string& key)
   {
      Guard guard(*this);
      for (;;)
      {
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

         guard.wait();
      }
   }

   void endProbe(const std::string& key)
   {
      Guard guard(*this);
      auto it = states_.find(key);
      if (it != states_.end() && it->second.probes > 0)
      {
         --it->second.probes;
         prune(key);
      }
      notifyAll();
   }

   // Keeps the descriptor of a lock object inherited across fork() open for
   // the life of the process: closing it would drop any lock this process
   // has since taken on the same inode.
   void parkInheritedLock(BoostFileLock& lock)
   {
      Guard guard(*this);
      parked_.push_back(std::unique_ptr<BoostFileLock>(new BoostFileLock()));
      parked_.back()->swap(lock);
   }

private:
   void prune(const std::string& key)
   {
      auto it = states_.find(key);
      if (it != states_.end() && it->second.idle())
         states_.erase(it);
   }

   void resetInChild() override
   {
      // fcntl locks are not inherited, so a child starts with nothing held;
      // closing the parent's parked descriptors is safe at this point for
      // the same reason
      states_.clear();
      parked_.clear();
   }

   std::map<std::string, PathState> states_;
   std::vector<std::unique_ptr<BoostFileLock> > parked_;
};

AdvisoryLockRegistration& lockRegistration()
{
   // Lock objects can be destroyed during static shutdown. Keep the registry
   // alive for the lifetime of the process so those destructors remain safe.
   static AdvisoryLockRegistration* pInstance =
      file_lock::ForkAwareRegistry::publish(new AdvisoryLockRegistration());
   return *pInstance;
}

// Same-process bookkeeping uses two keys. The path key names the file the
// kernel will open and is stable before and after the file exists: canonical()
// is only defined for existing paths, so the final component is resolved by
// hand, and a lock path that is a symlink keys on its target either way. It
// serializes creation of the file. The inode key identifies the file itself
// once it exists, so that hard links to one inode (which fcntl treats as one
// lock) share a single registration.
std::string pathKey(const FilePath& lockFilePath)
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

// The inode key of an existing lock file; empty if it does not exist.
Error inodeKey(const FilePath& lockFilePath, std::string* pKey)
{
   pKey->clear();
#ifdef _WIN32
   // st_ino is not meaningful on Windows; fall back to the path
   if (lockFilePath.exists())
      *pKey = pathKey(lockFilePath);
   return Success();
#else
   struct stat info;
   if (::stat(lockFilePath.getAbsolutePathNative().c_str(), &info) == -1)
   {
      if (errno == ENOENT)
         return Success();

      Error error = systemError(errno, ERROR_LOCATION);
      error.addProperty("lock-file", lockFilePath);
      return error;
   }

   std::ostringstream stream;
   stream << "inode:" << info.st_dev << ":" << info.st_ino;
   *pKey = stream.str();
   return Success();
#endif
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
   Impl()
      : processId(0)
   {
   }

   FilePath lockFilePath;
   std::string registrationKey;
   std::string inodeRegistrationKey;
   BoostFileLock lock;
   PidType processId;
};

bool AdvisoryFileLock::isLocked(const FilePath& lockFilePath) const
{
   bool isLocked = true;
   Error error = this->isLocked(lockFilePath, &isLocked);
   if (error)
      LOG_ERROR(error);

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

   // Held by this process (under any name): answer without opening the file.
   std::string key;
   Error keyError = inodeKey(lockFilePath, &key);
   if (keyError)
      return keyError;
   if (key.empty())
   {
      *pIsLocked = false;
      return Success();
   }

   ProbeScope probe(key);
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

   // Reserve the path before creating the file: ensureFile() opens and closes
   // a descriptor, which would drop a lock this process already holds on it.
   std::string key = pathKey(lockFilePath);
   AcquireScope reservation(key);
   if (!reservation.active())
      return noLockAvailableError(lockFilePath);

   // Advisory lock files are intentionally persistent. Deleting one on
   // release allows contenders to lock different inodes at the same path.
   error = lockFilePath.ensureFile();
   if (error)
      return error;

   // Then reserve the inode itself, waiting out any probe (through any name)
   // that still has it open: its close would drop the lock about to be taken.
   // Where the inode key is the path key (Windows), the reservation above
   // already covers it and a second one would wait on itself.
   std::string inode;
   error = inodeKey(lockFilePath, &inode);
   if (error)
      return error;
   if (inode.empty())
      return fileNotFoundError(lockFilePath, ERROR_LOCATION);

   boost::scoped_ptr<AcquireScope> pInodeReservation;
   if (inode != key)
   {
      pInodeReservation.reset(new AcquireScope(inode));
      if (!pInodeReservation->active())
         return noLockAvailableError(lockFilePath);
   }

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

      // The lock was taken by name; make sure the path still leads to the
      // inode reserved above (an older RStudio may unlink and recreate lock
      // files), or the registry would guard one inode while the kernel lock
      // sits on another.
      std::string lockedInode;
      error = inodeKey(lockFilePath, &lockedInode);
      if (!error && lockedInode != inode)
         error = noLockAvailableError(lockFilePath);
      if (error)
      {
         lock.unlock();
         return error;
      }

      LOG("Acquired lock: " << lockFilePath.getAbsolutePath());
      pImpl_->lockFilePath = lockFilePath;
      pImpl_->registrationKey = key;
      pImpl_->inodeRegistrationKey = inode;
      pImpl_->lock.swap(lock);
      pImpl_->processId = system::currentProcessId();
      reservation.markHeld();
      if (pInodeReservation)
         pInodeReservation->markHeld();
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
   if (pImpl_->processId != system::currentProcessId())
   {
      // Inherited across fork(): the kernel lock belongs to the parent and
      // this process's registry never recorded it. Unlocking or closing here
      // would instead drop a lock this process took on the same inode, and
      // deregistering would forget it.
      lockRegistration().parkInheritedLock(pImpl_->lock);
      LOG("Discarded inherited lock: " << pImpl_->lockFilePath.getAbsolutePath());
   }
   else
   {
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

      // Close the descriptor before allowing another file_lock in this
      // process to open the same path.
      pImpl_->lock = BoostFileLock();
      if (pImpl_->inodeRegistrationKey != pImpl_->registrationKey)
         lockRegistration().release(pImpl_->inodeRegistrationKey);
      lockRegistration().release(pImpl_->registrationKey);
   }

   pImpl_->registrationKey.clear();
   pImpl_->inodeRegistrationKey.clear();
   pImpl_->lockFilePath = FilePath();
   pImpl_->processId = 0;
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

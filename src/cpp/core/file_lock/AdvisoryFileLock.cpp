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

#include <set>
#include <sstream>

#include <core/FileLock.hpp>

#include <boost/filesystem/operations.hpp>
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

class AdvisoryLockRegistration : boost::noncopyable
{
public:
   AdvisoryLockRegistration()
      : processId_(system::currentProcessId())
   {
   }

   bool tryRegister(const std::string& lockFilePath)
   {
      boost::mutex::scoped_lock lock(mutex_);
      resetAfterFork();
      return registrations_.insert(lockFilePath).second;
   }

   void deregister(const std::string& lockFilePath)
   {
      boost::mutex::scoped_lock lock(mutex_);
      resetAfterFork();
      registrations_.erase(lockFilePath);
   }

private:
   void resetAfterFork()
   {
      PidType processId = system::currentProcessId();
      if (processId_ != processId)
      {
         registrations_.clear();
         processId_ = processId;
      }
   }

   boost::mutex mutex_;
   std::set<std::string> registrations_;
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

std::string registrationKey(const FilePath& lockFilePath)
{
   if (lockFilePath.exists())
      return lockFilePath.getCanonicalPath();

   return lockFilePath.getParent().getCanonicalPath() + "/" +
          lockFilePath.getFilename();
}

class AdvisoryLockReservation : boost::noncopyable
{
public:
   explicit AdvisoryLockReservation(const FilePath& lockFilePath)
      : key_(registrationKey(lockFilePath)),
        registered_(lockRegistration().tryRegister(key_))
   {
   }

   ~AdvisoryLockReservation()
   {
      if (registered_)
         lockRegistration().deregister(key_);
   }

   bool registered() const
   {
      return registered_;
   }

   const std::string& key() const
   {
      return key_;
   }

   void retain()
   {
      registered_ = false;
   }

private:
   std::string key_;
   bool registered_;
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

   AdvisoryLockReservation reservation(lockFilePath);
   if (!reservation.registered())
      return Success();

   // Check whether another process holds the lock. The reservation prevents
   // another file_lock in this process from opening and closing the same file;
   // POSIX fcntl locks are process-scoped and any such close would release all
   // of this process's locks on the file.
   try
   {
      BoostFileLock lock(
         systemPath.c_str());

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

   // Reserve the process-wide path before opening the file. Otherwise, a
   // second thread that started creating the file earlier could close its
   // descriptor after this thread acquired the process-scoped POSIX lock.
   AdvisoryLockReservation reservation(lockFilePath);
   if (!reservation.registered())
      return noLockAvailableError(lockFilePath);

   // Advisory lock files are intentionally persistent. Deleting one on
   // release allows contenders to lock different inodes at the same path.
   error = lockFilePath.ensureFile();
   if (error)
      return error;

   // try to acquire the lock
   try
   {
      BoostFileLock lock(
         string_utils::utf8ToSystem(
            lockFilePath.getAbsolutePath()).c_str());

      if (lock.try_lock())
      {
         LOG("Acquired lock: " << lockFilePath.getAbsolutePath());
         // set members
         pImpl_->lockFilePath = lockFilePath;
         pImpl_->registrationKey = reservation.key();
         pImpl_->lock.swap(lock);
         reservation.retain();

         return Success();
      }
      else
      {
         LOG("Failed to acquire lock: " << lockFilePath.getAbsolutePath());
         return noLockAvailableError(lockFilePath);
      }
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
   lockRegistration().deregister(pImpl_->registrationKey);
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

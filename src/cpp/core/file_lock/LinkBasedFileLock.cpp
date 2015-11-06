/*
 * LinkBasedFileLock.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <core/FileLock.hpp>

#include <errno.h>

#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <fcntl.h>

#include <set>
#include <vector>

#include <core/Algorithm.hpp>
#include <core/Thread.hpp>
#include <core/Error.hpp>
#include <core/Log.hpp>
#include <core/FilePath.hpp>
#include <core/FileSerializer.hpp>

#include <boost/foreach.hpp>

namespace rstudio {
namespace core {

namespace {

std::string makePidString()
{
   // get the pid (as a string)
   std::stringstream ss;
   ss << (long) ::getpid();
   return ss.str();
}
const std::string& pidString()
{
   static std::string instance = makePidString();
   return instance;
}

std::string makeHostName()
{
   char buffer[256];
   int status = ::gethostname(buffer, 255);
   if (status)
      LOG_ERROR(systemError(errno, ERROR_LOCATION));
   return std::string(buffer);
}
const std::string& hostName()
{
   static std::string instance = makeHostName();
   return instance;
}

std::string threadId()
{
   std::stringstream ss;
   ss << boost::this_thread::get_id();
   return ss.str().substr(2);
}

std::string proxyLockFileName()
{
   return std::string() +
         ".rstudio-lock" + "-" +
         hostName()      + "-" +
         pidString()     + "-" +
         threadId();
         
}

struct CloseFileScope
{
   CloseFileScope(int fd, ErrorLocation location)
      : fd_(fd), location_(location)
   {}
   
   ~CloseFileScope()
   {
      int errc = ::close(fd_);
      if (errc)
         LOG_ERROR(systemError(errno, location_));
   }
   
   int fd_;
   ErrorLocation location_;
};

class LockRegistration : boost::noncopyable
{
public:
   
   void registerLock(const FilePath& lockFilePath)
   {
      LOCK_MUTEX(mutex_)
      {
         registration_.insert(lockFilePath);
      }
      END_LOCK_MUTEX
   }
   
   void deregisterLock(const FilePath& lockFilePath)
   {
      LOCK_MUTEX(mutex_)
      {
         registration_.erase(lockFilePath);
      }
      END_LOCK_MUTEX
   }
   
   void refreshLocks()
   {
      LOCK_MUTEX(mutex_)
      {
         BOOST_FOREACH(const FilePath& lockFilePath, registration_)
         {
            lockFilePath.setLastWriteTime();
         }
      }
      END_LOCK_MUTEX
   }
   
   void clearLocks()
   {
      LOCK_MUTEX(mutex_)
      {
         BOOST_FOREACH(const FilePath& lockFilePath, registration_)
         {
            Error error = lockFilePath.removeIfExists();
            if (error)
               LOG_ERROR(error);
         }
         registration_.clear();
      }
      END_LOCK_MUTEX
   }
   
   ~LockRegistration()
   {
      try
      {
         clearLocks();
      }
      catch (...)
      {
      }
   }
   
private:
   
   boost::mutex mutex_;
   std::set<FilePath> registration_;
   
};

LockRegistration& lockRegistration()
{
   static LockRegistration instance;
   return instance;
}

Error writeLockFile(const FilePath& lockFilePath)
{
   
#ifndef _WIN32
   
   // generate proxy lockfile
   FilePath proxyPath = lockFilePath.parent().complete(proxyLockFileName());
   if (proxyPath.exists())
      return fileExistsError(ERROR_LOCATION);
   
   Error error = core::writeStringToFile(proxyPath, "");
   if (error)
      return error;
   RemoveOnExitScope scope(proxyPath, ERROR_LOCATION);
   
   // attempt to link to the desired location -- ignore return value
   // and just stat our original link after
   ::link(
            proxyPath.absolutePathNative().c_str(),
            lockFilePath.absolutePathNative().c_str());
   
   struct stat info;
   int errc = ::stat(proxyPath.absolutePathNative().c_str(), &info);
   if (errc)
      return systemError(errno, ERROR_LOCATION);
   
   if (info.st_nlink != 2)
      return fileExistsError(ERROR_LOCATION);
   
   return Success();
   
#else
   
   return -1;
   
#endif
}

bool isLockFileStale(const FilePath& lockFilePath, double seconds = 30.0)
{
   double diff = ::difftime(::time(NULL), lockFilePath.lastWriteTime());
   return diff >= seconds;
}

Error beginLocking(const FilePath& lockingPath)
{
   // attempt to clean up stale lockfiles
   if (lockingPath.exists() && isLockFileStale(lockingPath))
   {
      Error error = lockingPath.remove();
      if (error)
         LOG_ERROR(error);
   }
   
   // write the lock file to take ownership
   return writeLockFile(lockingPath);
}

} // end anonymous namespace

struct LinkBasedFileLock::Impl
{
   FilePath lockFilePath;
};

LinkBasedFileLock::LinkBasedFileLock()
   : pImpl_(new Impl())
{
}

LinkBasedFileLock::~LinkBasedFileLock()
{
}

FilePath LinkBasedFileLock::lockFilePath() const
{
   return pImpl_->lockFilePath;
}

bool LinkBasedFileLock::isLocked(const FilePath& lockFilePath)
{
   if (!lockFilePath.exists())
      return false;
   
   return !isLockFileStale(lockFilePath);
}

Error LinkBasedFileLock::acquire(const FilePath& lockFilePath)
{
   // use filesystem-based mutex to ensure interprocess locking; ie, attempt to
   // ensure that only one process can attempt to acquire a lock at a time
   FilePath lockingPath = lockFilePath.parent().complete(".rstudio-locking");
   Error error = beginLocking(lockingPath);
   if (error)
      return error;
   RemoveOnExitScope scope(lockingPath, ERROR_LOCATION);
   
   // bail if the lock file exists and is active
   if (lockFilePath.exists() && !isLockFileStale(lockFilePath))
      return fileExistsError(ERROR_LOCATION);
   
   // create or update the modified time of the lockfile (this allows
   // us to claim ownership of the file)
   if (lockFilePath.exists())
   {
      lockFilePath.setLastWriteTime();
   }
   else
   {
      Error error = lockFilePath.parent().ensureDirectory();
      if (error)
         return error;
      
      error = writeLockFile(lockFilePath);
      if (error)
         return error;
   }
   
   pImpl_->lockFilePath = lockFilePath;
   lockRegistration().registerLock(lockFilePath);
   return Success();
}

Error LinkBasedFileLock::release()
{
   const FilePath& lockFilePath = pImpl_->lockFilePath;
   
   Error error = lockFilePath.remove();
   if (error)
      LOG_ERROR(error);
   
   pImpl_->lockFilePath = FilePath();
   lockRegistration().deregisterLock(lockFilePath);
   return error;
}

void LinkBasedFileLock::refresh()
{
   lockRegistration().refreshLocks();
}

} // namespace core
} // namespace rstudio

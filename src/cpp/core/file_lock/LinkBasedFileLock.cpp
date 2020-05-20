/*
 * LinkBasedFileLock.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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
#include <fcntl.h>

#ifdef _MSC_VER
# include <io.h>
#else
# include <unistd.h>
#endif

#include <set>
#include <vector>

#include <shared_core/SafeConvert.hpp>
#include <core/Algorithm.hpp>
#include <core/Thread.hpp>
#include <shared_core/Error.hpp>
#include <core/Log.hpp>
#include <shared_core/FilePath.hpp>
#include <core/FileSerializer.hpp>
#include <core/system/Process.hpp>
#include <core/system/System.hpp>

#include <boost/system/error_code.hpp>

#define LOG(__X__)                                                             \
   do                                                                          \
   {                                                                           \
      std::stringstream ss;                                                    \
      ss << "(PID " << ::getpid() << "): " << __X__ << std::endl;              \
      ::rstudio::core::FileLock::log(ss.str());                                \
   } while (0)

namespace rstudio {
namespace core {

namespace {

const char * const kFileLockPrefix = ".rstudio-lock-41c29";

std::string pidString()
{
   PidType pid = system::currentProcessId();
   return safe_convert::numberToString((long) pid);
}

std::string hostName()
{
   char buffer[256];
   int status = ::gethostname(buffer, 255);
   if (status)
      LOG_ERROR(systemError(errno, ERROR_LOCATION));
   return std::string(buffer);
}

std::string threadId()
{
   std::stringstream ss;
   ss << boost::this_thread::get_id();
   return ss.str();
}

std::string proxyLockFileName()
{
   return std::string()
         + kFileLockPrefix
         + "-" + hostName()
         + "-" + pidString()
         + "-" + threadId();
         
}

bool isLockFileStale(const FilePath& lockFilePath)
{
   return LinkBasedFileLock::isLockFileStale(lockFilePath);
}

bool isLockFileOrphaned(const FilePath& lockFilePath)
{
#ifndef _WIN32
   
   Error error;
   
   // attempt to read pid from lockfile
   std::string pid;
   error = core::readStringFromFile(lockFilePath, &pid);
   if (error)
   {
      LOG_ERROR(error);
      return false;
   }
   pid = string_utils::trimWhitespace(pid);
   
#ifdef __linux__
   
   // on linux, we can check the proc filesystem for an associated
   // process -- if there is no such directory, then we assume that
   // this lockfile has been orphaned
   FilePath procPath("/proc/" + pid);
   if (!procPath.exists())
      return true;
   
#endif
   
   // call 'ps' to attempt to see if a process associated
   // with this process id exists (and get information about it)
   using namespace core::system;
   std::string command = "ps -p " + pid;
   ProcessOptions options;
   ProcessResult result;
   error = core::system::runCommand(command, options, &result);
   if (error)
   {
      LOG_ERROR(error);
      return false;
   }
   
   // ps will return a non-zero exit status if no process with
   // the requested id is available -- if there is no process,
   // then this lockfile has been orphaned
   if (result.exitStatus != EXIT_SUCCESS)
      return true;
   
#endif /* _WIN32 */
   
   // assume the process is not orphaned if all previous checks failed
   return false;
   
}

} // end anonymous namespace

bool LinkBasedFileLock::isLockFileStale(const FilePath& lockFilePath)
{
   // TODO: currently, we write the process ID of the owning process to the
   // lockfile, in order to detect whether the owning process has crashed
   // and the lockfile is orphaned. in load-balanced configurations, this is
   // unreliable as sessions across multiple machines may be attempting to
   // read / write lockfiles, so we disable this check here
   if (!s_isLoadBalanced)
   {
      if (isLockFileOrphaned(lockFilePath))
         return true;
   }
   
   double seconds = static_cast<double>(s_timeoutInterval.total_seconds());
   double diff = ::difftime(::time(nullptr), lockFilePath.getLastWriteTime());
   return diff >= seconds;
}

namespace {

void cleanStaleLockfiles(const FilePath& dir)
{
   std::vector<FilePath> children;
   Error error = dir.getChildren(children);
   if (error)
      LOG_ERROR(error);

   for (const FilePath& filePath : children )
   {
      if (boost::algorithm::starts_with(filePath.getFilename(), kFileLockPrefix) &&
          isLockFileStale(filePath))
      {
         Error error = filePath.removeIfExists();
         if (error)
            LOG_ERROR(error);
      }
   }
}

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
         for (const FilePath& lockFilePath : registration_)
         {
            LOG("Bumping write time: " << lockFilePath.getAbsolutePath());
            lockFilePath.setLastWriteTime();
         }
      }
      END_LOCK_MUTEX
   }
   
   void clearLocks()
   {
      LOCK_MUTEX(mutex_)
      {
         for (const FilePath& lockFilePath : registration_)
         {
            Error error = lockFilePath.removeIfExists();
            if (error)
               LOG_ERROR(error);
            LOG("Clearing lock: " << lockFilePath.getAbsolutePath());
         }
         registration_.clear();
      }
      END_LOCK_MUTEX
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
   FilePath proxyPath = lockFilePath.getParent().completePath(proxyLockFileName());
   
   // since the proxy lockfile should be unique, it should _never_ be possible
   // for a collision to be found. if that does happen, it must be a leftover
   // from a previous process that crashed in this stage
   Error error = proxyPath.removeIfExists();
   if (error)
      LOG_ERROR(error);
   
   // ensure the proxy file is created, and remove it when we're done
   std::string pid = pidString();
   RemoveOnExitScope scope(proxyPath, ERROR_LOCATION);
   error = core::writeStringToFile(proxyPath, pid);
   if (error)
   {
      // log the error since it isn't expected and could get swallowed
      // upstream by a caller ignore lock_not_available errors
      LOG_ERROR(error);
      return error;
   }
   
   // attempt to link to the desired location -- ignore return value
   // and just stat our original link after, as that's a more reliable
   // indicator of success on old NFS systems
   int status = ::link(
      proxyPath.getAbsolutePathNative().c_str(),
            lockFilePath.getAbsolutePathNative().c_str());
   
   // detect link failure
   if (status == -1)
   {
      // verbose logging
      int errorNumber = errno;
      LOG("ERROR: ::link() failed (errno " << errorNumber << ")" << std::endl <<
          "Attempted to link:" << std::endl << " - " <<
          "'" << proxyPath.getAbsolutePathNative() << "'" <<
          " => " <<
          "'" << lockFilePath.getAbsolutePathNative() << "'");
      
      // if this failed, we should still make a best-effort attempt to acquire
      // a lock by creating a file using O_CREAT | O_EXCL. note that we prefer
      // ::link() since older NFSes provide more guarantees as to its atomicity,
      // but not all NFS support ::link()
      int fd = ::open(
         lockFilePath.getAbsolutePathNative().c_str(),
               O_WRONLY | O_CREAT | O_EXCL,
               0755);
      
      if (fd == -1)
      {
         // verbose logging
         int errorNumber = errno;
         LOG("ERROR: ::open() failed (errno " << errorNumber << ")" <<
                                              std::endl << "Attempted to open:" << std::endl << " - " <<
                                              "'" << lockFilePath.getAbsolutePathNative() << "'");
         
         Error error = systemError(errorNumber, ERROR_LOCATION);
         error.addProperty("lock-file", lockFilePath);
         return error;
      }
      
      // acquired file descriptor -- now try writing our pid to the file
      // (save error number in case it fails and we need to report)
      int status = ::write(fd, pid.c_str(), pid.size());
      errorNumber = errno;
      
      // close file descriptor
      ::close(fd);
      
      // report if an error occurred during write
      if (status)
      {
         Error error = systemError(errorNumber, ERROR_LOCATION);
         error.addProperty("lock-file", lockFilePath);
         return error;
      }
      
      return Success();
   }

   struct stat info;
   int errc = ::stat(proxyPath.getAbsolutePathNative().c_str(), &info);
   if (errc)
   {
      int errorNumber = errno;
      
      // verbose logging
      LOG("ERROR: ::stat() failed (errno " << errorNumber << ")" << std::endl <<
                                           "Attempted to stat:" << std::endl << " - " <<
                                           "'" << proxyPath.getAbsolutePathNative() << "'");
      
      // log the error since it isn't expected and could get swallowed
      // upstream by a caller ignoring lock_not_available errors
      Error error = systemError(errorNumber, ERROR_LOCATION);
      LOG_ERROR(error);
      return error;
   }
   
   // assume that a failure here is the result of someone else
   // acquiring the lock before we could
   if (info.st_nlink != 2)
   {
      LOG("WARNING: Failed to acquire lock (info.st_nlink == " << info.st_nlink << ")");
      return fileExistsError(ERROR_LOCATION);
   }
   
   return Success();
   
#else

   return systemError(boost::system::errc::function_not_supported, ERROR_LOCATION);

#endif
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

bool LinkBasedFileLock::isLocked(const FilePath& lockFilePath) const
{
   if (!lockFilePath.exists())
      return false;
   
   return !isLockFileStale(lockFilePath);
}

Error LinkBasedFileLock::acquire(const FilePath& lockFilePath)
{
   using namespace boost::system;
   
   // if the lock file exists...
   if (lockFilePath.exists())
   {
      // ... and it's stale, it's a leftover lock from a previously
      // (crashed?) process. remove it and acquire our own lock
      if (isLockFileStale(lockFilePath))
      {
         // note that multiple processes may attempt to remove this
         // file at the same time, so errors shouldn't be fatal
         LOG("Removing stale lockfile: " << lockFilePath.getAbsolutePath());
         Error error = lockFilePath.remove();
         if (error)
            LOG_ERROR(error);
      }
      
      // ... it's not stale -- someone else has the lock, cannot proceed
      else
      {
         LOG("No lock available: " << lockFilePath.getAbsolutePath());
         Error error = systemError(errc::no_lock_available, ERROR_LOCATION);
         error.addProperty("lock-file", lockFilePath);
         return error;
      }
   }
   
   // ensure the parent directory exists
   Error error = lockFilePath.getParent().ensureDirectory();
   if (error)
      return error;

   // write the lock file -- this step _must_ be atomic and so only one
   // competing process should be able to succeed here
   Error writeError = writeLockFile(lockFilePath);
   if (writeError)
   {
      LOG("Failed to acquire lock: " << lockFilePath.getAbsolutePath());
      Error error = systemError(
               errc::no_lock_available,
               writeError,
               ERROR_LOCATION);
      error.addProperty("lock-file", lockFilePath);
      return error;
   }

   // clean any other stale lockfiles in that directory
   cleanStaleLockfiles(lockFilePath.getParent());
   
   // register our lock (for refresh)
   pImpl_->lockFilePath = lockFilePath;
   lockRegistration().registerLock(lockFilePath);
   LOG("Acquired lock: " << lockFilePath.getAbsolutePath());
   return Success();
}

Error LinkBasedFileLock::release()
{
   const FilePath& lockFilePath = pImpl_->lockFilePath;
   LOG("Released lock: " << lockFilePath.getAbsolutePath());
   
   Error error = lockFilePath.remove();
   if (error)
      LOG_ERROR(error);
   
   lockRegistration().deregisterLock(lockFilePath);
   pImpl_->lockFilePath = FilePath();
   return error;
}

void LinkBasedFileLock::refresh()
{
   lockRegistration().refreshLocks();
}

void LinkBasedFileLock::cleanUp()
{
   lockRegistration().clearLocks();
}

} // namespace core
} // namespace rstudio

/*
 * LinkBasedFileLock.cpp
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

#include <cstdint>
#include <iomanip>
#include <limits>
#include <map>
#include <sstream>
#include <string>
#include <vector>

#include <boost/optional.hpp>
#include <boost/system/error_code.hpp>
#include <boost/thread/mutex.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>
#include <shared_core/SafeConvert.hpp>

#include <core/FileSerializer.hpp>
#include <core/Log.hpp>
#include <core/StringUtils.hpp>
#include <core/system/System.hpp>
#ifndef _WIN32
# include <core/system/PosixSystem.hpp>
#endif

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

const char * const kLegacyFileLockPrefix = ".rstudio-lock-41c29";
const char * const kOwnerFilePrefix = ".rstudio-lock-owner-41c29";
const char * const kFileLockClaimPrefix = ".rstudio-lock-claim-41c29";
const char * const kReleasedProcessId = "-1";

struct LockMetadata
{
   boost::optional<PidType> processId;
   FilePath ownerFilePath;
   bool released = false;
#ifndef _WIN32
   dev_t device = 0;
   ino_t inode = 0;
   bool hasIdentity = false;
   bool identityFollowsSymlink = true;
#endif
};

struct LockInspection
{
   bool exists = false;
   bool stale = false;
   LockMetadata metadata;
};

std::string pidString()
{
   return safe_convert::numberToString(system::currentProcessId());
}

bool isProxyFile(const FilePath& filePath)
{
   const std::string filename = filePath.getFilename();
   const std::string legacyPrefix =
      std::string(kLegacyFileLockPrefix) + "-";
   const std::string ownerPrefix = std::string(kOwnerFilePrefix) + "-";
   return (filename.size() > legacyPrefix.size() &&
           filename.compare(0, legacyPrefix.size(), legacyPrefix) == 0) ||
          (filename.size() > ownerPrefix.size() &&
           filename.compare(0, ownerPrefix.size(), ownerPrefix) == 0);
}

FilePath proxyPathForToken(const FilePath& lockFilePath,
                           const std::string& token)
{
   return lockFilePath.getParent().completePath(
      std::string(kOwnerFilePrefix) + "-" + token);
}

FilePath claimPathForOwner(const FilePath& ownerFilePath)
{
   const std::string ownerPrefix = std::string(kOwnerFilePrefix) + "-";
   const std::string legacyPrefix =
      std::string(kLegacyFileLockPrefix) + "-";
   const std::string filename = ownerFilePath.getFilename();
   bool usesOwnerPrefix =
      filename.compare(0, ownerPrefix.size(), ownerPrefix) == 0;
   const std::string& prefix = usesOwnerPrefix ? ownerPrefix : legacyPrefix;
   std::string token = filename.substr(prefix.size());
   return ownerFilePath.getParent().completePath(
      std::string(kFileLockClaimPrefix) + "-" + token);
}

FilePath claimPathForLock(const FilePath& lockFilePath)
{
   // Use a stable FNV-1a hash to keep the claim name below NAME_MAX even when
   // the caller's lock filename is already near that limit.
   uint64_t hash = 14695981039346656037ULL;
   for (unsigned char character : lockFilePath.getFilename())
   {
      hash ^= character;
      hash *= 1099511628211ULL;
   }

   std::ostringstream stream;
   stream << kFileLockClaimPrefix << "-path-" << std::hex << hash;
   return lockFilePath.getParent().completePath(stream.str());
}

std::string lockContents(bool released)
{
   // Keep the public contents parseable as a PID by older RStudio versions.
   // The negative release sentinel is also treated as stale by those versions.
   return released ? std::string(kReleasedProcessId) + "\n"
                   : pidString() + "\n";
}

void parseLockContents(const std::string& contents, LockMetadata* pMetadata)
{
   std::string processId = string_utils::trimWhitespace(contents);
   if (processId == kReleasedProcessId)
   {
      pMetadata->released = true;
      return;
   }

   boost::optional<uint64_t> value = safe_convert::stringTo<uint64_t>(processId);
   if (value && *value > 0 &&
       *value <= static_cast<uint64_t>(std::numeric_limits<PidType>::max()))
   {
      pMetadata->processId = static_cast<PidType>(*value);
   }
}

#ifndef _WIN32
Error writeDescriptorContents(int descriptor, const std::string& contents)
{
   std::size_t written = 0;
   while (written < contents.size())
   {
      ssize_t result = ::write(
         descriptor,
         contents.data() + written,
         contents.size() - written);
      if (result == -1)
      {
         if (errno == EINTR)
            continue;
         return systemCallError("write", errno, ERROR_LOCATION);
      }
      written += static_cast<std::size_t>(result);
   }

   return Success();
}
#endif

Error readLockMetadata(const FilePath& lockFilePath,
                       LockMetadata* pMetadata)
{
#ifndef _WIN32
   int descriptor = ::open(
      lockFilePath.getAbsolutePathNative().c_str(),
      O_RDONLY | O_NONBLOCK);
   if (descriptor == -1)
   {
      Error error = systemError(errno, ERROR_LOCATION);
      error.addProperty("lock-file", lockFilePath);
      return error;
   }

   struct stat info;
   if (::fstat(descriptor, &info) == -1)
   {
      int errorNumber = errno;
      ::close(descriptor);
      return systemCallError("fstat", errorNumber, ERROR_LOCATION);
   }

   std::string contents;
   char buffer[64];
   while (contents.size() <= sizeof(buffer))
   {
      ssize_t result = ::read(descriptor, buffer, sizeof(buffer));
      if (result == -1)
      {
         if (errno == EINTR)
            continue;

         int errorNumber = errno;
         ::close(descriptor);
         return systemCallError("read", errorNumber, ERROR_LOCATION);
      }
      if (result == 0)
         break;

      contents.append(buffer, static_cast<std::size_t>(result));
   }
   ::close(descriptor);

   pMetadata->device = info.st_dev;
   pMetadata->inode = info.st_ino;
   pMetadata->hasIdentity = true;
#else
   std::string contents;
   Error error = core::readStringFromFile(lockFilePath, &contents);
   if (error)
      return error;
#endif

   parseLockContents(contents, pMetadata);
   return Success();
}

Error hasExpectedIdentity(const FilePath& filePath,
                          const LockMetadata& metadata,
                          bool* pMatches)
{
   *pMatches = false;
#ifndef _WIN32
   if (!metadata.hasIdentity)
      return Success();

   struct stat info;
   int status = metadata.identityFollowsSymlink
      ? ::stat(filePath.getAbsolutePathNative().c_str(), &info)
      : ::lstat(filePath.getAbsolutePathNative().c_str(), &info);
   if (status == -1)
   {
      if (errno == ENOENT)
         return Success();

      Error error = systemError(errno, ERROR_LOCATION);
      error.addProperty("path", filePath);
      return error;
   }

   *pMatches = info.st_dev == metadata.device &&
               info.st_ino == metadata.inode;
#endif
   return Success();
}

Error findOwnerFile(const FilePath& lockFilePath, LockMetadata* pMetadata)
{
   std::vector<FilePath> children;
   Error error = lockFilePath.getParent().getChildren(children);
   if (error)
      return error;

   for (const FilePath& child : children)
   {
      if (child == lockFilePath || !isProxyFile(child))
         continue;

#ifndef _WIN32
      bool matches = false;
      error = hasExpectedIdentity(child, *pMetadata, &matches);
      if (error)
         return error;
      if (matches)
#else
      if (lockFilePath.isEquivalentTo(child))
#endif
      {
         pMetadata->ownerFilePath = child;
         break;
      }
   }

   return Success();
}

Error inspectLockFile(const FilePath& lockFilePath,
                      LockInspection* pInspection)
{
#ifndef _WIN32
   struct stat pathInfo;
   if (::lstat(lockFilePath.getAbsolutePathNative().c_str(), &pathInfo) == -1)
   {
      if (errno == ENOENT)
         return Success();

      Error error = systemError(errno, ERROR_LOCATION);
      error.addProperty("lock-file", lockFilePath);
      return error;
   }

   if (S_ISLNK(pathInfo.st_mode))
   {
      struct stat targetInfo;
      if (::stat(lockFilePath.getAbsolutePathNative().c_str(), &targetInfo) == -1)
      {
         if (errno != ENOENT)
         {
            Error error = systemError(errno, ERROR_LOCATION);
            error.addProperty("lock-file", lockFilePath);
            return error;
         }

         pInspection->exists = true;
         pInspection->stale = true;
         pInspection->metadata.device = pathInfo.st_dev;
         pInspection->metadata.inode = pathInfo.st_ino;
         pInspection->metadata.hasIdentity = true;
         pInspection->metadata.identityFollowsSymlink = false;
         return Success();
      }
   }
#else
   if (!lockFilePath.exists())
      return Success();
#endif

   pInspection->exists = true;
   Error error = readLockMetadata(lockFilePath, &pInspection->metadata);
   if (error)
   {
      if (isFileNotFoundError(error))
      {
         pInspection->exists = false;
         return Success();
      }
      return error;
   }

   if (pInspection->metadata.released)
   {
      pInspection->stale = true;
      return findOwnerFile(lockFilePath, &pInspection->metadata);
   }

#ifndef _WIN32
   // A live local owner remains authoritative even if a refresh was delayed.
   // This prevents a sleeping or temporarily stalled process from losing its
   // lock and later interfering with the replacement owner.
   if (!FileLock::isLoadBalanced() && pInspection->metadata.processId)
   {
      pInspection->stale =
         !system::isProcessRunning(*pInspection->metadata.processId);
      return pInspection->stale
         ? findOwnerFile(lockFilePath, &pInspection->metadata)
         : Success();
   }
#endif

   std::time_t lastWriteTime;
   error = lockFilePath.getLastWriteTime(lastWriteTime);
   if (error)
   {
      if (isFileNotFoundError(error))
      {
         pInspection->exists = false;
         return Success();
      }
      return error;
   }

   double seconds =
      static_cast<double>(FileLock::getTimeoutInterval().total_seconds());
   double diff = ::difftime(::time(nullptr), lastWriteTime);
   pInspection->stale = diff >= seconds;
   return pInspection->stale
      ? findOwnerFile(lockFilePath, &pInspection->metadata)
      : Success();
}

Error noLockAvailableError(const FilePath& lockFilePath)
{
   Error error = systemError(
      boost::system::errc::no_lock_available,
      ERROR_LOCATION);
   error.addProperty("lock-file", lockFilePath);
   return error;
}

Error unlinkPath(const FilePath& filePath)
{
#ifdef _WIN32
   Error error = filePath.remove();
   if (error && !isFileNotFoundError(error))
      return error;
   return Success();
#else
   if (::unlink(filePath.getAbsolutePathNative().c_str()) == 0 ||
       errno == ENOENT)
   {
      return Success();
   }

   Error error = systemError(errno, ERROR_LOCATION);
   error.addProperty("path", filePath);
   return error;
#endif
}

Error claimLockFile(const FilePath& claimFilePath, bool* pClaimed)
{
#ifdef _WIN32
   *pClaimed = true;
   return Success();
#else
   for (int attempt = 0; attempt < 2; ++attempt)
   {
      int descriptor = ::open(
         claimFilePath.getAbsolutePathNative().c_str(),
         O_WRONLY | O_CREAT | O_EXCL,
         0600);
      if (descriptor != -1)
      {
         Error error = writeDescriptorContents(descriptor, lockContents(false));
         int closeResult = ::close(descriptor);
         if (error)
         {
            Error removeError = unlinkPath(claimFilePath);
            if (removeError)
               LOG_ERROR(removeError);
            return error;
         }
         if (closeResult == -1)
         {
            int errorNumber = errno;
            Error removeError = unlinkPath(claimFilePath);
            if (removeError)
               LOG_ERROR(removeError);
            return systemCallError("close", errorNumber, ERROR_LOCATION);
         }

         *pClaimed = true;
         return Success();
      }

      if (errno != EEXIST)
      {
         Error error = systemError(errno, ERROR_LOCATION);
         error.addProperty("path", claimFilePath);
         return error;
      }

      LockInspection inspection;
      Error error = inspectLockFile(claimFilePath, &inspection);
      if (error)
         return error;
      if (!inspection.exists || inspection.stale)
      {
         error = unlinkPath(claimFilePath);
         if (error)
            return error;
         continue;
      }

      *pClaimed = false;
      return Success();
   }

   *pClaimed = false;
   return Success();
#endif
}

Error removeLockFile(const FilePath& lockFilePath,
                     const LockMetadata& expectedMetadata)
{
   // A private claim beside the lock elects one stale-lock contender.
   // Inode checks then prevent an earlier inspection from deleting a newer
   // lock at the same public path.
   bool matches = false;
   Error error = hasExpectedIdentity(
      lockFilePath,
      expectedMetadata,
      &matches);
   if (error)
      return error;
   if (!matches)
      return noLockAvailableError(lockFilePath);

   if (!expectedMetadata.ownerFilePath.isEmpty())
   {
      error = hasExpectedIdentity(
         expectedMetadata.ownerFilePath,
         expectedMetadata,
         &matches);
      if (error)
         return error;
      if (!matches)
      {
         return noLockAvailableError(lockFilePath);
      }
   }

   FilePath claimFilePath = expectedMetadata.ownerFilePath.isEmpty()
      ? claimPathForLock(lockFilePath)
      : claimPathForOwner(expectedMetadata.ownerFilePath);
   bool claimed = false;
   error = claimLockFile(claimFilePath, &claimed);
   if (error)
      return error;
   if (!claimed)
      return noLockAvailableError(lockFilePath);

#ifndef _WIN32
   // An inspection is tied to the inode it read. If another contender has
   // already replaced that inode, leave the replacement untouched.
   error = hasExpectedIdentity(lockFilePath, expectedMetadata, &matches);
   if (error || !matches)
   {
      if (!claimFilePath.isEmpty())
      {
         Error claimError = unlinkPath(claimFilePath);
         if (claimError)
            LOG_ERROR(claimError);
      }
      return error ? error : noLockAvailableError(lockFilePath);
   }
#endif

   error = unlinkPath(lockFilePath);
   if (!error && !expectedMetadata.ownerFilePath.isEmpty())
      error = unlinkPath(expectedMetadata.ownerFilePath);

   if (!claimFilePath.isEmpty())
   {
      Error claimError = unlinkPath(claimFilePath);
      if (claimError)
         LOG_ERROR(claimError);
   }

   return error;
}

#ifndef _WIN32

Error writeLockContents(int descriptor, bool released)
{
   std::string contents = lockContents(released);
   if (::lseek(descriptor, 0, SEEK_SET) == -1)
      return systemCallError("lseek", errno, ERROR_LOCATION);
   if (::ftruncate(descriptor, 0) == -1)
      return systemCallError("ftruncate", errno, ERROR_LOCATION);

   return writeDescriptorContents(descriptor, contents);
}

Error createProxyFile(const FilePath& lockFilePath,
                      std::string* pToken,
                      FilePath* pProxyPath,
                      int* pDescriptor)
{
   for (int attempt = 0; attempt < 3; ++attempt)
   {
      std::string token = system::generateUuid(false);
      FilePath proxyPath = proxyPathForToken(lockFilePath, token);
      int descriptor = ::open(
         proxyPath.getAbsolutePathNative().c_str(),
         O_RDWR | O_CREAT | O_EXCL,
         0600);
      if (descriptor == -1)
      {
         if (errno == EEXIST)
            continue;

         Error error = systemError(errno, ERROR_LOCATION);
         error.addProperty("lock-file", lockFilePath);
         return error;
      }

      Error error = writeLockContents(descriptor, false);
      if (error)
      {
         ::close(descriptor);
         Error removeError = unlinkPath(proxyPath);
         if (removeError)
            LOG_ERROR(removeError);
         return error;
      }

      *pToken = token;
      *pProxyPath = proxyPath;
      *pDescriptor = descriptor;
      return Success();
   }

   Error error = systemError(boost::system::errc::file_exists, ERROR_LOCATION);
   error.addProperty("lock-file", lockFilePath);
   return error;
}

Error writeLockFile(const FilePath& lockFilePath,
                    std::string* pToken,
                    int* pDescriptor)
{
   std::string token;
   FilePath proxyPath;
   int proxyDescriptor = -1;
   Error error = createProxyFile(
      lockFilePath,
      &token,
      &proxyPath,
      &proxyDescriptor);
   if (error)
      return error;

   int status;
   if (FileLock::useSymlinks())
   {
      status = ::symlink(
         proxyPath.getAbsolutePathNative().c_str(),
         lockFilePath.getAbsolutePathNative().c_str());
   }
   else
   {
      status = ::link(
         proxyPath.getAbsolutePathNative().c_str(),
         lockFilePath.getAbsolutePathNative().c_str());
   }

   int linkError = status == -1 ? errno : 0;
   bool linked = status == 0;

   // Some older NFS implementations can report failure after creating the
   // hard link. The link count and inode identity are authoritative.
   if (!linked && !FileLock::useSymlinks())
   {
      struct stat info;
      if (::fstat(proxyDescriptor, &info) == 0 && info.st_nlink == 2)
         linked = lockFilePath.isEquivalentTo(proxyPath);
   }

   if (linked)
   {
      if (!lockFilePath.isEquivalentTo(proxyPath))
      {
         Error validationError = systemError(
            boost::system::errc::io_error,
            "Created lock does not refer to its owner file",
            ERROR_LOCATION);
         validationError.addProperty("lock-file", lockFilePath);
         validationError.addProperty("owner-file", proxyPath);
         Error releaseError = writeLockContents(
            proxyDescriptor,
            true);
         if (releaseError)
            LOG_ERROR(releaseError);
         ::close(proxyDescriptor);
         return validationError;
      }

      *pToken = token;
      *pDescriptor = proxyDescriptor;
      return Success();
   }

   if (linkError == EEXIST)
   {
      ::close(proxyDescriptor);
      Error removeError = unlinkPath(proxyPath);
      if (removeError)
         LOG_ERROR(removeError);
      return fileExistsError(lockFilePath, ERROR_LOCATION);
   }

   // If the filesystem cannot create links, fall back to O_EXCL. An empty or
   // partially written fallback file is treated as held until its timeout, so
   // publication before this write cannot let another contender take over.
   int descriptor = ::open(
      lockFilePath.getAbsolutePathNative().c_str(),
      O_RDWR | O_CREAT | O_EXCL,
      0600);
   if (descriptor == -1)
   {
      int errorNumber = errno;
      ::close(proxyDescriptor);
      Error removeError = unlinkPath(proxyPath);
      if (removeError)
         LOG_ERROR(removeError);

      Error openError = systemError(errorNumber, ERROR_LOCATION);
      openError.addProperty("lock-file", lockFilePath);
      return openError;
   }

   error = writeLockContents(descriptor, false);
   ::close(proxyDescriptor);
   Error removeError = unlinkPath(proxyPath);
   if (removeError)
      LOG_ERROR(removeError);

   if (error)
   {
      ::close(descriptor);
      Error lockRemoveError = unlinkPath(lockFilePath);
      if (lockRemoveError)
         LOG_ERROR(lockRemoveError);
      return error;
   }

   *pToken = token;
   *pDescriptor = descriptor;
   return Success();
}

#else

Error writeLockFile(const FilePath& lockFilePath,
                    std::string*,
                    int*)
{
   return systemError(
      boost::system::errc::function_not_supported,
      ERROR_LOCATION);
}

#endif

std::string registrationKey(const FilePath& lockFilePath)
{
   return lockFilePath.getParent().getCanonicalPath() + "/" +
          lockFilePath.getFilename();
}

struct RegisteredLock
{
   std::string token;
   int descriptor;
};

class LockRegistration : boost::noncopyable
{
public:
   LockRegistration()
      : processId_(system::currentProcessId())
   {
   }

   Error registerLock(const std::string& key,
                      const std::string& token,
                      int descriptor)
   {
      boost::mutex::scoped_lock lock(mutex_);
      resetAfterFork();
#ifndef _WIN32
      int registeredDescriptor = ::dup(descriptor);
      if (registeredDescriptor == -1)
      {
         return systemCallError("dup", errno, ERROR_LOCATION);
      }

      auto existing = registration_.find(key);
      if (existing != registration_.end())
         ::close(existing->second.descriptor);
      registration_[key] = RegisteredLock{
         token,
         registeredDescriptor};
#endif
      return Success();
   }

   void deregisterLock(const std::string& key, const std::string& token)
   {
      boost::mutex::scoped_lock lock(mutex_);
      resetAfterFork();
      auto it = registration_.find(key);
      if (it != registration_.end() && it->second.token == token)
      {
#ifndef _WIN32
         ::close(it->second.descriptor);
#endif
         registration_.erase(it);
      }
   }

   void refreshLocks()
   {
      boost::mutex::scoped_lock lock(mutex_);
      resetAfterFork();
      for (const auto& entry : registration_)
      {
#ifndef _WIN32
         LOG("Bumping write time for lock token: " << entry.second.token);
         if (::futimes(entry.second.descriptor, nullptr) == -1)
            LOG_ERROR(systemCallError("futimes", errno, ERROR_LOCATION));
#endif
      }
   }

   void clearLocks()
   {
      boost::mutex::scoped_lock lock(mutex_);
      resetAfterFork();
#ifndef _WIN32
      for (const auto& entry : registration_)
      {
         Error error = writeLockContents(
            entry.second.descriptor,
            true);
         if (error)
            LOG_ERROR(error);
         ::close(entry.second.descriptor);
      }
#endif
      registration_.clear();
   }

private:
   void resetAfterFork()
   {
      PidType processId = system::currentProcessId();
      if (processId_ != processId)
      {
#ifndef _WIN32
         for (const auto& entry : registration_)
            ::close(entry.second.descriptor);
#endif
         registration_.clear();
         processId_ = processId;
      }
   }

   boost::mutex mutex_;
   std::map<std::string, RegisteredLock> registration_;
   PidType processId_;
};

LockRegistration& lockRegistration()
{
   // Lock objects can be destroyed during static shutdown. Keep the registry
   // alive for the lifetime of the process so those destructors remain safe.
   static LockRegistration* pInstance = new LockRegistration();
   return *pInstance;
}

} // anonymous namespace

bool LinkBasedFileLock::isLockFileStale(const FilePath& lockFilePath)
{
   LockInspection inspection;
   Error error = inspectLockFile(lockFilePath, &inspection);
   if (error)
   {
      LOG_ERROR(error);
      return false;
   }

   return inspection.exists && inspection.stale;
}

struct LinkBasedFileLock::Impl
{
   Impl()
      : descriptor(-1),
        processId(0)
   {
   }

   FilePath lockFilePath;
   std::string registrationKey;
   std::string token;
   int descriptor;
   PidType processId;
};

LinkBasedFileLock::LinkBasedFileLock()
   : pImpl_(new Impl())
{
}

LinkBasedFileLock::~LinkBasedFileLock()
{
   if (pImpl_->descriptor != -1)
   {
      Error error = release();
      if (error)
         LOG_ERROR(error);
   }
}

FilePath LinkBasedFileLock::lockFilePath() const
{
   return pImpl_->lockFilePath;
}

bool LinkBasedFileLock::isLocked(const FilePath& lockFilePath) const
{
   bool isLocked = true;
   Error error = this->isLocked(lockFilePath, &isLocked);
   if (error)
      LOG_ERROR(error);
   return isLocked;
}

Error LinkBasedFileLock::isLocked(const FilePath& lockFilePath,
                                  bool* pIsLocked) const
{
   *pIsLocked = true;

   LockInspection inspection;
   Error error = inspectLockFile(lockFilePath, &inspection);
   if (error)
      return error;

   *pIsLocked = inspection.exists && !inspection.stale;
   return Success();
}

Error LinkBasedFileLock::acquire(const FilePath& lockFilePath)
{
   if (pImpl_->descriptor != -1)
      return noLockAvailableError(lockFilePath);

   Error error = lockFilePath.getParent().ensureDirectory();
   if (error)
      return error;

   LockInspection inspection;
   error = inspectLockFile(lockFilePath, &inspection);
   if (error)
      return error;

   if (inspection.exists)
   {
      if (!inspection.stale)
         return noLockAvailableError(lockFilePath);

      LOG("Removing stale lockfile: " << lockFilePath.getAbsolutePath());
      error = removeLockFile(lockFilePath, inspection.metadata);
      if (error)
      {
         if (FileLock::isNoLockAvailable(error))
            return noLockAvailableError(lockFilePath);
         return error;
      }
   }

   std::string token;
   int descriptor = -1;
   error = writeLockFile(
      lockFilePath,
      &token,
      &descriptor);
   if (error)
   {
      if (error == systemError(
                     boost::system::errc::file_exists,
                     ErrorLocation()))
      {
         return noLockAvailableError(lockFilePath);
      }

      error.addProperty("lock-file", lockFilePath);
      return error;
   }

   std::string key = registrationKey(lockFilePath);
   error = lockRegistration().registerLock(
      key,
      token,
      descriptor);
   if (error)
   {
#ifndef _WIN32
      Error releaseError = writeLockContents(
         descriptor,
         true);
      if (releaseError)
         LOG_ERROR(releaseError);
      ::close(descriptor);
#endif
      return error;
   }

   pImpl_->lockFilePath = lockFilePath;
   pImpl_->registrationKey = key;
   pImpl_->token = token;
   pImpl_->descriptor = descriptor;
   pImpl_->processId = system::currentProcessId();

   LOG("Acquired lock: " << lockFilePath.getAbsolutePath());
   return Success();
}

Error LinkBasedFileLock::release()
{
   if (pImpl_->descriptor == -1)
      return noLockAvailableError(pImpl_->lockFilePath);

   Error error;
#ifndef _WIN32
   if (pImpl_->processId == system::currentProcessId())
   {
      // Update the inode opened at acquisition. If a stale takeover replaced
      // the public path, this descriptor still refers only to the old owner and
      // cannot release or delete the successor's lock.
      error = writeLockContents(
         pImpl_->descriptor,
         true);
   }
   ::close(pImpl_->descriptor);
#endif

   lockRegistration().deregisterLock(
      pImpl_->registrationKey,
      pImpl_->token);
   LOG("Released lock: " << pImpl_->lockFilePath.getAbsolutePath());

   pImpl_->lockFilePath = FilePath();
   pImpl_->registrationKey.clear();
   pImpl_->token.clear();
   pImpl_->descriptor = -1;
   pImpl_->processId = 0;
   return error;
}

void LinkBasedFileLock::refresh()
{
   lockRegistration().refreshLocks();
}

void LinkBasedFileLock::cleanUp()
{
   // Individual lock objects mark their private owner inode released in their
   // destructors. Clearing the registry here must not delete shared pathnames:
   // after lease takeover, a pathname may already belong to another process.
   lockRegistration().clearLocks();
}

} // namespace core
} // namespace rstudio

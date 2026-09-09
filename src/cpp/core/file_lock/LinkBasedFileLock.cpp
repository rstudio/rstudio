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
#include <ctime>
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

#include <core/DateTime.hpp>
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
const char * const kFileLockTempPrefix = ".rstudio-lock-tmp-41c29";
const char * const kReleasedProcessId = "-1";

// Lock files must stay readable by other users: in a shared project, a
// collaborator's session inspects (and eventually expires) locks it did not
// create. The process umask still applies.
const int kLockFileMode = 0644;

// The identity of a lock is the inode behind it. For a symlink lock that is
// the target's inode (what the owner holds open), except for a broken symlink
// where only the link itself remains.
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

enum class RemoveResult
{
   Removed,   // the expected inode was removed
   Absent,    // nothing was at the path
   Mismatch   // a different inode was at the path and has been left in place
};

std::string pidString()
{
   return safe_convert::numberToString(system::currentProcessId());
}

bool hasPrefix(const std::string& filename, const char* prefix)
{
   std::string full = std::string(prefix) + "-";
   return filename.size() > full.size() &&
          filename.compare(0, full.size(), full) == 0;
}

bool isOwnerFile(const FilePath& filePath)
{
   const std::string filename = filePath.getFilename();
   return hasPrefix(filename, kLegacyFileLockPrefix) ||
          hasPrefix(filename, kOwnerFilePrefix);
}

bool isSweepableArtifact(const FilePath& filePath)
{
   // Temp files are excluded: a live contender is mid-way through an
   // identity-checked removal, and sweeping the file out from under it would
   // read as contention.
   return isOwnerFile(filePath) ||
          hasPrefix(filePath.getFilename(), kFileLockClaimPrefix);
}

FilePath proxyPathForToken(const FilePath& lockFilePath,
                           const std::string& token)
{
   return lockFilePath.getParent().completePath(
      std::string(kOwnerFilePrefix) + "-" + token);
}

FilePath claimPathForLock(const FilePath& lockFilePath)
{
   // Every contender for a public lock path must use the same claim name.
   // Use a stable FNV-1a hash to keep it below NAME_MAX even when the
   // caller's lock filename is already near that limit.
   uint64_t hash = 14695981039346656037ULL;
   for (unsigned char character : lockFilePath.getFilename())
   {
      hash ^= character;
      hash *= 1099511628211ULL;
   }

   std::ostringstream stream;
   stream << kFileLockClaimPrefix << "-" << std::hex << hash;
   return lockFilePath.getParent().completePath(stream.str());
}

#ifndef _WIN32
FilePath tempPathBeside(const FilePath& filePath)
{
   return filePath.getParent().completePath(
      std::string(kFileLockTempPrefix) + "-" + system::generateUuid(false));
}
#endif

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

Error noLockAvailableError(const FilePath& lockFilePath)
{
   Error error = systemError(
      boost::system::errc::no_lock_available,
      ERROR_LOCATION);
   error.addProperty("lock-file", lockFilePath);
   return error;
}

#ifndef _WIN32

bool isPermissionError(int errorNumber)
{
   return errorNumber == EACCES || errorNumber == EPERM;
}

Error unlinkPath(const FilePath& filePath)
{
   if (::unlink(filePath.getAbsolutePathNative().c_str()) == 0 ||
       errno == ENOENT)
   {
      return Success();
   }

   Error error = systemError(errno, ERROR_LOCATION);
   error.addProperty("path", filePath);
   return error;
}

void unlinkBestEffort(const FilePath& filePath)
{
   Error error = unlinkPath(filePath);
   if (error)
      LOG_ERROR(error);
}

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

Error writeLockContents(int descriptor, bool released)
{
   std::string contents = lockContents(released);
   if (::lseek(descriptor, 0, SEEK_SET) == -1)
      return systemCallError("lseek", errno, ERROR_LOCATION);
   if (::ftruncate(descriptor, 0) == -1)
      return systemCallError("ftruncate", errno, ERROR_LOCATION);

   return writeDescriptorContents(descriptor, contents);
}

Error descriptorIdentity(int descriptor, LockMetadata* pMetadata)
{
   struct stat info;
   if (::fstat(descriptor, &info) == -1)
      return systemCallError("fstat", errno, ERROR_LOCATION);

   pMetadata->device = info.st_dev;
   pMetadata->inode = info.st_ino;
   pMetadata->hasIdentity = true;
   pMetadata->identityFollowsSymlink = true;
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

   Error error = descriptorIdentity(descriptor, pMetadata);
   if (error)
   {
      ::close(descriptor);
      return error;
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

// Removes the entry at 'filePath' only if it is still the inode described by
// 'metadata'. POSIX has no conditional unlink, so the entry is first renamed
// aside (atomically taking whatever is at the path), checked, and either
// unlinked or linked back into place. A link-back that finds the path already
// re-occupied gives up on the displaced entry rather than clobber the newer
// one; that needs two contenders to publish in the same instant.
Error removeIfSameIdentity(const FilePath& filePath,
                           const LockMetadata& metadata,
                           RemoveResult* pResult)
{
   *pResult = RemoveResult::Absent;
#ifdef _WIN32
   Error error = filePath.remove();
   if (error)
   {
      if (isFileNotFoundError(error))
         return Success();
      return error;
   }
   *pResult = RemoveResult::Removed;
   return Success();
#else
   FilePath tempPath = tempPathBeside(filePath);
   if (::rename(filePath.getAbsolutePathNative().c_str(),
                tempPath.getAbsolutePathNative().c_str()) == -1)
   {
      if (errno == ENOENT)
         return Success();

      Error error = systemError(errno, ERROR_LOCATION);
      error.addProperty("path", filePath);
      return error;
   }

   bool matches = false;
   Error error = hasExpectedIdentity(tempPath, metadata, &matches);
   if (!error && matches)
   {
      *pResult = RemoveResult::Removed;
      return unlinkPath(tempPath);
   }

   // Not ours: put it back without following (or clobbering) anything.
   if (::linkat(AT_FDCWD, tempPath.getAbsolutePathNative().c_str(),
                AT_FDCWD, filePath.getAbsolutePathNative().c_str(), 0) == -1)
   {
      Error linkError = systemCallError("linkat", errno, ERROR_LOCATION);
      linkError.addProperty("path", filePath);
      linkError.addProperty("description",
                            "Could not restore a displaced lock entry");
      LOG_ERROR(linkError);
   }
   unlinkBestEffort(tempPath);

   *pResult = RemoveResult::Mismatch;
   return error;
#endif
}

Error findOwnerFile(const FilePath& lockFilePath, LockMetadata* pMetadata)
{
   std::vector<FilePath> children;
   Error error = lockFilePath.getParent().getChildren(children);
   if (error)
      return error;

   for (const FilePath& child : children)
   {
      if (child == lockFilePath || !isOwnerFile(child))
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

#ifndef _WIN32

// A live PID is not proof of a live owner: the owner may have crashed and the
// kernel may have handed its PID to an unrelated process. The owner wrote the
// lock after it started, so a process that started after the last refresh
// cannot be the owner. The timeout interval absorbs clock skew (e.g. an NFS
// server stamping mtimes) so a legitimate owner is never judged reused.
bool isOwnerProcessStale(PidType processId, std::time_t lastWriteTime)
{
   if (!system::isProcessRunning(processId))
      return true;

   system::ProcessInfo info;
   info.pid = processId;
   boost::posix_time::ptime created;
   Error error = info.creationTime(&created);
   if (error)
      return !system::isProcessRunning(processId);

   double startSeconds = date_time::secondsSinceEpoch(created);
   double tolerance =
      static_cast<double>(FileLock::getTimeoutInterval().total_seconds());
   return startSeconds > static_cast<double>(lastWriteTime) + tolerance;
}

#endif

Error inspectLockFile(const FilePath& lockFilePath,
                      LockInspection* pInspection)
{
   std::time_t lastWriteTime = 0;

#ifndef _WIN32
   struct stat info;
   if (::lstat(lockFilePath.getAbsolutePathNative().c_str(), &info) == -1)
   {
      if (errno == ENOENT)
         return Success();

      Error error = systemError(errno, ERROR_LOCATION);
      error.addProperty("lock-file", lockFilePath);
      return error;
   }

   if (S_ISLNK(info.st_mode))
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

         // broken symlink: nothing to read, and only the link itself to remove
         pInspection->exists = true;
         pInspection->stale = true;
         pInspection->metadata.device = info.st_dev;
         pInspection->metadata.inode = info.st_ino;
         pInspection->metadata.hasIdentity = true;
         pInspection->metadata.identityFollowsSymlink = false;
         return Success();
      }
      info = targetInfo;
   }

   pInspection->metadata.device = info.st_dev;
   pInspection->metadata.inode = info.st_ino;
   pInspection->metadata.hasIdentity = true;
   pInspection->metadata.identityFollowsSymlink = true;
   lastWriteTime = info.st_mtime;
#else
   if (!lockFilePath.exists())
      return Success();

   Error timeError = lockFilePath.getLastWriteTime(lastWriteTime);
   if (timeError)
   {
      if (isFileNotFoundError(timeError))
         return Success();
      return timeError;
   }
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

#ifndef _WIN32
      // Another user's lock we cannot read still expires by age; treating it
      // as unreadable (no PID) falls through to the timeout check below.
      if (!isPermissionError(error.getCode()))
         return error;
#else
      return error;
#endif
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
      pInspection->stale = isOwnerProcessStale(
         *pInspection->metadata.processId,
         lastWriteTime);
      return pInspection->stale
         ? findOwnerFile(lockFilePath, &pInspection->metadata)
         : Success();
   }
#endif

   double seconds =
      static_cast<double>(FileLock::getTimeoutInterval().total_seconds());
   double diff = ::difftime(::time(nullptr), lastWriteTime);
   pInspection->stale = diff >= seconds;
   return pInspection->stale
      ? findOwnerFile(lockFilePath, &pInspection->metadata)
      : Success();
}

// Removes stale owner and claim files left in a lock directory: owners
// orphaned when something other than the lock deleted the public path (a
// crash, or a caller cleaning up), and claims abandoned mid-takeover. Live
// entries are never touched; each candidate is inspected like a lock.
void sweepStaleArtifacts(const FilePath& directory, const FilePath& ownPath)
{
   std::vector<FilePath> children;
   Error error = directory.getChildren(children);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   for (const FilePath& child : children)
   {
      if (child == ownPath || !isSweepableArtifact(child))
         continue;

      LockInspection inspection;
      error = inspectLockFile(child, &inspection);
      if (error || !inspection.exists || !inspection.stale)
         continue;

      LOG("Removing stale lock artifact: " << child.getAbsolutePath());
      RemoveResult result;
      error = removeIfSameIdentity(child, inspection.metadata, &result);
      if (error)
         LOG_ERROR(error);
   }
}

// A private claim beside the lock elects one stale-lock contender. The claim
// is held open so that its identity can be re-checked before acting on it.
struct Claim
{
   FilePath path;
   int descriptor = -1;
   LockMetadata identity;
};

void closeClaim(Claim* pClaim)
{
#ifndef _WIN32
   if (pClaim->descriptor != -1)
      ::close(pClaim->descriptor);
#endif
   pClaim->descriptor = -1;
}

void releaseClaim(Claim* pClaim)
{
#ifndef _WIN32
   if (pClaim->descriptor != -1)
   {
      RemoveResult result;
      Error error = removeIfSameIdentity(
         pClaim->path,
         pClaim->identity,
         &result);
      if (error)
         LOG_ERROR(error);
   }
#endif
   closeClaim(pClaim);
}

Error claimLockFile(const FilePath& claimFilePath,
                    Claim* pClaim,
                    bool* pClaimed)
{
   *pClaimed = false;
   pClaim->path = claimFilePath;
#ifdef _WIN32
   *pClaimed = true;
   return Success();
#else
   for (int attempt = 0; attempt < 2; ++attempt)
   {
      int descriptor = ::open(
         claimFilePath.getAbsolutePathNative().c_str(),
         O_RDWR | O_CREAT | O_EXCL,
         kLockFileMode);
      if (descriptor != -1)
      {
         pClaim->descriptor = descriptor;
         Error error = writeDescriptorContents(descriptor, lockContents(false));
         if (!error)
            error = descriptorIdentity(descriptor, &pClaim->identity);
         if (error)
         {
            unlinkBestEffort(claimFilePath);
            closeClaim(pClaim);
            return error;
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

      // Another contender's claim. Only a stale one (dead or expired owner)
      // may be replaced, and only if it is still the inode we inspected.
      LockInspection inspection;
      Error error = inspectLockFile(claimFilePath, &inspection);
      if (error)
         return error;
      if (inspection.exists && !inspection.stale)
         return Success();

      if (inspection.exists)
      {
         RemoveResult result;
         error = removeIfSameIdentity(claimFilePath, inspection.metadata, &result);
         if (error)
            return error;
         if (result == RemoveResult::Mismatch)
            return Success();
      }
   }

   return Success();
#endif
}

Error removeLockFile(const FilePath& lockFilePath,
                     const LockMetadata& expectedMetadata)
{
   Claim claim;
   bool claimed = false;
   Error error = claimLockFile(claimPathForLock(lockFilePath), &claim, &claimed);
   if (error)
      return error;
   if (!claimed)
      return noLockAvailableError(lockFilePath);

#ifndef _WIN32
   // A contender that judged this claim stale (e.g. after a long stall in a
   // load-balanced deployment) may have replaced it; the replacement's owner
   // is now the elected contender.
   bool stillOurs = false;
   error = hasExpectedIdentity(claim.path, claim.identity, &stillOurs);
   if (error || !stillOurs)
   {
      closeClaim(&claim);
      return error ? error : noLockAvailableError(lockFilePath);
   }
#endif

   // An inspection is tied to the inode it read. If another contender has
   // already replaced that inode, the replacement is left untouched.
   RemoveResult result;
   error = removeIfSameIdentity(lockFilePath, expectedMetadata, &result);
   if (!error && result == RemoveResult::Mismatch)
      error = noLockAvailableError(lockFilePath);

   if (!error && !expectedMetadata.ownerFilePath.isEmpty())
   {
      RemoveResult ownerResult;
      Error ownerError = removeIfSameIdentity(
         expectedMetadata.ownerFilePath,
         expectedMetadata,
         &ownerResult);
      if (ownerError)
         LOG_ERROR(ownerError);
   }

   releaseClaim(&claim);
   return error;
}

#ifndef _WIN32

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
         kLockFileMode);
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
         unlinkBestEffort(proxyPath);
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

// Removes the public path and owner file of a lock held through
// 'descriptor', but only where they still refer to that descriptor's inode.
// The inode is also marked released, so any hard link that could not be
// removed (or one created by an older RStudio) does not read as held.
Error releaseLockFiles(const FilePath& lockFilePath,
                       const FilePath& ownerFilePath,
                       int descriptor)
{
   LockMetadata identity;
   Error error = descriptorIdentity(descriptor, &identity);
   if (error)
      return error;

   RemoveResult result;
   error = removeIfSameIdentity(lockFilePath, identity, &result);
   if (error)
      LOG_ERROR(error);

   if (!ownerFilePath.isEmpty())
   {
      Error ownerError = removeIfSameIdentity(ownerFilePath, identity, &result);
      if (ownerError)
         LOG_ERROR(ownerError);
   }

   return writeLockContents(descriptor, true);
}

Error writeLockFile(const FilePath& lockFilePath,
                    std::string* pToken,
                    FilePath* pOwnerFilePath,
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

         // Take back whatever we published, and nothing else: a public path
         // that does not refer to our owner belongs to someone else.
         Error releaseError = releaseLockFiles(
            lockFilePath,
            proxyPath,
            proxyDescriptor);
         if (releaseError)
            LOG_ERROR(releaseError);
         ::close(proxyDescriptor);
         return validationError;
      }

      *pToken = token;
      *pOwnerFilePath = proxyPath;
      *pDescriptor = proxyDescriptor;
      return Success();
   }

   if (linkError == EEXIST)
   {
      ::close(proxyDescriptor);
      unlinkBestEffort(proxyPath);
      return fileExistsError(lockFilePath, ERROR_LOCATION);
   }

   // If the filesystem cannot create links, fall back to O_EXCL. An empty or
   // partially written fallback file is treated as held until its timeout, so
   // publication before this write cannot let another contender take over.
   int descriptor = ::open(
      lockFilePath.getAbsolutePathNative().c_str(),
      O_RDWR | O_CREAT | O_EXCL,
      kLockFileMode);
   if (descriptor == -1)
   {
      int errorNumber = errno;
      ::close(proxyDescriptor);
      unlinkBestEffort(proxyPath);

      Error openError = systemError(errorNumber, ERROR_LOCATION);
      openError.addProperty("lock-file", lockFilePath);
      return openError;
   }

   error = writeLockContents(descriptor, false);
   ::close(proxyDescriptor);
   unlinkBestEffort(proxyPath);

   if (error)
   {
      ::close(descriptor);
      unlinkBestEffort(lockFilePath);
      return error;
   }

   *pToken = token;
   *pOwnerFilePath = FilePath();
   *pDescriptor = descriptor;
   return Success();
}

#else

Error writeLockFile(const FilePath& lockFilePath,
                    std::string*,
                    FilePath*,
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
   FilePath lockFilePath;
   FilePath ownerFilePath;
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
                      int descriptor,
                      const FilePath& lockFilePath,
                      const FilePath& ownerFilePath)
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
         registeredDescriptor,
         lockFilePath,
         ownerFilePath};
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
         LOG("Clearing lock: " << entry.second.lockFilePath.getAbsolutePath());
         Error error = releaseLockFiles(
            entry.second.lockFilePath,
            entry.second.ownerFilePath,
            entry.second.descriptor);
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
   FilePath ownerFilePath;
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
   bool isLocked = false;
   Error error = this->isLocked(lockFilePath, &isLocked);
   if (error)
   {
      LOG_ERROR(error);
      return false;
   }

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
   FilePath ownerFilePath;
   int descriptor = -1;
   error = writeLockFile(
      lockFilePath,
      &token,
      &ownerFilePath,
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
      descriptor,
      lockFilePath,
      ownerFilePath);
   if (error)
   {
#ifndef _WIN32
      Error releaseError = releaseLockFiles(
         lockFilePath,
         ownerFilePath,
         descriptor);
      if (releaseError)
         LOG_ERROR(releaseError);
      ::close(descriptor);
#endif
      return error;
   }

   pImpl_->lockFilePath = lockFilePath;
   pImpl_->ownerFilePath = ownerFilePath;
   pImpl_->registrationKey = key;
   pImpl_->token = token;
   pImpl_->descriptor = descriptor;
   pImpl_->processId = system::currentProcessId();

   LOG("Acquired lock: " << lockFilePath.getAbsolutePath());
   sweepStaleArtifacts(lockFilePath.getParent(), ownerFilePath);
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
      // Everything is checked against the inode opened at acquisition. If a
      // stale takeover replaced the public path, the successor's entries do
      // not match and are left alone.
      error = releaseLockFiles(
         pImpl_->lockFilePath,
         pImpl_->ownerFilePath,
         pImpl_->descriptor);
   }
   ::close(pImpl_->descriptor);
#endif

   lockRegistration().deregisterLock(
      pImpl_->registrationKey,
      pImpl_->token);
   LOG("Released lock: " << pImpl_->lockFilePath.getAbsolutePath());

   pImpl_->lockFilePath = FilePath();
   pImpl_->ownerFilePath = FilePath();
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
   // Locks still registered at shutdown are released here; each removal is
   // identity-checked, so a pathname taken over by another process after a
   // lease expiry is left untouched.
   lockRegistration().clearLocks();
}

} // namespace core
} // namespace rstudio

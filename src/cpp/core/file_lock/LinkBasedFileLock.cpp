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

#if defined(__APPLE__) || defined(__linux__)
# include <sys/acl.h>
#endif

#ifdef __APPLE__
# include <membership.h>
#endif

#ifdef __linux__
# include <acl/libacl.h>
#endif

#ifdef _MSC_VER
# include <io.h>
#else
# include <dirent.h>
# include <sys/time.h>
# include <unistd.h>
#endif

#include <atomic>
#include <cstdint>
#include <ctime>
#include <limits>
#include <map>
#include <memory>
#include <mutex>
#include <sstream>
#include <string>
#include <vector>

#include <fmt/format.h>

#include <boost/optional.hpp>
#include <boost/system/error_code.hpp>

#include "ForkAwareRegistry.hpp"

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>
#include <shared_core/Hash.hpp>
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
const char * const kFileLockClaimDirectory = ".rstudio-lock-claims-41c29";
const char * const kFileLockClaimTempPrefix = ".rstudio-lock-claims-tmp-41c29";
const char * const kFileLockTempPrefix = ".rstudio-lock-tmp-41c29";
const char * const kReleasedProcessId = "-1";

// Lock files must stay readable by other users: in a shared project, a
// collaborator's session inspects (and eventually expires) locks it did not
// create. The process umask still applies.
const int kLockFileMode = 0644;

#ifdef RSTUDIO_UNIT_TESTS_ENABLED
boost::function<void()> s_beforeRelease;
boost::function<void()> s_beforeRefresh;
boost::function<Error(int)> s_beforeWrite;
boost::function<void(const FilePath&)> s_beforeClaim;
boost::function<void(const FilePath&)> s_afterRename;
bool s_forceClaimDirectoryChownFailure = false;
bool s_forceFallback = false;
#endif

// Shared by the owner and registry snapshots. This mutex is never held by
// the fork handlers: an inherited owner cannot release in the child, and
// the child drops the registry's copies before taking any new locks.
struct LockState
{
   std::mutex mutex;
   bool released = false;
};

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

bool isTempFile(const FilePath& filePath)
{
   return hasPrefix(filePath.getFilename(), kFileLockTempPrefix);
}

bool isPreparedClaimDirectory(const FilePath& filePath)
{
   return hasPrefix(filePath.getFilename(), kFileLockClaimTempPrefix);
}

bool isSweepableArtifact(const FilePath& filePath)
{
   return isOwnerFile(filePath) ||
          hasPrefix(filePath.getFilename(), kFileLockClaimPrefix) ||
          isTempFile(filePath);
}

FilePath proxyPathForToken(const FilePath& lockFilePath,
                           const std::string& token)
{
   return lockFilePath.getParent().completePath(
      fmt::format("{}-{}", kOwnerFilePrefix, token));
}

FilePath claimPathForLock(const FilePath& lockFilePath)
{
   // Keep the public filename as its own path component so the filesystem
   // applies exactly the same case and normalization rules to both names.
   // The containing directory supplies the claim namespace without making
   // an already-near-NAME_MAX filename longer.
   return lockFilePath.getParent()
      .completePath(kFileLockClaimDirectory)
      .completePath(lockFilePath.getFilename());
}

FilePath legacyClaimPathForLock(const FilePath& lockFilePath)
{
   // Keep taking the claim used by deployed versions so a rolling upgrade
   // cannot run old and new stale-removal protocols at the same time.
   FilePath parent(lockFilePath.getParent().getCanonicalPath());
   return parent.completePath(
      fmt::format("{}-{}",
                  kFileLockClaimPrefix,
                  hash::crc32HexHash(lockFilePath.getFilename())));
}

#ifndef _WIN32

// A renamed-aside entry is named for the contender doing the removal, so
// that a sweep can tell an abandoned one (contender gone) from one whose
// removal is still in flight.
FilePath tempPathBeside(const FilePath& filePath)
{
   return filePath.getParent().completePath(
      fmt::format("{}-{}-{}",
                  kFileLockTempPrefix,
                  pidString(),
                  system::generateUuid(false)));
}

boost::optional<PidType> temporaryEntryContender(
   const std::string& filename,
   const char* prefixValue)
{
   std::string prefix = fmt::format("{}-", prefixValue);
   std::string rest = filename.substr(prefix.size());
   std::string::size_type end = rest.find('-');
   if (end == std::string::npos)
      return boost::none;

   boost::optional<uint64_t> value =
      safe_convert::stringTo<uint64_t>(rest.substr(0, end));
   if (!value || *value == 0 ||
       *value > static_cast<uint64_t>(std::numeric_limits<PidType>::max()))
   {
      return boost::none;
   }

   return static_cast<PidType>(*value);
}

#endif

std::string lockContents(bool released)
{
   // Keep the public contents parseable as a PID by older RStudio versions.
   // The negative release sentinel is also treated as stale by older local
   // readers; older load-balanced readers require an expired timestamp too.
   return fmt::format("{}\n", released ? kReleasedProcessId : pidString());
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
   return FileLock::noLockAvailableError(lockFilePath);
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
#ifdef RSTUDIO_UNIT_TESTS_ENABLED
   if (s_beforeWrite)
   {
      Error error = s_beforeWrite(descriptor);
      if (error)
         return error;
   }
#endif

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

// Stamps the lock with this host's clock via an explicit timestamp. A plain
// write instead leaves the mtime to the filesystem, which on a network mount
// is the server's clock; staleness is judged against the contender's clock
// (and, for a local owner, its process start time), so the two must not be
// mixed. Returns the error rather than logging it, so refreshLocks() -- which
// has no write of its own to fall back on -- can react to an unsupported mount.
Error stampWriteTime(int descriptor, bool released = false)
{
   // Legacy load-balanced readers only inspect age. Retire the exact inode
   // at the epoch so those readers also see an immediate release, regardless
   // of their timeout or small differences between host clocks.
   struct timeval now = {0, 0};
   if (!released && ::gettimeofday(&now, nullptr) == -1)
      return systemCallError("gettimeofday", errno, ERROR_LOCATION);

   struct timeval times[2] = {now, now};
   if (::futimes(descriptor, times) == -1)
      return systemCallError("futimes", errno, ERROR_LOCATION);

   return Success();
}

// Stamps the lock, tolerating mounts that refuse explicit timestamps (utimes
// needs ownership, which uid squashing takes away; some CIFS and FUSE backends
// lack the operation). Used where a write to the lock has just set the mtime
// anyway, so a failure here is cosmetic: a degraded mode worth one log line
// per process, not a failure that would keep every session from acquiring a
// lock there. After release, current readers still recognize the sentinel;
// legacy age-only readers must wait for timeout if backdating is denied.
void stampWriteTimeBestEffort(int descriptor, bool released = false)
{
   Error error = stampWriteTime(descriptor, released);

   static std::atomic<bool> s_reported(false);
   if (error && !s_reported.exchange(true))
   {
      error.addProperty("description",
                        "Lock timestamps will follow the filesystem clock");
      LOG_ERROR(error);
   }
}

// Writes the contents before truncating to their length, so that a write
// that fails (e.g. ENOSPC) leaves the previous contents, which still name
// the owner, rather than an empty file that is held until it ages out.
Error writeLockContents(int descriptor, bool released)
{
   std::string contents = lockContents(released);
   if (::lseek(descriptor, 0, SEEK_SET) == -1)
      return systemCallError("lseek", errno, ERROR_LOCATION);

   Error error = writeDescriptorContents(descriptor, contents);
   if (error)
      return error;

   if (::ftruncate(descriptor, static_cast<off_t>(contents.size())) == -1)
      return systemCallError("ftruncate", errno, ERROR_LOCATION);

   stampWriteTimeBestEffort(descriptor, released);
   return Success();
}

Error descriptorIdentity(int descriptor,
                         LockMetadata* pMetadata,
                         std::time_t* pLastWriteTime = nullptr)
{
   struct stat info;
   if (::fstat(descriptor, &info) == -1)
      return systemCallError("fstat", errno, ERROR_LOCATION);

   pMetadata->device = info.st_dev;
   pMetadata->inode = info.st_ino;
   pMetadata->hasIdentity = true;
   pMetadata->identityFollowsSymlink = true;
   if (pLastWriteTime != nullptr)
      *pLastWriteTime = info.st_mtime;
   return Success();
}

Error readDescriptorMetadata(int descriptor,
                             LockMetadata* pMetadata,
                             std::time_t* pLastWriteTime)
{
   Error error = descriptorIdentity(descriptor, pMetadata, pLastWriteTime);
   if (error)
      return error;

   std::string contents;
   char buffer[64];
   while (contents.size() <= sizeof(buffer))
   {
      ssize_t result = ::read(descriptor, buffer, sizeof(buffer));
      if (result == -1)
      {
         if (errno == EINTR)
            continue;
         return systemCallError("read", errno, ERROR_LOCATION);
      }
      if (result == 0)
         break;

      contents.append(buffer, static_cast<std::size_t>(result));
   }

   parseLockContents(contents, pMetadata);
   return Success();
}

#endif

// Reads identity, timestamp and contents from one opened descriptor, so that
// a lock replaced mid-inspection cannot be judged by another inode's age.
Error readLockMetadata(const FilePath& lockFilePath,
                       LockMetadata* pMetadata,
                       std::time_t* pLastWriteTime)
{
#ifndef _WIN32
   int descriptor = ::open(
      lockFilePath.getAbsolutePathNative().c_str(),
      O_RDONLY | O_NONBLOCK | O_CLOEXEC);
   if (descriptor == -1)
   {
      Error error = systemError(errno, ERROR_LOCATION);
      error.addProperty("lock-file", lockFilePath);
      return error;
   }

   Error error = readDescriptorMetadata(
      descriptor,
      pMetadata,
      pLastWriteTime);
   ::close(descriptor);
   return error;
#else
   std::string contents;
   Error error = core::readStringFromFile(lockFilePath, &contents);
   if (error)
      return error;

   error = lockFilePath.getLastWriteTime(*pLastWriteTime);
   if (error)
      return error;
   parseLockContents(contents, pMetadata);
   return Success();
#endif
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

#ifndef _WIN32

Error hasExpectedIdentityAt(int directoryDescriptor,
                            const FilePath& filePath,
                            const LockMetadata& metadata,
                            bool* pMatches)
{
   *pMatches = false;
   if (!metadata.hasIdentity)
      return Success();

   struct stat info;
   if (::fstatat(
          directoryDescriptor,
          filePath.getFilename().c_str(),
          &info,
          AT_SYMLINK_NOFOLLOW) == -1)
   {
      if (errno == ENOENT)
         return Success();

      Error error = systemError(errno, ERROR_LOCATION);
      error.addProperty("path", filePath);
      return error;
   }

   *pMatches = info.st_dev == metadata.device &&
               info.st_ino == metadata.inode;
   return Success();
}

Error unlinkAt(int directoryDescriptor, const FilePath& filePath)
{
   if (::unlinkat(
          directoryDescriptor,
          filePath.getFilename().c_str(),
          0) == 0 || errno == ENOENT)
   {
      return Success();
   }

   Error error = systemError(errno, ERROR_LOCATION);
   error.addProperty("path", filePath);
   return error;
}

void unlinkAtBestEffort(int directoryDescriptor, const FilePath& filePath)
{
   Error error = unlinkAt(directoryDescriptor, filePath);
   if (error)
      LOG_ERROR(error);
}

// Directory-relative counterpart to removeIfSameIdentity(). Claim operations
// stay anchored to the verified namespace even if its public name is replaced.
Error removeIfSameIdentityAt(int directoryDescriptor,
                             const FilePath& filePath,
                             const LockMetadata& metadata,
                             RemoveResult* pResult)
{
   *pResult = RemoveResult::Absent;
   bool matches = false;
   Error error = hasExpectedIdentityAt(
      directoryDescriptor,
      filePath,
      metadata,
      &matches);
   if (error)
      return error;
   if (!matches)
   {
      struct stat info;
      if (::fstatat(
             directoryDescriptor,
             filePath.getFilename().c_str(),
             &info,
             AT_SYMLINK_NOFOLLOW) == 0)
      {
         *pResult = RemoveResult::Mismatch;
      }
      return Success();
   }

   FilePath tempPath = tempPathBeside(filePath);
   if (::renameat(
          directoryDescriptor,
          filePath.getFilename().c_str(),
          directoryDescriptor,
          tempPath.getFilename().c_str()) == -1)
   {
      if (errno == ENOENT)
         return Success();

      Error error = systemError(errno, ERROR_LOCATION);
      error.addProperty("path", filePath);
      return error;
   }

#ifdef RSTUDIO_UNIT_TESTS_ENABLED
   if (s_afterRename)
      s_afterRename(filePath);
#endif

   error = hasExpectedIdentityAt(
      directoryDescriptor,
      tempPath,
      metadata,
      &matches);
   if (!error && matches)
   {
      *pResult = RemoveResult::Removed;
      return unlinkAt(directoryDescriptor, tempPath);
   }

   bool restored =
      ::linkat(directoryDescriptor, tempPath.getFilename().c_str(),
               directoryDescriptor, filePath.getFilename().c_str(), 0) == 0;
   int linkError = restored ? 0 : errno;
   if (restored)
   {
      unlinkAtBestEffort(directoryDescriptor, tempPath);
   }
   else if (linkError == EEXIST)
   {
      restored = true;
   }
   else
   {
      struct stat info;
      if (::fstatat(
             directoryDescriptor,
             filePath.getFilename().c_str(),
             &info,
             AT_SYMLINK_NOFOLLOW) == -1 && errno == ENOENT)
      {
         restored = ::renameat(
            directoryDescriptor,
            tempPath.getFilename().c_str(),
            directoryDescriptor,
            filePath.getFilename().c_str()) == 0;
      }
   }

   if (!restored)
   {
      Error restoreError = systemCallError("linkat", linkError, ERROR_LOCATION);
      restoreError.addProperty("path", filePath);
      restoreError.addProperty("description",
                               "Could not restore a displaced claim entry");
      LOG_ERROR(restoreError);
      unlinkAtBestEffort(directoryDescriptor, tempPath);
   }

   *pResult = RemoveResult::Mismatch;
   return error;
}

#endif

// Removes the entry at 'filePath' only if it is still the inode described by
// 'metadata'. POSIX has no conditional unlink, so after a cheap identity
// check the entry is renamed aside (atomically taking whatever is at the
// path), checked again, and either unlinked or linked back into place.
//
// The identity check and the rename are two system calls, and no POSIX
// primitive combines them, so an entry replaced in between is briefly
// displaced and then restored; a link-back that finds the path re-occupied
// gives up on the displaced entry rather than clobber the newer one. Every
// other step of the protocol is an atomic election (rename of the stale
// entry, link/O_EXCL publication), so this window is the whole residual
// risk of breaking a stale lock: it needs an abandoned entry plus three
// contenders acting within a few system calls of each other. Public-lock
// removals additionally run under the claim, which excludes other contenders
// entirely; only reclaiming an abandoned claim itself runs unprotected.
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
   bool matches = false;
   Error error = hasExpectedIdentity(filePath, metadata, &matches);
   if (error)
      return error;
   if (!matches)
   {
      struct stat info;
      if (::lstat(filePath.getAbsolutePathNative().c_str(), &info) == 0)
         *pResult = RemoveResult::Mismatch;
      return Success();
   }

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

#ifdef RSTUDIO_UNIT_TESTS_ENABLED
   if (s_afterRename)
      s_afterRename(filePath);
#endif

   error = hasExpectedIdentity(tempPath, metadata, &matches);
   if (!error && matches)
   {
      *pResult = RemoveResult::Removed;
      return unlinkPath(tempPath);
   }

   // Not ours: put it back without following (or clobbering) anything. Where
   // the filesystem cannot link (the same mounts that take the O_EXCL
   // fallback), fall back to a rename into a path we have just seen empty.
   bool restored =
      ::linkat(AT_FDCWD, tempPath.getAbsolutePathNative().c_str(),
               AT_FDCWD, filePath.getAbsolutePathNative().c_str(), 0) == 0;
   int linkError = restored ? 0 : errno;
   if (restored)
   {
      // linkat() leaves the renamed-aside name in place as a second link
      unlinkBestEffort(tempPath);
   }
   else if (linkError == EEXIST)
   {
      // The path was re-occupied by a newer entry while ours was aside. We
      // cannot restore ours without clobbering the newer one, but the
      // displaced entry may be a live owner's lock, so it must not be
      // discarded either. Leave it renamed-aside; the sweep reclaims it once
      // it (or the contender that displaced it) ages out. Treated as restored
      // so the failure path below does not delete it.
      restored = true;
   }
   else
   {
      struct stat info;
      if (::lstat(filePath.getAbsolutePathNative().c_str(), &info) == -1 &&
          errno == ENOENT)
      {
         restored = ::rename(tempPath.getAbsolutePathNative().c_str(),
                             filePath.getAbsolutePathNative().c_str()) == 0;
      }
   }

   if (!restored)
   {
      Error restoreError = systemCallError("linkat", linkError, ERROR_LOCATION);
      restoreError.addProperty("path", filePath);
      restoreError.addProperty("description",
                               "Could not restore a displaced lock entry");
      LOG_ERROR(restoreError);
      unlinkBestEffort(tempPath);
   }

   *pResult = RemoveResult::Mismatch;
   return error;
#endif
}

Error findOwnerFile(const FilePath& lockFilePath, LockMetadata* pMetadata)
{
   // Only a public lock path has a separate owner file; an artifact being
   // inspected for the sweep has nothing to find and no caller reads it.
   if (isSweepableArtifact(lockFilePath))
      return Success();

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

bool isProcessGone(PidType processId)
{
   return !system::isProcessRunning(processId) ||
          system::isProcessZombie(processId);
}

// A live PID is not proof of a live owner: a crashed owner's PID may have
// been handed to an unrelated process. The owner wrote the lock after it
// started, so a process that started after the last refresh cannot be the
// owner. Both timestamps come from this host's clock (see stampWriteTime),
// so only their resolution needs absorbing: /proc reports the start time in
// whole seconds since a boot time that is itself rounded. A wall-clock step
// also separates them, which is why callers consult this only for a lock
// that has already aged out: a live owner refreshes well within the timeout.
//
// Returns none when the start time is unavailable (e.g. /proc mounted with
// hidepid), so the caller can fall back to the age check rather than let
// bare PID existence pin the lock.
boost::optional<bool> isOwnerProcessReused(PidType processId,
                                           std::time_t lastWriteTime)
{
   system::ProcessInfo info;
   info.pid = processId;
   boost::posix_time::ptime created;
   Error error = info.creationTime(&created);
   if (error)
      return boost::none;

   const double kStartTimeToleranceSeconds = 5;
   double startSeconds = date_time::secondsSinceEpoch(created);
   return startSeconds >
          static_cast<double>(lastWriteTime) + kStartTimeToleranceSeconds;
}

#endif

void determineStaleness(std::time_t lastWriteTime,
                        LockInspection* pInspection)
{
   if (pInspection->metadata.released)
   {
      pInspection->stale = true;
      return;
   }

   double seconds =
      static_cast<double>(FileLock::getTimeoutInterval().total_seconds());
   double age = ::difftime(::time(nullptr), lastWriteTime);
   pInspection->stale = age >= seconds;

#ifndef _WIN32
   if (!FileLock::isLoadBalanced() && pInspection->metadata.processId)
   {
      PidType processId = *pInspection->metadata.processId;
      if (isProcessGone(processId))
      {
         pInspection->stale = true;
      }
      else if (pInspection->stale)
      {
         boost::optional<bool> reused =
            isOwnerProcessReused(processId, lastWriteTime);
         if (reused && !*reused)
         {
            double grace = seconds * FileLock::getLiveOwnerGraceMultiplier();
            pInspection->stale = age >= grace;
         }
      }
   }
#endif
}

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

#else
   if (!lockFilePath.exists())
      return Success();
#endif

   pInspection->exists = true;
   Error error = readLockMetadata(
      lockFilePath,
      &pInspection->metadata,
      &lastWriteTime);
   if (error)
   {
      if (isFileNotFoundError(error))
      {
         pInspection->exists = false;
         return Success();
      }

#ifndef _WIN32
      // Another user's lock we cannot read still expires by age. Identity and
      // timestamp then come from the pathname (one stat call, so they are at
      // least consistent with each other) and the PID stays unknown.
      if (!isPermissionError(error.getCode()))
         return error;

      pInspection->metadata.device = info.st_dev;
      pInspection->metadata.inode = info.st_ino;
      pInspection->metadata.hasIdentity = true;
      pInspection->metadata.identityFollowsSymlink = true;
      lastWriteTime = info.st_mtime;
#else
      return error;
#endif
   }

   determineStaleness(lastWriteTime, pInspection);

   return pInspection->stale
      ? findOwnerFile(lockFilePath, &pInspection->metadata)
      : Success();
}

#ifndef _WIN32

Error inspectClaimFileAt(int directoryDescriptor,
                         const FilePath& claimFilePath,
                         LockInspection* pInspection)
{
   struct stat namedInfo;
   if (::fstatat(
          directoryDescriptor,
          claimFilePath.getFilename().c_str(),
          &namedInfo,
          AT_SYMLINK_NOFOLLOW) == -1)
   {
      if (errno == ENOENT)
         return Success();

      Error error = systemError(errno, ERROR_LOCATION);
      error.addProperty("path", claimFilePath);
      return error;
   }

   pInspection->exists = true;
   if (!S_ISREG(namedInfo.st_mode))
   {
      Error error = systemError(
         boost::system::errc::invalid_argument,
         "Claim entry is not a regular file",
         ERROR_LOCATION);
      error.addProperty("path", claimFilePath);
      return error;
   }

   int flags = O_RDONLY | O_NONBLOCK | O_CLOEXEC;
#ifdef O_NOFOLLOW
   flags |= O_NOFOLLOW;
#endif
   int descriptor = ::openat(
      directoryDescriptor,
      claimFilePath.getFilename().c_str(),
      flags);
   std::time_t lastWriteTime = 0;
   if (descriptor == -1)
   {
      if (errno == ENOENT)
      {
         pInspection->exists = false;
         return Success();
      }
      if (!isPermissionError(errno))
      {
         Error error = systemError(errno, ERROR_LOCATION);
         error.addProperty("path", claimFilePath);
         return error;
      }

      pInspection->metadata.device = namedInfo.st_dev;
      pInspection->metadata.inode = namedInfo.st_ino;
      pInspection->metadata.hasIdentity = true;
      pInspection->metadata.identityFollowsSymlink = false;
      lastWriteTime = namedInfo.st_mtime;
   }
   else
   {
      Error error = readDescriptorMetadata(
         descriptor,
         &pInspection->metadata,
         &lastWriteTime);
      ::close(descriptor);
      if (error)
         return error;

      pInspection->metadata.identityFollowsSymlink = false;
   }

   determineStaleness(lastWriteTime, pInspection);
   return Success();
}

// A renamed-aside entry is abandoned once the contender that renamed it is
// gone. Its own timestamps say nothing (rename keeps the old owner's mtime),
// so the rename's ctime stands in: no removal stays in flight for a whole
// timeout. On a single host a gone contender is proof at once; but a PID that
// is live and unrelated (a container sharing the directory, a reused number)
// or a name we cannot parse must not pin the entry forever, so those fall
// through to the same ctime check the load-balanced path always uses.
bool isTemporaryEntryAbandoned(const FilePath& filePath, const char* prefix)
{
   if (!FileLock::isLoadBalanced())
   {
      boost::optional<PidType> contender = temporaryEntryContender(
         filePath.getFilename(),
         prefix);
      if (contender && isProcessGone(*contender))
         return true;
   }

   struct stat info;
   if (::lstat(filePath.getAbsolutePathNative().c_str(), &info) == -1)
      return false;

   double seconds =
      static_cast<double>(FileLock::getTimeoutInterval().total_seconds());
   return ::difftime(::time(nullptr), info.st_ctime) >= seconds;
}

bool isTempFileAbandoned(const FilePath& filePath)
{
   return isTemporaryEntryAbandoned(filePath, kFileLockTempPrefix);
}

bool isTemporaryEntryAbandonedAt(int directoryDescriptor,
                                 const FilePath& filePath,
                                 const char* prefix)
{
   if (!FileLock::isLoadBalanced())
   {
      boost::optional<PidType> contender = temporaryEntryContender(
         filePath.getFilename(),
         prefix);
      if (contender && isProcessGone(*contender))
         return true;
   }

   struct stat info;
   if (::fstatat(
          directoryDescriptor,
          filePath.getFilename().c_str(),
          &info,
          AT_SYMLINK_NOFOLLOW) == -1)
   {
      return false;
   }

   double seconds =
      static_cast<double>(FileLock::getTimeoutInterval().total_seconds());
   return ::difftime(::time(nullptr), info.st_ctime) >= seconds;
}

#endif

// Removes stale owner, legacy claim, temp, and prepared-namespace entries left
// in a lock directory. Live entries are never touched; file candidates are
// inspected like locks, while prepared namespaces must be empty directories.
void sweepStaleArtifacts(const FilePath& directory, const FilePath& ownPath)
{
   std::vector<FilePath> children;
   Error error = directory.getChildren(children);
   if (error)
   {
#ifndef _WIN32
      if (!isPermissionError(error.getCode()))
         LOG_ERROR(error);
#else
      LOG_ERROR(error);
#endif
      return;
   }

   for (const FilePath& child : children)
   {
      if (child == ownPath)
         continue;

#ifndef _WIN32
      if (isPreparedClaimDirectory(child))
      {
         if (!isTemporaryEntryAbandoned(child, kFileLockClaimTempPrefix))
            continue;

         // A prepared namespace is always an empty directory. Refuse files
         // and symlinks, and use rmdir() so a nonempty or concurrently
         // replaced entry is left untouched.
         struct stat info;
         if (::lstat(child.getAbsolutePathNative().c_str(), &info) == 0 &&
             S_ISDIR(info.st_mode) &&
             ::rmdir(child.getAbsolutePathNative().c_str()) == -1 &&
             errno != ENOENT && errno != ENOTEMPTY && errno != EEXIST &&
             !isPermissionError(errno))
         {
            Error removeError = systemError(errno, ERROR_LOCATION);
            removeError.addProperty("path", child);
            LOG_ERROR(removeError);
         }
         continue;
      }
#endif

      if (!isSweepableArtifact(child))
         continue;

#ifndef _WIN32
      // A hard-linked owner file still referenced by a public lock path is a
      // live (or merely timed-out) lock's owner, not an orphan: the normal
      // takeover path removes both together. Only an orphan whose public path
      // is gone has a single link. Skipping the still-linked ones by their
      // cheap link count keeps this per-acquire sweep from running a full
      // inspection (open/read/kill/proc) on every live owner in the directory.
      // Symlink-mode owners always have one link, so they fall through as
      // before.
      if (isOwnerFile(child))
      {
         struct stat info;
         if (::lstat(child.getAbsolutePathNative().c_str(), &info) == 0 &&
             S_ISREG(info.st_mode) && info.st_nlink > 1)
            continue;
      }
#endif

      LockInspection inspection;
      error = inspectLockFile(child, &inspection);
      if (error || !inspection.exists || !inspection.stale)
         continue;

#ifndef _WIN32
      // A caller's public basename may look like an internal temporary. Only
      // apply the temp lifecycle after proving the entry itself is stale.
      if (isTempFile(child) && !isTempFileAbandoned(child))
         continue;
#endif

      LOG("Removing stale lock artifact: " << child.getAbsolutePath());
      RemoveResult result;
      error = removeIfSameIdentity(child, inspection.metadata, &result);
      if (error)
         LOG_ERROR(error);
   }
}

#ifndef _WIN32

// Claims use the public basename inside their private directory, so every
// entry there is a claim or an identity-preserving removal temporary. Sweep
// stale entries whenever any lock in the directory is acquired.
void sweepStaleClaims(int directoryDescriptor,
                      const FilePath& directory,
                      const LockMetadata& ownIdentity)
{
   // Acquire needs only search permission. Open a separate read descriptor
   // for this best-effort sweep; a write-and-search-only namespace still
   // remains fully usable for locking.
   int flags = O_RDONLY | O_CLOEXEC;
#ifdef O_DIRECTORY
   flags |= O_DIRECTORY;
#endif
   int scanDescriptor = ::openat(directoryDescriptor, ".", flags);
   if (scanDescriptor == -1)
   {
      if (!isPermissionError(errno))
      {
         Error error = systemError(errno, ERROR_LOCATION);
         error.addProperty("path", directory);
         LOG_ERROR(error);
      }
      return;
   }

   DIR* entries = ::fdopendir(scanDescriptor);
   if (!entries)
   {
      int errorNumber = errno;
      ::close(scanDescriptor);
      Error error = systemError(errorNumber, ERROR_LOCATION);
      error.addProperty("path", directory);
      LOG_ERROR(error);
      return;
   }

   int readError = 0;
   for (;;)
   {
      errno = 0;
      dirent* pEntry = ::readdir(entries);
      if (!pEntry)
      {
         readError = errno;
         break;
      }

      std::string filename = pEntry->d_name;
      if (filename == "." || filename == "..")
         continue;

      FilePath child = directory.completePath(filename);
      bool ownClaim = false;
      Error error = hasExpectedIdentityAt(
         directoryDescriptor,
         child,
         ownIdentity,
         &ownClaim);
      if (error || ownClaim)
         continue;

      LockInspection inspection;
      error = inspectClaimFileAt(
         directoryDescriptor,
         child,
         &inspection);
      if (error || !inspection.exists || !inspection.stale)
         continue;

      // A public basename may itself look like an internal temp name. Only
      // apply the temp lifecycle after identity and liveness checks, so an
      // own or concurrently live claim can never be classified by its name.
      if (hasPrefix(filename, kFileLockTempPrefix) &&
          !isTemporaryEntryAbandonedAt(
             directoryDescriptor,
             child,
             kFileLockTempPrefix))
      {
         continue;
      }

      RemoveResult result;
      error = removeIfSameIdentityAt(
         directoryDescriptor,
         child,
         inspection.metadata,
         &result);
      if (error)
         LOG_ERROR(error);
   }

   ::closedir(entries);
   if (readError != 0)
   {
      Error error = systemError(readError, ERROR_LOCATION);
      error.addProperty("path", directory);
      LOG_ERROR(error);
   }
}

#endif

// A claim in the private namespace elects one acquirer through removal and
// publication. Both it and the namespace stay open so their identities can be
// re-checked without traversing a replaced directory name.
struct Claim
{
   ~Claim();

   FilePath path;
   int descriptor = -1;
   int directoryDescriptor = -1;
   LockMetadata identity;
   LockMetadata directoryIdentity;
};

void closeClaim(Claim* pClaim)
{
#ifndef _WIN32
   if (pClaim->descriptor != -1)
      ::close(pClaim->descriptor);
   if (pClaim->directoryDescriptor != -1)
      ::close(pClaim->directoryDescriptor);
#endif
   pClaim->descriptor = -1;
   pClaim->directoryDescriptor = -1;
}

void releaseClaim(Claim* pClaim)
{
#ifndef _WIN32
   if (pClaim->descriptor != -1 && pClaim->directoryDescriptor != -1)
   {
      RemoveResult result;
      Error error = removeIfSameIdentityAt(
         pClaim->directoryDescriptor,
         pClaim->path,
         pClaim->identity,
         &result);
      if (error)
         LOG_ERROR(error);
   }
#endif
   closeClaim(pClaim);
}

Claim::~Claim()
{
   releaseClaim(this);
}

Error validateClaim(const Claim& claim, const FilePath& lockFilePath)
{
#ifndef _WIN32
   bool namedDirectory = false;
   Error error = hasExpectedIdentity(
      claim.path.getParent(),
      claim.directoryIdentity,
      &namedDirectory);
   if (error)
      return error;
   if (!namedDirectory)
      return noLockAvailableError(lockFilePath);

   bool held = false;
   error = hasExpectedIdentityAt(
      claim.directoryDescriptor,
      claim.path,
      claim.identity,
      &held);
   if (error)
      return error;
   if (!held)
      return noLockAvailableError(lockFilePath);
#endif
   return Success();
}

Error claimLockFile(const FilePath& claimFilePath,
                    int claimDirectoryDescriptor,
                    Claim* pClaim,
                    bool* pClaimed)
{
   *pClaimed = false;
   pClaim->path = claimFilePath;
#ifdef _WIN32
   (void)claimDirectoryDescriptor;
   *pClaimed = true;
   return Success();
#else
   for (int attempt = 0; attempt < 2; ++attempt)
   {
      int descriptor = ::openat(
         claimDirectoryDescriptor,
         claimFilePath.getFilename().c_str(),
         O_RDWR | O_CREAT | O_EXCL | O_CLOEXEC,
         kLockFileMode);
      if (descriptor != -1)
      {
         pClaim->descriptor = descriptor;
         Error error = writeDescriptorContents(descriptor, lockContents(false));
         if (!error)
         {
            stampWriteTimeBestEffort(descriptor);
            error = descriptorIdentity(descriptor, &pClaim->identity);
         }
         if (error)
         {
            // Initialization may have stalled past expiry. The reusable
            // claim name can now belong to a successor; leave any incomplete
            // publication to expire instead of unlinking that name.
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
      Error error = inspectClaimFileAt(
         claimDirectoryDescriptor,
         claimFilePath,
         &inspection);
      if (error)
         return error;
      if (inspection.exists && !inspection.stale)
         return Success();

      if (inspection.exists)
      {
         RemoveResult result;
         error = removeIfSameIdentityAt(
            claimDirectoryDescriptor,
            claimFilePath,
            inspection.metadata,
            &result);
         if (error)
            return error;
         if (result == RemoveResult::Mismatch)
            return Success();
      }
   }

   return Success();
#endif
}

Error verifyClaimDirectory(const FilePath& claimDirectory)
{
#ifndef _WIN32
   struct stat info;
   if (::lstat(claimDirectory.getAbsolutePathNative().c_str(), &info) == -1)
   {
      Error error = systemError(errno, ERROR_LOCATION);
      error.addProperty("path", claimDirectory);
      return error;
   }

   if (!S_ISDIR(info.st_mode))
   {
      Error error = systemError(
         boost::system::errc::not_a_directory,
         ERROR_LOCATION);
      error.addProperty("path", claimDirectory);
      return error;
   }
#else
   (void)claimDirectory;
#endif
   return Success();
}

bool isClaimDirectoryRetryError(const Error& error)
{
   if (error.getCode() == ENOENT)
      return true;
#ifdef ESTALE
   return error.getCode() == ESTALE;
#else
   return false;
#endif
}

#ifndef _WIN32

#ifdef __linux__

Error currentGroups(std::vector<gid_t>* pGroups)
{
   pGroups->clear();
   pGroups->push_back(::getegid());

   int count = ::getgroups(0, nullptr);
   if (count == -1)
      return systemCallError("getgroups", errno, ERROR_LOCATION);
   if (count == 0)
      return Success();

   std::vector<gid_t> supplementary(static_cast<std::size_t>(count));
   if (::getgroups(count, supplementary.data()) == -1)
      return systemCallError("getgroups", errno, ERROR_LOCATION);
   pGroups->insert(
      pGroups->end(),
      supplementary.begin(),
      supplementary.end());
   return Success();
}

bool containsGroup(const std::vector<gid_t>& groups, gid_t expected)
{
   for (gid_t group : groups)
   {
      if (group == expected)
         return true;
   }
   return false;
}

Error currentModePermissions(const struct stat& parentInfo,
                             mode_t* pPermissions)
{
   if (::geteuid() == parentInfo.st_uid)
   {
      *pPermissions = (parentInfo.st_mode >> 6) & 07;
      return Success();
   }

   std::vector<gid_t> groups;
   Error error = currentGroups(&groups);
   if (error)
      return error;
   *pPermissions = containsGroup(groups, parentInfo.st_gid)
      ? (parentInfo.st_mode >> 3) & 07
      : parentInfo.st_mode & 07;
   return Success();
}

#endif

#endif

#if defined(__APPLE__) || defined(__linux__)

bool isAclUnsupported(int errorNumber)
{
   if (errorNumber == ENOTSUP)
      return true;
#if defined(EOPNOTSUPP) && EOPNOTSUPP != ENOTSUP
   return errorNumber == EOPNOTSUPP;
#else
   return false;
#endif
}

#endif

#ifdef __linux__

struct PosixAccessControlList
{
   mode_t owner = 0;
   mode_t group = 0;
   mode_t other = 0;
   mode_t mask = 07;
   std::map<uid_t, mode_t> users;
   std::map<gid_t, mode_t> groups;
};

Error aclCallError(const char* operation,
                   int errorNumber,
                   const FilePath& path)
{
   Error error = systemCallError(operation, errorNumber, ERROR_LOCATION);
   error.addProperty("path", path);
   return error;
}

Error readAclPermissions(acl_entry_t entry, mode_t* pPermissions)
{
   *pPermissions = 0;
   acl_permset_t permissionSet;
   if (::acl_get_permset(entry, &permissionSet) == -1)
      return systemCallError("acl_get_permset", errno, ERROR_LOCATION);

   int result = ::acl_get_perm(permissionSet, ACL_READ);
   if (result == -1)
      return systemCallError("acl_get_perm", errno, ERROR_LOCATION);
   if (result == 1)
      *pPermissions |= 04;

   result = ::acl_get_perm(permissionSet, ACL_WRITE);
   if (result == -1)
      return systemCallError("acl_get_perm", errno, ERROR_LOCATION);
   if (result == 1)
      *pPermissions |= 02;

   result = ::acl_get_perm(permissionSet, ACL_EXECUTE);
   if (result == -1)
      return systemCallError("acl_get_perm", errno, ERROR_LOCATION);
   if (result == 1)
      *pPermissions |= 01;

   return Success();
}

Error readPosixAccessControlList(const FilePath& directory,
                                 PosixAccessControlList* pAccess)
{
   acl_t acl = ::acl_get_file(
      directory.getAbsolutePathNative().c_str(),
      ACL_TYPE_ACCESS);
   if (!acl)
      return aclCallError("acl_get_file", errno, directory);

   bool hasMask = false;
   acl_entry_t entry;
   int entryId = ACL_FIRST_ENTRY;
   for (;;)
   {
      int result = ::acl_get_entry(acl, entryId, &entry);
      entryId = ACL_NEXT_ENTRY;
      if (result == 0)
         break;
      if (result == -1)
      {
         int errorNumber = errno;
         ::acl_free(acl);
         return aclCallError("acl_get_entry", errorNumber, directory);
      }

      acl_tag_t tag;
      if (::acl_get_tag_type(entry, &tag) == -1)
      {
         int errorNumber = errno;
         ::acl_free(acl);
         return aclCallError("acl_get_tag_type", errorNumber, directory);
      }

      mode_t permissions;
      Error error = readAclPermissions(entry, &permissions);
      if (error)
      {
         ::acl_free(acl);
         error.addProperty("path", directory);
         return error;
      }

      if (tag == ACL_USER_OBJ)
      {
         pAccess->owner = permissions;
      }
      else if (tag == ACL_GROUP_OBJ)
      {
         pAccess->group = permissions;
      }
      else if (tag == ACL_OTHER)
      {
         pAccess->other = permissions;
      }
      else if (tag == ACL_MASK)
      {
         pAccess->mask = permissions;
         hasMask = true;
      }
      else if (tag == ACL_USER || tag == ACL_GROUP)
      {
         void* pQualifier = ::acl_get_qualifier(entry);
         if (!pQualifier)
         {
            int errorNumber = errno;
            ::acl_free(acl);
            return aclCallError(
               "acl_get_qualifier",
               errorNumber,
               directory);
         }

         if (tag == ACL_USER)
         {
            uid_t user = *static_cast<uid_t*>(pQualifier);
            pAccess->users[user] = permissions;
         }
         else
         {
            gid_t group = *static_cast<gid_t*>(pQualifier);
            pAccess->groups[group] = permissions;
         }
         ::acl_free(pQualifier);
      }
   }
   ::acl_free(acl);

   if (hasMask)
   {
      pAccess->group &= pAccess->mask;
      for (auto& user : pAccess->users)
         user.second &= pAccess->mask;
      for (auto& group : pAccess->groups)
         group.second &= pAccess->mask;
   }

   return Success();
}

mode_t currentUserPermissions(const PosixAccessControlList& access,
                              const struct stat& parentInfo,
                              const std::vector<gid_t>& processGroups)
{
   uid_t user = ::geteuid();
   if (user == parentInfo.st_uid)
      return access.owner;

   auto namedUser = access.users.find(user);
   if (namedUser != access.users.end())
      return namedUser->second;

   bool matchedGroup = false;
   mode_t permissions = 0;
   if (containsGroup(processGroups, parentInfo.st_gid))
   {
      matchedGroup = true;
      permissions |= access.group;
   }
   for (const auto& namedGroup : access.groups)
   {
      if (containsGroup(processGroups, namedGroup.first))
      {
         matchedGroup = true;
         permissions |= namedGroup.second;
      }
   }

   return matchedGroup ? permissions : access.other;
}

Error addPosixAclEntry(acl_t* pAcl,
                       acl_tag_t tag,
                       const void* pQualifier,
                       mode_t permissions)
{
   acl_entry_t entry;
   if (::acl_create_entry(pAcl, &entry) == -1)
      return systemCallError("acl_create_entry", errno, ERROR_LOCATION);
   if (::acl_set_tag_type(entry, tag) == -1)
      return systemCallError("acl_set_tag_type", errno, ERROR_LOCATION);
   if (pQualifier && ::acl_set_qualifier(entry, pQualifier) == -1)
      return systemCallError("acl_set_qualifier", errno, ERROR_LOCATION);

   acl_permset_t permissionSet;
   if (::acl_get_permset(entry, &permissionSet) == -1)
      return systemCallError("acl_get_permset", errno, ERROR_LOCATION);
   if (::acl_clear_perms(permissionSet) == -1)
      return systemCallError("acl_clear_perms", errno, ERROR_LOCATION);
   if ((permissions & 04) &&
       ::acl_add_perm(permissionSet, ACL_READ) == -1)
   {
      return systemCallError("acl_add_perm", errno, ERROR_LOCATION);
   }
   if ((permissions & 02) &&
       ::acl_add_perm(permissionSet, ACL_WRITE) == -1)
   {
      return systemCallError("acl_add_perm", errno, ERROR_LOCATION);
   }
   if ((permissions & 01) &&
       ::acl_add_perm(permissionSet, ACL_EXECUTE) == -1)
   {
      return systemCallError("acl_add_perm", errno, ERROR_LOCATION);
   }

   return Success();
}

Error applyTranslatedPosixAcl(const FilePath& parent,
                              const struct stat& parentInfo,
                              const struct stat& preparedInfo,
                              int descriptor,
                              bool* pApplied)
{
   *pApplied = false;
   PosixAccessControlList parentAccess;
   Error error = readPosixAccessControlList(parent, &parentAccess);
   if (error && isAclUnsupported(error.getCode()))
      return Success();
   if (error)
      return error;

   std::vector<gid_t> processGroups;
   error = currentGroups(&processGroups);
   if (error)
      return error;

   uid_t childOwner = ::geteuid();
   gid_t childGroup = preparedInfo.st_gid;
   mode_t childOwnerPermissions = currentUserPermissions(
      parentAccess,
      parentInfo,
      processGroups);
   // The successful mkdir proved these permissions at creation time. Keep
   // the creator able to publish and later retire claims if the parent ACL
   // changed while its metadata was being copied.
   childOwnerPermissions |= 03;

   std::map<uid_t, mode_t> namedUsers = parentAccess.users;
   namedUsers.erase(childOwner);
   if (parentInfo.st_uid != childOwner)
      namedUsers[parentInfo.st_uid] |= parentAccess.owner;

   std::map<gid_t, mode_t> namedGroups = parentAccess.groups;
   mode_t childGroupPermissions = 0;
   bool childGroupNamed = false;
   if (childGroup == parentInfo.st_gid)
   {
      childGroupPermissions |= parentAccess.group;
      childGroupNamed = true;
   }
   auto namedChildGroup = namedGroups.find(childGroup);
   if (namedChildGroup != namedGroups.end())
   {
      childGroupPermissions |= namedChildGroup->second;
      childGroupNamed = true;
      namedGroups.erase(namedChildGroup);
   }

   if (!childGroupNamed)
   {
      // A changed owning group cannot exactly preserve POSIX ACL fallback
      // semantics. Use only permissions common to "other" and every parent
      // group entry, so membership in the child's group never widens access.
      childGroupPermissions = parentAccess.other & parentAccess.group;
      for (const auto& namedGroup : parentAccess.groups)
         childGroupPermissions &= namedGroup.second;
   }

   if (parentInfo.st_gid != childGroup)
      namedGroups[parentInfo.st_gid] |= parentAccess.group;

   acl_t childAcl = ::acl_init(
      static_cast<int>(
         4 + namedUsers.size() + namedGroups.size()));
   if (!childAcl)
      return aclCallError("acl_init", errno, parent);

   error = addPosixAclEntry(
      &childAcl,
      ACL_USER_OBJ,
      nullptr,
      childOwnerPermissions);
   for (const auto& namedUser : namedUsers)
   {
      if (error)
         break;
      error = addPosixAclEntry(
         &childAcl,
         ACL_USER,
         &namedUser.first,
         namedUser.second);
   }
   if (!error)
   {
      error = addPosixAclEntry(
         &childAcl,
         ACL_GROUP_OBJ,
         nullptr,
         childGroupPermissions);
   }
   for (const auto& namedGroup : namedGroups)
   {
      if (error)
         break;
      error = addPosixAclEntry(
         &childAcl,
         ACL_GROUP,
         &namedGroup.first,
         namedGroup.second);
   }
   if (!error)
   {
      error = addPosixAclEntry(
         &childAcl,
         ACL_OTHER,
         nullptr,
         parentAccess.other);
   }
   if (!error && (!namedUsers.empty() || !namedGroups.empty()) &&
       ::acl_calc_mask(&childAcl) == -1)
   {
      error = systemCallError("acl_calc_mask", errno, ERROR_LOCATION);
   }
   if (!error && ::acl_valid(childAcl) == -1)
      error = systemCallError("acl_valid", errno, ERROR_LOCATION);
   if (!error && ::acl_set_fd(descriptor, childAcl) == -1)
   {
      int errorNumber = errno;
      if (!isAclUnsupported(errorNumber))
         error = systemCallError("acl_set_fd", errorNumber, ERROR_LOCATION);
   }
   else if (!error)
   {
      *pApplied = true;
   }

   ::acl_free(childAcl);
   if (error)
      error.addProperty("path", parent);
   return error;
}

#endif

#ifdef __APPLE__

Error effectiveDirectoryPermissions(const FilePath& directory,
                                    mode_t* pPermissions)
{
   *pPermissions = 0;
   struct Permission
   {
      int accessMode;
      mode_t mode;
   };
   const Permission permissions[] = {
      {R_OK, 04},
      {W_OK, 02},
      {X_OK, 01}
   };

   for (const Permission& permission : permissions)
   {
      if (::faccessat(
             AT_FDCWD,
             directory.getAbsolutePathNative().c_str(),
             permission.accessMode,
             AT_EACCESS) == 0)
      {
         *pPermissions |= permission.mode;
      }
      else if (!isPermissionError(errno))
      {
         Error error = systemCallError("faccessat", errno, ERROR_LOCATION);
         error.addProperty("path", directory);
         return error;
      }
   }

   return Success();
}

Error addDarwinAclEntry(acl_t* pAcl,
                        const uuid_t identity,
                        mode_t permissions)
{
   if (permissions == 0)
      return Success();

   if (!*pAcl)
   {
      *pAcl = ::acl_init(1);
      if (!*pAcl)
         return systemCallError("acl_init", errno, ERROR_LOCATION);
   }

   acl_entry_t entry;
   if (::acl_create_entry(pAcl, &entry) == -1)
      return systemCallError("acl_create_entry", errno, ERROR_LOCATION);
   if (::acl_set_tag_type(entry, ACL_EXTENDED_ALLOW) == -1)
      return systemCallError("acl_set_tag_type", errno, ERROR_LOCATION);
   if (::acl_set_qualifier(entry, identity) == -1)
      return systemCallError("acl_set_qualifier", errno, ERROR_LOCATION);

   acl_permset_t permissionSet;
   if (::acl_get_permset(entry, &permissionSet) == -1)
      return systemCallError("acl_get_permset", errno, ERROR_LOCATION);
   if (::acl_clear_perms(permissionSet) == -1)
      return systemCallError("acl_clear_perms", errno, ERROR_LOCATION);

   if ((permissions & 04) &&
       ::acl_add_perm(permissionSet, ACL_LIST_DIRECTORY) == -1)
   {
      return systemCallError("acl_add_perm", errno, ERROR_LOCATION);
   }
   if (permissions & 02)
   {
      const acl_perm_t writePermissions[] = {
         ACL_ADD_FILE,
         ACL_ADD_SUBDIRECTORY,
         ACL_DELETE_CHILD
      };
      for (acl_perm_t permission : writePermissions)
      {
         if (::acl_add_perm(permissionSet, permission) == -1)
            return systemCallError("acl_add_perm", errno, ERROR_LOCATION);
      }
   }
   if ((permissions & 01) &&
       ::acl_add_perm(permissionSet, ACL_SEARCH) == -1)
   {
      return systemCallError("acl_add_perm", errno, ERROR_LOCATION);
   }

   acl_flagset_t flagSet;
   if (::acl_get_flagset_np(entry, &flagSet) == -1)
      return systemCallError("acl_get_flagset_np", errno, ERROR_LOCATION);
   if (::acl_clear_flags_np(flagSet) == -1)
      return systemCallError("acl_clear_flags_np", errno, ERROR_LOCATION);
   return Success();
}

Error translateDarwinAcl(const FilePath& parent,
                         const struct stat& parentInfo,
                         const struct stat& preparedInfo,
                         bool groupAligned,
                         acl_t* pAcl)
{
   if (parentInfo.st_uid != preparedInfo.st_uid)
   {
      uuid_t ownerIdentity;
      int status = ::mbr_uid_to_uuid(parentInfo.st_uid, ownerIdentity);
      if (status != 0)
         return systemCallError("mbr_uid_to_uuid", status, ERROR_LOCATION);

      Error error = addDarwinAclEntry(
         pAcl,
         ownerIdentity,
         (parentInfo.st_mode >> 6) & 07);
      if (error)
         return error;
   }

   if (!groupAligned)
   {
      uuid_t groupIdentity;
      int status = ::mbr_gid_to_uuid(parentInfo.st_gid, groupIdentity);
      if (status != 0)
         return systemCallError("mbr_gid_to_uuid", status, ERROR_LOCATION);

      Error error = addDarwinAclEntry(
         pAcl,
         groupIdentity,
         (parentInfo.st_mode >> 3) & 07);
      if (error)
         return error;
   }

   if (*pAcl && ::acl_valid(*pAcl) == -1)
      return systemCallError("acl_valid", errno, ERROR_LOCATION);
   return Success();
}

#endif

Error applyClaimDirectoryPermissions(const FilePath& parent,
                                     const struct stat& parentInfo,
                                     struct stat preparedInfo,
                                     int descriptor)
{
#if defined(__APPLE__)
   acl_t parentAcl = ::acl_get_file(
      parent.getAbsolutePathNative().c_str(),
      ACL_TYPE_EXTENDED);
   if (!parentAcl && errno != ENOENT && !isAclUnsupported(errno))
   {
      Error error = systemCallError("acl_get_file", errno, ERROR_LOCATION);
      error.addProperty("path", parent);
      return error;
   }
#endif

   bool groupAligned = preparedInfo.st_gid == parentInfo.st_gid;
   bool chownDenied = false;
   if (!groupAligned)
   {
#ifdef RSTUDIO_UNIT_TESTS_ENABLED
      chownDenied = s_forceClaimDirectoryChownFailure;
#endif
      if (!chownDenied &&
          ::fchown(
             descriptor,
             static_cast<uid_t>(-1),
             parentInfo.st_gid) == 0)
      {
         preparedInfo.st_gid = parentInfo.st_gid;
         groupAligned = true;
      }
      else if (!chownDenied && isPermissionError(errno))
      {
         chownDenied = true;
      }
      else if (!chownDenied)
      {
         int errorNumber = errno;
#if defined(__APPLE__)
         if (parentAcl)
            ::acl_free(parentAcl);
#endif
         Error error = systemError(errorNumber, ERROR_LOCATION);
         error.addProperty("path", parent);
         return error;
      }
   }

#ifdef __linux__
   bool aclApplied = false;
   Error error = applyTranslatedPosixAcl(
      parent,
      parentInfo,
      preparedInfo,
      descriptor,
      &aclApplied);
   if (error)
      return error;

   mode_t mode;
   if (aclApplied)
   {
      struct stat appliedInfo;
      if (::fstat(descriptor, &appliedInfo) == -1)
      {
         Error statError = systemCallError("fstat", errno, ERROR_LOCATION);
         statError.addProperty("path", parent);
         return statError;
      }
      mode = (appliedInfo.st_mode & 0777) | (parentInfo.st_mode & S_ISGID);
   }
   else
   {
      mode_t ownerPermissions;
      Error modeError = currentModePermissions(
         parentInfo,
         &ownerPermissions);
      if (modeError)
         return modeError;
      ownerPermissions |= 03;
      mode_t groupPermissions = groupAligned
         ? (parentInfo.st_mode >> 3) & 07
         : ((parentInfo.st_mode >> 3) & 07) &
              (parentInfo.st_mode & 07);
      mode = (parentInfo.st_mode & S_ISGID) |
             (ownerPermissions << 6) |
             (groupPermissions << 3) |
             (parentInfo.st_mode & 07);
   }

   if (::fchmod(descriptor, mode) == -1)
   {
      Error chmodError = systemCallError("fchmod", errno, ERROR_LOCATION);
      chmodError.addProperty("path", parent);
      return chmodError;
   }
#elif defined(__APPLE__)
   mode_t ownerPermissions;
   Error error = effectiveDirectoryPermissions(parent, &ownerPermissions);
   if (error)
   {
      if (parentAcl)
         ::acl_free(parentAcl);
      return error;
   }
   // mkdir() already proved write and search access. Preserve those rights
   // if the parent policy changes while its ACL and mode are translated.
   ownerPermissions |= 03;

   error = translateDarwinAcl(
      parent,
      parentInfo,
      preparedInfo,
      groupAligned,
      &parentAcl);
   if (error)
   {
      if (parentAcl)
         ::acl_free(parentAcl);
      error.addProperty("path", parent);
      return error;
   }

   if (parentAcl)
   {
      int status = ::acl_set_fd(descriptor, parentAcl);
      int errorNumber = status == -1 ? errno : 0;
      ::acl_free(parentAcl);
      if (status == -1)
      {
         if (isAclUnsupported(errorNumber))
            status = 0;
      }
      if (status == -1)
      {
         Error error = systemCallError(
            "acl_set_fd",
            errorNumber,
            ERROR_LOCATION);
         error.addProperty("path", parent);
         return error;
      }
   }

   mode_t groupPermissions = groupAligned
      ? (parentInfo.st_mode >> 3) & 07
      : ((parentInfo.st_mode >> 3) & 07) &
           (parentInfo.st_mode & 07);
   mode_t mode = (parentInfo.st_mode & S_ISGID) |
                 (ownerPermissions << 6) |
                 (groupPermissions << 3) |
                 (parentInfo.st_mode & 07);
   if (::fchmod(descriptor, mode) == -1)
   {
      Error chmodError = systemCallError("fchmod", errno, ERROR_LOCATION);
      chmodError.addProperty("path", parent);
      return chmodError;
   }
#else
   if (!groupAligned)
   {
      Error error = systemError(EPERM, ERROR_LOCATION);
      error.addProperty("path", parent);
      return error;
   }
   mode_t mode = parentInfo.st_mode & (S_ISGID | 0777);
   if (::fchmod(descriptor, mode) == -1)
   {
      Error error = systemCallError("fchmod", errno, ERROR_LOCATION);
      error.addProperty("path", parent);
      return error;
   }
#endif

   return Success();
}

Error ensureClaimDirectory(const FilePath& lockFilePath,
                           const FilePath& claimDirectory)
{
#ifndef _WIN32
   struct stat existingInfo;
   if (::lstat(claimDirectory.getAbsolutePathNative().c_str(),
               &existingInfo) == 0)
   {
      return verifyClaimDirectory(claimDirectory);
   }
   else if (errno != ENOENT)
   {
      Error error = systemError(errno, ERROR_LOCATION);
      error.addProperty("path", claimDirectory);
      return error;
   }

   struct stat parentInfo;
   if (::stat(
          lockFilePath.getParent().getAbsolutePathNative().c_str(),
          &parentInfo) == -1)
   {
      Error error = systemError(errno, ERROR_LOCATION);
      error.addProperty("path", lockFilePath.getParent());
      return error;
   }

   FilePath preparedDirectory;
   for (int attempt = 0; attempt < 3; ++attempt)
   {
      preparedDirectory = lockFilePath.getParent().completePath(
         fmt::format("{}-{}-{}",
                     kFileLockClaimTempPrefix,
                     pidString(),
                     system::generateUuid(false)));
      if (::mkdir(preparedDirectory.getAbsolutePathNative().c_str(), 0700) == 0)
         break;
      if (errno != EEXIST)
      {
         Error error = systemError(errno, ERROR_LOCATION);
         error.addProperty("path", preparedDirectory);
         return error;
      }
      preparedDirectory = FilePath();
   }

   if (preparedDirectory.isEmpty())
   {
      Error error = systemError(
         boost::system::errc::file_exists,
         ERROR_LOCATION);
      error.addProperty("path", claimDirectory);
      return error;
   }

   int flags = O_RDONLY | O_CLOEXEC;
#ifdef O_DIRECTORY
   flags |= O_DIRECTORY;
#endif
#ifdef O_NOFOLLOW
   flags |= O_NOFOLLOW;
#endif
   int descriptor = ::open(
      preparedDirectory.getAbsolutePathNative().c_str(),
      flags);
   if (descriptor == -1)
   {
      int errorNumber = errno;
      ::rmdir(preparedDirectory.getAbsolutePathNative().c_str());
      Error error = systemError(errorNumber, ERROR_LOCATION);
      error.addProperty("path", preparedDirectory);
      return error;
   }

   struct stat preparedInfo;
   if (::fstat(descriptor, &preparedInfo) == -1)
   {
      int errorNumber = errno;
      ::close(descriptor);
      ::rmdir(preparedDirectory.getAbsolutePathNative().c_str());
      Error error = systemError(errorNumber, ERROR_LOCATION);
      error.addProperty("path", preparedDirectory);
      return error;
   }
   if (!S_ISDIR(preparedInfo.st_mode))
   {
      ::close(descriptor);
      ::rmdir(preparedDirectory.getAbsolutePathNative().c_str());
      Error error = systemError(
         boost::system::errc::not_a_directory,
         ERROR_LOCATION);
      error.addProperty("path", preparedDirectory);
      return error;
   }

   // The prepared directory stays private until it has the parent's access
   // policy. Sticky is deliberately omitted: peers allowed to publish in the
   // parent must also be able to retire one another's stale claims here.
   Error permissionError = applyClaimDirectoryPermissions(
      lockFilePath.getParent(),
      parentInfo,
      preparedInfo,
      descriptor);
   if (permissionError)
   {
      ::close(descriptor);
      ::rmdir(preparedDirectory.getAbsolutePathNative().c_str());
      permissionError.addProperty("path", preparedDirectory);
      return permissionError;
   }

   int status = ::rename(
      preparedDirectory.getAbsolutePathNative().c_str(),
      claimDirectory.getAbsolutePathNative().c_str());
   int renameError = status == -1 ? errno : 0;
   ::close(descriptor);

   if (status == -1)
   {
      ::rmdir(preparedDirectory.getAbsolutePathNative().c_str());
      if (renameError != EEXIST && renameError != ENOTEMPTY)
      {
         Error error = systemError(renameError, ERROR_LOCATION);
         error.addProperty("path", claimDirectory);
         return error;
      }
   }
   return verifyClaimDirectory(claimDirectory);
#else
   (void)lockFilePath;
   (void)claimDirectory;
   return Success();
#endif
}

// Takes the claim for a public lock path. *pHeld is false when another
// contender holds it (or replaced ours after judging it stale, e.g. after a
// long stall in a load-balanced deployment); the caller must then leave the
// public path alone.
Error acquireClaimAt(const FilePath& lockFilePath,
                     const FilePath& claimFilePath,
                     bool sweepClaims,
                     Claim* pClaim,
                     bool* pHeld)
{
   *pHeld = false;
   Error error;

#ifndef _WIN32
   int claimDirectoryDescriptor = -1;
   for (;;)
   {
      int flags = O_CLOEXEC;
#ifdef O_SEARCH
      flags |= O_SEARCH;
#elif defined(O_PATH)
      flags |= O_PATH;
#else
      flags |= O_RDONLY;
#endif
#ifdef O_DIRECTORY
      flags |= O_DIRECTORY;
#endif
#ifdef O_NOFOLLOW
      flags |= O_NOFOLLOW;
#endif
      claimDirectoryDescriptor = ::open(
         claimFilePath.getParent().getAbsolutePathNative().c_str(),
         flags);
      if (claimDirectoryDescriptor == -1)
      {
         error = systemError(errno, ERROR_LOCATION);
         error.addProperty("path", claimFilePath.getParent());
         return error;
      }

      struct stat openedInfo;
      struct stat namedInfo;
      if (::fstat(claimDirectoryDescriptor, &openedInfo) == -1 ||
          ::lstat(claimFilePath.getParent().getAbsolutePathNative().c_str(),
                  &namedInfo) == -1)
      {
         int errorNumber = errno;
         ::close(claimDirectoryDescriptor);
         error = systemError(errorNumber, ERROR_LOCATION);
         error.addProperty("path", claimFilePath.getParent());
         return error;
      }

      if (!S_ISDIR(openedInfo.st_mode) || !S_ISDIR(namedInfo.st_mode))
      {
         ::close(claimDirectoryDescriptor);
         error = systemError(
            boost::system::errc::not_a_directory,
            ERROR_LOCATION);
         error.addProperty("path", claimFilePath.getParent());
         return error;
      }

      if (openedInfo.st_dev == namedInfo.st_dev &&
          openedInfo.st_ino == namedInfo.st_ino)
      {
         break;
      }

      ::close(claimDirectoryDescriptor);
      claimDirectoryDescriptor = -1;
   }
#else
   int claimDirectoryDescriptor = -1;
#endif

#ifndef _WIN32
   pClaim->directoryDescriptor = claimDirectoryDescriptor;
   error = descriptorIdentity(
      claimDirectoryDescriptor,
      &pClaim->directoryIdentity);
   if (error)
   {
      closeClaim(pClaim);
      return error;
   }
   pClaim->directoryIdentity.identityFollowsSymlink = false;

#ifdef RSTUDIO_UNIT_TESTS_ENABLED
   if (s_beforeClaim)
      s_beforeClaim(claimFilePath);
#endif
#endif

   error = claimLockFile(
      claimFilePath,
      claimDirectoryDescriptor,
      pClaim,
      pHeld);
   if (error || !*pHeld)
   {
      closeClaim(pClaim);
      return error;
   }

#ifndef _WIN32
   error = validateClaim(*pClaim, lockFilePath);
   *pHeld = !error;
   if (error || !*pHeld)
   {
      // Identity-checked, so a contender's replacement is left in place but
      // our own claim does not linger to block every later takeover.
      *pHeld = false;
      releaseClaim(pClaim);
   }

   if (!error && *pHeld && sweepClaims)
   {
      sweepStaleClaims(
         pClaim->directoryDescriptor,
         claimFilePath.getParent(),
         pClaim->identity);
   }
#endif
   return error;
}

Error acquireClaim(const FilePath& lockFilePath, Claim* pClaim, bool* pHeld)
{
   FilePath claimFilePath = claimPathForLock(lockFilePath);
#ifndef _WIN32
   bool retiredInaccessibleDirectory = false;
#endif
   for (int attempt = 0; attempt < 16; ++attempt)
   {
      Error error = ensureClaimDirectory(
         lockFilePath,
         claimFilePath.getParent());
      if (error)
      {
         if (isClaimDirectoryRetryError(error))
            continue;
         return error;
      }

      error = acquireClaimAt(
         lockFilePath,
         claimFilePath,
         true,
         pClaim,
         pHeld);
#ifndef _WIN32
      if (error &&
          isPermissionError(error.getCode()) &&
          !retiredInaccessibleDirectory)
      {
         // The parent policy may have authorized this contender after an
         // older user created the namespace. If no acquisition is active,
         // retire that empty namespace and rebuild it from the current ACL.
         int status = ::rmdir(
            claimFilePath.getParent().getAbsolutePathNative().c_str());
         if (status == 0 || errno == ENOENT)
         {
            retiredInaccessibleDirectory = true;
            closeClaim(pClaim);
            continue;
         }
      }
#endif
      if (!error || !isClaimDirectoryRetryError(error))
         return error;

      closeClaim(pClaim);
   }

   Error error = systemError(boost::system::errc::resource_unavailable_try_again,
                             ERROR_LOCATION);
   error.addProperty("path", claimFilePath.getParent());
   return error;
}

Error acquireLegacyClaim(const FilePath& lockFilePath,
                         Claim* pClaim,
                         bool* pHeld)
{
   return acquireClaimAt(
      lockFilePath,
      legacyClaimPathForLock(lockFilePath),
      false,
      pClaim,
      pHeld);
}

Error validateClaims(const Claim& legacyClaim,
                     const Claim& claim,
                     const FilePath& lockFilePath)
{
   Error error = validateClaim(legacyClaim, lockFilePath);
   if (error)
      return error;
   return validateClaim(claim, lockFilePath);
}

Error removeLockFile(const FilePath& lockFilePath,
                     const LockMetadata& expectedMetadata)
{
   // The caller holds the claim through both this removal and publication.
   // An inspection is tied to the inode it read. If another contender has
   // already replaced that inode, the replacement is left untouched.
   RemoveResult result;
   Error error = removeIfSameIdentity(lockFilePath, expectedMetadata, &result);
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
         O_RDWR | O_CREAT | O_EXCL | O_CLOEXEC,
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

// Release through the descriptor, never through the reusable public name.
// A claim can expire during any filesystem call or scheduler stall, so it
// cannot make a pathname check followed by unlink safe against a successor.
// The released public entry is reclaimed by the next acquisition. Only the
// private UUID owner name can be removed here: no acquisition reuses it.
Error releaseLockFiles(const FilePath& ownerFilePath,
                       int descriptor)
{
#ifdef RSTUDIO_UNIT_TESTS_ENABLED
   if (s_beforeRelease)
      s_beforeRelease();
#endif

   Error error = writeLockContents(descriptor, true);
   if (error)
      return error;

   if (!ownerFilePath.isEmpty())
      unlinkBestEffort(ownerFilePath);
   return Success();
}

Error writeLockFile(const FilePath& lockFilePath,
                    const Claim& legacyClaim,
                    const Claim& claim,
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

   // Preparing the private inode may have stalled beyond the claim's lease.
   // Check again immediately before publishing through the public name.
   error = validateClaims(legacyClaim, claim, lockFilePath);
   if (error)
   {
      ::close(proxyDescriptor);
      unlinkBestEffort(proxyPath);
      return error;
   }

   int status;
#ifdef RSTUDIO_UNIT_TESTS_ENABLED
   if (s_forceFallback)
   {
      status = -1;
      errno = EOPNOTSUPP;
   }
   else
#endif
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

   // Some NFS implementations report failure (EEXIST on a retransmit, EIO on
   // a lost reply) after creating the link. What the public path now refers
   // to is authoritative: if it is our owner file, the lock is ours, and
   // giving up would leave a live-looking lock nobody refreshes.
   if (!linked)
      linked = lockFilePath.isEquivalentTo(proxyPath);

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
   // partially written fallback file is treated as held until its timeout.
   // A write can outlast that timeout, so validate ownership afterwards and
   // never clean up a failed publication through the reusable public name.
   LOG((FileLock::useSymlinks() ? "symlink" : "link")
       << "() failed (errno " << linkError << "); falling back to O_EXCL: "
       << lockFilePath.getAbsolutePath());
   error = validateClaims(legacyClaim, claim, lockFilePath);
   if (error)
   {
      ::close(proxyDescriptor);
      unlinkBestEffort(proxyPath);
      return error;
   }

   int descriptor = ::open(
      lockFilePath.getAbsolutePathNative().c_str(),
      O_RDWR | O_CREAT | O_EXCL | O_CLOEXEC,
      kLockFileMode);
   if (descriptor == -1)
   {
      int errorNumber = errno;

      // A misreported link() can also surface here, as EEXIST on our own
      // owner file; check before giving the proxy up.
      if (errorNumber == EEXIST && lockFilePath.isEquivalentTo(proxyPath))
      {
         *pToken = token;
         *pOwnerFilePath = proxyPath;
         *pDescriptor = proxyDescriptor;
         return Success();
      }

      ::close(proxyDescriptor);
      unlinkBestEffort(proxyPath);

      Error openError = systemError(errorNumber, ERROR_LOCATION);
      openError.addProperty("lock-file", lockFilePath);
      return openError;
   }

   error = writeLockContents(descriptor, false);
   ::close(proxyDescriptor);
   unlinkBestEffort(proxyPath);

   if (!error)
   {
      LockMetadata identity;
      error = descriptorIdentity(descriptor, &identity);
      if (!error)
      {
         bool matches = false;
         error = hasExpectedIdentity(lockFilePath, identity, &matches);
         if (!error && !matches)
            error = noLockAvailableError(lockFilePath);
      }
   }

   if (error)
   {
      // As with a failed claim write, an incomplete public entry is left
      // for timeout cleanup. Even an identity check followed by unlink can
      // delete a successor if the filesystem call stalls.
      ::close(descriptor);
      return error;
   }

   *pToken = token;
   *pOwnerFilePath = FilePath();
   *pDescriptor = descriptor;
   return Success();
}

#else

Error writeLockFile(const FilePath& lockFilePath,
                    const Claim&,
                    const Claim&,
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
   return fmt::format("{}/{}",
                      lockFilePath.getParent().getCanonicalPath(),
                      lockFilePath.getFilename());
}

struct RegisteredLock
{
   std::string token;
   int descriptor;
   FilePath lockFilePath;
   FilePath ownerFilePath;
   std::shared_ptr<LockState> state;
};

#ifndef _WIN32
Error releaseRegisteredLock(const FilePath& ownerFilePath,
                            int descriptor,
                            const std::shared_ptr<LockState>& state)
{
   std::lock_guard<std::mutex> guard(state->mutex);
   if (state->released)
      return Success();

   state->released = true;
   return releaseLockFiles(ownerFilePath, descriptor);
}
#endif

class LockRegistration : public file_lock::ForkAwareRegistry
{
public:
   Error registerLock(const std::string& key,
                      const std::string& token,
                      int descriptor,
                      const FilePath& lockFilePath,
                      const FilePath& ownerFilePath,
                      const std::shared_ptr<LockState>& state)
   {
      Guard guard(*this);
#ifndef _WIN32
      int registeredDescriptor = ::fcntl(descriptor, F_DUPFD_CLOEXEC, 0);
      if (registeredDescriptor == -1)
      {
         return systemCallError("fcntl", errno, ERROR_LOCATION);
      }

      auto existing = registration_.find(key);
      if (existing != registration_.end())
         ::close(existing->second.descriptor);
      registration_[key] = RegisteredLock{
         token,
         registeredDescriptor,
         lockFilePath,
         ownerFilePath,
         state};
#endif
      return Success();
   }

   // Returns whether the lock was still registered under this token. A caller
   // whose lock was already released by clearLocks() (at cleanUp) sees false
   // and must not repeat the filesystem release.
   bool deregisterLock(const std::string& key, const std::string& token)
   {
      Guard guard(*this);
      auto it = registration_.find(key);
      if (it == registration_.end() || it->second.token != token)
         return false;

#ifndef _WIN32
      ::close(it->second.descriptor);
#endif
      registration_.erase(it);
      return true;
   }

   void refreshLocks()
   {
#ifndef _WIN32
      // The mutex is held across fork(), so filesystem I/O (which can stall
      // on a network mount) must not run under it. Duplicates stay valid if
      // a lock is released meanwhile.
      std::vector<RegisteredLock> locks;
      {
         Guard guard(*this);
         for (const auto& entry : registration_)
         {
            int descriptor = ::fcntl(entry.second.descriptor, F_DUPFD_CLOEXEC, 0);
            if (descriptor == -1)
               LOG_ERROR(systemCallError("fcntl", errno, ERROR_LOCATION));
            else
            {
               locks.push_back(entry.second);
               locks.back().descriptor = descriptor;
            }
         }
      }

#ifdef RSTUDIO_UNIT_TESTS_ENABLED
      if (s_beforeRefresh)
         s_beforeRefresh();
#endif

      for (const RegisteredLock& lock : locks)
      {
         std::lock_guard<std::mutex> guard(lock.state->mutex);
         if (lock.state->released)
         {
            ::close(lock.descriptor);
            continue;
         }

         LOG("Bumping write time for lock token: " << lock.token);

         Error error = stampWriteTime(lock.descriptor);
         if (error)
         {
            // The mount refuses explicit timestamps. Unlike acquisition, a
            // refresh has no write of its own to set the mtime, so without a
            // fallback the lock would never be bumped and would age out under
            // a live owner. Rewriting the (unchanged) contents lets the
            // write's own mtime stand in, as it does at acquisition.
            Error rewriteError = writeLockContents(lock.descriptor, false);
            if (rewriteError)
               LOG_ERROR(rewriteError);
         }

         ::close(lock.descriptor);
      }
#endif
   }

   void clearLocks()
   {
      // as in refreshLocks(), no filesystem I/O under the mutex
      std::map<std::string, RegisteredLock> registration;
      {
         Guard guard(*this);
         registration.swap(registration_);
      }

#ifndef _WIN32
      for (const auto& entry : registration)
      {
         LOG("Clearing lock: " << entry.second.lockFilePath.getAbsolutePath());
         Error error = releaseRegisteredLock(
            entry.second.ownerFilePath,
            entry.second.descriptor,
            entry.second.state);
         if (error)
            LOG_ERROR(error);
         ::close(entry.second.descriptor);
      }
#endif
   }

private:
   void resetInChild() override
   {
      // the parent keeps its leases; the child merely drops its copies
#ifndef _WIN32
      for (const auto& entry : registration_)
         ::close(entry.second.descriptor);
#endif
      registration_.clear();
   }

   std::map<std::string, RegisteredLock> registration_;
};

LockRegistration& lockRegistration()
{
   // Lock objects can be destroyed during static shutdown. Keep the registry
   // alive for the lifetime of the process so those destructors remain safe.
   static LockRegistration* pInstance = nullptr;
   return file_lock::ForkAwareRegistry::instance(pInstance);
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
   std::shared_ptr<LockState> state;
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

#ifdef RSTUDIO_UNIT_TESTS_ENABLED
FilePath LinkBasedFileLock::claimPathForTesting(const FilePath& lockFilePath)
{
   return claimPathForLock(lockFilePath);
}

FilePath LinkBasedFileLock::legacyClaimPathForTesting(
   const FilePath& lockFilePath)
{
   return legacyClaimPathForLock(lockFilePath);
}

void LinkBasedFileLock::setBeforeReleaseForTesting(const boost::function<void()>& callback)
{
   s_beforeRelease = callback;
}

void LinkBasedFileLock::setBeforeRefreshForTesting(const boost::function<void()>& callback)
{
   s_beforeRefresh = callback;
}

void LinkBasedFileLock::setBeforeWriteForTesting(const boost::function<Error(int)>& callback)
{
   s_beforeWrite = callback;
}

void LinkBasedFileLock::setBeforeClaimForTesting(
   const boost::function<void(const FilePath&)>& callback)
{
   s_beforeClaim = callback;
}

void LinkBasedFileLock::setAfterRenameForTesting(
   const boost::function<void(const FilePath&)>& callback)
{
   s_afterRename = callback;
}

void LinkBasedFileLock::setForceClaimDirectoryChownFailureForTesting(
   bool forceFailure)
{
   s_forceClaimDirectoryChownFailure = forceFailure;
}

void LinkBasedFileLock::setForceFallbackForTesting(bool forceFallback)
{
   s_forceFallback = forceFallback;
}
#endif

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

   if (lockFilePath.getFilename() == kFileLockClaimDirectory)
   {
      Error error = systemError(
         boost::system::errc::invalid_argument,
         ERROR_LOCATION);
      error.addProperty("lock-file", lockFilePath);
      return error;
   }

   Error error = lockFilePath.getParent().ensureDirectory();
   if (error)
      return error;

   LockInspection inspection;
   error = inspectLockFile(lockFilePath, &inspection);
   if (error)
      return error;

   if (inspection.exists && !inspection.stale)
      return noLockAvailableError(lockFilePath);

   // Take the legacy hashed claim first so deployed versions serialize with
   // this acquisition during a rolling upgrade. The exact-name claim then
   // adds the filesystem's case and Unicode alias semantics. Keep both until
   // publication has been validated; RAII covers every failure.
   Claim legacyClaim;
   bool legacyHeld = false;
   error = acquireLegacyClaim(lockFilePath, &legacyClaim, &legacyHeld);
   if (error)
      return error;
   if (!legacyHeld)
      return noLockAvailableError(lockFilePath);

   Claim claim;
   bool held = false;
   error = acquireClaim(lockFilePath, &claim, &held);
   if (error)
      return error;
   if (!held)
      return noLockAvailableError(lockFilePath);

   inspection = LockInspection();
   error = inspectLockFile(lockFilePath, &inspection);
   if (error)
      return error;
   if (inspection.exists && !inspection.stale)
      return noLockAvailableError(lockFilePath);

   error = validateClaims(legacyClaim, claim, lockFilePath);
   if (error)
      return error;

   if (inspection.exists)
   {
      LOG("Removing stale lockfile: " << lockFilePath.getAbsolutePath());
      error = removeLockFile(lockFilePath, inspection.metadata);
      if (error)
      {
         LOG("Failed to remove stale lockfile: " << lockFilePath.getAbsolutePath() << " (" << error.getSummary() << ")");
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
      legacyClaim,
      claim,
      &token,
      &ownerFilePath,
      &descriptor);
   if (error)
   {
      LOG("Failed to acquire lock: " << lockFilePath.getAbsolutePath() << " (" << error.getSummary() << ")");
      if (error == systemError(
                     boost::system::errc::file_exists,
                     ErrorLocation()))
      {
         return noLockAvailableError(lockFilePath);
      }

      error.addProperty("lock-file", lockFilePath);
      return error;
   }

   // A stalled publication can finish after another contender has reclaimed
   // its claim. Retire only the inode we hold open in that case; the reusable
   // public name may already belong to the successor.
   error = validateClaims(legacyClaim, claim, lockFilePath);
   if (error)
   {
#ifndef _WIN32
      Error releaseError = releaseLockFiles(ownerFilePath, descriptor);
      if (releaseError)
         LOG_ERROR(releaseError);
      ::close(descriptor);
#endif
      return error;
   }

   std::string key = registrationKey(lockFilePath);
   std::shared_ptr<LockState> state(new LockState());
   error = lockRegistration().registerLock(
      key,
      token,
      descriptor,
      lockFilePath,
      ownerFilePath,
      state);
   if (error)
   {
#ifndef _WIN32
      Error releaseError = releaseLockFiles(
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
   pImpl_->state = state;

   releaseClaim(&claim);
   LOG("Acquired lock: " << lockFilePath.getAbsolutePath());
   sweepStaleArtifacts(lockFilePath.getParent(), ownerFilePath);
   return Success();
}

Error LinkBasedFileLock::release()
{
   // Nothing to release; callers that tolerate a failed acquire (to support
   // filesystems without working locks) still release unconditionally.
   if (pImpl_->descriptor == -1)
      return Success();

   // Stop new refresh snapshots before releasing. Snapshots already copied
   // out of the registry share state with this owner, so they cannot rewrite
   // its released marker if their timestamp update needs the write fallback.
   bool wasRegistered = lockRegistration().deregisterLock(
      pImpl_->registrationKey,
      pImpl_->token);

   Error error;
#ifndef _WIN32
   if (wasRegistered && pImpl_->processId == system::currentProcessId())
   {
      error = releaseRegisteredLock(
         pImpl_->ownerFilePath,
         pImpl_->descriptor,
         pImpl_->state);
   }
   ::close(pImpl_->descriptor);
#endif

   LOG("Released lock: " << pImpl_->lockFilePath.getAbsolutePath());

   pImpl_->lockFilePath = FilePath();
   pImpl_->ownerFilePath = FilePath();
   pImpl_->registrationKey.clear();
   pImpl_->token.clear();
   pImpl_->descriptor = -1;
   pImpl_->processId = 0;
   pImpl_->state.reset();
   return error;
}

void LinkBasedFileLock::refresh()
{
   lockRegistration().refreshLocks();
}

void LinkBasedFileLock::cleanUp()
{
   // Release the registered inodes; their public names may already belong
   // to successor owners and are left for acquisition to reclaim.
   lockRegistration().clearLocks();
}

} // namespace core
} // namespace rstudio

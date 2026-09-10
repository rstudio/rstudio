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
const char * const kFileLockTempPrefix = ".rstudio-lock-tmp-41c29";
const char * const kReleasedProcessId = "-1";

// Lock files must stay readable by other users: in a shared project, a
// collaborator's session inspects (and eventually expires) locks it did not
// create. The process umask still applies.
const int kLockFileMode = 0644;

#ifdef RSTUDIO_UNIT_TESTS_ENABLED
boost::function<void()> s_beforeRelease;
boost::function<void()> s_beforeRefresh;
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
   // Every contender for a public lock path must use the same claim name.
   // Hash the lock filename to keep it below NAME_MAX even when the caller's
   // lock filename is already near that limit.
   return lockFilePath.getParent().completePath(
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

boost::optional<PidType> tempFileContender(const FilePath& filePath)
{
   std::string prefix = fmt::format("{}-", kFileLockTempPrefix);
   std::string rest = filePath.getFilename().substr(prefix.size());
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
   // The negative release sentinel is also treated as stale by those versions.
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
Error stampWriteTime(int descriptor)
{
   struct timeval now;
   if (::gettimeofday(&now, nullptr) == -1)
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
// lock there.
void stampWriteTimeBestEffort(int descriptor)
{
   Error error = stampWriteTime(descriptor);

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

   stampWriteTimeBestEffort(descriptor);
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

   Error error = descriptorIdentity(descriptor, pMetadata, pLastWriteTime);
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

   error = lockFilePath.getLastWriteTime(*pLastWriteTime);
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

   if (pInspection->metadata.released)
   {
      pInspection->stale = true;
      return findOwnerFile(lockFilePath, &pInspection->metadata);
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
         // The owner is gone; no need to wait for the lock to age out.
         pInspection->stale = true;
      }
      else if (pInspection->stale)
      {
         // Aged out, but a live local owner remains authoritative for a
         // while longer (see FileLock::getLiveOwnerGraceMultiplier), so a
         // sleeping or temporarily stalled process does not lose its lock
         // and later interfere with the replacement owner.
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

   return pInspection->stale
      ? findOwnerFile(lockFilePath, &pInspection->metadata)
      : Success();
}

#ifndef _WIN32

// A renamed-aside entry is abandoned once the contender that renamed it is
// gone. Its own timestamps say nothing (rename keeps the old owner's mtime),
// so the rename's ctime stands in: no removal stays in flight for a whole
// timeout. On a single host a gone contender is proof at once; but a PID that
// is live and unrelated (a container sharing the directory, a reused number)
// or a name we cannot parse must not pin the entry forever, so those fall
// through to the same ctime check the load-balanced path always uses.
bool isTempFileAbandoned(const FilePath& filePath)
{
   if (!FileLock::isLoadBalanced())
   {
      boost::optional<PidType> contender = tempFileContender(filePath);
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

#endif

// Removes stale owner, claim and temp files left in a lock directory: owners
// orphaned when something other than the lock deleted the public path (a
// crash, or a caller cleaning up), claims abandoned mid-takeover, and
// entries renamed aside by a contender that died mid-removal. Live entries
// are never touched; each candidate is inspected like a lock.
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

#ifndef _WIN32
      if (isTempFile(child))
      {
         if (isTempFileAbandoned(child))
         {
            LOG("Removing abandoned lock temp file: " << child.getAbsolutePath());
            unlinkBestEffort(child);
         }
         continue;
      }

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

// Takes the claim for a public lock path. *pHeld is false when another
// contender holds it (or replaced ours after judging it stale, e.g. after a
// long stall in a load-balanced deployment); the caller must then leave the
// public path alone.
Error acquireClaim(const FilePath& lockFilePath, Claim* pClaim, bool* pHeld)
{
   *pHeld = false;
   Error error = claimLockFile(claimPathForLock(lockFilePath), pClaim, pHeld);
   if (error || !*pHeld)
      return error;

#ifndef _WIN32
   error = hasExpectedIdentity(pClaim->path, pClaim->identity, pHeld);
   if (error || !*pHeld)
   {
      // Identity-checked, so a contender's replacement is left in place but
      // our own claim does not linger to block every later takeover.
      *pHeld = false;
      releaseClaim(pClaim);
   }
#endif
   return error;
}

Error removeLockFile(const FilePath& lockFilePath,
                     const LockMetadata& expectedMetadata)
{
   Claim claim;
   bool held = false;
   Error error = acquireClaim(lockFilePath, &claim, &held);
   if (error)
      return error;
   if (!held)
      return noLockAvailableError(lockFilePath);

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
   // partially written fallback file is treated as held until its timeout, so
   // publication before this write cannot let another contender take over.
   LOG((FileLock::useSymlinks() ? "symlink" : "link")
       << "() failed (errno " << linkError << "); falling back to O_EXCL: "
       << lockFilePath.getAbsolutePath());
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
   static LockRegistration* pInstance =
      file_lock::ForkAwareRegistry::publish(new LockRegistration());
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

void LinkBasedFileLock::setBeforeReleaseForTesting(const boost::function<void()>& callback)
{
   s_beforeRelease = callback;
}

void LinkBasedFileLock::setBeforeRefreshForTesting(const boost::function<void()>& callback)
{
   s_beforeRefresh = callback;
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
      {
         LOG("No lock available: " << lockFilePath.getAbsolutePath());
         return noLockAvailableError(lockFilePath);
      }

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

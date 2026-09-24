/*
 * FileSerializer.cpp
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

#include <core/FileSerializer.hpp>

#include <utility>
#include <iostream>
#include <set>
#include <sstream>
#include <algorithm>
#include <gsl/gsl-lite.hpp>

#include <boost/algorithm/string/trim.hpp>
#include <boost/algorithm/string/predicate.hpp>
#include <boost/iostreams/copy.hpp>
#include <boost/thread.hpp>

#ifdef _WIN32
# include <cstddef>
# include <cstring>
# include <vector>
# include <windows.h>
#else
# include <cerrno>
# include <fcntl.h>
# include <sys/stat.h>
# include <unistd.h>
#endif

#include <shared_core/FilePath.hpp>
#include <shared_core/Memory.hpp>
#include <core/DateTime.hpp>
#include <core/Log.hpp>
#include <core/StringUtils.hpp>
#include <core/Thread.hpp>

namespace rstudio {
namespace core {

namespace {

bool isFileLockedError(const Error& error)
{
   // exclusive file access is only present on Windows
#ifndef _WIN32
   return false;
#else
   return (error && error.getCode() == ERROR_SHARING_VIOLATION);
#endif
}

// Build an Error describing a failed file operation, annotated with the path.
// The code is captured by the caller immediately after the failing syscall so
// that it is not clobbered by any intervening cleanup (close, etc.).
Error fileError(int code, const FilePath& filePath, const ErrorLocation& location)
{
   Error error = systemError(code, location);
   error.addProperty("path", filePath.getAbsolutePath());
   return error;
}

// Write the given contents to filePath.
//
// We deliberately work directly against the operating system's file APIs rather
// than a std::ostream. A stream defers the actual write until its buffer is
// flushed, and its destructor (which must not throw) swallows any error from
// that final flush -- which is how a save onto a full disk could appear to
// succeed. Going through the OS directly lets us check the write and the close
// for errors instead.
//
// When durable is true we additionally flush all the way to physical storage
// (fsync / FlushFileBuffers) before returning. This is required to reliably
// surface a full disk (ENOSPC) or an exceeded quota (EDQUOT): with delayed
// allocation (ext4, btrfs, XFS) a small write into the page cache succeeds even
// on a full disk, and the failure only becomes visible when those pages are
// flushed. It is also relatively expensive, so callers that do not need their
// write to survive a crash (the great majority) leave it off.
#ifdef _WIN32

// Write all of contents to hFile, looping in case WriteFile performs a partial
// write. The handle is left open.
Error writeAll(HANDLE hFile, const std::string& contents, const FilePath& filePath)
{
   const char* data = contents.data();
   std::size_t remaining = contents.size();
   while (remaining > 0)
   {
      // WriteFile takes a DWORD count; cap each call well within its range
      const std::size_t kMaxWrite = 0x7fffffff;
      DWORD toWrite = (remaining > kMaxWrite) ? static_cast<DWORD>(kMaxWrite)
                                              : static_cast<DWORD>(remaining);

      DWORD written = 0;
      if (!::WriteFile(hFile, data, toWrite, &written, nullptr))
      {
         Error error = LAST_SYSTEM_ERROR();
         error.addProperty("path", filePath.getAbsolutePath());
         return error;
      }

      data += written;
      remaining -= written;
   }

   return Success();
}

Error writeContentsToFile(const FilePath& filePath,
                          const std::string& contents,
                          bool truncate,
                          bool durable)
{
   // request GENERIC_WRITE in both modes: FlushFileBuffers requires it, so an
   // append handle opened with only FILE_APPEND_DATA would fail at flush time
   HANDLE hFile = ::CreateFileW(
      filePath.getAbsolutePathW().c_str(),
      GENERIC_WRITE,
      0, // exclusive access (matches FilePath::openForWrite)
      nullptr,
      truncate ? CREATE_ALWAYS : OPEN_ALWAYS,
      FILE_ATTRIBUTE_NORMAL,
      nullptr);

   if (hFile == INVALID_HANDLE_VALUE)
   {
      Error error = LAST_SYSTEM_ERROR();
      error.addProperty("path", filePath.getAbsolutePath());
      return error;
   }

   // in append mode, move to the end of the file before writing -- a GENERIC_WRITE
   // handle does not auto-append the way a FILE_APPEND_DATA-only handle would
   if (!truncate)
   {
      LARGE_INTEGER offset;
      offset.QuadPart = 0;
      if (!::SetFilePointerEx(hFile, offset, nullptr, FILE_END))
      {
         Error error = LAST_SYSTEM_ERROR();
         (void) ::CloseHandle(hFile);
         error.addProperty("path", filePath.getAbsolutePath());
         return error;
      }
   }

   Error error = writeAll(hFile, contents, filePath);
   if (error)
   {
      (void) ::CloseHandle(hFile);
      return error;
   }

   // when durability is requested, flush to physical storage; this is the point
   // at which a full disk or an exceeded quota is reliably reported
   if (durable && !::FlushFileBuffers(hFile))
   {
      Error error = LAST_SYSTEM_ERROR();
      (void) ::CloseHandle(hFile);
      error.addProperty("path", filePath.getAbsolutePath());
      return error;
   }

   if (!::CloseHandle(hFile))
   {
      Error error = LAST_SYSTEM_ERROR();
      error.addProperty("path", filePath.getAbsolutePath());
      return error;
   }

   return Success();
}

#else

// Write all of contents to fd, handling partial writes and EINTR. The
// descriptor is left open.
Error writeAll(int fd, const std::string& contents, const FilePath& filePath)
{
   const char* data = contents.data();
   std::size_t remaining = contents.size();
   while (remaining > 0)
   {
      ssize_t written = ::write(fd, data, remaining);
      if (written < 0)
      {
         if (errno == EINTR)
            continue;

         return fileError(errno, filePath, ERROR_LOCATION);
      }

      data += written;
      remaining -= static_cast<std::size_t>(written);
   }

   return Success();
}

// Flush fd to physical storage. On macOS plain fsync() does not push data to
// the physical platter, so we prefer F_FULLFSYNC and fall back to fsync() when
// the filesystem does not support it.
Error syncFile(int fd, const FilePath& filePath)
{
   int rc = -1;
#ifdef __APPLE__
   do
   {
      rc = ::fcntl(fd, F_FULLFSYNC, 0);
   }
   while (rc == -1 && errno == EINTR);

   if (rc == -1 && (errno == ENOTSUP || errno == ENOTTY || errno == EINVAL))
   {
      do
      {
         rc = ::fsync(fd);
      }
      while (rc == -1 && errno == EINTR);
   }
#else
   do
   {
      rc = ::fsync(fd);
   }
   while (rc == -1 && errno == EINTR);
#endif

   if (rc == -1)
      return fileError(errno, filePath, ERROR_LOCATION);

   return Success();
}

// A close() that returns EINTR has still closed the descriptor on Linux, so we
// must not retry it (that could close an unrelated fd that was opened in the
// meantime).
Error closeFile(int fd, const FilePath& filePath)
{
   if (::close(fd) == -1 && errno != EINTR)
      return fileError(errno, filePath, ERROR_LOCATION);

   return Success();
}

Error writeContentsToFile(const FilePath& filePath,
                          const std::string& contents,
                          bool truncate,
                          bool durable)
{
   // open the file, retrying only on EINTR (there are no sharing violations on
   // POSIX, so the higher-level retry loop runs this exactly once)
   int flags = O_WRONLY | O_CREAT | (truncate ? O_TRUNC : O_APPEND);
   int fd = -1;
   do
   {
      fd = ::open(filePath.getAbsolutePath().c_str(), flags, 0666);
   }
   while (fd == -1 && errno == EINTR);

   if (fd == -1)
      return fileError(errno, filePath, ERROR_LOCATION);

   // when durability is requested, flush to physical storage; this is the point
   // at which a full disk or an exceeded quota is reliably reported
   Error error = writeAll(fd, contents, filePath);
   if (!error && durable)
      error = syncFile(fd, filePath);

   if (error)
   {
      (void) ::close(fd);
      return error;
   }

   return closeFile(fd, filePath);
}

#endif

// Write the given contents to filePath. On Windows, openForWrite requests
// exclusive access, so a file held open by another process (commonly backup
// software) fails with a sharing violation; maxOpenRetrySeconds asks us to keep
// retrying for that long before giving up. A sharing violation occurs before
// any data is written, so retrying the whole operation is safe.
Error writeContentsToFileWithRetry(const FilePath& filePath,
                                   const std::string& contents,
                                   bool truncate,
                                   int maxOpenRetrySeconds,
                                   bool durable)
{
   using namespace boost::posix_time;

   // do not allow negative values - regular signed int was chosen here for
   // easier integration with other parts of the codebase
   if (maxOpenRetrySeconds < 0)
      maxOpenRetrySeconds = 0;

   ptime startTime = second_clock::universal_time();
   int numTries = 0;

   while (true)
   {
      Error error = writeContentsToFile(filePath, contents, truncate, durable);

      // success and all non-sharing-violation errors are returned immediately
      if (!isFileLockedError(error))
         return error;

      error.addOrUpdateProperty("open-attempts", ++numTries);

      // stop retrying if we've spent more than the requested amount of time
      if ((second_clock::universal_time() - startTime) >= seconds(maxOpenRetrySeconds))
      {
         error.addProperty("description", "Timed out while attempting to reopen the file");
         return error;
      }

      // wait a moment before retrying
      boost::this_thread::sleep(milliseconds(500));
   }
}

#ifndef _WIN32
// Flush a directory so that a newly created or renamed entry within it survives
// a crash. This is a POSIX-only durability optimization (Windows has no portable
// equivalent) and is best-effort: the rename has already completed and is
// visible, so a failure here only affects crash durability, not correctness.
void syncDirectory(const FilePath& dirPath)
{
   int flags = O_RDONLY;
#ifdef O_DIRECTORY
   flags |= O_DIRECTORY;
#endif

   int fd = -1;
   do
   {
      fd = ::open(dirPath.getAbsolutePath().c_str(), flags);
   }
   while (fd == -1 && errno == EINTR);

   if (fd == -1)
      return;

   int rc;
   do
   {
      rc = ::fsync(fd);
   }
   while (rc == -1 && errno == EINTR);

   (void) ::close(fd);
}
#endif

// A dot prefix keeps these out of the Files pane and out of watchers that
// filter hidden files.
const char* const kAtomicWriteTempPrefix = ".rstudio-tmp-";

bool isPermissionError(const Error& error)
{
   if (!error || error.getName() != boost::system::system_category().name())
      return false;

#ifdef _WIN32
   return error.getCode() == ERROR_ACCESS_DENIED;
#else
   return error.getCode() == EACCES || error.getCode() == EPERM;
#endif
}

// Follow filePath through any symlinks to the file they point to (which need
// not exist yet), so that we replace that file rather than the link.
Error resolveSymlinks(const FilePath& filePath, FilePath* pResolved)
{
   // the same limit as Linux's MAXSYMLINKS
   const int kMaxSymlinks = 40;

   FilePath resolved = filePath;
   for (int i = 0; resolved.isSymlink(); i++)
   {
      if (i == kMaxSymlinks)
      {
         Error error = systemError(boost::system::errc::too_many_symbolic_link_levels, ERROR_LOCATION);
         error.addProperty("path", filePath.getAbsolutePath());
         return error;
      }

      std::string target;
      Error error = resolved.readSymlink(target);
      if (error)
         return error;

      // a relative target is relative to the directory holding the link
      resolved = resolved.getParent().completePath(target);
   }

   *pResolved = resolved;
   return Success();
}

Error atomicWriteTempPath(const FilePath& targetPath, FilePath* pTempPath)
{
   FilePath uniquePath;
   Error error = FilePath::uniqueFilePath(targetPath.getParent().getAbsolutePath(), uniquePath);
   if (error)
      return error;

   *pTempPath = targetPath.getParent().completePath(kAtomicWriteTempPrefix + uniquePath.getFilename());
   return Success();
}

Error writeInPlace(const FilePath& targetPath,
                   const std::string& contents,
                   const AtomicWriteOptions& options)
{
#ifndef _WIN32
   // restrict the file before the new contents land in it, rather than
   // writing private contents into a file we can't make private; the file
   // must already exist for an in-place write to succeed here
   if (options.ownerOnly && ::chmod(targetPath.getAbsolutePath().c_str(), 0600) == -1)
      return fileError(errno, targetPath, ERROR_LOCATION);
#endif

   return writeContentsToFileWithRetry(targetPath,
                                       contents,
                                       true /* truncate */,
                                       options.maxRetrySeconds,
                                       options.durable);
}

// The error for a failed step of an atomic write. It names the file being
// written, which is what the caller and the user know about; the temporary
// file is recorded separately.
Error atomicWriteError(int code,
                       const FilePath& targetPath,
                       const FilePath& tempPath,
                       const ErrorLocation& location)
{
   Error error = fileError(code, targetPath, location);
   error.addProperty("temp-path", tempPath.getAbsolutePath());
   return error;
}

#ifdef _WIN32

// Create a temporary file next to targetPath and write contents to it. On
// failure the temporary file is removed, so the caller has nothing to clean up.
// *pTempCreated reports whether the temporary file could be created at all,
// which is the only failure the caller may fall back from.
Error writeTempFile(const FilePath& targetPath,
                    const std::string& contents,
                    const AtomicWriteOptions& options,
                    FilePath* pTempPath,
                    bool* pTempCreated)
{
   *pTempCreated = false;

   Error error = atomicWriteTempPath(targetPath, pTempPath);
   if (error)
      return error;

   // CREATE_NEW, so that we never write through a file someone else put there
   HANDLE hFile = ::CreateFileW(
      pTempPath->getAbsolutePathW().c_str(),
      GENERIC_WRITE,
      0,
      nullptr,
      CREATE_NEW,
      FILE_ATTRIBUTE_NORMAL,
      nullptr);

   if (hFile == INVALID_HANDLE_VALUE)
      return atomicWriteError(::GetLastError(), targetPath, *pTempPath, ERROR_LOCATION);

   *pTempCreated = true;

   error = writeAll(hFile, contents, targetPath);
   if (!error && options.durable && !::FlushFileBuffers(hFile))
      error = atomicWriteError(::GetLastError(), targetPath, *pTempPath, ERROR_LOCATION);

   if (error)
   {
      error.addOrUpdateProperty("temp-path", pTempPath->getAbsolutePath());
      (void) ::CloseHandle(hFile);
      pTempPath->removeIfExists();
      return error;
   }

   if (!::CloseHandle(hFile))
   {
      error = atomicWriteError(::GetLastError(), targetPath, *pTempPath, ERROR_LOCATION);
      pTempPath->removeIfExists();
      return error;
   }

   return Success();
}

// Rename tempPath over targetPath with POSIX semantics, which (unlike the
// legacy rename) supersedes a target that another process has open with
// FILE_SHARE_DELETE, as our readers do; they keep reading the old contents.
// Needs Windows 10 1607+ and a filesystem that supports it (NTFS): elsewhere
// it fails with ERROR_INVALID_PARAMETER or ERROR_NOT_SUPPORTED.
bool posixRename(const FilePath& tempPath, const FilePath& targetPath, DWORD* pCode)
{
   HANDLE hFile = ::CreateFileW(
      tempPath.getAbsolutePathW().c_str(),
      DELETE,
      FILE_SHARE_READ | FILE_SHARE_WRITE | FILE_SHARE_DELETE,
      nullptr,
      OPEN_EXISTING,
      FILE_ATTRIBUTE_NORMAL,
      nullptr);

   if (hFile == INVALID_HANDLE_VALUE)
   {
      *pCode = ::GetLastError();
      return false;
   }

   // the kernel parses the new name as an NT path, which wants backslashes
   std::wstring newName = targetPath.getAbsolutePathW();
   std::replace(newName.begin(), newName.end(), L'/', L'\\');

   std::size_t nameBytes = newName.size() * sizeof(WCHAR);
   std::vector<char> buffer(offsetof(FILE_RENAME_INFO, FileName) + nameBytes + sizeof(WCHAR), 0);
   FILE_RENAME_INFO* pInfo = reinterpret_cast<FILE_RENAME_INFO*>(buffer.data());
   pInfo->Flags = FILE_RENAME_FLAG_REPLACE_IF_EXISTS | FILE_RENAME_FLAG_POSIX_SEMANTICS;
   pInfo->RootDirectory = nullptr;
   pInfo->FileNameLength = static_cast<DWORD>(nameBytes);
   std::memcpy(pInfo->FileName, newName.data(), nameBytes);

   BOOL renamed = ::SetFileInformationByHandle(hFile, FileRenameInfoEx, pInfo, static_cast<DWORD>(buffer.size()));
   *pCode = renamed ? ERROR_SUCCESS : ::GetLastError();
   (void) ::CloseHandle(hFile);

   return renamed != FALSE;
}

bool isReadOnly(const FilePath& filePath)
{
   DWORD attributes = ::GetFileAttributesW(filePath.getAbsolutePathW().c_str());
   return attributes != INVALID_FILE_ATTRIBUTES && (attributes & FILE_ATTRIBUTE_READONLY) != 0;
}

Error replaceFile(const FilePath& tempPath,
                  const FilePath& targetPath,
                  const AtomicWriteOptions& options)
{
   using namespace boost::posix_time;

   // the contents were flushed before the rename when durable was requested,
   // and NTFS journals the rename itself; the legacy rename can also be asked
   // to write through
   DWORD flags = MOVEFILE_REPLACE_EXISTING;
   if (options.durable)
      flags |= MOVEFILE_WRITE_THROUGH;

   // A target that another process holds open without FILE_SHARE_DELETE
   // (indexers, antivirus and backup software do so briefly) can't be replaced
   // until it's closed; retry for a while.
   bool posixSupported = true;
   ptime deadline = microsec_clock::universal_time() + seconds(std::max(options.maxRetrySeconds, 0));
   while (true)
   {
      DWORD code = ERROR_SUCCESS;
      bool renamed = false;
      if (posixSupported)
      {
         renamed = posixRename(tempPath, targetPath, &code);
         posixSupported = renamed || (code != ERROR_INVALID_PARAMETER && code != ERROR_NOT_SUPPORTED);
      }

      if (!renamed && !posixSupported)
      {
         renamed = ::MoveFileExW(tempPath.getAbsolutePathW().c_str(),
                                 targetPath.getAbsolutePathW().c_str(),
                                 flags) != FALSE;
         if (!renamed)
            code = ::GetLastError();
      }

      if (renamed)
         return Success();

      // the legacy rename reports a target in use as ERROR_ACCESS_DENIED, but
      // that is also the (permanent) answer for a read-only target
      bool inUse = code == ERROR_SHARING_VIOLATION ||
                   code == ERROR_LOCK_VIOLATION ||
                   (code == ERROR_ACCESS_DENIED && !isReadOnly(targetPath));

      if (!inUse || microsec_clock::universal_time() >= deadline)
         return atomicWriteError(static_cast<int>(code), targetPath, tempPath, ERROR_LOCATION);

      boost::this_thread::sleep(milliseconds(50));
   }
}

#else

// Create a temporary file next to targetPath and write contents to it. On
// failure the temporary file is removed, so the caller has nothing to clean up.
// *pTempCreated reports whether the temporary file could be created at all,
// which is the only failure the caller may fall back from.
Error writeTempFile(const FilePath& targetPath,
                    const std::string& contents,
                    const AtomicWriteOptions& options,
                    FilePath* pTempPath,
                    bool* pTempCreated)
{
   *pTempCreated = false;

   // when replacing a file, the new one takes over its owner, group and mode
   struct stat targetStat;
   bool replacing = ::stat(targetPath.getAbsolutePath().c_str(), &targetStat) == 0;

   Error error = atomicWriteTempPath(targetPath, pTempPath);
   if (error)
      return error;

   // O_EXCL, so that we never write through a file or link someone else put
   // there. When the mode is set explicitly below, start out private so the
   // file is never more widely readable than the one it replaces.
   mode_t createMode = (replacing || options.ownerOnly) ? 0600 : 0666;
   int fd = -1;
   do
   {
      fd = ::open(pTempPath->getAbsolutePath().c_str(),
                  O_WRONLY | O_CREAT | O_EXCL | O_CLOEXEC,
                  createMode);
   }
   while (fd == -1 && errno == EINTR);

   if (fd == -1)
      return atomicWriteError(errno, targetPath, *pTempPath, ERROR_LOCATION);

   *pTempCreated = true;

   if (replacing)
   {
      // Only root can give a file to another user, and other users can only
      // set a group they belong to, so this is best-effort. When it fails the
      // new file is ours, which is also what repairs a state file that an
      // earlier 'sudo rstudio' left owned by root.
      uid_t uid = (::geteuid() == 0) ? targetStat.st_uid : static_cast<uid_t>(-1);
      if (::fchown(fd, uid, targetStat.st_gid) == -1)
      {
         int code = errno;
         DLOGF("Couldn't carry the owner of '{}' over to its replacement (errno {})", targetPath.getAbsolutePath(), code);
      }
   }

   // setuid, setgid and sticky bits are deliberately not carried over, since
   // the new file may have a different owner
   if (replacing || options.ownerOnly)
   {
      mode_t mode = options.ownerOnly ? 0600 : (targetStat.st_mode & 0777);
      if (::fchmod(fd, mode) == -1)
         error = fileError(errno, targetPath, ERROR_LOCATION);
   }

   if (!error)
      error = writeAll(fd, contents, targetPath);
   if (!error && options.durable)
      error = syncFile(fd, targetPath);

   if (error)
      (void) ::close(fd);
   else
      error = closeFile(fd, targetPath);

   if (error)
   {
      error.addOrUpdateProperty("temp-path", pTempPath->getAbsolutePath());
      pTempPath->removeIfExists();
   }

   return error;
}

Error replaceFile(const FilePath& tempPath,
                  const FilePath& targetPath,
                  const AtomicWriteOptions& options)
{
   if (::rename(tempPath.getAbsolutePath().c_str(), targetPath.getAbsolutePath().c_str()) == -1)
   {
      Error error = fileError(errno, targetPath, ERROR_LOCATION);
      error.addProperty("temp-path", tempPath.getAbsolutePath());
      return error;
   }

   // the file's contents were flushed before the rename; flush the directory
   // as well, so that the rename itself survives a crash
   if (options.durable)
      syncDirectory(targetPath.getParent());

   return Success();
}

#endif

// The directories this process has already swept for the temporary files of
// interrupted writes. Leaked, as writes may still be in flight during static
// teardown.
std::set<std::string>& s_sweptDirectories = make_leaked<std::set<std::string>>();
boost::mutex& s_sweptDirectoriesMutex = make_leaked<boost::mutex>();

// Sweep dir the first time this process writes into it. That covers every
// directory written atomically without each writer having to remember to,
// and the earlier write that left a file behind is what crashed, so a fresh
// process is the right one to clean up after it. A directory that can't be
// listed yet (it may not exist until the write below creates it) is swept
// on a later write instead. Two threads may race to sweep the same directory,
// which is harmless.
void removeStaleAtomicWriteTempFilesOnce(const FilePath& dir)
{
   std::string key = dir.getAbsolutePath();

   LOCK_MUTEX(s_sweptDirectoriesMutex)
   {
      if (s_sweptDirectories.count(key) != 0)
         return;
   }
   END_LOCK_MUTEX

   if (!removeStaleAtomicWriteTempFiles(dir))
      return;

   LOCK_MUTEX(s_sweptDirectoriesMutex)
   {
      s_sweptDirectories.insert(key);
   }
   END_LOCK_MUTEX
}

} // anonymous namespace

std::string stringifyStringPair(const std::pair<std::string,std::string>& pair)
{
   return pair.first + "=\"" + string_utils::jsonLiteralEscape(pair.second) + "\"";
}

Error writeStringMapToFile(const core::FilePath& filePath,
                           const std::map<std::string,std::string>& map)
{
   return writeCollectionToFile<std::map<std::string,std::string> >(
                                                      filePath, 
                                                      map, 
                                                      stringifyStringPair);
}

ReadCollectionAction parseStringPair(
                     const std::string& line, 
                     std::pair<const std::string,std::string>* pPair)
{
   std::string::size_type pos = line.find("=");
   if ( pos != std::string::npos )
   {
      std::string name = line.substr(0, pos);
      boost::algorithm::trim(name);
      std::string value = line.substr(pos + 1);
      boost::algorithm::trim(value);
      if (value.length() >= 2 && value[0] == '"' && value[value.length() - 1] == '"')
      {
         value = string_utils::jsonLiteralUnescape(value);
      }
    
      // HACK: workaround the fact that std::map uses const for the Key
      std::string* pFirst = const_cast<std::string*>(&(pPair->first));
      *pFirst = name;

      pPair->second = value;

      return ReadCollectionAddLine;
   } 
   else
   {
      return ReadCollectionIgnoreLine;
   }
}


Error readStringMapFromFile(const core::FilePath& filePath,
                            std::map<std::string,std::string>* pMap)
{
   return readCollectionFromFile<std::map<std::string,std::string> >(
                                                      filePath,
                                                      pMap,
                                                      parseStringPair);
}

   
std::string stringifyString(const std::string& str)
{
   return str;
}
   
   
Error writeStringVectorToFile(const core::FilePath& filePath,
                              const std::vector<std::string>& vector)
{  
   return writeCollectionToFile<std::vector<std::string> >(filePath,
                                                           vector,
                                                           stringifyString);
   
}
   
   
ReadCollectionAction parseString(const std::string& line, std::string* pStr)
{
   *pStr = line;
   return ReadCollectionAddLine;
}
   
Error readStringVectorFromFile(const core::FilePath& filePath,
                               std::vector<std::string>* pVector,
                               bool trimAndIgnoreBlankLines)
{
   return readCollectionFromFile<std::vector<std::string> > (
         filePath, pVector, parseString, trimAndIgnoreBlankLines);
   
}

Error writeStringToFile(const FilePath& filePath,
                        const std::string& str,
                        string_utils::LineEnding lineEnding,
                        bool truncate,
                        int maxOpenRetrySeconds,
                        bool logError,
                        bool durable)
{
   // normalize line endings up front
   std::string contents = str;
   string_utils::convertLineEndings(&contents, lineEnding);

   // write the contents; when durable is set we flush all the way to physical
   // storage so that deferred write failures (a full disk or an exceeded quota)
   // are reported rather than silently discarded
   Error error = writeContentsToFileWithRetry(filePath, contents, truncate, maxOpenRetrySeconds, durable);
   if (error && logError)
      LOG_ERROR(error);

   return error;
}

bool isDiskSpaceError(const Error& error)
{
   if (!error)
      return false;

   // ENOSPC / EDQUOT (and the Windows equivalents) are reported in the system
   // category: errno on POSIX, the Win32 error code on Windows
   if (error.getName() != boost::system::system_category().name())
      return false;

   int code = error.getCode();

#ifdef _WIN32
   return code == ERROR_DISK_FULL || code == ERROR_HANDLE_DISK_FULL;
#else
   if (code == ENOSPC)
      return true;
# ifdef EDQUOT
   if (code == EDQUOT)
      return true;
# endif
   return false;
#endif
}

Error writeStringToFileAtomic(const FilePath& filePath,
                              const std::string& str,
                              string_utils::LineEnding lineEnding,
                              const AtomicWriteOptions& options)
{
   std::string contents = str;
   string_utils::convertLineEndings(&contents, lineEnding);

   FilePath targetPath;
   Error error = resolveSymlinks(filePath, &targetPath);
   if (error)
      return error;

   removeStaleAtomicWriteTempFilesOnce(targetPath.getParent());

   FilePath tempPath;
   bool tempCreated = false;
   error = writeTempFile(targetPath, contents, options, &tempPath, &tempCreated);
   if (error)
   {
      // No file could be created next to the target, e.g. because its
      // directory isn't writable even though the file itself is. A failure
      // after that point (setting the mode, writing, flushing) is reported as
      // is: rewriting in place would truncate the target before finding out
      // whether the write can succeed.
      if (!tempCreated && options.allowInPlaceFallback && isPermissionError(error))
         return writeInPlace(targetPath, contents, options);

      return error;
   }

   error = replaceFile(tempPath, targetPath, options);
   if (error)
   {
      tempPath.removeIfExists();
      return error;
   }

   return Success();
}

bool isAtomicWriteTempFile(const FilePath& filePath)
{
   return boost::algorithm::starts_with(filePath.getFilename(), kAtomicWriteTempPrefix);
}

bool removeStaleAtomicWriteTempFiles(const FilePath& dir, std::time_t maxAgeSeconds)
{
   if (!dir.isDirectory())
      return false;

   std::vector<FilePath> children;
   Error error = dir.getChildren(children);
   if (error)
   {
      LOG_ERROR(error);
      return false;
   }

   std::time_t now = std::time(nullptr);
   for (const FilePath& child : children)
   {
      if (!isAtomicWriteTempFile(child) || child.isDirectory())
         continue;

      if (now - child.getLastWriteTime() < maxAgeSeconds)
         continue;

      error = child.removeIfExists();
      if (error)
         LOG_ERROR(error);
   }

   return true;
}

Error readStringFromFile(const FilePath& filePath,
                         std::string* pStr,
                         string_utils::LineEnding lineEnding,
                         int startLine,
                         int endLine,
                         int startCharacter,
                         int endCharacter)
{
   using namespace boost::system::errc;
   
   // open file
   std::shared_ptr<std::istream> pIfs;
   Error error = filePath.openForRead(pIfs);
   if (error)
      return error;

   try
   {
      // if a line region was specified, read that region instead of the
      // entire file.
      if (endLine > startLine)
      {
         // set exception mask; note that we can't let failbit create an
         // exception here because reading eof can trigger failbit in our case.
         pIfs->exceptions(std::istream::badbit);

         int currentLine = 0;
         std::string content;
         std::string line;
         // loop over each line in the file. (consider: is there a more
         // performant way to seek past the first N lines?)
         while (++currentLine <= endLine
                && !pIfs->eof())
         {
            std::getline(*pIfs, line);
            if (currentLine >= startLine)
            {
               // compute the portion of the line to be read; if this is the
               // start or end of the region to be read, use the character
               // offsets supplied
               int lineLength = gsl::narrow_cast<int>(line.length());
               content += line.substr(
                        currentLine == startLine ?
                           std::min(
                              std::max(startCharacter - 1,  0),
                              lineLength) :
                           0,
                        currentLine == endLine ?
                           std::min(endCharacter, lineLength) :
                           lineLength);
               if (currentLine != endLine)
               {
                  content += "\n";
               }
            }
         }
         *pStr = content;
      }
      // reading the entire file
      else
      {
         // set exception mask (required for proper reporting of errors)
         pIfs->exceptions(std::istream::failbit | std::istream::badbit);

         // copy file to string stream
         std::ostringstream ostr;
         boost::iostreams::copy(*pIfs, ostr);
         *pStr = ostr.str();
      }

      string_utils::convertLineEndings(pStr, lineEnding);

      // return success
      return Success();
   }
   catch(const std::exception& e)
   {
      Error error = systemError(boost::system::errc::io_error, 
                                ERROR_LOCATION);
      error.addProperty("what", e.what());
      error.addProperty("path", filePath.getAbsolutePath());
      return error;
   }
}

bool stripBOM(std::string* pStr)
{
   if (boost::algorithm::starts_with(*pStr, "\xEF\xBB\xBF"))
   {
      pStr->erase(0, 3);
      return true;
   }
   else if (boost::algorithm::starts_with(*pStr, "\xFF\xFE"))
   {
      pStr->erase(0, 2);
      return true;
   }
   else if (boost::algorithm::starts_with(*pStr, "\xFE\xFF"))
   {
      pStr->erase(0, 2);
      return true;
   }
   return false;
}

} // namespace core
} // namespace rstudio


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
#include <sstream>
#include <algorithm>
#include <gsl/gsl-lite.hpp>

#include <boost/algorithm/string/trim.hpp>
#include <boost/algorithm/string/predicate.hpp>
#include <boost/iostreams/copy.hpp>
#include <boost/thread.hpp>

#ifdef _WIN32
# include <windows.h>
#else
# include <cerrno>
# include <fcntl.h>
# include <sys/stat.h>
# include <unistd.h>
#endif

#include <shared_core/FilePath.hpp>
#include <core/DateTime.hpp>
#include <core/Log.hpp>
#include <core/StringUtils.hpp>

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

Error writeContentsToFile(const FilePath& filePath,
                          const std::string& contents,
                          bool truncate,
                          bool durable)
{
   HANDLE hFile = ::CreateFileW(
      filePath.getAbsolutePathW().c_str(),
      truncate ? GENERIC_WRITE : FILE_APPEND_DATA,
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

   // write all of the bytes, looping in case WriteFile performs a partial write
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
         (void) ::CloseHandle(hFile);
         error.addProperty("path", filePath.getAbsolutePath());
         return error;
      }

      data += written;
      remaining -= written;
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

   // write all of the bytes, handling partial writes and EINTR
   const char* data = contents.data();
   std::size_t remaining = contents.size();
   while (remaining > 0)
   {
      ssize_t written = ::write(fd, data, remaining);
      if (written < 0)
      {
         if (errno == EINTR)
            continue;

         int code = errno;
         (void) ::close(fd);
         return fileError(code, filePath, ERROR_LOCATION);
      }

      data += written;
      remaining -= static_cast<std::size_t>(written);
   }

   // when durability is requested, flush to physical storage; this is the point
   // at which a full disk or an exceeded quota is reliably reported. On macOS
   // plain fsync() does not push data to the physical platter, so we prefer
   // F_FULLFSYNC and fall back to fsync() when the filesystem does not support it.
   if (durable)
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
      {
         int code = errno;
         (void) ::close(fd);
         return fileError(code, filePath, ERROR_LOCATION);
      }
   }

   // close the file; a close() that returns EINTR has still closed the
   // descriptor on Linux, so we must not retry it (that could close an
   // unrelated fd that was opened in the meantime)
   if (::close(fd) == -1 && errno != EINTR)
      return fileError(errno, filePath, ERROR_LOCATION);

   return Success();
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

Error writeStringToFileAtomic(const FilePath& filePath,
                              const std::string& str,
                              string_utils::LineEnding lineEnding)
{
   // generate a unique temporary file in the same directory
   FilePath tmpFile;
   Error error = FilePath::uniqueFilePath(filePath.getParent().getAbsolutePath(), tmpFile);
   if (error)
      return error;

   // write to the temporary file, flushing it to physical storage so the
   // contents are durable before we rename it into place
   error = writeStringToFile(tmpFile,
                             str,
                             lineEnding,
                             true /* truncate */,
                             0 /* maxOpenRetrySeconds */,
                             true /* logError */,
                             true /* durable */);
   if (error)
   {
      tmpFile.removeIfExists();
      return error;
   }

   // atomically rename into place
   error = tmpFile.move(filePath, FilePath::MoveDirect);
   if (error)
   {
      tmpFile.removeIfExists();
      return error;
   }

   // the temporary file's contents were already flushed to disk by
   // writeStringToFile; flush the parent directory as well so that the rename
   // itself is durable across a crash (best-effort, POSIX only)
#ifndef _WIN32
   syncDirectory(filePath.getParent());
#endif

   return Success();
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


/*
 * FileLogDestination.cpp
 * 
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant to the terms of a commercial license agreement
 * with Posit, then this program is licensed to you under the following terms:
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

#include <shared_core/FileLogDestination.hpp>

#include <boost/algorithm/string.hpp>
#include <boost/thread.hpp>

#include <vector>

#include <shared_core/DateTime.hpp>
#include <shared_core/Error.hpp>
#include <shared_core/Logger.hpp>
#include <shared_core/SafeConvert.hpp>

#ifndef _WIN32
#include <shared_core/system/PosixSystem.hpp>
#include <shared_core/system/SyslogDestination.hpp>
#endif

#include "config.h"

namespace rstudio {
namespace core {
namespace log {

// FileLogOptions ======================================================================================================
FileLogOptions::FileLogOptions(FilePath in_directory) :
   m_directory(std::move(in_directory)),
   m_fileMode(s_defaultFileMode),
   m_maxSizeMb(s_defaultMaxSizeMb),
   m_rotationDays(s_defaultRotationDays),
   m_maxRotations(s_defaultMaxRotations),
   m_deletionDays(s_defaultDeletionDays),
   m_doRotation(s_defaultDoRotation),
   m_includePid(s_defaultIncludePid),
   m_warnSyslog(s_defaultWarnSyslog),
   m_forceDirectory(s_defaultForceDirectory)
{
}

FileLogOptions::FileLogOptions(FilePath in_directory,
                               bool in_warnSyslog) :
   m_directory(std::move(in_directory)),
   m_fileMode(s_defaultFileMode),
   m_maxSizeMb(s_defaultMaxSizeMb),
   m_rotationDays(s_defaultRotationDays),
   m_maxRotations(s_defaultMaxRotations),
   m_deletionDays(s_defaultDeletionDays),
   m_doRotation(s_defaultDoRotation),
   m_includePid(s_defaultIncludePid),
   m_warnSyslog(in_warnSyslog),
   m_forceDirectory(s_defaultForceDirectory)
{
}

FileLogOptions::FileLogOptions(
   FilePath in_directory,
   std::string in_fileMode,
   double in_maxSizeMb,
   int in_rotationDays,
   int in_maxRotations,
   int in_deletionDays,
   bool in_doRotation,
   bool in_includePid,
   bool in_warnSyslog,
   bool in_forceDirectory) :
      m_directory(std::move(in_directory)),
      m_fileMode(std::move(in_fileMode)),
      m_maxSizeMb(in_maxSizeMb),
      m_rotationDays(in_rotationDays),
      m_maxRotations(in_maxRotations),
      m_deletionDays(in_deletionDays),
      m_doRotation(in_doRotation),
      m_includePid(in_includePid),
      m_warnSyslog(in_warnSyslog),
      m_forceDirectory(in_forceDirectory)
{
}

int FileLogOptions::getDeletionDays() const
{
   return m_deletionDays;
}

const FilePath& FileLogOptions::getDirectory() const
{
   return m_directory;
}

const std::string& FileLogOptions::getFileMode() const
{
   return m_fileMode;
}

bool FileLogOptions::getForceDirectory() const
{
   return m_forceDirectory;
}

int FileLogOptions::getMaxRotations() const
{
   return m_maxRotations;
}

double FileLogOptions::getMaxSizeMb() const
{
   return m_maxSizeMb;
}

int FileLogOptions::getRotationDays() const
{
   return m_rotationDays;
}

bool FileLogOptions::doRotation() const
{
   return m_doRotation;
}

bool FileLogOptions::warnSyslog() const
{
   return m_warnSyslog;
}

bool FileLogOptions::includePid() const
{
   return m_includePid;
}

void FileLogOptions::setDeletionDays(int in_deletionDays)
{
   m_deletionDays = in_deletionDays;
}

void FileLogOptions::setDirectory(const FilePath& in_directory)
{
   m_directory = in_directory;
}

void FileLogOptions::setFileMode(const std::string& in_fileMode)
{
   m_fileMode = in_fileMode;
}

void FileLogOptions::setForceDirectory(bool in_forceDirectory)
{
   m_forceDirectory = in_forceDirectory;
}

void FileLogOptions::setMaxRotations(int in_maxRotations)
{
   m_maxRotations = in_maxRotations;
}

void FileLogOptions::setMaxSizeMb(double in_maxSizeMb)
{
   m_maxSizeMb = in_maxSizeMb;
}

void FileLogOptions::setDoRotation(bool in_doRotation)
{
   m_doRotation = in_doRotation;
}

void FileLogOptions::setRotationDays(int in_rotationDays)
{
   m_rotationDays = in_rotationDays;
}

void FileLogOptions::setWarnSyslog(bool in_warnSyslog)
{
   m_warnSyslog = in_warnSyslog;
}

// FileLogDestination ==================================================================================================
struct FileLogDestination::Impl
{
   Impl(const std::string& in_name, FileLogOptions in_options) :
      LogOptions(std::move(in_options)),
      LogName(in_name + ".log")
   {
#ifndef _WIN32
      if (!LogOptions.getDirectory().exists())
      {
         // Create each directory listed in the final desired logging directory
         // with 0775 permissions - this will ensure that regular users cannot
         // access/modify the directory/contents and requires other RStudio products
         // to create their subdirectory as root
         std::string pathStr = LogOptions.getDirectory().getAbsolutePath();
         std::vector<std::string> pathParts;
         boost::split(pathParts, pathStr, boost::is_any_of("/"));

         FilePath path("/");
         for (const auto& part : pathParts)
         {
            if (part.empty())
               continue;

            Error error = path.completeChildPath(part, path);
            if (error)
               return;

            if (!path.exists())
            {
               Error error = path.ensureDirectory();
               if (error)
                  return;

               path.changeFileMode(FileMode::USER_READ_WRITE_EXECUTE_GROUP_READ_WRITE_EXECUTE_ALL_READ_EXECUTE);
            }
         }
      }
      else if (LogOptions.getDirectory().getAbsolutePath() == RSTUDIO_DEFAULT_LOG_PATH)
      {
         // fix-up legacy log directory permissions
         // the original release of standardized file-logging caused logging dirs
         // to be created with 0777 permissions which is too permissive
         FilePath paths[2] = {FilePath("/var/log/rstudio"), FilePath(RSTUDIO_DEFAULT_LOG_PATH)};
         for (const FilePath& path : paths)
         {
            FileMode mode;
            if (!path.getFileMode(mode))
            {
               if (mode == FileMode::ALL_READ_WRITE_EXECUTE)
                  path.changeFileMode(FileMode::USER_READ_WRITE_EXECUTE_GROUP_READ_WRITE_EXECUTE_ALL_READ_EXECUTE);
            }
         }
      }
#else
      LogOptions.getDirectory().ensureDirectory();
#endif

      // initialize LogFile path here - this ensures that it is set properly
      // in case we attempt to chown the file (due to permissions changing) before
      // attempting to write to the log file for the first time during this process run
      verifyLogFilePath();
   }

   ~Impl()
   {
      closeLogFile();
   }

   bool verifyLogFilePath()
   {
      Error error = LogOptions.getDirectory().completeChildPath(LogName, LogFile);
      if (error)
         return false;

      return true;
   }

   void closeLogFile()
   {
      if (LogOutputStream)
      {
         LogOutputStream->flush();
         LogOutputStream.reset();
      }
   }

   // Returns true if the log file was opened, false otherwise.
   bool openLogFile()
   {
      // We can't safely log in this function.
      Error error = LogFile.ensureFile();
      if (error)
         return false;

#ifndef _WIN32
      // Attempt to change the file mode, but if this fails we will not prevent
      // the log entry from attempting to be written
      LogFile.changeFileMode(LogOptions.getFileMode());
#endif

      error = LogFile.openForWrite(LogOutputStream, false);
      if (error)
         return false;

      return true;
   }

   FilePath getLogRotationFile(const FilePath& logFile,
                                  int logFileNumber)
   {
      std::string numberStr = safe_convert::numberToString(logFileNumber);

      std::string stem = logFile.getStem();
      stem = stem.substr(0, stem.find_first_of('.'));

      FilePath rotatedLogFile;
      FilePath(LogOptions.getDirectory()).completeChildPath(stem + "." + numberStr + ".log", rotatedLogFile);

      return rotatedLogFile;
   }

   bool rotateLogFileImpl(const FilePath& logFile,
                          int logFileNumber = 0)
   {
      logFileNumber += 1;

      // Defensive break in case we somehow find ourselves in a situation where
      // we have an unreasonable amount of log files to rotate
      if (LogOptions.getMaxRotations() < 1000 && logFileNumber > 1000)
         return false;

      FilePath rotatedLogFile = getLogRotationFile(logFile, logFileNumber);

      // Recursively rotate each file in the numeric sequence
      // (.1.log becomes .2.log, .2.log becomes .3.log, etc)
      if (rotatedLogFile.exists())
      {
         if (!rotateLogFileImpl(rotatedLogFile, logFileNumber))
            return false;
      }

      if (LogOptions.getMaxRotations() > 0)
      {
         // If we are over the maximum configured allowed rotated log files, delete this
         // file instead of rotating it
         if (logFileNumber > LogOptions.getMaxRotations())
         {
            logFile.remove();
            return true;
         }
      }

      if (LogOptions.getDeletionDays() > 0)
      {
         // Check to see if the logfile is stale
         // If so, we will delete it instead of rotating it
         std::time_t lastWrite = logFile.getLastWriteTime();
         boost::posix_time::ptime lastWriteTime = lastWrite != 0 ? core::date_time::timeFromStdTime(lastWrite) :
                                                                   boost::posix_time::microsec_clock::universal_time();
         boost::posix_time::ptime now = boost::posix_time::microsec_clock::universal_time();
         if ((now - lastWriteTime) > boost::posix_time::hours(24) * LogOptions.getDeletionDays())
         {
            logFile.remove();
            return true;
         }
      }

      // Now do the current rotation for this file
      Error error = logFile.move(rotatedLogFile);
      if (error)
         return false;

      return true;
   }

#ifndef _WIN32
   void chownLogs(const FilePath& logFile,
                  const core::system::User& user,
                  int logFileNumber = 0)
   {
      logFileNumber += 1;

      // Defensive break in case we somehow find ourselves in a situation where
      // we have an unreasonable amount of log files to chown
      if (LogOptions.getMaxRotations() < 1000 && logFileNumber > 1000)
         return;

      FilePath rotatedLogFile = getLogRotationFile(logFile, logFileNumber);

      // Recursively chown each file in the numeric sequence
      if (rotatedLogFile.exists())
         chownLogs(rotatedLogFile, user, logFileNumber);

      // Now do the current chown for this file
      logFile.changeOwnership(user);
   }

   void setLogDirOwner(const core::system::User& user)
   {
      LogOptions.getDirectory().changeOwnership(user);
   }
#endif

   boost::posix_time::ptime getTimestampFromLogLine(const std::string& line)
   {
      // Empty line means the file hasn't actually been written to yet, so bail early
      if (line.empty())
         return boost::posix_time::microsec_clock::universal_time();

      boost::posix_time::ptime time;
      std::string timeStr;

      // Check for time in human readable log format first
      size_t pos = line.find(' ');
      if (pos != std::string::npos)
      {
         timeStr = line.substr(0, pos);
         if (core::date_time::parseUtcTimeFromIso8601String(timeStr, &time))
            return time;
      }

      // First bit of text was not a valid timestamp, so maybe this is JSON
      pos = line.find("\"time\":");
      if (pos != std::string::npos)
      {
         // Skip over "time":" to get to the start of the timestamp
         pos += 8;
         size_t endPos = line.find('"', pos);
         if (endPos != std::string::npos)
         {
            timeStr = line.substr(pos, endPos - pos);
            if (core::date_time::parseUtcTimeFromIso8601String(timeStr, &time))
               return time;
         }
      }
      else
      {
         // Time is not in new ISO format - look for our old format
         // Example: 28 Nov 2018 22:50:50.345 [rserver]
         pos = line.find('[');
         if (pos != std::string::npos)
         {
            timeStr = line.substr(0, pos - 1);
            if (core::date_time::parseUtcTimeFromFormatString(timeStr, "%d %b %Y %H:%M:%S", &time))
               return time;
         }
      }

      // Couldn't find it - return the current time to ensure no rotation is actually done
      return boost::posix_time::microsec_clock::universal_time();
   }

   boost::posix_time::ptime getFirstEntryTimestamp()
   {
      std::string firstLogLine;

      // Read the first log line from the file to determine when the file was initially created
      std::shared_ptr<std::istream> fileStream;
      Error error = LogFile.openForRead(fileStream);
      if (error)
         return boost::posix_time::microsec_clock::universal_time();

      try
      {
         fileStream->exceptions(std::istream::badbit);

         std::string line;
         std::getline(*fileStream, line);

         return getTimestampFromLogLine(line);
      }
      catch (const std::exception&)
      {
         // Swallow errors - we can't log them!
         return boost::posix_time::microsec_clock::universal_time();
      }
   }

   bool shouldTimeRotate()
   {
      if (LogOptions.getRotationDays() == 0)
         return false;

      // If this new logging entry is X days after the initial log entry, rotate the log
      //
      // We cache the result of the first entry timestamp because time always marches forward -
      // if another process or another logger within this same process rotates the file without us realizing,
      // this logger should still never rotate sooner than X days after what we read as the initial timestamp.
      // We will periodically recheck the initial timestamp again, but caching it will greatly improve performance
      boost::posix_time::ptime now = boost::posix_time::microsec_clock::universal_time();
      boost::posix_time::time_duration rotateTime = boost::posix_time::hours(24) * LogOptions.getRotationDays();

      if (FirstLogLineTime)
      {
         if ((now - FirstLogLineTime.get()) >= rotateTime)
         {
            // We should rotate based on the cached entry, but it's possible we were already rotated
            // by some other logger - check the timestamp again
            FirstLogLineTime = getFirstEntryTimestamp();
         }
         else
         {
            // Too soon to rotate
            return false;
         }
      }
      else
      {
         // No cached entry - get it
         FirstLogLineTime = getFirstEntryTimestamp();
      }

      return ((now - FirstLogLineTime.get()) >= rotateTime);
   }

   // Returns true if it is safe to log; false otherwise.
   bool rotateLogFile()
   {
      // Calculate the maximum size in bytes.
      const uintmax_t maxSize = 1048576.0 * LogOptions.getMaxSizeMb();

      // Only rotate if we're configured to rotate.
      if (LogOptions.doRotation())
      {
         if (LogFile.getSize() >= maxSize || shouldTimeRotate())
         {
            if (!rotateLogFileImpl(LogFile))
               return false;
         }

         // successfully rotated the log (no errors while performing rotation)
         return true;
      }
      else
      {
         // we are configured not to rotate logs, which means the log file can grow unboundedly large
         // thus, we are safe to log
         return true;
      }
   }

   FileLogOptions LogOptions;
   FilePath LogFile;
   std::string LogName;
   boost::mutex Mutex;
   std::shared_ptr<std::ostream> LogOutputStream;
   boost::optional<boost::posix_time::ptime> FirstLogLineTime;

#ifndef _WIN32
   std::shared_ptr<core::system::SyslogDestination> SyslogDest;
#endif
};

FileLogDestination::FileLogDestination(
   const std::string& in_id,
   LogLevel in_logLevel,
   LogMessageFormatType in_formatType,
   const std::string& in_programId,
   FileLogOptions in_logOptions,
   bool in_reloadable) :
      ILogDestination(in_id, in_logLevel, in_formatType, in_reloadable),
      m_impl(new Impl(in_programId, std::move(in_logOptions)))
{
#ifndef _WIN32
   if (in_logOptions.warnSyslog())
   {
      // We need to duplicate warn/error logs to syslog
      // To accomplish this, we will manage or own SyslogDestination which we will
      // forward logging calls to
      m_impl->SyslogDest = std::make_shared<core::system::SyslogDestination>(
               in_id, log::LogLevel::WARN, in_formatType, in_programId);
   }
#endif
}

FileLogDestination::~FileLogDestination()
{
   if (m_impl->LogOutputStream.get())
      m_impl->LogOutputStream->flush();
}

std::string FileLogDestination::path()
{
   return m_impl->LogFile.getAbsolutePath();
}

void FileLogDestination::refresh(const RefreshParams& in_refreshParams)
{
   // Close the log file to ensure that if we just forked old FDs are cleared out
   m_impl->closeLogFile();

#ifndef _WIN32
   if (in_refreshParams.newUser)
   {
      // If we can, change the log owner to the currently running user id to ensure
      // that we can continue writing to the log if we just changed our running user
      m_impl->chownLogs(m_impl->LogFile, in_refreshParams.newUser.get());

      if (in_refreshParams.chownLogDir)
         m_impl->setLogDirOwner(in_refreshParams.newUser.get());
   }

   if (m_impl->SyslogDest)
      m_impl->SyslogDest->refresh();
#endif
}

void FileLogDestination::writeLog(LogLevel in_logLevel, const std::string& in_message)
{
   // Don't write logs that are more detailed than the configured maximum.
   if (in_logLevel > m_logLevel)
      return;

   // Lock the mutex before attempting to write.
   try
   {
      boost::lock_guard<boost::mutex> lock(m_impl->Mutex);

#ifndef _WIN32
         // First write to syslog if configured
         if (in_logLevel <= LogLevel::WARN && m_impl->SyslogDest)
            m_impl->SyslogDest->writeLog(in_logLevel, in_message);
#endif

      // Check to make sure path to file is valid. If not, log nothing.
      if (!m_impl->verifyLogFilePath())
         return;

      // Rotate the log file if necessary. If it fails to rotate, log nothing.
      if (!m_impl->rotateLogFile())
         return;

      // Open the log file. If it fails to open, log nothing.
      if (!m_impl->openLogFile())
      {
         m_impl->closeLogFile();
         return;
      }

      (*m_impl->LogOutputStream) << in_message;
      m_impl->LogOutputStream->flush();

      // If the output stream has bad state after writing, it might have been closed. Try re-opening it and writing the
      // message again. Often it is not possible to tell that a stream has failed until a write is attempted.
      if (!m_impl->LogOutputStream->good())
      {
         if (!m_impl->openLogFile())
         {
            m_impl->closeLogFile();
            return;
         }

         (*m_impl->LogOutputStream) << in_message;
         m_impl->LogOutputStream->flush();
      }

      m_impl->closeLogFile();
   }
   catch (...)
   {
      // Swallow exceptions because we'd trigger recursive logging otherwise.
   }
}

} // namespace log
} // namespace core
} // namespace rstudio


/*
 * FileLogWriter.cpp
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

#include <core/FileLogWriter.hpp>

#include <core/FileInfo.hpp>
#include <core/FileSerializer.hpp>
#include <core/system/System.hpp>
#include <core/Thread.hpp>

#ifndef _WIN32
#include <core/system/FileMode.hpp>
#include <core/system/PosixChildProcess.hpp>
#include <stdio.h>
#endif

namespace rstudio {
namespace core {

FileLogWriter::FileLogWriter(const std::string& programIdentity,
                             int logLevel,
                             const FileLoggerOptions& options,
                             const std::string& logSection)
                                : programIdentity_(programIdentity),
                                  logLevel_(logLevel),
                                  options_(options)
{
   options.logDir.ensureDirectory();

   std::string logFileName = programIdentity + ".";

   if (!logSection.empty())
   {
      logFileName += (logSection + ".");
   }

   if (options.includePid)
   {
      std::string pidStr = core::system::currentProcessPidStr();
      logFileName += (pidStr + ".");
   }

   logFile_ = options.logDir.childPath(logFileName + "log");
   rotatedLogFile_ = options.logDir.childPath(logFileName + "rotated.log");

   createFile();
}

FileLogWriter::~FileLogWriter()
{
}

void FileLogWriter::createFile()
{
   if (!logFile_.exists())
   {
      // swallow errors -- we can't log so it doesn't matter
      core::appendToFile(logFile_, "");
   }

   // attempt to set the desired file permissions
#ifndef _WIN32
   core::system::changeFileMode(logFile_, options_.fileMode);
#endif
}

void FileLogWriter::log(core::system::LogLevel logLevel,
                        const std::string& message)
{
   log(programIdentity_, logLevel, message);
}

void FileLogWriter::log(const std::string& programIdentity,
                        core::system::LogLevel logLevel,
                        const std::string& message)
{
   if (logLevel < logLevel_)
      return;

   LOCK_MUTEX(mutex_)
   {
      rotateLogFile();

      // Swallow errors--we can't do anything anyway
      core::appendToFile(logFile_, formatLogEntry(programIdentity, message));
   }
   END_LOCK_MUTEX
}

bool FileLogWriter::rotateLogFile()
{
   if (!options_.rotate)
      return false;

   if (logFile_.exists() && logFile_.size() > (1048576.0 * options_.maxSizeMb))
   {
      Error error = rotatedLogFile_.removeIfExists();
      if (error)
      {
         rotateLogFilePrivileged();
      }
      else
      {
         error = logFile_.move(rotatedLogFile_);
         if (error)
            rotateLogFilePrivileged();
      }

      createFile();
      return true;
   }
   return false;
}

void FileLogWriter::rotateLogFilePrivileged()
{
   // note: on posix, when renaming a file to one that already exists
   // the existing file is replaced atomically, so no prior delete is required
#ifndef _WIN32
   std::string fromFile = logFile_.absolutePath();
   std::string toFile = rotatedLogFile_.absolutePath();

   auto moveFunc = [=]()
   {
      // attempt to rotate the log file as the root user
      // this is necessary in processes where we started as root
      // (and created the log file as root) and then gave up our privilege
      int err = 0;

      if (::rename(fromFile.c_str(), toFile.c_str()) != 0)
         err = errno;

      return err;
   };

   core::system::forkAndRunPrivileged(moveFunc);
#endif
}

} // namespace core
} // namespace rstudio

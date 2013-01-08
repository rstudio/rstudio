/*
 * FileLogWriter.cpp
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

#include <core/FileLogWriter.hpp>


#include <core/FileInfo.hpp>
#include <core/FileSerializer.hpp>
#include <core/system/System.hpp>

namespace core {

FileLogWriter::FileLogWriter(const std::string& programIdentity,
                             int logLevel,
                             const FilePath& logDir)
                                : programIdentity_(programIdentity),
                                  logLevel_(logLevel)
{
   logDir.ensureDirectory();

   logFile_ = logDir.childPath(programIdentity + ".log");

   if (!logFile_.exists())
   {
      // swallow errors -- we can't log so it doesn't matter
      core::appendToFile(logFile_, "");
   }
}

FileLogWriter::~FileLogWriter()
{
   try
   {
      // we don't keep a file handle open so do nothing here
   }
   catch(...)
   {
   }
}

void FileLogWriter::log(core::system::LogLevel logLevel,
                        const std::string& message)
{
   if (logLevel > logLevel_)
      return;

   rotateLogFile();

   // Swallow errors--we can't do anything anyway
   core::appendToFile(logFile_, formatLogEntry(programIdentity_, message));
}



#define LOGMAX (4096*1024)  // remove every 4 megabytes
bool FileLogWriter::rotateLogFile()
{
   if (logFile_.exists() && logFile_.size() > LOGMAX)
   {
      logFile_.remove();
      return true;
   }
   return false;
}


} // namespace core

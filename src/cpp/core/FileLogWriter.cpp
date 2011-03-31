/*
 * FileLogWriter.cpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <core/FileLogWriter.hpp>

#include <boost/algorithm/string/replace.hpp>
#include <boost/date_time/posix_time/posix_time.hpp>

#include <core/DateTime.hpp>
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
   core::appendToFile(logFile_, formatLogEntry(message));
}

std::string FileLogWriter::formatLogEntry(const std::string& message)
{
   // replace newlines with standard escape sequence
   std::string cleanedMessage(message);
   boost::algorithm::replace_all(cleanedMessage, "\n", "|||");

   // generate time string
   using namespace boost::posix_time;
   ptime time = microsec_clock::universal_time();
   std::string dateTime = date_time::format(time,  "%d %b %Y %H:%M:%S");

   // generate log entry
   std::ostringstream ostr;
   ostr << dateTime
        << " [" << programIdentity_ << "] "
        << cleanedMessage
        << std::endl;
   return ostr.str();
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

/*
 * SyslogLogWriter.cpp
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

#include <core/SyslogLogWriter.hpp>

#include <syslog.h>

#include <iostream>

#include <boost/algorithm/string.hpp>

#include <core/system/System.hpp>

namespace {

int logPriority(int logLevel)
{
   using namespace core::system;

   // map universal log entry type to posix constant
   switch(logLevel)
   {
      case kLogLevelError:
         return LOG_ERR;

      case kLogLevelWarning:
         return LOG_WARNING ;

      case kLogLevelInfo:
         return LOG_INFO ;

      case kLogLevelDebug:
         return LOG_DEBUG ;

      default:
         return LOG_WARNING;
   }
}

} // anonymous namespace

namespace core {

SyslogLogWriter::~SyslogLogWriter()
{
   try
   {
      ::closelog();
   }
   catch(...)
   {
   }
}

SyslogLogWriter::SyslogLogWriter(const std::string& programIdentity,
                                 int logLevel)
   : programIdentity_(programIdentity),
     logToStderr_(core::system::stderrIsTerminal())
{
   // copy program identity into new string whose buffer will stay
   // around long enough to successfully register with openlog
   // (passing the c_str of programIdentity wasn't working on OSX)
   std::string* pProgramIdentity = new std::string(programIdentity);

   // initialize log options
   int logOptions = LOG_CONS | LOG_PID ;

   // open log
   ::openlog(pProgramIdentity->c_str(), logOptions, LOG_USER);
   ::setlogmask(LOG_UPTO(logPriority(logLevel)));
}

void SyslogLogWriter::log(core::system::LogLevel logLevel,
                          const std::string& message)
{
   if (logToStderr_)
      std::cerr << formatLogEntry(programIdentity_, message, false);

   // unix system log entries are delimited by newlines so we replace
   // them with an alternate delimiter
   std::string cleanedMessage(message);
   boost::algorithm::replace_all(cleanedMessage, "\n", "|||");

   // log to the sys-log, to display in real-time use e.g
   //   tail --follow --lines=0 /var/log/user.log
   ::syslog(logPriority(logLevel), "%s", cleanedMessage.c_str()) ;
}

void SyslogLogWriter::log(const std::string&,
                          core::system::LogLevel logLevel,
                          const std::string& message)
{
   log(logLevel, message);
}

} // namespace core


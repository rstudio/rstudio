/*
 * SyslogDestination.cpp
 * 
 * Copyright (C) 2022 by Posit, PBC
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

#include <shared_core/system/SyslogDestination.hpp>

#include <cassert>
#include <iostream>
#include <syslog.h>

#include <boost/algorithm/string.hpp>
#include <boost/regex.hpp>

#include <shared_core/Logger.hpp>

namespace rstudio {
namespace core {
namespace system {

namespace {

int logLevelToLogPriority(log::LogLevel in_logLevel)
{
   switch(in_logLevel)
   {
      case log::LogLevel::ERR:
         return LOG_ERR;
      case log::LogLevel::WARN:
         return LOG_WARNING;
      case log::LogLevel::DEBUG:
         return LOG_DEBUG;
      case log::LogLevel::INFO:
         return LOG_INFO;
      default:
      {
         // This shouldn't happen.
         assert(false);
         return LOG_INFO;
      }
   }
}

} // anonymous namespace

struct SyslogDestination::Impl
{
   /**
    * @brief Constructor.
    *
    * param in_programId        The ID of the program for which system logs should be written.
    */
   explicit Impl(const std::string& in_programId) :
      ProgramId(in_programId)
   {
   }

   /** The program ID. It must be persisted. */
   std::string ProgramId;
};

PRIVATE_IMPL_DELETER_IMPL(SyslogDestination);

SyslogDestination::SyslogDestination(const std::string& in_id,
                                     log::LogLevel in_logLevel,
                                     log::LogMessageFormatType in_formatType,
                                     const std::string& in_programId,
                                     bool in_reloadable) :
   ILogDestination(in_id, in_logLevel, in_formatType, in_reloadable),
   m_impl(new Impl(in_programId))
{
   // Open the system log. Don't set a mask because filtering is done at a higher level.
   ::openlog(m_impl->ProgramId.c_str(), LOG_CONS | LOG_PID, LOG_USER);
}

SyslogDestination::~SyslogDestination()
{
   try
   {
      ::closelog();
   }
   catch(...)
   {
      // Ignore if we fail to close the log.
   }
}

void SyslogDestination::refresh(const log::RefreshParams&)
{
   // Close and re-open the log.
   ::closelog();
   ::openlog(m_impl->ProgramId.c_str(), LOG_CONS | LOG_PID, LOG_USER);
}

void SyslogDestination::writeLog(
   log::LogLevel in_logLevel,
   const std::string& in_message)
{
   // Don't write logs that are more detailed than the configured maximum.
   if (in_logLevel > m_logLevel)
      return;

   // Remove the leading date and program ID, since those are set by syslog directly.
   std::string message = boost::regex_replace(in_message, boost::regex("^[^\\]]*\\]\\s"), "");
   ::syslog(logLevelToLogPriority(in_logLevel), "%s", message.c_str());

   // Also log to stderr if there is a tty attached.
   if (::isatty(STDERR_FILENO) == 1)
      std::cerr << in_message;
}

} // namespace system
} // namespace core
} // namespace rstudio


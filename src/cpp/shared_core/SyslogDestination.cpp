/*
 * SyslogDestination.cpp
 * 
 * Copyright (C) 2019 by RStudio, Inc.
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

#include <shared_core/SyslogDestination.hpp>

#include <cassert>
#include <syslog.h>

#include <shared_core/Logger.hpp>

namespace rstudio {
namespace core {
namespace system {

namespace {

int logLevelToLogPriority(log::LogLevel in_logLevel)
{
   switch(in_logLevel)
   {
      case log::LogLevel::ERROR:
         return LOG_ERR;
      case log::LogLevel::WARNING:
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

SyslogDestination::SyslogDestination(const std::string& in_programId)
{
   // Open the system log. Don't set a mask because filtering is done at a higher level.
   ::openlog(in_programId.c_str(), LOG_CONS | LOG_PID, LOG_USER);
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

unsigned int SyslogDestination::getSyslogId()
{
   // We use 0 for std::err and 1 for syslog.
   return 1;
}

unsigned int SyslogDestination::getId() const
{
   return getSyslogId();
}

void SyslogDestination::writeLog(
   log::LogLevel in_logLevel,
   const std::string& in_message)
{
   ::syslog(logLevelToLogPriority(in_logLevel), "%s", in_message.c_str());
}

} // namespace system
} // namespace core
} // namespace rstudio


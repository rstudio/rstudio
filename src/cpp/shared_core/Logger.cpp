/*
 * Logger.cpp
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

#include <shared_core/Logger.hpp>

#include <boost/algorithm/string.hpp>
#include <boost/date_time/posix_time/posix_time.hpp>

#include <sstream>

// We do a little special handling for syslog because it does its own formatting.
#include <shared_core/SyslogDestination.hpp>
#include <shared_core/ILogDestination.hpp>
#include <shared_core/DateTime.hpp>

namespace rstudio {
namespace core {

namespace {

std::string formatLogMessage(
   LogLevel in_logLevel,
   const std::string& in_message,
   const std::string& in_programId,
   bool in_formatForSyslog = false)
{
   std::string levelPrefix;
   switch (in_logLevel)
   {
      case LogLevel::ERROR:
      {
         levelPrefix = "ERROR: ";
         break;
      }
      case LogLevel::WARNING:
      {
         levelPrefix = "WARNING: ";
         break;
      }
      case LogLevel::INFO:
      {
         levelPrefix = "INFO: ";
         break;
      }
      case LogLevel::DEBUG:
      {
         levelPrefix = "DEBUG: ";
         break;
      }
      case LogLevel::OFF:
      default:
      {
         // This shouldn't be possible. If it happens don't add a log level.
         assert(false);
         break;
      }
   }

   if (!in_formatForSyslog)
   {
      // Add the time.
      using namespace boost::posix_time;
      ptime time = microsec_clock::universal_time();

      std::ostringstream oss;
      oss << system::date_time::format(time, "%d %b %Y %H:%M:%S")
          << " [" << in_programId << "] "
          << levelPrefix
          << in_message << std::endl;
      return oss.str();
   }

   // Formatting for syslog.
   std::string formattedMessage = levelPrefix + in_message;
   boost::algorithm::replace_all(formattedMessage, "\n", "|||");
   return formattedMessage;
}

// Logger Object =======================================================================================================
/**
 * @brief Class which logs messages to destinations.
 *
 * Multiple destinations may be added to this logger in order to write the same message to each destination.
 * The default log level is ERROR.
 */
struct Logger : boost::noncopyable
{
public:
   /**
    * @brief Writes a pre-formatted message to all registered destinations.
    *
    * @param in_logLevel    The log level of the message, which is passed to the destination for informational purposes.
    * @param in_message     The pre-formatted message.
    */
   void writeMessageToAllDestinations(LogLevel in_logLevel, const std::string& in_message);

   /**
    * @brief Constructor to prevent multiple instances of Logger.
    */
   Logger() :
      MaxLogLevel(LogLevel::OFF),
      ProgramId(""),
      LogDestinations()
   { };

   // The maximum level of message to write.
   LogLevel MaxLogLevel;

   // The ID of the program fr which to write logs.
   std::string ProgramId;

   // The registered log destinations.
   std::map<unsigned int, std::unique_ptr<ILogDestination> > LogDestinations;
};

Logger& logger()
{
   static Logger logger;
   return logger;
}

void Logger::writeMessageToAllDestinations(LogLevel in_logLevel, const std::string& in_message)
{
   // Preformat the message for non-syslog loggers.
   std::string formattedMessage = formatLogMessage(in_logLevel, in_message, ProgramId);

   const auto destEnd = LogDestinations.end();
   for (auto iter = LogDestinations.begin(); iter != destEnd; ++iter)
   {
      if (iter->first == SyslogDestination::getSyslogId())
      {
         iter->second->writeLog(in_logLevel, formatLogMessage(in_logLevel, in_message, ProgramId, true));
      }
      else
      {
         iter->second->writeLog(in_logLevel, formattedMessage);
      }
   }
}

} // anonymous namespace

// Logging functions
void setProgramId(const std::string& in_programId)
{
   if (!logger().ProgramId.empty())
      logWarningMessage("Changing the program id from " + logger().ProgramId + " to " + in_programId);

   logger().ProgramId = in_programId;
}

void setLogLevel(LogLevel in_logLevel)
{
   logger().MaxLogLevel = in_logLevel;
}

void addLogDestination(std::unique_ptr<ILogDestination> in_destination)
{
   Logger& log = logger();
   if (log.LogDestinations.find(in_destination->getId()) == log.LogDestinations.end())
   {
      log.LogDestinations.insert(std::make_pair(in_destination->getId(), std::move(in_destination)));
   }
   else
   {
      logDebugMessage(
         "Attempted to register a log destination that has already been registered with id" +
         std::to_string(in_destination->getId()));
   }
}

void removeLogDestination(unsigned int in_destinationId)
{
   Logger& log = logger();
   auto iter = log.LogDestinations.find(in_destinationId);
   if (iter != log.LogDestinations.end())
   {
      log.LogDestinations.erase(iter);
   }
   else
   {
      logDebugMessage(
         "Attempted to unregister a log destination that has not been registered with id" +
         std::to_string(in_destinationId));
   }
}

void logError(const Error& in_error)
{
   Logger& log = logger();
   if (log.MaxLogLevel >= LogLevel::ERROR)
   {
      log.writeMessageToAllDestinations(LogLevel::ERROR, in_error.summary());
   }
}

void logErrorAsWarning(const Error& in_error)
{
   Logger& log = logger();
   if (log.MaxLogLevel >= LogLevel::WARNING)
   {
      log.writeMessageToAllDestinations(LogLevel::WARNING, in_error.summary());
   }
}

void logErrorAsInfo(const Error& in_error)
{
   Logger& log = logger();
   if (log.MaxLogLevel >= LogLevel::INFO)
   {
      log.writeMessageToAllDestinations(LogLevel::INFO, in_error.summary());
   }
}

void logErrorAsDebug(const Error& in_error)
{
   Logger& log = logger();
   if (log.MaxLogLevel >= LogLevel::DEBUG)
   {
      log.writeMessageToAllDestinations(LogLevel::DEBUG, in_error.summary());
   }
}

void logErrorMessage(const std::string& in_message)
{
   Logger& log = logger();
   if (log.MaxLogLevel >= LogLevel::ERROR)
      log.writeMessageToAllDestinations(LogLevel::ERROR, in_message);
}

void logWarningMessage(const std::string& in_message)
{
   Logger& log = logger();
   if (log.MaxLogLevel >= LogLevel::WARNING)
      log.writeMessageToAllDestinations(LogLevel::WARNING, in_message);
}

void logInfoMessage(const std::string& in_message)
{
   Logger& log = logger();
   if (log.MaxLogLevel >= LogLevel::INFO)
      log.writeMessageToAllDestinations(LogLevel::INFO, in_message);
}

void logDebugMessage(const std::string& in_message)
{
   Logger& log = logger();
   if (log.MaxLogLevel >= LogLevel::DEBUG)
      log.writeMessageToAllDestinations(LogLevel::DEBUG, in_message);
}

} // namespace core
} // namespace rstudio


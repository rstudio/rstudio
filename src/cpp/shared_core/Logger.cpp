/*
 * Logger.cpp
 * 
 * Copyright (C) 2021 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant to the terms of a commercial license agreement
 * with RStudio, then this program is licensed to you under the following terms:
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

#include <cassert>
#include <sstream>

#include <boost/algorithm/string.hpp>
#include <boost/noncopyable.hpp>
#include <boost/optional.hpp>

#include <shared_core/DateTime.hpp>
#include <shared_core/Error.hpp>
#include <shared_core/FileLogDestination.hpp>
#include <shared_core/json/Json.hpp>
#include <shared_core/ReaderWriterMutex.hpp>
#include <shared_core/StderrLogDestination.hpp>

namespace rstudio {
namespace core {
namespace log {

typedef std::map<std::string, std::shared_ptr<ILogDestination>> LogMap;
typedef std::map<std::string, LogMap> SectionLogMap;

namespace {

constexpr const char* s_loggedFrom = "LOGGED FROM";

std::ostream& operator<<(std::ostream& io_ostream, LogLevel in_logLevel)
{
   switch (in_logLevel)
   {
      case LogLevel::ERR:
      {
         io_ostream << "ERROR";
         break;
      }
      case LogLevel::WARN:
      {
         io_ostream << "WARNING";
         break;
      }
      case LogLevel::DEBUG:
      {
         io_ostream << "DEBUG";
         break;
      }
      case LogLevel::INFO:
      {
         io_ostream << "INFO";
         break;
      }
      case LogLevel::OFF:
      {
         io_ostream << "OFF";
         break;
      }
      default:
      {
         assert(false); // This shouldn't be possible
         if (in_logLevel > LogLevel::INFO)
            io_ostream << "INFO";
         else
            io_ostream << "OFF";
      }
   }

   return io_ostream;
}

std::string formatLogMessage(
   LogLevel in_logLevel,
   const std::string& in_message,
   const std::string& in_programId,
   bool humanReadableFormat,
   const ErrorLocation& in_loggedFrom = ErrorLocation())
{
   using namespace boost::posix_time;
   ptime time = microsec_clock::universal_time();

   // Don't allow newlines in messages since each log message should be one distinct line
   std::string message = boost::algorithm::trim_right_copy(in_message);
   boost::algorithm::replace_all(message, "\n", "|||");

   if (humanReadableFormat)
   {
      std::ostringstream oss;

      oss << core::date_time::format(time, core::date_time::kIso8601Format)
          << " [" << in_programId << "] ";

      oss << in_logLevel << " " << message;

      if (in_loggedFrom.hasLocation())
         oss << s_delim << " " << s_loggedFrom << ": " << cleanDelimiters(in_loggedFrom.asString());

      oss << std::endl;

      return oss.str();
   }
   else
   {
      json::Object logObject;
      logObject["time"] = core::date_time::format(time, core::date_time::kIso8601Format);
      logObject["service"] = in_programId;

      std::ostringstream logLevel;
      logLevel << in_logLevel;
      logObject["level"] = logLevel.str();

      logObject["message"] = message;

      return logObject.write() + "\n";
   }
}


} // anonymous namespace

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
    * @param in_logLevel        The log level of the message, which is passed to the destination for informational purposes.
    * @param in_message         The pre-formatted message.
    * @param in_section         The section to which to log this message.
    * @param in_loggedFrom      The location from which the error message was logged.
    */
   void writeMessageToDestinations(
      LogLevel in_logLevel,
      const std::string& in_message,
      const std::string& in_section = std::string(),
      const ErrorLocation& in_loggedFrom = ErrorLocation());

   /**
    * @brief Constructor to prevent multiple instances of Logger.
    */
   Logger() :
      MaxLogLevel(LogLevel::OFF),
      ProgramId("")
   { };

   // The maximum level of message to write across all log sections.
   LogLevel MaxLogLevel;

   // The ID of the program fr which to write logs.
   std::string ProgramId;

   // The registered default log destinations. Any logs without a section or with an unregistered section will be logged
   // to these destinations.
   LogMap DefaultLogDestinations;

   // The registered sectioned log destinations. Any logs with a registered section will be logged to the destinations
   // assigned to that section.
   SectionLogMap SectionedLogDestinations;

   // The read-write mutex to protect the maps.
   thread::ReaderWriterMutex Mutex;
};

Logger& logger()
{
   // Intentionally leak the logger object to avoid some crashes because we don't (can't currently) always wait for all
   // the background threads of a process, and destruction isn't necessary.
   static Logger* logger = new Logger();
   return *logger;
}

void Logger::writeMessageToDestinations(
   LogLevel in_logLevel,
   const std::string& in_message,
   const std::string& in_section,
   const ErrorLocation& in_loggedFrom)
{
   READ_LOCK_BEGIN(Mutex)

   // Don't log this message, it's too detailed for any of the logs.
   if (in_logLevel > MaxLogLevel)
      return;

   LogMap* logMap = &DefaultLogDestinations;
   if (!in_section.empty())
   {
      // override the logger if the destination was configured, otherwise log it
      // to the default log destinations
      if (SectionedLogDestinations.find(in_section) != SectionedLogDestinations.end())
      {
         auto logDestIter = SectionedLogDestinations.find(in_section);
         logMap = &logDestIter->second;
      }
   }

   // Preformat the message for non-syslog loggers.
   const auto destEnd = logMap->end();
   bool prettyFormat = false, jsonFormat = false;
   for (auto iter = logMap->begin(); iter != destEnd; ++iter)
   {
      if (iter->second->getLogMessageFormatType() == LogMessageFormatType::JSON)
         jsonFormat = true;
      else
         prettyFormat = true;
   }

   std::string prettyMessage, jsonMessage;
   if (prettyFormat)
      prettyMessage = formatLogMessage(in_logLevel, in_message, ProgramId, true, in_loggedFrom);
   if (jsonFormat)
      jsonMessage = formatLogMessage(in_logLevel, in_message, ProgramId, false, in_loggedFrom);


   for (auto iter = logMap->begin(); iter != destEnd; ++iter)
   {
      std::string& messageToWrite = iter->second->getLogMessageFormatType() == LogMessageFormatType::PRETTY ?
               prettyMessage : jsonMessage;
      iter->second->writeLog(in_logLevel, messageToWrite);
   }

   RW_LOCK_END(false)
}

// Logging functions
void setProgramId(const std::string& in_programId)
{
   WRITE_LOCK_BEGIN(logger().Mutex)
   {
      if (!logger().ProgramId.empty() && (logger().ProgramId != in_programId))
         logWarningMessage("Changing the program id from " + logger().ProgramId + " to " + in_programId);

      logger().ProgramId = in_programId;
   }
   RW_LOCK_END(false)
}

void addLogDestination(const std::shared_ptr<ILogDestination>& in_destination)
{
   WRITE_LOCK_BEGIN(logger().Mutex)
   {
      LogMap& logMap = logger().DefaultLogDestinations;
      if (logMap.find(in_destination->getId()) == logMap.end())
      {
         logMap.insert(std::make_pair(in_destination->getId(), in_destination));
         if (in_destination->getLogLevel() > logger().MaxLogLevel)
            logger().MaxLogLevel = in_destination->getLogLevel();
         return;
      }
   }
   RW_LOCK_END(false)

   logDebugMessage(
      "Attempted to register a log destination that has already been registered with id " +
      in_destination->getId());
}

void addLogDestination(const std::shared_ptr<ILogDestination>& in_destination, const std::string& in_section)
{
   WRITE_LOCK_BEGIN(logger().Mutex)
   {
      Logger& log = logger();
      SectionLogMap& secLogMap = log.SectionedLogDestinations;
      if (secLogMap.find(in_section) == secLogMap.end())
         secLogMap.insert(std::make_pair(in_section, LogMap()));

      LogMap& logMap = secLogMap.find(in_section)->second;

      if (logMap.find(in_destination->getId()) == logMap.end())
      {
         logMap.insert(std::make_pair(in_destination->getId(), in_destination));
         if (log.MaxLogLevel < in_destination->getLogLevel())
            log.MaxLogLevel = in_destination->getLogLevel();

         return;
      }
   }
   RW_LOCK_END(false)

   logDebugMessage(
      "Attempted to register a log destination that has already been registered with id " +
      in_destination->getId() +
      " to section " +
      in_section);
}

std::string cleanDelimiters(const std::string& in_str)
{
   std::string toClean(in_str);
   std::replace(toClean.begin(), toClean.end(), s_delim, ' ');
   return toClean;
}

void logError(const Error& in_error)
{
   if (!in_error.isExpected())
      logger().writeMessageToDestinations(LogLevel::ERR, in_error.asString());
}

void logError(const Error& in_error, const ErrorLocation& in_location)
{
   if (!in_error.isExpected())
      logger().writeMessageToDestinations(LogLevel::ERR, in_error.asString(), "", in_location);
}

void logErrorAsWarning(const Error& in_error)
{
   if (!in_error.isExpected())
      logger().writeMessageToDestinations(LogLevel::WARN, in_error.asString());
}

void logErrorAsInfo(const Error& in_error)
{
   if (!in_error.isExpected())
      logger().writeMessageToDestinations(LogLevel::INFO, in_error.asString());
}

void logErrorAsDebug(const Error& in_error)
{
   if (!in_error.isExpected())
      logger().writeMessageToDestinations(LogLevel::DEBUG, in_error.asString());
}

void logErrorMessage(const std::string& in_message, const std::string& in_section)
{
   logErrorMessage(in_message, in_section, ErrorLocation());
}

void logErrorMessage(const std::string& in_message, const ErrorLocation& in_loggedFrom)
{
   logErrorMessage(in_message, "", in_loggedFrom);
}

void logErrorMessage(const std::string& in_message, const std::string& in_section, const ErrorLocation& in_loggedFrom)
{
   Logger& log = logger();
   if (log.MaxLogLevel >= LogLevel::ERR)
      log.writeMessageToDestinations(LogLevel::ERR, in_message, in_section, in_loggedFrom);
}

void logWarningMessage(const std::string& in_message, const std::string& in_section)
{
   logWarningMessage(in_message, in_section, ErrorLocation());
}

void logWarningMessage(const std::string& in_message, const ErrorLocation& in_loggedFrom)
{
   logWarningMessage(in_message, "", in_loggedFrom);
}

void logWarningMessage(const std::string& in_message, const std::string& in_section, const ErrorLocation& in_loggedFrom)
{
   Logger& log = logger();
   if (log.MaxLogLevel >= LogLevel::WARN)
      log.writeMessageToDestinations(LogLevel::WARN, in_message, in_section, in_loggedFrom);
}

void logDebugMessage(const std::string& in_message, const std::string& in_section)
{
   logDebugMessage(in_message, in_section, ErrorLocation());
}

void logDebugMessage(const std::string& in_message, const ErrorLocation& in_loggedFrom)
{
   logDebugMessage(in_message, "", in_loggedFrom);
}

void logDebugMessage(const std::string& in_message, const std::string& in_section, const ErrorLocation& in_loggedFrom)
{
   logger().writeMessageToDestinations(LogLevel::DEBUG, in_message, in_section, in_loggedFrom);
}

void logInfoMessage(const std::string& in_message, const std::string& in_section)
{
   logInfoMessage(in_message, in_section, ErrorLocation());
}

void logInfoMessage(const std::string& in_message, const ErrorLocation& in_loggedFrom)
{
   logInfoMessage(in_message, "", in_loggedFrom);
}

void logInfoMessage(const std::string& in_message, const std::string& in_section, const ErrorLocation& in_loggedFrom)
{
   logger().writeMessageToDestinations(LogLevel::INFO, in_message, in_section, in_loggedFrom);
}

bool isLogLevel(LogLevel in_logLevel)
{
   return logger().MaxLogLevel >= in_logLevel;
}

void refreshAllLogDestinations(const log::RefreshParams& in_refreshParams)
{
   Logger& log = logger();

   WRITE_LOCK_BEGIN(log.Mutex)
   {
      for (auto& dest : log.DefaultLogDestinations)
         dest.second->refresh(in_refreshParams);

      for (auto& section : log.SectionedLogDestinations)
      {
         for (auto& dest : section.second)
            dest.second->refresh(in_refreshParams);
      }
   }
   RW_LOCK_END(false);
}

void removeLogDestination(const std::string& in_destinationId, const std::string& in_section)
{
   Logger& log = logger();
   if (in_section.empty())
   {
      // Remove the log from default destinations if it's found. Keep track of whether we find it for logging purposes.
      bool found = false;

      WRITE_LOCK_BEGIN(log.Mutex)
      {
         auto iter = log.DefaultLogDestinations.find(in_destinationId);
         if (iter != log.DefaultLogDestinations.end())
         {
            found = true;
            log.DefaultLogDestinations.erase(iter);
         }

         // Remove it from any sections it may have been registered to.
         std::vector<std::string> sectionsToRemove;
         for (auto secIter: log.SectionedLogDestinations)
         {
            iter = secIter.second.find(in_destinationId);
            if (iter != secIter.second.end())
            {
               found = true;
               secIter.second.erase(iter);
            }

            if (secIter.second.empty())
               sectionsToRemove.push_back(secIter.first);
         }

         // Clean up any empty sections.
         for (const std::string& toRemove: sectionsToRemove)
            log.SectionedLogDestinations.erase(log.SectionedLogDestinations.find(toRemove));
      }
      RW_LOCK_END(false);

      // Log a debug message if this destination wasn't registered.
      if (!found)
      {
         logDebugMessage(
            "Attempted to unregister a log destination that has not been registered with id" +
            in_destinationId);
      }
   }
   else if (log.SectionedLogDestinations.find(in_section) != log.SectionedLogDestinations.end())
   {
      WRITE_LOCK_BEGIN(log.Mutex)
      {
         auto secIter = log.SectionedLogDestinations.find(in_section);
         auto iter = secIter->second.find(in_destinationId);
         if (iter != secIter->second.end())
         {
            secIter->second.erase(iter);
            if (secIter->second.empty())
               log.SectionedLogDestinations.erase(secIter);

            return;
         }
      }
      RW_LOCK_END(false);

      logDebugMessage(
         "Attempted to unregister a log destination that has not been registered to the specified section (" +
         in_section +
         ") with id " +
         in_destinationId);
   }
   else
   {
      logDebugMessage(
         "Attempted to unregister a log destination that has not been registered to the specified section (" +
         in_section +
         ") with id " +
         in_destinationId);
   }
}

void removeReloadableLogDestinations()
{
   WRITE_LOCK_BEGIN(logger().Mutex)
   {
      for (auto iter = logger().DefaultLogDestinations.begin(); iter != logger().DefaultLogDestinations.end();)
      {
         if (iter->second->isReloadable())
            iter = logger().DefaultLogDestinations.erase(iter);
         else
            ++iter;
      }

      for (auto iter = logger().SectionedLogDestinations.begin(); iter != logger().SectionedLogDestinations.end(); ++iter)
      {
         for (auto sectionIter = iter->second.begin(); sectionIter != iter->second.end();)
         {
            if (sectionIter->second->isReloadable())
               sectionIter = iter->second.erase(sectionIter);
            else
               ++sectionIter;
         }
      }
   }
   RW_LOCK_END(false)

   logDebugMessage("Cleared all previously registered log destinations marked as reloadable");
}

std::ostream& writeError(const Error& in_error, std::ostream& io_os)
{
   return io_os << writeError(in_error);
}

std::string writeError(const Error& in_error)
{
   return formatLogMessage(LogLevel::ERR, in_error.asString(), logger().ProgramId, true);
}

namespace {

template <typename T>
bool hasLogDestination()
{
   Logger& log = logger();
   WRITE_LOCK_BEGIN(log.Mutex)
      {
         LogMap* logMap = &log.DefaultLogDestinations;

         const auto destEnd = logMap->end();
         for (auto iter = logMap->begin(); iter != destEnd; ++iter)
         {
            if (std::dynamic_pointer_cast<T, ILogDestination>(iter->second) != nullptr)
               return true;
         }
      }
   RW_LOCK_END(false);
   return false;
}

} // anonymous namespace

bool hasFileLogDestination()
{
   return hasLogDestination<FileLogDestination>();
}

bool hasStderrLogDestination()
{
   return hasLogDestination<StderrLogDestination>();
}

} // namespace log
} // namespace core
} // namespace rstudio

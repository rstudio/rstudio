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

#include <boost/noncopyable.hpp>
#include <boost/date_time/posix_time/posix_time.hpp>
#include <boost/optional.hpp>

#include <sstream>

// We do a little special handling for syslog because it does its own formatting.
#include <shared_core/Error.hpp>
#include <shared_core/DateTime.hpp>
#include <shared_core/ILogDestination.hpp>
#include <shared_core/system/SyslogDestination.hpp>

namespace rstudio {
namespace core {
namespace log {

typedef std::map<unsigned int, std::shared_ptr<ILogDestination> > LogMap;
typedef std::map<LogSection, LogMap> SectionLogMap;

namespace {

constexpr const char* s_occurredAt = "OCCURRED AT";
constexpr const char* s_loggedFrom = "LOGGED FROM";
constexpr const char* s_causedBy = "CAUSED BY";

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
   const ErrorLocation& in_loggedFrom = ErrorLocation(),
   bool in_formatForSyslog = false)
{
   std::ostringstream oss;

   if (!in_formatForSyslog)
   {
      // Add the time.
      using namespace boost::posix_time;
      ptime time = microsec_clock::universal_time();

      oss << system::date_time::format(time, "%d %b %Y %H:%M:%S")
          << " [" << in_programId << "] ";
   }

   oss << in_logLevel << " " << in_message;

   if (in_loggedFrom.hasLocation())
      oss << s_delim << " " << s_loggedFrom << ": " << cleanDelims(in_loggedFrom.asString());

   oss << std::endl;

   if (in_formatForSyslog)
   {
      // Newlines delimit logs in syslog, so replace them with |||
      return boost::replace_all_copy(oss.str(), "\n", "|||");
   }

   return oss.str();
}

} // anonymous namespace

// Log Section =========================================================================================================
struct LogSection::Impl
{
   std::string Name;
   boost::optional<LogLevel> MaxLogLevel;
};

LogSection::LogSection(const std::string& in_name) :
   m_impl(new Impl())
{
   m_impl->Name = in_name;
}

LogSection::LogSection(const std::string& in_name, LogLevel in_logLevel) :
   m_impl(new Impl())
{
   m_impl->Name = in_name;
   m_impl->MaxLogLevel = in_logLevel;
}

bool LogSection::operator<(const LogSection& in_other) const
{
   return m_impl->Name < in_other.m_impl->Name;
}

bool LogSection::operator==(const LogSection& in_other) const
{
   return m_impl->Name == in_other.m_impl->Name;
}

LogLevel LogSection::getMaxLogLevel(LogLevel in_defaultLevel) const
{
   return boost::get_optional_value_or(m_impl->MaxLogLevel, in_defaultLevel);
}

const std::string& LogSection::getName() const
{
   return m_impl->Name;
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
 * @param in_loggedFrom         The location from which the error message was logged.
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
      DefaultLogLevel(LogLevel::OFF),
      ProgramId("")
   { };

   // The maximum level of message to write across all log sections.
   LogLevel MaxLogLevel;

   // The default maximum level of message to write.
   LogLevel DefaultLogLevel;

   // The ID of the program fr which to write logs.
   std::string ProgramId;

   // The registered default log destinations. Any logs without a section or with an unregistered section will be logged
   // to these destinations.
   LogMap DefaultLogDestinations;

   // The registered sectioned log destinations. Any logs with a registered section will be logged to the destinations
   // assigned to that section.
   SectionLogMap SectionedLogDestinations;
};

Logger& logger()
{
   static Logger logger;
   return logger;
}

void Logger::writeMessageToDestinations(
   LogLevel in_logLevel,
   const std::string& in_message,
   const std::string& in_section,
   const ErrorLocation& in_loggedFrom)
{
   LogLevel maxLogLevel = DefaultLogLevel;
   LogMap* logMap = &DefaultLogDestinations;
   if (!in_section.empty())
   {
      // Don't log anything if there is no logger for this section.
      if (SectionedLogDestinations.find(in_section) == SectionedLogDestinations.end())
         return;
      auto logDestIter = SectionedLogDestinations.find(in_section);
      logMap = &logDestIter->second;
      maxLogLevel = logDestIter->first.getMaxLogLevel(maxLogLevel);
   }

   // Don't log this message, it's too detailed for the configuration.
   if (in_logLevel > maxLogLevel)
      return;

   // Preformat the message for non-syslog loggers.
   std::string formattedMessage = formatLogMessage(in_logLevel, in_message, ProgramId, in_loggedFrom);

   const auto destEnd = logMap->end();
   for (auto iter = logMap->begin(); iter != destEnd; ++iter)
   {
#ifndef _WIN32
      if (iter->first == system::SyslogDestination::getSyslogId())
         iter->second->writeLog(in_logLevel, formatLogMessage(in_logLevel, in_message, ProgramId, in_loggedFrom, true));
      else
#endif
         iter->second->writeLog(in_logLevel, formattedMessage);
   }
}

// Logging functions
void setProgramId(const std::string& in_programId)
{
   if (!logger().ProgramId.empty() && (logger().ProgramId != in_programId))
      logWarningMessage("Changing the program id from " + logger().ProgramId + " to " + in_programId);

   logger().ProgramId = in_programId;
}

void setLogLevel(LogLevel in_logLevel)
{
   logger().DefaultLogLevel = in_logLevel;
   if (in_logLevel > logger().MaxLogLevel)
      logger().MaxLogLevel = in_logLevel;
}

void addLogDestination(const std::shared_ptr<ILogDestination>& in_destination)
{
   LogMap& logMap = logger().DefaultLogDestinations;
   if (logMap.find(in_destination->getId()) == logMap.end())
      logMap.insert(std::make_pair(in_destination->getId(), in_destination));
   else {
      logDebugMessage(
         "Attempted to register a log destination that has already been registered with id " +
          std::to_string(in_destination->getId()));
   }
}

void addLogDestination(const std::shared_ptr<ILogDestination>& in_destination, const LogSection& in_section)
{
   Logger& log = logger();
   SectionLogMap& secLogMap =log.SectionedLogDestinations;
   if (secLogMap.find(in_section) == secLogMap.end())
      secLogMap.insert(std::make_pair(in_section, LogMap()));

   LogMap& logMap = secLogMap.find(in_section)->second;

   if (logMap.find(in_destination->getId()) == logMap.end())
      logMap.insert(std::make_pair(in_destination->getId(), in_destination));
   else
   {
      logDebugMessage(
         "Attempted to register a log destination that has already been registered with id " +
         std::to_string(in_destination->getId()) +
         " to section " +
         in_section.getName());
   }

   if (in_section.getMaxLogLevel(log.DefaultLogLevel) > log.MaxLogLevel)
      log.MaxLogLevel = in_section.getMaxLogLevel(log.DefaultLogLevel);
}

std::string cleanDelims(const std::string& in_toClean)
{
   std::string toClean(in_toClean);
   std::replace(toClean.begin(), toClean.end(), s_delim, ' ');
   return toClean;
}

void logError(const Error& in_error)
{
   Logger& log = logger();
   if (log.MaxLogLevel >= LogLevel::ERR)
   {
      log.writeMessageToDestinations(LogLevel::ERR, in_error.asString());
   }
}

void logError(const Error& in_error, const ErrorLocation& in_location)
{
   Logger& log = logger();
   if (log.MaxLogLevel >= LogLevel::ERR)
      log.writeMessageToDestinations(LogLevel::ERR, in_error.asString(), "", in_location);
}

void logErrorAsWarning(const Error& in_error)
{
   Logger& log = logger();
   if (log.MaxLogLevel >= LogLevel::WARN)
   {
      log.writeMessageToDestinations(LogLevel::WARN, in_error.asString());
   }
}

void logErrorAsInfo(const Error& in_error)
{
   Logger& log = logger();
   if (log.MaxLogLevel >= LogLevel::INFO)
   {
      log.writeMessageToDestinations(LogLevel::INFO, in_error.asString());
   }
}

void logErrorAsDebug(const Error& in_error)
{
   Logger& log = logger();
   if (log.MaxLogLevel >= LogLevel::DEBUG)
   {
      log.writeMessageToDestinations(LogLevel::DEBUG, in_error.asString());
   }
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
   Logger& log = logger();
   if (log.MaxLogLevel >= LogLevel::DEBUG)
      log.writeMessageToDestinations(LogLevel::DEBUG, in_message, in_section, in_loggedFrom);
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
   Logger& log = logger();
   if (log.MaxLogLevel >= LogLevel::INFO)
      log.writeMessageToDestinations(LogLevel::INFO, in_message, in_section, in_loggedFrom);
}

void removeLogDestination(unsigned int in_destinationId, const std::string& in_section)
{
   Logger& log = logger();
   if (in_section.empty())
   {
      // Remove the log from default destinations if it's found. Keep track of whether we find it for logging purposes.
      bool found = false;
      auto iter = log.DefaultLogDestinations.find(in_destinationId);
      if (iter != log.DefaultLogDestinations.end())
      {
         found = true;
         log.DefaultLogDestinations.erase(iter);
      }

      // Remove it from any sections it may have been registered to.
      std::vector<LogSection> sectionsToRemove;
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
      for (const LogSection& toRemove: sectionsToRemove)
         log.SectionedLogDestinations.erase(log.SectionedLogDestinations.find(toRemove));

      // Log a debug message if this destination wasn't registered.
      if (!found)
      {
         logDebugMessage(
            "Attempted to unregister a log destination that has not been registered with id" +
            std::to_string(in_destinationId));
      }
   }
   else if (log.SectionedLogDestinations.find(in_section) != log.SectionedLogDestinations.end())
      {
         auto secIter = log.SectionedLogDestinations.find(in_section);
         auto iter = secIter->second.find(in_destinationId);
         if (iter != secIter->second.end())
         {
            secIter->second.erase(iter);
            if (secIter->second.empty())
               log.SectionedLogDestinations.erase(secIter);
         }
         else
         {
            logDebugMessage(
               "Attempted to unregister a log destination that has not been registered to the specified section (" +
               in_section +
               ") with id " +
               std::to_string(in_destinationId));
         }
      }
}

std::ostream& writeError(const Error& in_error, std::ostream& io_os)
{
   return io_os << writeError(in_error);
}

std::string writeError(const Error& in_error)
{
   return formatLogMessage(LogLevel::ERR, in_error.asString(), logger().ProgramId);
}

} // namespace log
} // namespace core
} // namespace rstudio

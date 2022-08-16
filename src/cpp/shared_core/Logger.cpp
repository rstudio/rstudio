/*
 * Logger.cpp
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

#include <shared_core/Logger.hpp>

#include <cassert>
#include <sstream>
#include <typeindex>
#include <unordered_map>

#include <boost/algorithm/string.hpp>
#include <boost/noncopyable.hpp>
#include <boost/optional.hpp>

#include <shared_core/DateTime.hpp>
#include <shared_core/Error.hpp>
#include <shared_core/FileLogDestination.hpp>
#include <shared_core/json/Json.hpp>
#include <shared_core/ReaderWriterMutex.hpp>
#include <shared_core/SafeConvert.hpp>
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

LogLevel logLevelFromStr(const std::string& in_str)
{
   if (in_str == "ERROR")
      return LogLevel::ERR;
   else if (in_str == "WARNING")
      return LogLevel::WARN;
   else if (in_str == "INFO")
      return LogLevel::INFO;
   else
      return LogLevel::DEBUG;
}

json::Object errorLocationToJson(const ErrorLocation& in_location)
{
   json::Object errorLocation;
   errorLocation["file"] = in_location.getFile();
   errorLocation["function"] = in_location.getFunction();
   errorLocation["lineNumber"] = static_cast<int64_t>(in_location.getLine());
   return errorLocation;
}

json::Object errorToJson(const Error& in_error)
{
   json::Object error;
   if (in_error.hasCause())
      error["cause"] = errorToJson(in_error.getCause());

   error["code"] = in_error.getCode();

   if (in_error.getLocation().hasLocation())
      error["location"] = errorLocationToJson(in_error.getLocation());

   error["message"] = in_error.getMessage();

   if (!in_error.getName().empty())
      error["type"] = in_error.getName();

   const ErrorProperties& props = in_error.getProperties();
   if (!props.empty())
   {
      json::Object properties;
      for (const auto& prop : props)
         properties[prop.first] = prop.second;
      error["properties"] = properties;
   }

   return error;
}

std::unordered_map<std::type_index, boost::function<void(json::Object&, const std::pair<std::string, boost::any>&)>> s_logMessagePropertiesToJsonMap =
{
   {typeid(int), [](json::Object& obj, const std::pair<std::string, boost::any>& prop){obj[prop.first] = boost::any_cast<int>(prop.second);}},
   {typeid(uint64_t), [](json::Object& obj, const std::pair<std::string, boost::any>& prop){obj[prop.first] = boost::any_cast<uint64_t>(prop.second);}},
   {typeid(uint32_t), [](json::Object& obj, const std::pair<std::string, boost::any>& prop){obj[prop.first] = boost::any_cast<uint32_t>(prop.second);}},
   {typeid(float), [](json::Object& obj, const std::pair<std::string, boost::any>& prop){obj[prop.first] = boost::any_cast<float>(prop.second);}},
   {typeid(double), [](json::Object& obj, const std::pair<std::string, boost::any>& prop){obj[prop.first] = boost::any_cast<double>(prop.second);}},
   {typeid(bool), [](json::Object& obj, const std::pair<std::string, boost::any>& prop){obj[prop.first] = boost::any_cast<bool>(prop.second);}},
   {typeid(std::string), [](json::Object& obj, const std::pair<std::string, boost::any>& prop){obj[prop.first] = boost::any_cast<std::string>(prop.second);}},
   {typeid(const char*), [](json::Object& obj, const std::pair<std::string, boost::any>& prop){obj[prop.first] = boost::any_cast<const char*>(prop.second);}},
   {typeid(char*), [](json::Object& obj, const std::pair<std::string, boost::any>& prop){obj[prop.first] = boost::any_cast<char*>(prop.second);}},
   {typeid(json::Object), [](json::Object& obj, const std::pair<std::string, boost::any>& prop){obj[prop.first] = boost::any_cast<json::Object>(prop.second);}},
   {typeid(json::Array), [](json::Object& obj, const std::pair<std::string, boost::any>& prop){obj[prop.first] = boost::any_cast<json::Array>(prop.second);}},
   {typeid(Error), [](json::Object& obj, const std::pair<std::string, boost::any>& prop){obj[prop.first] = errorToJson(boost::any_cast<Error>(prop.second));}}
};

json::Object logMessagePropertiesToJson(const LogMessageProperties& in_properties)
{
   json::Object obj;

   for (const auto& prop : in_properties)
   {
      const std::type_info& tinfo = prop.second.type();
      auto iter = s_logMessagePropertiesToJsonMap.find(std::type_index(tinfo));
      if (iter != s_logMessagePropertiesToJsonMap.end())
      {
         try
         {
            iter->second(obj, prop);
         }
         catch (const boost::bad_any_cast&)
         {
            // an unsupported property type is being logged
            BOOST_ASSERT(false);
         }
      }
      else
      {
         // type does not match registered type converters
         BOOST_ASSERT(false);
      }
   }

   return obj;
}

std::unordered_map<std::type_index, boost::function<std::string(const std::pair<std::string, boost::any>&)>> s_logMessagePropertiesToStringMap =
{
   {typeid(int), [](const std::pair<std::string, boost::any>& prop){return safe_convert::numberToString(boost::any_cast<int>(prop.second));}},
   {typeid(uint64_t), [](const std::pair<std::string, boost::any>& prop){return safe_convert::numberToString(boost::any_cast<uint64_t>(prop.second));}},
   {typeid(uint32_t), [](const std::pair<std::string, boost::any>& prop){return safe_convert::numberToString(boost::any_cast<uint32_t>(prop.second));}},
   {typeid(float), [](const std::pair<std::string, boost::any>& prop){return safe_convert::numberToString(boost::any_cast<float>(prop.second));}},
   {typeid(double), [](const std::pair<std::string, boost::any>& prop){return safe_convert::numberToString(boost::any_cast<double>(prop.second));}},
   {typeid(bool), [](const std::pair<std::string, boost::any>& prop){return boost::any_cast<bool>(prop.second) ? "true" : "false";}},
   {typeid(std::string), [](const std::pair<std::string, boost::any>& prop){return boost::any_cast<std::string>(prop.second);}},
   {typeid(const char*), [](const std::pair<std::string, boost::any>& prop){return boost::any_cast<const char*>(prop.second);}},
   {typeid(char*), [](const std::pair<std::string, boost::any>& prop){return boost::any_cast<char*>(prop.second);}},
   {typeid(json::Object), [](const std::pair<std::string, boost::any>& prop){return boost::any_cast<json::Object>(prop.second).write();}},
   {typeid(json::Array), [](const std::pair<std::string, boost::any>& prop){return boost::any_cast<json::Array>(prop.second).write();}},
   {typeid(Error), [](const std::pair<std::string, boost::any>& prop){return errorToJson(boost::any_cast<Error>(prop.second)).write();}}
};

std::string logMessagePropertiesToString(const LogMessageProperties& in_properties)
{
   std::vector<std::string> properties;
   for (const auto& prop : in_properties)
   {
      const std::type_info& tinfo = prop.second.type();
      auto iter = s_logMessagePropertiesToStringMap.find(std::type_index(tinfo));
      if (iter != s_logMessagePropertiesToStringMap.end())
      {
         try
         {
            std::string val = iter->second(prop);
            properties.push_back(prop.first + ": " + val);
         }
         catch (const boost::bad_any_cast&)
         {
            // an unsupported property type is being logged
            BOOST_ASSERT(false);
         }
      }
      else
      {
         // type does not match registered type converters
         BOOST_ASSERT(false);
      }
   }

   std::string propStr = "[" + boost::join(properties, ", ") + "]";

   // replace newlines - there should be no newlines within a log line
   boost::algorithm::replace_all(propStr, "\n", "|||");

   return propStr;
}

std::string formatLogMessage(
   LogLevel in_logLevel,
   const std::string& in_message,
   const std::string& in_programId,
   bool humanReadableFormat,
   const boost::optional<LogMessageProperties>& in_properties,
   const ErrorLocation& in_loggedFrom = ErrorLocation(),
   const Error& in_error = Success())
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

      if (in_error)
         oss << in_logLevel << " " << in_error.asString();
      else
         oss << in_logLevel << " " << message;

      if (in_properties)
      {
          std::string properties = logMessagePropertiesToString(in_properties.get());
          oss << " " << properties;
      }

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

      if (in_error)
         logObject["error"] = errorToJson(in_error);
      else
         logObject["message"] = message;

      if (in_properties)
      {
         json::Object properties = logMessagePropertiesToJson(in_properties.get());
         logObject["properties"] = properties;
      }

      if (in_loggedFrom.hasLocation())
         logObject["location"] = errorLocationToJson(in_loggedFrom);

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
    * @param in_properties      The LogMessageProperties to log with the message.
    * @param in_loggedFrom      The location from which the error message was logged.
    * @param in_error           The error (if any) to log.
    */
   void writeMessageToDestinations(
      LogLevel in_logLevel,
      const std::string& in_message,
      const std::string& in_section = std::string(),
      const boost::optional<LogMessageProperties>& in_properties = boost::none,
      const ErrorLocation& in_loggedFrom = ErrorLocation(),
      const Error& in_error = Success());

   /**
    * @brief Writes a message to all registered destinations by invoking an action if any configured loggers are of the right log level.
    *
    * @param in_logLevel        The log level of the message, which is passed to the destination for informational purposes.
    * @param in_action          The action to generate the message.
    * @param in_section         The section to which to log this message.
    * @param in_properties      The LogMessageProperties to log with the message.
    * @param in_loggedFrom      The location from which the error message was logged.
    * @param in_error           The error (if any) to log.
    */
   void writeMessageToDestinations(
      LogLevel in_logLevel,
      const boost::function<std::string(boost::optional<LogMessageProperties>*)>& in_action,
      const std::string& in_section = std::string(),
      const ErrorLocation& in_loggedFrom = ErrorLocation(),
      const Error& in_error = Success());

   /**
    * @brief Writes a fully formed log message that was previously logged by a different source to all registered destinations.
    *
    * @param in_source          The source of the fully formed log message.
    * @param in_message         The full log message.
    */
   void writePassthroughMessageToDestinations(const std::string& in_source,
                                              const std::string& in_message);

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
   const boost::function<std::string(boost::optional<LogMessageProperties>*)>& in_action,
   const std::string& in_section,
   const ErrorLocation& in_loggedFrom,
   const Error& in_error)
{
   READ_LOCK_BEGIN(Mutex)

   // Don't log this message, it's too detailed for any of the logs.
   if (in_logLevel > MaxLogLevel)
      return;

   RW_LOCK_END(false)

   boost::optional<LogMessageProperties> props = boost::none;
   std::string message = in_action(&props);
   writeMessageToDestinations(in_logLevel, message, in_section, props, in_loggedFrom, in_error);
}

void Logger::writeMessageToDestinations(
   LogLevel in_logLevel,
   const std::string& in_message,
   const std::string& in_section,
   const boost::optional<LogMessageProperties>& in_properties,
   const ErrorLocation& in_loggedFrom,
   const Error& in_error)
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
      prettyMessage = formatLogMessage(in_logLevel, in_message, ProgramId, true, in_properties, in_loggedFrom, in_error);
   if (jsonFormat)
      jsonMessage = formatLogMessage(in_logLevel, in_message, ProgramId, false, in_properties, in_loggedFrom, in_error);


   for (auto iter = logMap->begin(); iter != destEnd; ++iter)
   {
      std::string& messageToWrite = iter->second->getLogMessageFormatType() == LogMessageFormatType::PRETTY ?
               prettyMessage : jsonMessage;
      iter->second->writeLog(in_logLevel, messageToWrite);
   }

   RW_LOCK_END(false)
}

void Logger::writePassthroughMessageToDestinations(const std::string& in_source,
                                                   const std::string& in_message)
{
   std::string logMessage;
   std::string logLevel;

   // determine format of the log by attempting to parse it into json
   json::Object jsonLogEntry;
   if (!jsonLogEntry.parse(in_message))
   {
      // add the source as a parameter on the json
      jsonLogEntry["logSource"] = in_source;
      logMessage = jsonLogEntry.write() + "\n";

      json::readObject(jsonLogEntry, "level", logLevel);

      // if there's no log level defined for some reason, don't bother logging the message
      if (logLevel.empty())
         return;
   }
   else
   {
      // add the source immediately after the binary name
      logMessage = in_message;
      size_t braceIndex = logMessage.find(']');
      if (braceIndex != std::string::npos)
      {
         logMessage.insert(braceIndex, ", log-source: " + in_source);

         try
         {
            braceIndex = logMessage.find(']');
            size_t spaceIndex = logMessage.find(' ', braceIndex + 2);
            logLevel = logMessage.substr(braceIndex + 2, spaceIndex - braceIndex - 2);
         }
         catch (const std::out_of_range&)
         {
            // Since this is logging code and we're trying to relog a message from another source,
            // just don't do anything if we cannot properly parse the log message
            return;
         }
      }
   }

   LogLevel level = logLevelFromStr(logLevel);

   READ_LOCK_BEGIN(Mutex)

   LogMap* logMap = &DefaultLogDestinations;
   const auto destEnd = logMap->end();
   for (auto iter = logMap->begin(); iter != destEnd; ++iter)
   {
      iter->second->writeLog(level, logMessage);
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
      logger().writeMessageToDestinations(LogLevel::ERR, std::string(), std::string(), boost::none, ErrorLocation(), in_error);
}

void logError(const Error& in_error, const ErrorLocation& in_location)
{
   if (!in_error.isExpected())
      logger().writeMessageToDestinations(LogLevel::ERR, std::string(), "", boost::none, in_location, in_error);
}

void logErrorAsWarning(const Error& in_error)
{
   if (!in_error.isExpected())
      logger().writeMessageToDestinations(LogLevel::WARN, std::string(), "", boost::none, ErrorLocation(), in_error);
}

void logErrorAsInfo(const Error& in_error)
{
   if (!in_error.isExpected())
      logger().writeMessageToDestinations(LogLevel::INFO, std::string(), "", boost::none, ErrorLocation(), in_error);
}

void logErrorAsDebug(const Error& in_error)
{
   if (!in_error.isExpected())
      logger().writeMessageToDestinations(LogLevel::DEBUG, std::string(), "", boost::none, ErrorLocation(), in_error);
}

void logErrorMessage(const std::string& in_message, const std::string& in_section)
{
   logErrorMessage(in_message, in_section, boost::none, ErrorLocation());
}

void logErrorMessage(const std::string& in_message, const ErrorLocation& in_loggedFrom)
{
   logErrorMessage(in_message, "", boost::none, in_loggedFrom);
}

void logErrorMessage(const std::string& in_message,
                     const std::string& in_section,
                     const boost::optional<LogMessageProperties>& in_properties,
                     const ErrorLocation& in_loggedFrom)
{
   Logger& log = logger();
   if (log.MaxLogLevel >= LogLevel::ERR)
      log.writeMessageToDestinations(LogLevel::ERR, in_message, in_section, in_properties, in_loggedFrom);
}

void logWarningMessage(const std::string& in_message, const std::string& in_section)
{
   logWarningMessage(in_message, in_section, boost::none, ErrorLocation());
}

void logWarningMessage(const std::string& in_message, const ErrorLocation& in_loggedFrom)
{
   logWarningMessage(in_message, "", boost::none, in_loggedFrom);
}

void logWarningMessage(const std::string& in_message,
                       const std::string& in_section,
                       const boost::optional<LogMessageProperties>& in_properties,
                       const ErrorLocation& in_loggedFrom)
{
   Logger& log = logger();
   if (log.MaxLogLevel >= LogLevel::WARN)
      log.writeMessageToDestinations(LogLevel::WARN, in_message, in_section, in_properties, in_loggedFrom);
}

void logDebugMessage(const std::string& in_message, const std::string& in_section)
{
   logDebugMessage(in_message, in_section, boost::none, ErrorLocation());
}

void logDebugMessage(const std::string& in_message, const ErrorLocation& in_loggedFrom)
{
   logDebugMessage(in_message, "", boost::none, in_loggedFrom);
}

void logDebugMessage(const std::string& in_message,
                     const std::string& in_section,
                     const boost::optional<LogMessageProperties>& in_properties,
                     const ErrorLocation& in_loggedFrom)
{
   logger().writeMessageToDestinations(LogLevel::DEBUG, in_message, in_section, in_properties, in_loggedFrom);
}

void logDebugAction(const boost::function<std::string(boost::optional<LogMessageProperties>*)>& in_action,
                     const std::string& in_section)
{
   logger().writeMessageToDestinations(LogLevel::DEBUG, in_action, in_section, ErrorLocation());
}

void logInfoMessage(const std::string& in_message, const std::string& in_section)
{
   logInfoMessage(in_message, in_section, boost::none, ErrorLocation());
}

void logInfoMessage(const std::string& in_message, const ErrorLocation& in_loggedFrom)
{
   logInfoMessage(in_message, "", boost::none, in_loggedFrom);
}

void logInfoMessage(const std::string& in_message,
                    const std::string& in_section,
                    const boost::optional<LogMessageProperties>& in_properties,
                    const ErrorLocation& in_loggedFrom)
{
   logger().writeMessageToDestinations(LogLevel::INFO, in_message, in_section, in_properties, in_loggedFrom);
}

void logPassthroughMessage(const std::string& in_source, const std::string& in_message)
{
   logger().writePassthroughMessageToDestinations(in_source, in_message);
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
   return formatLogMessage(LogLevel::ERR, in_error.asString(), logger().ProgramId, true, boost::none);
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

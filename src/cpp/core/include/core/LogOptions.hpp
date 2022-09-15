/*
 * LogOptions.hpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef CORE_LOG_OPTIONS_HPP
#define CORE_LOG_OPTIONS_HPP

#include <string>

#include <boost/variant.hpp>

#include <core/ConfigProfile.hpp>
#include <core/Log.hpp>
#include <core/Thread.hpp>

#include <core/system/Types.hpp>

#include <shared_core/FilePath.hpp>
#include <shared_core/FileLogDestination.hpp>

namespace rstudio {
namespace core {
namespace log {

struct StdErrLogOptions
{
};

struct SysLogOptions
{
};

typedef boost::variant<StdErrLogOptions,
   SysLogOptions,
   FileLogOptions> LoggerOptions;

class LogOptions
{
public:
   LogOptions(const std::string& executableName);

   LogOptions(const std::string& executableName,
              LogLevel defaultLogLevel,
              LoggerType defaultLoggerType,
              LogMessageFormatType defaultMessageFormatType,
              const LoggerOptions& defaultLoggerOptions);

   virtual ~LogOptions()
   { }

   core::Error read();

   // gets the current log message format type
   LogMessageFormatType logMessageFormatType(const std::string& loggerName = std::string()) const;

   // gets the current log level
   LogLevel logLevel(const std::string& loggerName = std::string()) const;

   // gets the lowest log level defined
   LogLevel lowestLogLevel() const;

   // gets the current logger type
   LoggerType loggerType(const std::string& loggerName = std::string()) const;

   // gets the current logger's specific options
   LoggerOptions loggerOptions(const std::string& loggerName = std::string()) const;

   std::vector<std::string> loggerOverrides() const;

   static FilePath defaultLogDirectory();

   FilePath getLogConfigFile();
   
private:
   void initProfile();

   void setLowestLogLevel();

   std::vector<ConfigProfile::Level> getLevels(const std::string& loggerName) const;

   std::string executableName_;

   std::string defaultLogLevel_;
   std::string defaultLoggerType_;
   std::string defaultMessageFormatType_;
   LoggerOptions defaultLoggerOptions_;

   LogLevel lowestLogLevel_;

   ConfigProfile profile_;
};

void forwardLogOptionsEnvVars(core::system::Options* pEnvironment);

} // namespace log
} // namespace core
} // namespace rstudio

#endif // CORE_LOG_OPTIONS_HPP



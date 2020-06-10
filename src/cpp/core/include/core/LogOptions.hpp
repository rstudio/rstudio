/*
 * LogOptions.hpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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

#ifndef CORE_LOG_OPTIONS_HPP
#define CORE_LOG_OPTIONS_HPP

#include <string>

#include <boost/variant.hpp>

#include <core/ConfigProfile.hpp>
#include <core/Log.hpp>
#include <core/Thread.hpp>

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
              const LoggerOptions& defaultLoggerOptions);

   virtual ~LogOptions()
   { }

   core::Error read();

   // gets the current log level
   LogLevel logLevel(const std::string& loggerName = std::string()) const;

   // gets the lowest log level defined
   LogLevel lowestLogLevel() const;

   // gets the current logger type
   LoggerType loggerType(const std::string& loggerName = std::string()) const;

   // gets the current logger's specific options
   LoggerOptions loggerOptions(const std::string& loggerName = std::string()) const;

   std::vector<std::string> loggerOverrides() const;

private:
   void initProfile();

   void setLowestLogLevel();

   std::vector<ConfigProfile::Level> getLevels(const std::string& loggerName) const;

   std::string executableName_;

   std::string defaultLogLevel_;
   std::string defaultLoggerType_;
   LoggerOptions defaultLoggerOptions_;

   LogLevel lowestLogLevel_;

   ConfigProfile profile_;
};

} // namespace log
} // namespace core
} // namespace rstudio

#endif // CORE_LOG_OPTIONS_HPP



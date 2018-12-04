/*
 * LogOptions.hpp
 *
 * Copyright (C) 2018 by RStudio, Inc.
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

#include <boost/variant.hpp>

#include <core/ConfigProfile.hpp>
#include <core/FilePath.hpp>
#include <core/Log.hpp>
#include <core/Thread.hpp>

namespace rstudio {
namespace core {

struct StdErrLoggerOptions
{
};

struct SysLoggerOptions
{
};

struct FileLoggerOptions
{
   static constexpr const char* defaultFileMode = "666";
   static constexpr int defaultMaxSizeMb = 2;
   static constexpr bool defaultRotate = true;
   static constexpr bool defaultIncludePid = false;

   FileLoggerOptions();

   FileLoggerOptions(const FilePath& logDir) :
      FileLoggerOptions(logDir, defaultFileMode, defaultMaxSizeMb, defaultRotate, defaultIncludePid)
   {
   }

   FileLoggerOptions(const FilePath& logDir,
                     const std::string& fileMode,
                     double maxSizeMb,
                     bool rotate,
                     bool includePid) :
      logDir(logDir),
      fileMode(fileMode),
      maxSizeMb(maxSizeMb),
      rotate(rotate),
      includePid(includePid)
   {
   }

   FilePath logDir;
   std::string fileMode;
   double maxSizeMb;
   bool rotate;
   bool includePid;
};

typedef boost::variant<StdErrLoggerOptions,
                       SysLoggerOptions,
                       FileLoggerOptions> LoggerOptions;

class LogOptions
{
public:
   LogOptions(const std::string& executableName);

   LogOptions(const std::string& executableName,
              int defaultLogLevel,
              int defaultLoggerType,
              const LoggerOptions& defaultLoggerOptions);

   virtual ~LogOptions() {}

   core::Error read();

   // gets the current log level
   int logLevel(const std::string& loggerName = std::string()) const;

   // gets the lowest log level defined
   int lowestLogLevel() const;

   // gets the current logger type
   int loggerType(const std::string& loggerName = std::string()) const;

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

   int lowestLogLevel_;

   ConfigProfile profile_;
};

} // namespace core
} // namespace rstudio

#endif // CORE_LOG_OPTIONS_HPP



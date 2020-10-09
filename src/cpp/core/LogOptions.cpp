/*
 * LogOptions.cpp
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

#include <core/LogOptions.hpp>

#include <vector>

#include <core/system/Environment.hpp>
#include <core/system/System.hpp>
#include <core/system/Xdg.hpp>

#include "config.h"


namespace rstudio {
namespace core {
namespace log {

#define kLogLevel          "log-level"
#define kLoggerType        "logger-type"
#define kLogDir            "log-dir"
#define kLogFileMode       "log-file-mode"
#define kLogFileIncludePid "log-file-include-pid"
#define kRotate            "rotate"
#define kMaxSizeMb         "max-size-mb"
#define kLogConfFile       "logging.conf"
#define kLogConfEnvVar     "RS_LOG_CONF_FILE"

#define kFileLogger        "file"
#define kStdErrLogger      "stderr"
#define kSysLogger         "syslog"

#define kLoggingLevelDebug "debug"
#define kLoggingLevelInfo  "info"
#define kLoggingLevelWarn  "warn"
#define kLoggingLevelError "error"

#define kBaseLevel       0
#define kBinaryLevel     1
#define kLogSectionLevel 2

namespace {

// pick default log path location
#ifdef RSTUDIO_SERVER
const FilePath kDefaultLogPath("/var/log/rstudio-server");
#else
// desktop - store logs under user dir
const FilePath kDefaultLogPath = core::system::xdg::userDataDir().completePath("log");
#endif

std::string logLevelToString(LogLevel logLevel)
{
   switch (logLevel)
   {
      case LogLevel::DEBUG:
         return kLoggingLevelDebug;
      case LogLevel::INFO:
         return kLoggingLevelInfo;
      case LogLevel::WARN:
         return kLoggingLevelWarn;
      case LogLevel::ERR:
         return kLoggingLevelError;
      case LogLevel::OFF:
      default:
         return kLoggingLevelWarn;
   }
}

std::string loggerTypeToString(LoggerType loggerType)
{
   switch (loggerType)
   {
      case LoggerType::kFile:
         return kFileLogger;
      case LoggerType::kStdErr:
         return kStdErrLogger;
      case LoggerType::kSysLog:
         return kSysLogger;
      default:
         return kSysLogger;
   }
}

LogLevel strToLogLevel(const std::string& logLevelStr)
{
   if (logLevelStr == kLoggingLevelWarn)
      return LogLevel::WARN;
   else if (logLevelStr == kLoggingLevelError)
       return LogLevel::ERR;
   else if (logLevelStr == kLoggingLevelInfo)
       return LogLevel::INFO;
   else if (logLevelStr == kLoggingLevelDebug)
       return LogLevel::DEBUG;
   else
       return LogLevel::WARN;
}

LoggerType strToLoggerType(const std::string& loggerTypeStr)
{
   if (loggerTypeStr == kSysLogger)
      return LoggerType::kSysLog;
   else if (loggerTypeStr == kFileLogger)
      return LoggerType::kFile;
   else if (loggerTypeStr == kStdErrLogger)
      return LoggerType::kStdErr;
   else
      return LoggerType::kSysLog;
}

struct LoggerOptionsVisitor : boost::static_visitor<>
{
   LoggerOptionsVisitor(ConfigProfile& profile) :
      profile_(profile)
   {
   }

   void setDefaultFileLoggerOptions()
   {
      FileLogOptions defaultOptions(kDefaultLogPath);
      profile_.addParams(
         kLogDir, defaultOptions.getDirectory().getAbsolutePath(),
         kLogFileMode, defaultOptions.getFileMode(),
         kRotate, defaultOptions.doRotation(),
         kLogFileIncludePid, defaultOptions.includePid(),
         kMaxSizeMb, defaultOptions.getMaxSizeMb());
   }

   void operator()(const StdErrLogOptions& options)
   {
      setDefaultFileLoggerOptions();
   }

   void operator()(const SysLogOptions& options)
   {
      setDefaultFileLoggerOptions();
   }

   void operator()(const FileLogOptions& options)
   {
      // set file logger option defaults to those that were passed in
      profile_.addParams(
         kLogDir, options.getDirectory().getAbsolutePath(),
         kRotate, options.doRotation(),
         kMaxSizeMb, options.getMaxSizeMb(),
         kLogFileIncludePid, options.includePid(),
         kLogFileMode, options.getFileMode());
   }

   ConfigProfile& profile_;
};

} // anonymous namespace


LogOptions::LogOptions(const std::string& executableName) :
   executableName_(executableName),
   defaultLogLevel_(logLevelToString(LogLevel::WARN)),
   defaultLoggerType_(loggerTypeToString(LoggerType::kSysLog)),
   defaultLoggerOptions_(SysLogOptions()),
   lowestLogLevel_(LogLevel::WARN)
{
   initProfile();
}

LogOptions::LogOptions(const std::string& executableName,
                       LogLevel logLevel,
                       LoggerType loggerType,
                       const LoggerOptions& options) :
   executableName_(executableName),
   defaultLogLevel_(logLevelToString(logLevel)),
   defaultLoggerType_(loggerTypeToString(loggerType)),
   defaultLoggerOptions_(options),
   lowestLogLevel_(logLevel)
{
   initProfile();
}

void LogOptions::initProfile()
{
   // base level - * (applies to all loggers/binaries)
   // first override - @ (specific binary)
   // second override - (logger name)
   profile_.addSections(
      {{ kBaseLevel,       "*" },
       { kBinaryLevel,     "@" },
       { kLogSectionLevel, std::string() }});

   // add base params
   profile_.addParams(
      kLogLevel, defaultLogLevel_,
      kLoggerType, defaultLoggerType_);

   // add logger-specific params
   LoggerOptionsVisitor visitor(profile_);
   boost::apply_visitor(visitor, defaultLoggerOptions_);
}

Error LogOptions::read()
{
   // first, look for config file in a specific environment variable
   FilePath optionsFile(core::system::getenv(kLogConfEnvVar));
   if (!optionsFile.exists())
   {
   #ifdef RSTUDIO_SERVER
      optionsFile = core::system::xdg::systemConfigFile(kLogConfFile);
   #else
      // desktop - read user file first, and only read admin file if the user file does not exist
      optionsFile = core::system::xdg::userConfigDir().completeChildPath(kLogConfFile);
      if (!optionsFile.exists())
            optionsFile = core::system::xdg::systemConfigFile(kLogConfFile);
   #endif
   }

   // if the options file does not exist, that's fine - we'll just use default values
   if (!optionsFile.exists())
      return Success();

   Error error = profile_.load(optionsFile);
   if (error)
      return error;

   setLowestLogLevel();

   return Success();
}

void LogOptions::setLowestLogLevel()
{
   // first, set the log level for this particular binary
   std::string logLevel;
   profile_.getParam(
      kLogLevel, &logLevel, {{ kBaseLevel,   std::string() },
                             { kBinaryLevel, executableName_ }});
   lowestLogLevel_ = strToLogLevel(logLevel);

   // break out early if we are already at debug level (since we cannot go lower)
   if (lowestLogLevel_ == LogLevel::DEBUG)
      return;

   // now, override it with the lowest log level specified for named loggers
   std::vector<std::string> sectionNames = profile_.getLevelNames(kLogSectionLevel);
   for (const std::string& name : sectionNames)
   {
      profile_.getParam(
         kLogLevel, &logLevel, {{ kBaseLevel,       std::string() },
                                { kBinaryLevel,     executableName_ },
                                { kLogSectionLevel, name }});
      LogLevel level = strToLogLevel(logLevel);
      if (level > lowestLogLevel_)
         lowestLogLevel_ = level;

      if (lowestLogLevel_ == LogLevel::DEBUG)
         return;
   }
}

LogLevel LogOptions::logLevel(const std::string& loggerName) const
{
   std::vector<ConfigProfile::Level> levels = getLevels(loggerName);

   std::string logLevel = defaultLogLevel_;

   profile_.getParam(kLogLevel, &logLevel, levels);

   return strToLogLevel(logLevel);
}

LogLevel LogOptions::lowestLogLevel() const
{
   return lowestLogLevel_;
}

LoggerType LogOptions::loggerType(const std::string& loggerName) const
{
   std::vector<ConfigProfile::Level> levels = getLevels(loggerName);

   std::string loggerType = defaultLoggerType_;

   profile_.getParam(kLoggerType, &loggerType, levels);

   return strToLoggerType(loggerType);
}

LoggerOptions LogOptions::loggerOptions(const std::string& loggerName) const
{
   LoggerType type = loggerType(loggerName);

   switch (type)
   {
      case LoggerType::kFile:
      {
         std::vector<ConfigProfile::Level> levels = getLevels(loggerName);

         std::string logDir, fileMode;
         bool rotate, includePid;
         double maxSizeMb;

         profile_.getParam(kLogDir, &logDir, levels);
         profile_.getParam(kRotate, &rotate, levels);
         profile_.getParam(kMaxSizeMb, &maxSizeMb, levels);
         profile_.getParam(kLogFileIncludePid, &includePid, levels);
         profile_.getParam(kLogFileMode, &fileMode, levels);

         return FileLogOptions(FilePath(logDir), fileMode, maxSizeMb, rotate, includePid);
      }

      case LoggerType::kStdErr:
         return StdErrLogOptions();

      case LoggerType::kSysLog:
         return SysLogOptions();

      default:
         return SysLogOptions();
   }
}

std::vector<ConfigProfile::Level> LogOptions::getLevels(const std::string& loggerName) const
{
   std::vector<ConfigProfile::Level> levels = {{ kBaseLevel,   std::string() },
                                               { kBinaryLevel, executableName_ }};
   if (!loggerName.empty())
      levels.emplace_back(kLogSectionLevel, loggerName);
   return levels;
}

std::vector<std::string> LogOptions::loggerOverrides() const
{
   return profile_.getLevelNames(kLogSectionLevel);
}

} // namespace log
} // namespace core
} // namespace rstudio

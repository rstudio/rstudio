/*
 * LogOptions.cpp
 *
 * Copyright (C) 2022 by RStudio, PBC
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

#include <boost/format.hpp>

#include <core/system/Environment.hpp>
#include <core/system/System.hpp>
#include <core/system/Xdg.hpp>

#include "config.h"


namespace rstudio {
namespace core {
namespace log {

#define kLogLevel          "log-level"
#define kLoggerType        "logger-type"
#define kLogMessageFormat  "log-message-format"
#define kLogDir            "log-dir"
#define kLogFileMode       "log-file-mode"
#define kLogFileIncludePid "log-file-include-pid"
#define kRotate            "rotate"
#define kMaxSizeMb         "max-size-mb"
#define kRotateDays        "rotate-days"
#define kMaxRotations      "max-rotations"
#define kDeleteDays        "delete-days"
#define kWarnSyslog        "warn-syslog"
#define kLogConfFile       "logging.conf"

#define kLogLevelEnvVar    "RS_LOG_LEVEL"
#define kLogTypeEnvVar     "RS_LOGGER_TYPE"
#define kLogFormatEnvVar   "RS_LOG_MESSAGE_FORMAT"
#define kLogDirEnvVar      "RS_LOG_DIR"
#define kLogConfEnvVar     "RS_LOG_CONF_FILE"

#define kFileLogger        "file"
#define kStdErrLogger      "stderr"
#define kSysLogger         "syslog"

#define kLogMessageFormatPretty  "pretty"
#define kLogMessageFormatJson    "json"

#define kLoggingLevelDebug "debug"
#define kLoggingLevelInfo  "info"
#define kLoggingLevelWarn  "warn"
#define kLoggingLevelError "error"

#define kBaseLevel         0
#define kBinaryLevel       1
#define kLogSectionLevel   2

namespace {

// pick default log path location

FilePath defaultLogPathImpl()
{
#ifdef RSTUDIO_SERVER
#ifdef RSTUDIO_PRO_BUILD
   return FilePath(RSTUDIO_DEFAULT_LOG_PATH);
#else
   // For open-source Server, support logging even if RStudio Server is run
   // as a non-root user.
   if (core::system::effectiveUserIsRoot())
   {
      // server: root uses default documented logging directory
      return FilePath(RSTUDIO_DEFAULT_LOG_PATH);
   }
   else
   {
      // server: prefer user data directory if we're not running as root
      return core::system::xdg::userDataDir().completePath("log");
   }
#endif
#else
   // desktop: always stored in user data directory
   return core::system::xdg::userDataDir().completePath("log");
#endif
}

FilePath& defaultLogPath()
{
   static FilePath instance = defaultLogPathImpl();
   return instance;
}


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

std::string logMessageFormatTypeToString(LogMessageFormatType formatType)
{
   switch (formatType)
   {
      case LogMessageFormatType::JSON:
         return kLogMessageFormatJson;
      case LogMessageFormatType::PRETTY:
      default:
         return kLogMessageFormatPretty;
   }
}

LogLevel strToLogLevel(const std::string& logLevelStr)
{
   if (boost::iequals(logLevelStr, kLoggingLevelWarn))
      return LogLevel::WARN;
   else if (boost::iequals(logLevelStr, kLoggingLevelError))
       return LogLevel::ERR;
   else if (boost::iequals(logLevelStr, kLoggingLevelInfo))
       return LogLevel::INFO;
   else if (boost::iequals(logLevelStr, kLoggingLevelDebug))
       return LogLevel::DEBUG;
   else
       return LogLevel::WARN;
}

LoggerType strToLoggerType(const std::string& loggerTypeStr)
{
   if (boost::iequals(loggerTypeStr, kSysLogger))
      return LoggerType::kSysLog;
   else if (boost::iequals(loggerTypeStr, kFileLogger))
      return LoggerType::kFile;
   else if (boost::iequals(loggerTypeStr, kStdErrLogger))
      return LoggerType::kStdErr;
   else
      return LoggerType::kSysLog;
}

LogMessageFormatType strToLogMessageFormatType(const std::string& logMessageFormatTypeStr)
{
   if (boost::iequals(logMessageFormatTypeStr, kLogMessageFormatJson))
      return LogMessageFormatType::JSON;
   else
      return LogMessageFormatType::PRETTY;
}

struct LoggerOptionsVisitor : boost::static_visitor<>
{
   LoggerOptionsVisitor(ConfigProfile& profile) :
      profile_(profile)
   {
   }

   void setDefaultFileLoggerOptions()
   {
      FileLogOptions defaultOptions(defaultLogPath());
      profile_.addParams(
         kLogDir, defaultOptions.getDirectory().getAbsolutePath(),
         kLogFileMode, defaultOptions.getFileMode(),
         kRotate, defaultOptions.doRotation(),
         kLogFileIncludePid, defaultOptions.includePid(),
         kMaxSizeMb, defaultOptions.getMaxSizeMb(),
         kRotateDays, defaultOptions.getRotationDays(),
         kMaxRotations, defaultOptions.getMaxRotations(),
         kDeleteDays, defaultOptions.getDeletionDays(),
         kWarnSyslog, defaultOptions.warnSyslog());
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
         kLogFileMode, options.getFileMode(),
         kRotateDays, options.getRotationDays(),
         kMaxRotations, options.getMaxRotations(),
         kDeleteDays, options.getDeletionDays(),
         kWarnSyslog, options.warnSyslog());
   }

   ConfigProfile& profile_;
};

// used to determine if file log directory has been programmatically forced
// the logging directory can be forced programmatically, but all other
// fields could still be overrideable via logging configuration
struct ForcedFileLogOptionsVisitor : boost::static_visitor<bool>
{
   bool operator()(const StdErrLogOptions& options)
   {
      return false;
   }

   bool operator()(const SysLogOptions& options)
   {
      return false;
   }

   bool operator()(const FileLogOptions& options)
   {
      return options.getForceDirectory();
   }
};

// if force log directory is set, we use this visitor to determine
// which directory to force it to
struct LogDirVisitor : boost::static_visitor<FilePath>
{
   FilePath operator()(const StdErrLogOptions& options)
   {
      return FilePath(defaultLogPath());
   }

   FilePath operator()(const SysLogOptions& options)
   {
      return FilePath(defaultLogPath());
   }

   FilePath operator()(const FileLogOptions& options)
   {
      return options.getDirectory();
   }
};

} // anonymous namespace


LogOptions::LogOptions(const std::string& executableName) :
   executableName_(executableName),
   defaultLogLevel_(logLevelToString(LogLevel::WARN)),
   defaultLoggerType_(loggerTypeToString(LoggerType::kSysLog)),
   defaultMessageFormatType_(logMessageFormatTypeToString(LogMessageFormatType::PRETTY)),
   defaultLoggerOptions_(SysLogOptions()),
   lowestLogLevel_(LogLevel::WARN)
{
   initProfile();
}

LogOptions::LogOptions(const std::string& executableName,
                       LogLevel logLevel,
                       LoggerType loggerType,
                       LogMessageFormatType messageFormatType,
                       const LoggerOptions& options) :
   executableName_(executableName),
   defaultLogLevel_(logLevelToString(logLevel)),
   defaultLoggerType_(loggerTypeToString(loggerType)),
   defaultMessageFormatType_(logMessageFormatTypeToString(messageFormatType)),
   defaultLoggerOptions_(options),
   lowestLogLevel_(logLevel)
{
   initProfile();
}

FilePath LogOptions::defaultLogDirectory()
{
   return defaultLogPath();
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
      kLoggerType, defaultLoggerType_,
      kLogMessageFormat, defaultMessageFormatType_);

   // add logger-specific params
   LoggerOptionsVisitor visitor(profile_);
   boost::apply_visitor(visitor, defaultLoggerOptions_);
}

Error LogOptions::read()
{
   FilePath optionsFile = getLogConfigFile();

   // if the options file does not exist, that's fine - we'll just use default values
   if (!optionsFile.exists())
      return Success();

   Error error = profile_.load(optionsFile);
   if (error)
      return error;

   setLowestLogLevel();

   return Success();
}

FilePath LogOptions::getLogConfigFile() {

   // look for config file in a specific environment variable
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

   return optionsFile;
}

void LogOptions::setLowestLogLevel()
{
   // first, set the log level for this particular binary
   // override with env var setting if set
   std::string logLevel;
   std::string envValue = core::system::getenv(kLogLevelEnvVar);
   if (!envValue.empty())
      logLevel = envValue;
   else
   {
      profile_.getParam(
         kLogLevel, &logLevel, {{ kBaseLevel,   std::string() },
                                { kBinaryLevel, executableName_ }});
   }

   lowestLogLevel_ = strToLogLevel(logLevel);

   // break out early if we are already at debug level (since we cannot go lower)
   // or if the env var was set (the env var overrides all named loggers)
   if (lowestLogLevel_ == LogLevel::DEBUG || !envValue.empty())
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

   std::string envValue = core::system::getenv(kLogLevelEnvVar);
   if (!envValue.empty())
      logLevel = envValue;
   else
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

   std::string envValue = core::system::getenv(kLogTypeEnvVar);
   if (!envValue.empty())
      loggerType = envValue;
   else
      profile_.getParam(kLoggerType, &loggerType, levels);

   return strToLoggerType(loggerType);
}

LogMessageFormatType LogOptions::logMessageFormatType(const std::string& loggerName) const
{
   std::vector<ConfigProfile::Level> levels = getLevels(loggerName);

   std::string messageFormatStr;

   std::string envValue = core::system::getenv(kLogFormatEnvVar);
   if (!envValue.empty())
      messageFormatStr = envValue;
   else
      profile_.getParam(kLogMessageFormat, &messageFormatStr, levels);

   return strToLogMessageFormatType(messageFormatStr);
}

LoggerOptions LogOptions::loggerOptions(const std::string& loggerName) const
{
   LoggerType type = loggerType(loggerName);

   switch (type)
   {
      case LoggerType::kFile:
      {
         std::vector<ConfigProfile::Level> levels = getLevels(loggerName);

         std::string logDir, fileMode, messageFormatStr;
         bool rotate, includePid, warnSyslog;
         double maxSizeMb;
         int rotateDays, maxRotations, deleteDays;

         profile_.getParam(kRotate, &rotate, levels);
         profile_.getParam(kMaxSizeMb, &maxSizeMb, levels);
         profile_.getParam(kLogFileIncludePid, &includePid, levels);
         profile_.getParam(kLogFileMode, &fileMode, levels);
         profile_.getParam(kRotateDays, &rotateDays, levels);
         profile_.getParam(kMaxRotations, &maxRotations, levels);
         profile_.getParam(kDeleteDays, &deleteDays, levels);
         profile_.getParam(kWarnSyslog, &warnSyslog, levels);

         profile_.getParam(kLogDir, &logDir, levels);
         FilePath loggingDir(logDir);

         // determine if the log directory should be programatically forced, preventing
         // it from being overrideable via conf file - note we still allow it to be overridden
         // via environment variable as an escape hatch
         ForcedFileLogOptionsVisitor forceVisitor;
         bool forceLogDir = boost::apply_visitor(forceVisitor, defaultLoggerOptions_);
         if (forceLogDir)
         {
            LogDirVisitor dirVisitor;
            loggingDir = boost::apply_visitor(dirVisitor, defaultLoggerOptions_);
         }

         // override log dir via env var if present
         std::string logDirOverride = core::system::getenv(kLogDirEnvVar);
         if (!logDirOverride.empty())
            loggingDir = FilePath(logDirOverride);

         return FileLogOptions(loggingDir, fileMode, maxSizeMb, rotateDays, maxRotations, deleteDays, rotate, includePid, warnSyslog, forceLogDir);
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

void forwardLogOptionsEnvVars(core::system::Options* pEnvironment)
{
   // forward relevant log configuration environment variables (i.e. all those we respect above)
   core::system::forwardEnvVars({kLogLevelEnvVar, kLogTypeEnvVar,
                                 kLogFormatEnvVar,   kLogDirEnvVar,
                                 kLogConfEnvVar},
                                pEnvironment);
}

} // namespace log
} // namespace core
} // namespace rstudio

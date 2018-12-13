/*
 * LogOptions.cpp
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

#include <core/LogOptions.hpp>
#include <core/system/System.hpp>

#include "config.h"

namespace rstudio {
namespace core {

#define kLogLevel          "log-level"
#define kLoggerType        "logger-type"
#define kLogDir            "log-dir"
#define kLogFileMode       "log-file-mode"
#define kLogFileIncludePid "log-file-include-pid"
#define kRotate            "rotate"
#define kMaxSizeMb         "max-size-mb"

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

std::string logLevelToString(int logLevel)
{
   switch (logLevel)
   {
      case core::system::kLogLevelDebug:
         return "debug";
      case core::system::kLogLevelInfo:
         return "info";
      case core::system::kLogLevelWarning:
         return "warn";
      case core::system::kLogLevelError:
         return "error";
      default:
         return "warn";
   }
}

std::string loggerTypeToString(int loggerType)
{
   switch (loggerType)
   {
      case kLoggerTypeFile:
         return "file";
      case kLoggerTypeStdErr:
         return "stderr";
      case kLoggerTypeSysLog:
         return "syslog";
      default:
         return "syslog";
   }
}

int strToLogLevel(const std::string& logLevelStr)
{
   if (logLevelStr == "warn")
      return core::system::kLogLevelWarning;
   else if (logLevelStr == "error")
      return core::system::kLogLevelError;
   else if (logLevelStr == "info")
      return core::system::kLogLevelInfo;
   else if (logLevelStr == "debug")
      return core::system::kLogLevelDebug;
   else
      return core::system::kLogLevelWarning;
}

int strToLoggerType(const std::string& loggerTypeStr)
{
   if (loggerTypeStr == "syslog")
      return kLoggerTypeSysLog;
   else if (loggerTypeStr == "file")
      return kLoggerTypeFile;
   else if (loggerTypeStr == "stderr")
      return kLoggerTypeStdErr;
   else
      return kLoggerTypeSysLog;
}

struct LoggerOptionsVisitor : boost::static_visitor<>
{
   LoggerOptionsVisitor(ConfigProfile& profile) :
      profile_(profile)
   {
   }

   void setDefaultFileLoggerOptions()
   {
      FileLoggerOptions defaultOptions;
      profile_.addParams(kLogDir, defaultOptions.logDir.absolutePath(),
                         kLogFileMode, defaultOptions.fileMode,
                         kRotate, defaultOptions.rotate,
                         kLogFileIncludePid, defaultOptions.includePid,
                         kMaxSizeMb, defaultOptions.maxSizeMb);
   }

   void operator()(const StdErrLoggerOptions& options)
   {
      setDefaultFileLoggerOptions();
   }

   void operator()(const SysLoggerOptions& options)
   {
      setDefaultFileLoggerOptions();
   }

   void operator ()(const FileLoggerOptions& options)
   {
      // set file logger option defaults to those that were passed in
      profile_.addParams(kLogDir, options.logDir.absolutePath(),
                         kRotate, options.rotate,
                         kMaxSizeMb, options.maxSizeMb,
                         kLogFileIncludePid, options.includePid,
                         kLogFileMode, options.fileMode);
   }

   ConfigProfile& profile_;
};

} // anonymous namespace

FileLoggerOptions::FileLoggerOptions()
{
   // pick default log path location
#ifdef RSTUDIO_SERVER
   FilePath defaultLogPath("/var/log/rstudio-server");
#else
   // desktop - store logs under user dir
   FilePath defaultLogPath = core::system::userSettingsPath(core::system::userHomePath(),
                                                            "RStudio-Desktop",
                                                            false).complete("logs");
#endif
   logDir = defaultLogPath;
   fileMode = defaultFileMode;
   maxSizeMb = defaultMaxSizeMb;
   rotate = defaultRotate;
   includePid = defaultIncludePid;
}

LogOptions::LogOptions(const std::string& executableName) :
   executableName_(executableName),
   defaultLogLevel_(logLevelToString(core::system::kLogLevelWarning)),
   defaultLoggerType_(loggerTypeToString(kLoggerTypeSysLog)),
   defaultLoggerOptions_(SysLoggerOptions()),
   lowestLogLevel_(core::system::kLogLevelWarning)
{
   initProfile();
}

LogOptions::LogOptions(const std::string& executableName,
                       int logLevel,
                       int loggerType,
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
   profile_.addSections({{kBaseLevel, "*"},
                         {kBinaryLevel, "@"},
                         {kLogSectionLevel, std::string()}});

   // add base params
   profile_.addParams(kLogLevel, defaultLogLevel_,
                      kLoggerType, defaultLoggerType_);

   // add logger-specific params
   LoggerOptionsVisitor visitor(profile_);
   boost::apply_visitor(visitor, defaultLoggerOptions_);
}

Error LogOptions::read()
{
#ifdef RSTUDIO_SERVER
   FilePath optionsFile("/etc/rstudio/logging.conf");
#else
   // desktop - read user file first, and only read admin file if the user file does not exist
   FilePath optionsFile = core::system::userSettingsPath(core::system::userHomePath(),
                                                         "RStudio-Desktop",
                                                         false).complete("logging.conf");
   if (!optionsFile.exists())
      #ifdef _WIN32
         optionsFile = core::system::systemSettingsPath("RStudio", false).complete("logging.conf");
      #else
         optionsFile = FilePath("/etc/rstudio/logging.conf");
   #endif
#endif

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
   profile_.getParam(kLogLevel, &logLevel, {{kBaseLevel, std::string()}, {kBinaryLevel, executableName_}});
   lowestLogLevel_ = strToLogLevel(logLevel);

   // break out early if we are already at debug level (since we cannot go lower)
   if (lowestLogLevel_ == core::system::kLogLevelDebug)
      return;

   // now, override it with the lowest log level specified for named loggers
   std::vector<std::string> sectionNames = profile_.getLevelNames(kLogSectionLevel);
   for (const std::string& name : sectionNames)
   {
      profile_.getParam(kLogLevel, &logLevel, {{kBaseLevel, std::string()},
                                               {kBinaryLevel, executableName_},
                                               {kLogSectionLevel, name}});
      int level = strToLogLevel(logLevel);
      if (level < lowestLogLevel_)
         lowestLogLevel_ = level;

      if (lowestLogLevel_ == core::system::kLogLevelDebug)
         return;
   }
}

int LogOptions::logLevel(const std::string& loggerName) const
{
   std::vector<ConfigProfile::Level> levels = getLevels(loggerName);

   std::string logLevel = defaultLogLevel_;

   profile_.getParam(kLogLevel, &logLevel, levels);

   return strToLogLevel(logLevel);
}

int LogOptions::lowestLogLevel() const
{
   return lowestLogLevel_;
}

int LogOptions::loggerType(const std::string& loggerName) const
{
   std::vector<ConfigProfile::Level> levels = getLevels(loggerName);

   std::string loggerType = defaultLoggerType_;

   profile_.getParam(kLoggerType, &loggerType, levels);

   return strToLoggerType(loggerType);
}

LoggerOptions LogOptions::loggerOptions(const std::string& loggerName) const
{
   int type = loggerType(loggerName);

   switch (type)
   {
      case kLoggerTypeFile:
      {
         std::vector<ConfigProfile::Level> levels = getLevels(loggerName);

         FileLoggerOptions options;
         std::string logDir;

         profile_.getParam(kLogDir, &logDir, levels);
         profile_.getParam(kRotate, &options.rotate, levels);
         profile_.getParam(kMaxSizeMb, &options.maxSizeMb, levels);
         profile_.getParam(kLogFileIncludePid, &options.includePid, levels);
         profile_.getParam(kLogFileMode, &options.fileMode, levels);

         options.logDir = FilePath(logDir);
         return options;
      }

      case kLoggerTypeStdErr:
         return StdErrLoggerOptions();

      case kLoggerTypeSysLog:
         return SysLoggerOptions();

      default:
         return SysLoggerOptions();
   }
}

std::vector<ConfigProfile::Level> LogOptions::getLevels(const std::string& loggerName) const
{
   std::vector<ConfigProfile::Level> levels = {{kBaseLevel, std::string()}, {kBinaryLevel, executableName_}};
      if (!loggerName.empty())
         levels.push_back({kLogSectionLevel, loggerName});
   return levels;
}

std::vector<std::string> LogOptions::loggerOverrides() const
{
   return profile_.getLevelNames(kLogSectionLevel);
}

} // namespace core
} // namespace rstudio

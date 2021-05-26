/*
 * log-options.ts
 *
 * Copyright (C) 2021 by RStudio, PBC
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

import { Err, Success } from "./err";
import { LogLevel, LoggerType, FileLogOptions } from "./log";

export class StdErrLogOptions {}
export class SysLogOptions {}
export type LoggerOptions = StdErrLogOptions | SysLogOptions | FileLogOptions

const kLogLevel          = "log-level"
const kLoggerType        = "logger-type"
const kLogDir            = "log-dir"
const kLogFileMode       = "log-file-mode"
const kLogFileIncludePid = "log-file-include-pid"
const kRotate            = "rotate"
const kMaxSizeMb         = "max-size-mb"
const kLogConfFile       = "logging.conf"
const kLogConfEnvVar     = "RS_LOG_CONF_FILE"

const kFileLogger        = "file"
const kStdErrLogger      = "stderr"
const kSysLogger         = "syslog"

const kLoggingLevelDebug = "debug"
const kLoggingLevelInfo  = "info"
const kLoggingLevelWarn  = "warn"
const kLoggingLevelError = "error"

const kBaseLevel         = 0
const kBinaryLevel       = 1
const kLogSectionLevel   = 2 

export class LogOptions
{
  constructor(
    private readonly executableName: string,
    private readonly defaultLogLevel: LogLevel,
    private readonly defaultLoggerType: LoggerType,
    private readonly defaultLoggerOptions: LoggerOptions
  ) {
    this.initProfile();
  }

  initProfile() {
    // base level - * (applies to all loggers/binaries)
    // first override - @ (specific binary)
    // second override - (logger name)
    // profile_.addSections(
    //    {{ kBaseLevel,       "*" },
    //     { kBinaryLevel,     "@" },
    //     { kLogSectionLevel, std::string() }});

    // // add base params
    // profile_.addParams(
    //    kLogLevel, defaultLogLevel_,
    //    kLoggerType, defaultLoggerType_);

    // // add logger-specific params
    // LoggerOptionsVisitor visitor(profile_);
    // boost::apply_visitor(visitor, defaultLoggerOptions_);
   }
   
  read(): Err {
    return Success();
    // // first, look for config file in a specific environment variable
    // let optionsFile = getenv(kLogConfEnvVar);
    // if (!fs.existsSync(optionsFile)) {
    //   // desktop - read user file first, and only read admin file if the user file does not exist
    //   optionsFile = core::system::xdg::userConfigDir().completeChildPath(kLogConfFile);
    //   if (!fs.existsSync(optionsFile)) {
    //     optionsFile = core::system::xdg::systemConfigFile(kLogConfFile)
    //   }
    // }

    // // if the options file does not exist, that's fine - we'll just use default values
    // if (!optionsFile.exists())
    //   return;
 
    // Error error = profile_.load(optionsFile);
    // if (error)
    //    return error;

    // this.setLowestLogLevel();
  }

  private setLowestLogLevel() {
    //  // first, set the log level for this particular binary
    //  std::string logLevel;
    //  profile_.getParam(
    //     kLogLevel, &logLevel, {{ kBaseLevel,   std::string() },
    //                            { kBinaryLevel, executableName_ }});
    //  lowestLogLevel_ = strToLogLevel(logLevel);
  
    //  // break out early if we are already at debug level (since we cannot go lower)
    //  if (lowestLogLevel_ == LogLevel::DEBUG)
    //     return;
  
    //  // now, override it with the lowest log level specified for named loggers
    //  std::vector<std::string> sectionNames = profile_.getLevelNames(kLogSectionLevel);
    //  for (const std::string& name : sectionNames)
    //  {
    //     profile_.getParam(
    //        kLogLevel, &logLevel, {{ kBaseLevel,       std::string() },
    //                               { kBinaryLevel,     executableName_ },
    //                               { kLogSectionLevel, name }});
    //     LogLevel level = strToLogLevel(logLevel);
    //     if (level > lowestLogLevel_)
    //        lowestLogLevel_ = level;
  
    //     if (lowestLogLevel_ == LogLevel::DEBUG)
    //        return;
    //  }
  }
   
   // gets the current log level
   //LogLevel logLevel(const std::string& loggerName = std::string()) const;

   // gets the lowest log level defined
   //LogLevel lowestLogLevel() const;

   // gets the current logger type
   //LoggerType loggerType(const std::string& loggerName = std::string()) const;

   // gets the current logger's specific options
   //LoggerOptions loggerOptions(const std::string& loggerName = std::string()) const;

   //std::vector<std::string> loggerOverrides() const;

//private:
   //void initProfile();

   //void setLowestLogLevel();

   //std::vector<ConfigProfile::Level> getLevels(const std::string& loggerName) const;

   //std::string executableName_;

   //std::string defaultLogLevel_;
   //std::string defaultLoggerType_;
   //LoggerOptions defaultLoggerOptions_;

   //LogLevel lowestLogLevel_;

   //ConfigProfile profile_;
};

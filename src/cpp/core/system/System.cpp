/*
 * System.cpp
 *
 * Copyright (C) 2009-19 by RStudio, Inc.
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

#include <atomic>

#include <boost/variant.hpp>

#include <core/system/System.hpp>

#include <core/Hash.hpp>
#include <core/Log.hpp>
#include <core/LogOptions.hpp>

#include <core/system/Environment.hpp>

#ifndef _WIN32
#include <shared_core/SyslogDestination.hpp>
#endif

#include <shared_core/FileLogDestination.hpp>
#include <shared_core/FilePath.hpp>
#include <shared_core/SafeConvert.hpp>
#include <shared_core/StderrLogDestination.hpp>

namespace rstudio {
namespace core {
namespace system {

#ifdef _WIN32
#define kPathSeparator ";"
#else
#define kPathSeparator ":"
#endif


bool realPathsEqual(const FilePath& a, const FilePath& b)
{
   FilePath aReal, bReal;

   Error error = realPath(a, &aReal);
   if (error)
   {
      LOG_ERROR(error);
      return false;
   }

   error = realPath(b, &bReal);
   if (error)
   {
      LOG_ERROR(error);
      return false;
   }

   return aReal == bReal;
}

void addToSystemPath(const FilePath& path, bool prepend)
{
   std::string systemPath = system::getenv("PATH");
   if (prepend)
      systemPath = path.getAbsolutePath() + kPathSeparator + systemPath;
   else
      systemPath = systemPath + kPathSeparator + path.getAbsolutePath();
   system::setenv("PATH", systemPath);
}


int exitFailure(const Error& error, const ErrorLocation& loggedFromLocation)
{
   if (!error.isExpected())
      core::log::logError(error, loggedFromLocation);
   return EXIT_FAILURE;
}

int exitFailure(const std::string& errMsg,
                const ErrorLocation& loggedFromLocation)
{
   core::log::logErrorMessage(errMsg, loggedFromLocation);
   return EXIT_FAILURE;
}
   
int exitSuccess()
{
   return EXIT_SUCCESS;
}

std::string generateShortenedUuid()
{
   std::string uuid = core::system::generateUuid(false);
   return core::hash::crc32HexHash(uuid);
}

// logger's program identity (this process's binary name)
std::string s_programIdentity;

// logging options representing the latest state of the logging configuration file
boost::shared_ptr<log::LogOptions> s_logOptions;

// mutex for logging synchronization
boost::recursive_mutex s_loggingMutex;

namespace {

static std::atomic_uint s_fileLogId(3);
static std::shared_ptr<log::ILogDestination> s_stdErrLogDest;
#ifndef _WIN32
static std::shared_ptr<log::ILogDestination> s_syslogDest;
#endif

void initializeLogWriter()
{
   using namespace log;

   // requires prior synchronization
   int loggerType = s_logOptions->loggerType();

   // options currently only used for file logging
   LoggerOptions options = s_logOptions->loggerOptions();

   switch (loggerType)
   {
      case kLoggerTypeFile:
      {
         addLogDestination(
            std::shared_ptr<ILogDestination>(new FileLogDestination(
               s_fileLogId.fetch_add(1),
               s_programIdentity,
               boost::get<FileLogOptions>(options))));
         break;
      }
      case kLoggerTypeStdErr:
      {
         if (s_stdErrLogDest == nullptr)
            s_stdErrLogDest.reset(new StderrLogDestination());
         addLogDestination(s_stdErrLogDest);
         break;
      }
      case kLoggerTypeSysLog:
      default:
      {
#ifndef _WIN32
         if (s_syslogDest == nullptr)
            s_syslogDest.reset(new SyslogDestination(s_programIdentity));
         addLogDestination(s_syslogDest);
#endif
      }
   }
}

void initializeLogWriter(const std::string& logSection)
{
   using namespace log;

   // requires prior synchronization
   int loggerType = s_logOptions->loggerType(logSection);

   // options currently only used for file logging
   LoggerOptions options = s_logOptions->loggerOptions(logSection);

   switch (loggerType)
   {
      case kLoggerTypeFile:
      {
         addLogDestination(
            std::shared_ptr<ILogDestination>(new FileLogDestination(
               s_fileLogId.fetch_add(1),
               s_programIdentity,
               boost::get<FileLogOptions>(options))),
            LogSection(logSection, s_logOptions->logLevel(logSection)));
         break;
      }
      case kLoggerTypeStdErr:
      {
         if (s_stdErrLogDest == nullptr)
            s_stdErrLogDest.reset(new StderrLogDestination());
         addLogDestination(
            s_stdErrLogDest,
            LogSection(logSection, s_logOptions->logLevel(logSection)));
         break;
      }
      case kLoggerTypeSysLog:
      default:
      {
#ifndef _WIN32
         if (s_syslogDest == nullptr)
            s_syslogDest.reset(new SyslogDestination(s_programIdentity));
         addLogDestination(
            s_syslogDest,
            LogSection(logSection, s_logOptions->logLevel(logSection)));
#endif
      }
   }
}

void initializeLogWriters()
{
   // requires prior synchronization
   log::setProgramId(s_programIdentity);
   log::setLogLevel(s_logOptions->logLevel());

   // Initialize primary log writer
   initializeLogWriter();

   // Initialize secondary log writers
   std::vector<std::string> logSections = s_logOptions->loggerOverrides();
   for (const std::string& loggerName : logSections)
      initializeLogWriter(loggerName);
}
} // anonymous namespace

log::LogLevel lowestLogLevel()
{
   RECURSIVE_LOCK_MUTEX(s_loggingMutex)
   {
      if (!s_logOptions)
         return log::LogLevel::WARNING;

      return s_logOptions->lowestLogLevel();
   }
   END_LOCK_MUTEX

   // default return - only occurs if we fail to lock mutex
   return log::LogLevel::WARNING;
}

Error initLog()
{
   // requires prior synchronization

   Error error = s_logOptions->read();
   if (error)
      return error;

   initializeLogWriters();

   return Success();
}

Error reinitLog()
{
   RECURSIVE_LOCK_MUTEX(s_loggingMutex)
   {
      return initLog();
   }
   END_LOCK_MUTEX

   return Success();
}

Error initializeStderrLog(const std::string& programIdentity,
                          log::LogLevel logLevel,
                          bool enableConfigReload)
{
   RECURSIVE_LOCK_MUTEX(s_loggingMutex)
   {
      // create default stderr logger options
      log::StdErrLoggerOptions options;
      s_logOptions.reset(new log::LogOptions(programIdentity, logLevel, kLoggerTypeStdErr, options));
      s_programIdentity = programIdentity;

      Error error = initLog();
      if (error)
         return error;
   }
   END_LOCK_MUTEX

   if (enableConfigReload)
      initializeLogConfigReload();

   return Success();
}

Error initializeLog(const std::string& programIdentity,
                    log::LogLevel logLevel,
                    const FilePath& logDir,
                    bool enableConfigReload)
{
   RECURSIVE_LOCK_MUTEX(s_loggingMutex)
   {
      // create default file logger options
      log::FileLogOptions options(logDir);
      s_logOptions.reset(new log::LogOptions(programIdentity, logLevel, kLoggerTypeFile, options));
      s_programIdentity = programIdentity;

      Error error = initLog();
      if (error)
         return error;
   }
   END_LOCK_MUTEX

   if (enableConfigReload)
      initializeLogConfigReload();

   return Success();
}

void log(log::LogLevel logLevel,
         const char* message,
         const std::string& logSection)
{
   log(logLevel, std::string(message), logSection);
}

void log(log::LogLevel logLevel,
         const std::string& message,
         const std::string& logSection)
{
   switch (logLevel)
   {
      case log::LogLevel::ERROR:
      {
         log::logErrorMessage(message, logSection);
         break;
      }
      case log::LogLevel::WARNING:
      {
         log::logWarningMessage(message, logSection);
         break;
      }
      case log::LogLevel::DEBUG:
      {
         log::logDebugMessage(message, logSection);
         break;
      }
      case log::LogLevel::INFO:
      {
         log::logInfoMessage(message, logSection);
         break;
      }
      case log::LogLevel::OFF:
      default:
      {
         return;
      }
   }
}

const char* logLevelToStr(log::LogLevel level)
{
   switch(level)
   {
      case log::LogLevel::ERROR:
         return "ERROR";
      case log::LogLevel::WARNING:
         return "WARNING";
      case log::LogLevel::INFO:
         return "INFO";
      case log::LogLevel::DEBUG:
         return "DEBUG";
      case log::LogLevel::OFF:
         return "OFF";
      default:
         LOG_WARNING_MESSAGE("Unexpected log level: " +
                             safe_convert::numberToString(static_cast<int>(level)));
         return "ERROR";
   }
}

std::string currentProcessPidStr()
{
   return safe_convert::numberToString(currentProcessId());
}

} // namespace system
} // namespace core
} // namespace rstudio


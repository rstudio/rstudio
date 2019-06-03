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

#include <boost/variant.hpp>

#include <core/system/System.hpp>

#include <core/FilePath.hpp>
#include <core/FileLogWriter.hpp>
#include <core/Hash.hpp>
#include <core/Log.hpp>
#include <core/LogOptions.hpp>
#include <core/SafeConvert.hpp>
#include <core/StderrLogWriter.hpp>

#include <core/system/Environment.hpp>

#ifndef _WIN32
#include <core/SyslogLogWriter.hpp>
#endif

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
      systemPath = path.absolutePath() + kPathSeparator + systemPath;
   else
      systemPath = systemPath + kPathSeparator + path.absolutePath();
   system::setenv("PATH", systemPath);
}


int exitFailure(const Error& error, const ErrorLocation& loggedFromLocation)
{
   if (!error.expected())
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
boost::shared_ptr<LogOptions> s_logOptions;

// mutex for logging synchronization
boost::recursive_mutex s_loggingMutex;

namespace {

// main log writer for the binary
LogWriter* s_pLogWriter = nullptr;

// lazy initalized named loggers
// these are secondary loggers to the main binary logger
// useful for isolating specific sections of code within the binary
typedef std::map<std::string, boost::shared_ptr<LogWriter> > SecondaryLoggerMap;
SecondaryLoggerMap s_secondaryLogWriters;

// additional log writers
// these are created entirely in code and are not modifiable by the
// logging configuration file
std::vector<boost::shared_ptr<LogWriter> > s_additionalLogWriters;

LogWriter* initializeLogWriter(const std::string& logSection = std::string())
{
   // requires prior synchronization

   int logLevel = s_logOptions->logLevel(logSection);
   int loggerType = s_logOptions->loggerType(logSection);

   // options currently only used for file logging
   LoggerOptions options = s_logOptions->loggerOptions(logSection);

   switch (loggerType)
   {
      case kLoggerTypeFile:
         return new FileLogWriter(s_programIdentity, logLevel, boost::get<FileLoggerOptions>(options), logSection);
      case kLoggerTypeStdErr:
         return new StderrLogWriter(s_programIdentity, logLevel);
      case kLoggerTypeSysLog:
      default:
#ifndef _WIN32
         return new SyslogLogWriter(s_programIdentity, logLevel);
#else
         return nullptr;
#endif
   }
}

void initializeMainLogWriter()
{
   // requires prior synchronization

   if (s_pLogWriter)
      delete s_pLogWriter;

   s_pLogWriter = initializeLogWriter();
}

void initializeSecondaryLogWriters()
{
   // requires prior synchronization

   s_secondaryLogWriters.clear();

   std::vector<std::string> logSections = s_logOptions->loggerOverrides();
   for (const std::string& loggerName : logSections)
   {
      LogWriter* pLogWriter = initializeLogWriter(loggerName);
      if (pLogWriter)
         s_secondaryLogWriters.emplace(loggerName, boost::shared_ptr<LogWriter>(pLogWriter));
   }
}

} // anonymous namespace

LogLevel lowestLogLevel()
{
   RECURSIVE_LOCK_MUTEX(s_loggingMutex)
   {
      if (!s_logOptions)
         return kLogLevelWarning;

      int lowestLevel = s_logOptions->lowestLogLevel();
      for (const boost::shared_ptr<LogWriter>& logWriter : s_additionalLogWriters)
      {
         if (logWriter->logLevel() < lowestLevel)
            lowestLevel = logWriter->logLevel();
      }
      return static_cast<LogLevel>(lowestLevel);
   }
   END_LOCK_MUTEX

   // default return - only occurs if we fail to lock mutex
   return kLogLevelWarning;
}

Error initLog()
{
   // requires prior synchronization

   Error error = s_logOptions->read();
   if (error)
      return error;

   initializeMainLogWriter();
   initializeSecondaryLogWriters();

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
                          int logLevel,
                          bool enableConfigReload)
{
   RECURSIVE_LOCK_MUTEX(s_loggingMutex)
   {
      // create default stderr logger options
      StdErrLoggerOptions options;
      s_logOptions.reset(new LogOptions(programIdentity, logLevel, kLoggerTypeStdErr, options));
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
                    int logLevel,
                    const FilePath& logDir,
                    bool enableConfigReload)
{
   RECURSIVE_LOCK_MUTEX(s_loggingMutex)
   {
      // create default file logger options
      FileLoggerOptions options;
      options.logDir = logDir;
      s_logOptions.reset(new LogOptions(programIdentity, logLevel, kLoggerTypeFile, options));
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

void setLogToStderr(bool logToStderr)
{
   RECURSIVE_LOCK_MUTEX(s_loggingMutex)
   {
      if (s_pLogWriter)
         s_pLogWriter->setLogToStderr(logToStderr);
   }
   END_LOCK_MUTEX
}

void addLogWriter(boost::shared_ptr<core::LogWriter> pLogWriter)
{
   s_additionalLogWriters.push_back(pLogWriter);
}

void log(LogLevel logLevel,
         const char* message,
         const std::string& logSection)
{
   log(logLevel, std::string(message), logSection);
}

void log(LogLevel logLevel,
         const std::string& message,
         const std::string& logSection)
{
   RECURSIVE_LOCK_MUTEX(s_loggingMutex)
   {
      bool logToMainLogger = true;

      if (!logSection.empty())
      {
         // check to see if this log section has a logger dedicated to it
         // if so, we will use it to log, instead of using the main binary logger
         SecondaryLoggerMap::iterator iter = s_secondaryLogWriters.find(logSection);
         if (iter != s_secondaryLogWriters.end())
         {
            iter->second->log(logLevel, message);
            logToMainLogger = false;
         }
      }

      if (s_pLogWriter && logToMainLogger)
         s_pLogWriter->log(logLevel, message);
   }
   END_LOCK_MUTEX

   // not synchronized because additional log writers are static
   // and do not change at run-time
   std::for_each(s_additionalLogWriters.begin(),
                 s_additionalLogWriters.end(),
                 boost::bind(&LogWriter::log, _1, logLevel, message));
}

const char* logLevelToStr(LogLevel level)
{
   switch(level)
   {
      case kLogLevelError:
         return "ERROR";
      case kLogLevelWarning:
         return "WARNING";
      case kLogLevelInfo:
         return "INFO";
      case kLogLevelDebug:
         return "DEBUG";
      default:
         LOG_WARNING_MESSAGE("Unexpected log level: " +
                             safe_convert::numberToString(level));
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


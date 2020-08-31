/*
 * System.cpp
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

#include <atomic>
#include <unordered_set>

#include <boost/variant.hpp>

#include <core/system/System.hpp>

#include <core/Hash.hpp>
#include <core/Log.hpp>
#include <core/LogOptions.hpp>

#include <core/system/Environment.hpp>

#ifndef _WIN32
#include <shared_core/system/SyslogDestination.hpp>
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
   if (error)
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

int getNextFileLogId()
{
   static std::atomic_uint fileLogId(3);
   return fileLogId.fetch_add(1);
}

std::shared_ptr<log::ILogDestination> getStdErrLogDest(log::LogLevel logLevel)
{
   using namespace log;
   static std::shared_ptr<ILogDestination> stdErrLogDest(new StderrLogDestination(logLevel));
   return stdErrLogDest;
}

#ifndef _WIN32

std::shared_ptr<log::ILogDestination> getSysLogDest(log::LogLevel logLevel)
{
   using namespace log;
   static std::shared_ptr<ILogDestination> syslogDest(new SyslogDestination(logLevel, s_programIdentity));
   return syslogDest;
}

#endif

void initializeLogWriter()
{
   using namespace log;

   // requires prior synchronization
   LoggerType loggerType = s_logOptions->loggerType();

   // options currently only used for file logging
   LoggerOptions options = s_logOptions->loggerOptions();

   log::LogLevel logLevel = s_logOptions->logLevel();

   switch (loggerType)
   {
      case LoggerType::kFile:
      {
         addLogDestination(
            std::shared_ptr<ILogDestination>(new FileLogDestination(
               getNextFileLogId(),
               logLevel,
               s_programIdentity,
               boost::get<FileLogOptions>(options))));
         break;
      }
      case LoggerType::kStdErr:
      {
         addLogDestination(getStdErrLogDest(logLevel));
         break;
      }
      case LoggerType::kSysLog:
      default:
      {
#ifndef _WIN32
         addLogDestination(getSysLogDest(logLevel));
#endif
      }
   }
}

void initializeLogWriter(const std::string& logSection)
{
   using namespace log;

   // requires prior synchronization
   LoggerType loggerType = s_logOptions->loggerType(logSection);

   // options currently only used for file logging
   LoggerOptions options = s_logOptions->loggerOptions(logSection);

   log::LogLevel logLevel = s_logOptions->logLevel(logSection);

   switch (loggerType)
   {
      case LoggerType::kFile:
      {
         addLogDestination(
            std::shared_ptr<ILogDestination>(new FileLogDestination(
               getNextFileLogId(),
               logLevel,
               s_programIdentity,
               boost::get<FileLogOptions>(options))),
               logSection);
         break;
      }
      case LoggerType::kStdErr:
      {
         addLogDestination(getStdErrLogDest(logLevel), logSection);
         break;
      }
      case LoggerType::kSysLog:
      default:
      {
#ifndef _WIN32
         addLogDestination(getSysLogDest(logLevel), logSection);
#endif
      }
   }
}

void initializeLogWriters()
{
   // requires prior synchronization
   log::setProgramId(s_programIdentity);

   // Initialize primary log writer
   initializeLogWriter();

   // Initialize secondary log writers
   std::vector<std::string> logSections = s_logOptions->loggerOverrides();
   for (const std::string& loggerName : logSections)
      initializeLogWriter(loggerName);
}
} // anonymous namespace

log::LoggerType loggerType(const std::string& in_sectionName)
{
   RECURSIVE_LOCK_MUTEX(s_loggingMutex)
   {
      if (s_logOptions)

         return s_logOptions->loggerType(in_sectionName);
   }
   END_LOCK_MUTEX

   // default return - only occurs if we fail to lock mutex
   return log::LoggerType::kSysLog;
}

log::LogLevel lowestLogLevel()
{
   RECURSIVE_LOCK_MUTEX(s_loggingMutex)
   {
      if (!s_logOptions)
         return log::LogLevel::WARN;

      return s_logOptions->lowestLogLevel();
   }
   END_LOCK_MUTEX

   // default return - only occurs if we fail to lock mutex
   return log::LogLevel::WARN;
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
      log::StdErrLogOptions options;
      s_logOptions.reset(new log::LogOptions(programIdentity, logLevel, log::LoggerType::kStdErr, options));
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
      s_logOptions.reset(new log::LogOptions(programIdentity, logLevel, log::LoggerType::kFile, options));
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
      case log::LogLevel::ERR:
      {
         log::logErrorMessage(message, logSection);
         break;
      }
      case log::LogLevel::WARN:
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
      case log::LogLevel::ERR:
         return "ERROR";
      case log::LogLevel::WARN:
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

void createProcessTree(const std::vector<ProcessInfo>& processes,
                       ProcessTreeT *pOutTree)
{
   // first pass, create the nodes in the tree
   for (const ProcessInfo& process : processes)
   {
      ProcessTreeT::iterator iter = pOutTree->find(process.pid);
      if (iter == pOutTree->end())
      {
         // process not found, so create a new entry for it
         boost::shared_ptr<ProcessTreeNode> nodePtr = boost::shared_ptr<ProcessTreeNode>(
                                                         new ProcessTreeNode());

         nodePtr->data = boost::shared_ptr<ProcessInfo>(new ProcessInfo(process));

         (*pOutTree)[process.pid] = nodePtr;
      }
   }

   // second pass, link the nodes together
   for (ProcessTreeT::value_type& element : *pOutTree)
   {
      PidType parent = element.second->data->ppid;
      ProcessTreeT::iterator iter = pOutTree->find(parent);

      // if we cannot find the parent in the tree, move on
      if (iter == pOutTree->end())
         continue;

      // add this node to its parent's children
      iter->second->children.push_back(element.second);
   }
}

void getChildren(const boost::shared_ptr<ProcessTreeNode>& node,
                 std::vector<ProcessInfo>* pOutChildren,
                 int depth)
{
   std::unordered_set<PidType> visited;
   for (const boost::shared_ptr<ProcessTreeNode>& child : node->children)
   {
      // cycle protection - only visit the node and its children if it hasn't been visited already
      // depth protection - do not infinitely recurse for process creation bombs
      if (visited.count(child->data->pid) == 0 && depth < 100)
      {
         visited.insert(child->data->pid);
         pOutChildren->push_back(*child->data.get());
         getChildren(child, pOutChildren, depth + 1);
      }
   }
}

} // namespace system
} // namespace core
} // namespace rstudio


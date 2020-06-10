/*
 * FileLock.cpp
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

#include <core/FileLock.hpp>

// #define RSTUDIO_ENABLE_DEBUG_MACROS
#include <core/Macros.hpp>

#include <core/Settings.hpp>
#include <shared_core/Error.hpp>
#include <core/Log.hpp>
#include <core/FileSerializer.hpp>
#include <core/http/SocketUtils.hpp>
#include <core/system/Environment.hpp>
#include <core/system/Xdg.hpp>

#include <boost/algorithm/string.hpp>

// borrowed from SessionConstants.hpp
#define kRStudioSessionRoute "RSTUDIO_SESSION_ROUTE"

namespace rstudio {
namespace core {

namespace {

const char * const kLockTypeAdvisory  = "advisory";
const char * const kLockTypeLinkBased = "linkbased";

// use advisory locks on Windows by default; link-based elsewhere
#ifdef _WIN32
# define kLockTypeDefault      kLockTypeAdvisory
# define kLockTypeDefaultEnum  (FileLock::LOCKTYPE_ADVISORY)
#else
# define kLockTypeDefault      kLockTypeLinkBased
# define kLockTypeDefaultEnum  (FileLock::LOCKTYPE_LINKBASED)
#endif 

const char * const kLocksConfFile    = "file-locks";
const double kDefaultRefreshRate     = 20.0;
const double kDefaultTimeoutInterval = 30.0;

std::string lockTypeToString(FileLock::LockType type)
{
   switch (type)
   {
   case FileLock::LOCKTYPE_ADVISORY:  return kLockTypeAdvisory;
   case FileLock::LOCKTYPE_LINKBASED: return kLockTypeLinkBased;
   }
   
   // not reached
   return std::string();
}

FileLock::LockType stringToLockType(const std::string& lockType,
                                    FileLock::LockType defaultLockType)
{
   using namespace boost::algorithm;
   
   if (boost::iequals(lockType, kLockTypeAdvisory))
      return FileLock::LOCKTYPE_ADVISORY;
   else if (boost::iequals(lockType, kLockTypeLinkBased))
      return FileLock::LOCKTYPE_LINKBASED;
   
   LOG_WARNING_MESSAGE("unrecognized lock type '" + lockType + "'");
   return defaultLockType;
}

double getFieldPositive(const Settings& settings,
                        const std::string& name,
                        double defaultValue)
{
   double value = settings.getDouble(name, defaultValue);
   if (value < 0)
   {
      LOG_WARNING_MESSAGE("invalid field '" + name + "': must be positive");
      return defaultValue;
   }
   
   return value;
}

bool s_isInitialized = false;

} // end anonymous namespace

bool FileLock::verifyInitialized()
{
   if (!s_isInitialized)
   {
      static bool s_warned = false;
      if (s_warned)
         return s_isInitialized;
      
      s_warned = true;
      LOG_WARNING_MESSAGE(
               "FileLock classes not yet initialized; please call "
               "'FileLock::initialize()' and 'FileLock::cleanUp()' as appropriate");
   }
   
   return s_isInitialized;
}

void FileLock::initialize(FileLock::LockType fallbackLockType)
{
   // read settings
   FilePath locksConfPath = core::system::xdg::systemConfigFile(kLocksConfFile);

   Settings settings;
   if (locksConfPath.exists())
   {
      Error error = settings.initialize(locksConfPath);
      if (error)
         LOG_ERROR(error);
   }
   
#ifdef _WIN32
   // TODO: link-based locks are not yet implemented on Windows
   FileLock::s_defaultType = LOCKTYPE_ADVISORY;
#else
   // default lock type
   std::string lockTypePref = settings.get("lock-type");
   FileLock::s_defaultType = lockTypePref.empty()
         ? fallbackLockType
         : stringToLockType(lockTypePref, fallbackLockType);
#endif

   // timeout interval
   double timeoutInterval = getFieldPositive(settings, "timeout-interval", kDefaultTimeoutInterval);
   FileLock::s_timeoutInterval = boost::posix_time::seconds(static_cast<long>(timeoutInterval));
   
   // refresh rate
   double refreshRate = getFieldPositive(settings, "refresh-rate", kDefaultRefreshRate);
   FileLock::s_refreshRate = boost::posix_time::seconds(static_cast<long>(refreshRate));
   
   // logging
   bool loggingEnabled = settings.getBool("enable-logging", false);
   FileLock::s_loggingEnabled = loggingEnabled;
   
   // logfile
   std::string logFile = settings.get("log-file");
   FileLock::s_logFile = FilePath(logFile);
   
   std::stringstream ss;
   ss << "(PID " << ::getpid() << "): Initialized file locks ("
      << "lock-type=" << lockTypeToString(FileLock::s_defaultType) << ", "
      << "timeout-interval=" << FileLock::s_timeoutInterval.total_seconds() << "s, "
      << "refresh-rate=" << FileLock::s_refreshRate.total_seconds() << "s, "
      << "log-file=" << logFile << ")"
      << std::endl;
   FileLock::log(ss.str());

   std::string distributedLockingOption = core::system::getenv(kRStudioDistributedLockingEnabled);
   bool distributedLockingEnabled = (distributedLockingOption == "1");

   FileLock::s_isLoadBalanced =
         !core::system::getenv(kRStudioSessionRoute).empty() ||
         distributedLockingEnabled;

   s_isInitialized = true;
}

void FileLock::initialize()
{
   initialize(kLockTypeDefaultEnum);
}

void FileLock::log(const std::string& message)
{
   if (s_logFile.isEmpty() || !isLoggingEnabled())
   {
      // if we were constructed without a path, or file lock logging was not explicitly enabled
      // (legacy option), then debug log to the in-proc logger
      LOG_DEBUG_MESSAGE_NAMED(kFileLockingLogSection, message);
   }
   else
   {
      // avoid logging too many errors (e.g. if the user has specified
      // a file that does not exist)
      static int counter = 0;
      static int max = 5;
      
      if (counter < max)
      {
         // append to logfile
         Error error = core::writeStringToFile(
                  s_logFile,
                  message,
                  string_utils::LineEndingPosix,
                  false);

         if (error)
         {
            LOG_ERROR(error);
            ++counter;
         }
      }
      else if (counter == max)
      {
         LOG_ERROR_MESSAGE("failed to write lockfile logs");
         ++counter;
      }
      else
      {
         // do nothing
      }
   }
}

// definitions for static members
// NOTE: these will be overridden when FileLock::initialize() is called
FileLock::LockType FileLock::s_defaultType(FileLock::LOCKTYPE_LINKBASED);
boost::posix_time::seconds FileLock::s_timeoutInterval(static_cast<long>(kDefaultTimeoutInterval));
boost::posix_time::seconds FileLock::s_refreshRate(static_cast<long>(kDefaultRefreshRate));
bool FileLock::s_loggingEnabled(false);
bool FileLock::s_isLoadBalanced(false);
FilePath FileLock::s_logFile;

boost::shared_ptr<FileLock> FileLock::create(LockType type)
{
   verifyInitialized();
   
   switch (type)
   {
   case LOCKTYPE_ADVISORY:  return boost::shared_ptr<FileLock>(new AdvisoryFileLock());
   case LOCKTYPE_LINKBASED: return boost::shared_ptr<FileLock>(new LinkBasedFileLock());
   }
   
   // shouldn't be reached
   return boost::shared_ptr<FileLock>(new AdvisoryFileLock());
}

boost::shared_ptr<FileLock> FileLock::createDefault()
{
   verifyInitialized();
   return FileLock::create(s_defaultType);
}

void FileLock::refresh()
{
   verifyInitialized();
   AdvisoryFileLock::refresh();
   LinkBasedFileLock::refresh();
}

void FileLock::cleanUp()
{
   verifyInitialized();
   AdvisoryFileLock::cleanUp();
   LinkBasedFileLock::cleanUp();
}

namespace {

void schedulePeriodicExecution(
      const boost::system::error_code& ec,
      boost::asio::deadline_timer& timer,
      boost::posix_time::seconds interval,
      boost::function<void()> callback)
{
   try
   {
      // bail on boost errors (these are very unexpected)
      if (ec)
      {
         if (!core::isShutdownError(ec))
            LOG_ERROR(core::Error(ec, ERROR_LOCATION));
         return;
      }
      
      // execute callback
      callback();

      // reschedule
      boost::system::error_code errc;
      timer.expires_at(timer.expires_at() + interval, errc);
      if (errc)
      {
         LOG_ERROR(Error(errc, ERROR_LOCATION));
         return;
      }
      
      timer.async_wait(boost::bind(
                          schedulePeriodicExecution,
                          boost::asio::placeholders::error,
                          boost::ref(timer),
                          interval,
                          callback));
   }
   catch (...)
   {
      // swallow errors
   }
}

} // end anonymous namespace

void FileLock::refreshPeriodically(boost::asio::io_service& service,
                                   boost::posix_time::seconds interval)
{
   // protect against re-entrancy
   static bool s_isRefreshing = false;
   if (s_isRefreshing)
      return;
   s_isRefreshing = true;
   
   verifyInitialized();
   
   static boost::asio::deadline_timer timer(service, interval);
   timer.async_wait(boost::bind(
                       schedulePeriodicExecution,
                       boost::asio::placeholders::error,
                       boost::ref(timer),
                       interval,
                       FileLock::refresh));
}

} // end namespace core
} // end namespace rstudio

/*
 * FileLock.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
#include <core/Error.hpp>
#include <core/Log.hpp>
#include <core/FileSerializer.hpp>
#include <core/http/SocketUtils.hpp>

#include <boost/algorithm/string.hpp>

namespace rstudio {
namespace core {

namespace file_lock {
void initialize()
{
   FileLock::initialize();
}
} // end namespace file_lock

namespace {

const char * const kLockTypeAdvisory  = "advisory";
const char * const kLockTypeLinkBased = "linkbased";

// use advisory locks on Windows by default; link-based elsewhere
#ifdef _WIN32
# define kLockTypeDefault kLockTypeAdvisory
#else
# define kLockTypeDefault kLockTypeLinkBased
#endif 

const char * const kLocksConfPath    = "/etc/rstudio/file-locks";
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

FileLock::LockType stringToLockType(const std::string& lockType)
{
   using namespace boost::algorithm;
   
   if (boost::iequals(lockType, kLockTypeAdvisory))
      return FileLock::LOCKTYPE_ADVISORY;
   else if (boost::iequals(lockType, kLockTypeLinkBased))
      return FileLock::LOCKTYPE_LINKBASED;
   
   LOG_WARNING_MESSAGE("unrecognized lock type '" + lockType + "'");
   return FileLock::LOCKTYPE_LINKBASED;
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

void FileLock::initialize(FilePath locksConfPath)
{
   if (locksConfPath.empty())
      locksConfPath = FilePath(kLocksConfPath);
   
   Settings settings;
   if (locksConfPath.exists())
   {
      Error error = settings.initialize(locksConfPath);
      if (error)
         LOG_ERROR(error);
   }
   
   FileLock::initialize(settings);
}

void FileLock::initialize(const Settings& settings)
{
   // default lock type
   FileLock::s_defaultType = stringToLockType(settings.get("lock-type", kLockTypeDefault));

   // timeout interval
   double timeoutInterval = getFieldPositive(settings, "timeout-interval", kDefaultTimeoutInterval);
   FileLock::s_timeoutInterval = boost::posix_time::seconds(timeoutInterval);
   
   // refresh rate
   double refreshRate = getFieldPositive(settings, "refresh-rate", kDefaultRefreshRate);
   FileLock::s_refreshRate = boost::posix_time::seconds(refreshRate);
   
   // logging
   bool loggingEnabled = settings.getBool("enable-logging", false);
   FileLock::s_loggingEnabled = loggingEnabled;
   
   // logfile
   std::string logFile = settings.get("log-file");
   FileLock::s_logFile = FilePath(logFile);
   
   // report when logging is enabled
   if (loggingEnabled)
   {
      std::stringstream ss;
      ss << "(PID " << ::getpid() << "): Initialized file locks ("
         << "lock-type=" << lockTypeToString(FileLock::s_defaultType) << ", "
         << "timeout-interval=" << FileLock::s_timeoutInterval.total_seconds() << "s, "
         << "refresh-rate=" << FileLock::s_refreshRate.total_seconds() << "s, "
         << "log-file=" << logFile << ")"
         << std::endl;
      FileLock::log(ss.str());
   }
   
   s_isInitialized = true;
}

void FileLock::log(const std::string& message)
{
   if (!isLoggingEnabled())
      return;
   
   if (s_logFile.empty())
   {
      // if we were constructed without a path, log to stderr
      std::cerr << message;
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

// default values for static members
FileLock::LockType FileLock::s_defaultType(FileLock::LOCKTYPE_LINKBASED);
boost::posix_time::seconds FileLock::s_timeoutInterval(kDefaultTimeoutInterval);
boost::posix_time::seconds FileLock::s_refreshRate(kDefaultRefreshRate);
bool FileLock::s_loggingEnabled(false);
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

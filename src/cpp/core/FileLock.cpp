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

#define RSTUDIO_ENABLE_DEBUG_MACROS
#include <core/Macros.hpp>

#include <core/Settings.hpp>
#include <core/Error.hpp>
#include <core/Log.hpp>
#include <core/FileSerializer.hpp>

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

const char * const kLocksConfPath = "/etc/rstudio/locks.conf";
#define kDefaultRefreshRate     20.0
#define kDefaultTimeoutInterval 30.0

std::string lockTypeToString(FileLock::LockType type)
{
   switch (type)
   {
   case FileLock::LOCKTYPE_ADVISORY:  return "advisory";
   case FileLock::LOCKTYPE_LINKBASED: return "linkbased";
   }
   
   // not reached
   return std::string();
}

FileLock::LockType stringToLockType(const std::string& lockType)
{
   using namespace boost::algorithm;
   
   if (boost::iequals(lockType, "advisory"))
      return FileLock::LOCKTYPE_ADVISORY;
   else if (boost::iequals(lockType, "linkbased"))
      return FileLock::LOCKTYPE_LINKBASED;
   
   LOG_WARNING_MESSAGE("unrecognized lock type '" + lockType + "'");
   return FileLock::LOCKTYPE_ADVISORY;
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

} // end anonymous namespace

bool s_isInitialized = false;

void FileLock::ensureInitialized()
{
   if (s_isInitialized)
      return;
   
   FileLock::initialize();
}

void FileLock::initialize(FilePath locksConfPath)
{
   s_isInitialized = true;
   
   if (locksConfPath.empty())
      locksConfPath = FilePath(kLocksConfPath);
   
   if (!locksConfPath.exists())
      return;
   
   Settings settings;
   Error error = settings.initialize(locksConfPath);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }
   
   FileLock::initialize(settings);
}

void FileLock::initialize(const Settings& settings)
{
   s_isInitialized = true;
   
   // default lock type
   FileLock::s_defaultType = stringToLockType(settings.get("lock-type", "advisory"));
   
   // timeout interval
   double timeoutInterval = getFieldPositive(settings, "timeout-interval", kDefaultTimeoutInterval);
   FileLock::s_timeoutInterval = boost::posix_time::seconds(timeoutInterval);
   
   // refresh rate
   double refreshRate = getFieldPositive(settings, "refresh-rate", kDefaultRefreshRate);
   FileLock::s_refreshRate = boost::posix_time::seconds(refreshRate);
   
   DEBUG_BLOCK("lock initialization")
   {
      std::cerr << "Type: "    << lockTypeToString(FileLock::s_defaultType) << std::endl;
      std::cerr << "Timeout: " << FileLock::s_timeoutInterval.total_seconds() << std::endl;
      std::cerr << "Refresh: " << FileLock::s_refreshRate.total_seconds() << std::endl;
   }
}

// default values for static members
FileLock::LockType FileLock::s_defaultType(FileLock::LOCKTYPE_ADVISORY);
boost::posix_time::seconds FileLock::s_timeoutInterval(kDefaultTimeoutInterval);
boost::posix_time::seconds FileLock::s_refreshRate(kDefaultRefreshRate);

boost::shared_ptr<FileLock> FileLock::create(LockType type)
{
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
   return FileLock::create(s_defaultType);
}

void FileLock::refresh()
{
   AdvisoryFileLock::refresh();
   LinkBasedFileLock::refresh();
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
      // log errors (and attempt to continue if possible)
      if (ec)
         LOG_ERROR(core::Error(ec, ERROR_LOCATION));
      
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

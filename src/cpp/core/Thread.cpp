/*
 * Thread.cpp
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

#include <core/Backtrace.hpp>

#include <shared_core/Error.hpp>

#include <core/Macros.hpp>
#include <core/StringUtils.hpp>
#include <core/Thread.hpp>

#include <core/system/System.hpp>

namespace rstudio {
namespace core {
namespace thread {

namespace {

// main thread id
// we initialize this statically but the value can be overridden
// via initializeMainThreadId() if necessary
boost::thread::id s_mainThreadId = boost::this_thread::get_id();

} // end anonymous namespace

void safeLaunchThread(boost::function<void()> threadMain,
                      boost::thread* pThread)
{
   try
   {
      // block all signals for launch of background thread (will cause it
      // to never receive signals)
      core::system::SignalBlocker signalBlocker;
      Error error = signalBlocker.blockAll();
      if (error)
         LOG_ERROR(error);

      boost::thread t(threadMain);

      if (pThread)
         *pThread = MOVE_THREAD(t);
   }
   catch(const boost::thread_resource_error& e)
   {
      LOG_ERROR(Error(boost::thread_error::ec_from_exception(e),
                      ERROR_LOCATION));
   }
}

void initializeMainThreadId(boost::thread::id id)
{
   s_mainThreadId = id;
}

bool isMainThread()
{
   return s_mainThreadId == boost::this_thread::get_id();
}

bool assertMainThread(
      const std::string& reason,
      const std::string& functionName,
      const core::ErrorLocation& errorLocation)
{
   if (isMainThread())
      return true;
   
   // log an error
   std::string errorMessage;
   if (reason.empty())
   {
      errorMessage = core::string_utils::sprintf(
               "'%s' was called from non-main thread",
               functionName.c_str());
   }
   else
   {
      errorMessage = core::string_utils::sprintf(
               "'%s' was called from non-main thread (%s)",
               functionName.c_str(),
               reason.c_str());
   }

   core::log::logErrorMessage(errorMessage, errorLocation);

#ifndef RSTUDIO_PACKAGE_BUILD
   // print a backtrace in developer builds
   core::backtrace::printBacktrace();
#endif

   return false;
}

} // namespace thread
} // namespace core
} // namespace rstudio

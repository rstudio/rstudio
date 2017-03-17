/*
 * Thread.cpp
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

#include <core/Thread.hpp>
#include <core/Macros.hpp>

#include <core/system/System.hpp>

namespace rstudio {
namespace core {
namespace thread {

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

} // namespace core
} // namespace thread
} // namespace rstudio

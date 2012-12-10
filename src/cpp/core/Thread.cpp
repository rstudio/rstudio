/*
 * Thread.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <core/Thread.hpp>

#include <core/system/System.hpp>

namespace core {
namespace thread {

Error safeLaunchThread(boost::function<void()> threadMain,
                       boost::thread* pThread)
{
   try
   {
      // block all signals for launch of background thread (will cause it
      // to never receive signals)
      core::system::SignalBlocker signalBlocker;
      Error error = signalBlocker.blockAll();
      if (error)
         return error;

      boost::thread t(threadMain);

      if (pThread)
         *pThread = t.move();

      return Success();
   }
   catch(const boost::thread_resource_error& e)
   {
      return Error(boost::thread_error::ec_from_exception(e),
                   ERROR_LOCATION);
   }
}

} // namespace core
} // namespace thread


// dummy function to satisfy boost linking requirement (fixed in 1.45)
// see: https://svn.boost.org/trac/boost/ticket/4258
#if defined(__GNUC__) && defined(_WIN64)

namespace boost {

void tss_cleanup_implemented()
{
}

} // namespace boost

#endif

/*
 * WaitUtils.cpp
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

#include <core/WaitUtils.hpp>
#include <boost/system/error_code.hpp>

#include <core/BoostThread.hpp>

namespace rstudio {
namespace core {

Error waitWithTimeout(const boost::function<WaitResult()>& waitFunction,
                      int initialWaitMs,
                      int incrementWaitMs,
                      int maxWaitSec)
{
   // wait for the session to be available
   using boost::system_time;
   using namespace boost::posix_time;
   using namespace boost::system;

   // wait 30ms (this value is somewhat arbitrary -- ideally it would
   // be as close as possible to the expected total launch time of
   // rsession (observed to be ~35ms on a 3ghz MacPro w/ SSD disk)
   boost::this_thread::sleep(milliseconds(initialWaitMs));

   // try the connection again and if we don't get it try to reconnect
   // every 10ms until a timeout occurs -- we use a low granularity
   // here because expect our initial guess of 30ms to be pretty close
   // to the total launch time
   boost::system_time timeoutTime = boost::get_system_time() + seconds(maxWaitSec);
   while(boost::get_system_time() < timeoutTime)
   {
      WaitResult result = waitFunction();
      if (result.type == WaitSuccess)
         return Success();

      if (result.type == WaitContinue)
      {
         // try again after waiting a short while
         boost::this_thread::sleep(milliseconds(incrementWaitMs));
         continue;
      }
      else /* if (result.type == WaitError) */
      {
         // unrecoverable error (return connectError below)
         return result.error;
      }
   }

   return systemError(boost::system::errc::timed_out, ERROR_LOCATION);
}

} // namespace core
} // namespace rstudio

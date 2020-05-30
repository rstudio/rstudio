/*
 * ExponentialBackoff.cpp
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

#include <boost/make_shared.hpp>

#include <core/ExponentialBackoff.hpp>

namespace rstudio {
namespace core {

ExponentialBackoff::ExponentialBackoff(boost::asio::io_service& ioService,
                                       const boost::posix_time::time_duration& initialWait,
                                       const boost::posix_time::time_duration& maxWait,
                                       const boost::function<void(ExponentialBackoffPtr)>& action) :
   ioService_(ioService),
   initialWait_(initialWait),
   maxWait_(maxWait),
   maxNumRetries_(0),
   totalNumTries_(0),
   action_(action),
   lastWait_(boost::posix_time::not_a_date_time)
{
}

ExponentialBackoff::ExponentialBackoff(boost::asio::io_service& ioService,
                                       const boost::posix_time::time_duration& initialWait,
                                       const boost::posix_time::time_duration& maxWait,
                                       unsigned int maxNumRetries,
                                       const boost::function<void(ExponentialBackoffPtr)>& action) :
   ioService_(ioService),
   initialWait_(initialWait),
   maxWait_(maxWait),
   maxNumRetries_(maxNumRetries),
   totalNumTries_(0),
   action_(action),
   lastWait_(boost::posix_time::not_a_date_time)
{
}

bool ExponentialBackoff::next()
{
   ExponentialBackoffPtr instance = shared_from_this();

   RECURSIVE_LOCK_MUTEX(mutex_)
   {
      // we should not continue trying if we've hit the max retries
      if (maxNumRetries_ != 0 &&
          totalNumTries_ >= maxNumRetries_ + 1)
      {
         // reset action so that all captured references are freed
         action_ = boost::function<void(ExponentialBackoffPtr)>();
         return false;
      }

      if (totalNumTries_ == 0)
      {
         // initial invocation of the action
         ++totalNumTries_;
         action_(instance);
         return true;
      }

      boost::posix_time::time_duration nextWait;
      if (lastWait_.is_not_a_date_time())
      {
         // we have not waited yet, so set the first wait to the initial wait value
         nextWait = initialWait_;
      }
      else
      {
         // calculate the next amount of time to wait by doubling the previous time
         nextWait = lastWait_ * 2;
      }

      // construct timeout with overflow protection
      // it is possible doubling the last amount of time caused an overflow
      // if that is the case, we will just use the maximum wait time
      boost::posix_time::time_duration timeout =
            (nextWait > maxWait_ || nextWait.is_negative()) ?
               maxWait_ :
               nextWait;

      lastWait_ = timeout;

      if (maxNumRetries_ == 0 && timeout == maxWait_)
      {
         // maxNumTries is 0 and we've reached the max wait
         // this indicates that the we should only try the operation one more time
         maxNumRetries_ = totalNumTries_ + 2;
      }

      boost::shared_ptr<boost::asio::deadline_timer> timer =
            boost::make_shared<boost::asio::deadline_timer>(ioService_, timeout);

      timer->async_wait([=](const boost::system::error_code& error) mutable
      {
         // timer expired - explicitly free it
         // capturing the timer pointer must be done here in order to keep it alive
         // if it were not captured, it would go out of scope and the callback would not
         // fire properly
         timer.reset();

         // wait has finished - update state and perform the action
         RECURSIVE_LOCK_MUTEX(instance->mutex_)
         {
            ++instance->totalNumTries_;
         }
         END_LOCK_MUTEX

         instance->action_(instance);
      });

      return true;
   }
   END_LOCK_MUTEX

   return false;
}

} // namespace core 
} // namespace rstudio




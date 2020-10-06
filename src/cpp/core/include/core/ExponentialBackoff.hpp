/*
 * ExponentialBackoff.hpp
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

#ifndef CORE_EXPONENTIAL_BACKOFF_HPP
#define CORE_EXPONENTIAL_BACKOFF_HPP

#include <boost/asio.hpp>
#include <boost/enable_shared_from_this.hpp>
#include <boost/date_time.hpp>
#include <boost/function.hpp>

#include <core/Thread.hpp>

namespace rstudio {
namespace core {

class ExponentialBackoff;
typedef boost::shared_ptr<ExponentialBackoff> ExponentialBackoffPtr;

class ExponentialBackoff : public boost::enable_shared_from_this<ExponentialBackoff>
{
public:
   ExponentialBackoff(boost::asio::io_service& ioService,
                      const boost::posix_time::time_duration& initialWait,
                      const boost::posix_time::time_duration& maxWait,
                      const boost::function<void(ExponentialBackoffPtr)>& action);

   ExponentialBackoff(boost::asio::io_service& ioService,
                      const boost::posix_time::time_duration& initialWait,
                      const boost::posix_time::time_duration& maxWait,
                      unsigned int maxNumRetries,
                      const boost::function<void(ExponentialBackoffPtr)>& action);

   // keep trying the action
   // invoke this to indicate that the action should be retried
   //
   // the action is not retried immediately, but is invoked after waiting
   // an appropriate amount of time based on an exponential backoff algorithm
   //
   // returns false if the max number of retries has been reached
   bool next();

private:
   boost::asio::io_service& ioService_;
   boost::posix_time::time_duration initialWait_;
   boost::posix_time::time_duration maxWait_;
   unsigned int maxNumRetries_;
   unsigned int totalNumTries_;
   boost::function<void(ExponentialBackoffPtr)> action_;

   boost::posix_time::time_duration lastWait_;
   boost::recursive_mutex mutex_;
};

} // namespace core 
} // namespace rstudio


#endif // CORE_EXPONENTIAL_BACKOFF_HPP


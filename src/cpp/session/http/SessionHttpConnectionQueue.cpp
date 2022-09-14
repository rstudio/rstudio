/*
 * SessionHttpConnectionQueue.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <session/SessionHttpConnectionQueue.hpp>

#include <core/Log.hpp>
#include <shared_core/Error.hpp>
#include <core/Thread.hpp>

#include <core/http/Request.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {

void HttpConnectionQueue::enqueConnection(
                              boost::shared_ptr<HttpConnection> ptrConnection)
{
   LOCK_MUTEX(*pMutex_)
   {
      // Add the new connection to the end of the queue
      queue_.push_back(ptrConnection);
   }
   END_LOCK_MUTEX

   pWaitCondition_->notify_all();
}


boost::shared_ptr<HttpConnection> HttpConnectionQueue::doDequeConnection()
{
   LOCK_MUTEX(*pMutex_)
   {
      if (!queue_.empty())
      {
         // remove the first connection
         boost::shared_ptr<HttpConnection> next = queue_.front();
         queue_.erase(queue_.begin());

         // note last connection time
         lastConnectionTime_ =
                     boost::posix_time::second_clock::universal_time();

         // return it
         return next;
      }
      else
      {
         return boost::shared_ptr<HttpConnection>();
      }
   }
   END_LOCK_MUTEX

   // keep compiler happy
   return boost::shared_ptr<HttpConnection>();
}

boost::shared_ptr<HttpConnection> HttpConnectionQueue::dequeConnection()
{
   // perform the deque
   boost::shared_ptr<HttpConnection> connection = doDequeConnection();

   // return the connection
   return connection;
}

boost::shared_ptr<HttpConnection> HttpConnectionQueue::dequeConnection(
            const boost::posix_time::time_duration& waitDuration)
{
   // first see if we already have one
   boost::shared_ptr<HttpConnection> ptrConnection = dequeConnection();
   if (ptrConnection)
      return ptrConnection;

   // now wait the specified interval for one to materialize
   if (waitForConnection(waitDuration))
      return dequeConnection();
   else
      return boost::shared_ptr<HttpConnection>();
}

std::string HttpConnectionQueue::peekNextConnectionUri()
{
   LOCK_MUTEX(*pMutex_)
   {
      if (!queue_.empty())
         return queue_.front()->request().uri();
      else
         return std::string();
   }
   END_LOCK_MUTEX

   // keep compiler happy
   return std::string();
}

bool HttpConnectionQueue::waitForConnection(
                     const boost::posix_time::time_duration& waitDuration)
{
   using namespace boost;
   try
   {
      unique_lock<mutex> lock(*pMutex_);
      system_time timeoutTime = get_system_time() + waitDuration;
      return pWaitCondition_->timed_wait(lock, timeoutTime);
   }
   catch(const thread_resource_error& e)
   {
      Error waitError(boost::thread_error::ec_from_exception(e), ERROR_LOCATION);
      LOG_ERROR(waitError);
      return false;
   }
}

boost::posix_time::ptime HttpConnectionQueue::lastConnectionTime()
{
    LOCK_MUTEX(*pMutex_)
    {
      return lastConnectionTime_;
    }
    END_LOCK_MUTEX

    // keep compiler happy
    return boost::posix_time::ptime();
}

boost::shared_ptr<HttpConnection> HttpConnectionQueue::dequeMatchingConnection(
        const HttpConnectionMatcher matcher,
        const std::chrono::steady_clock::time_point now)
{
   LOCK_MUTEX(*pMutex_)
      {
         size_t sz = queue_.size();
         boost::shared_ptr<HttpConnection> next;
         for (size_t i = 0; i < sz; i++)
         {
            next = queue_[i];
            if (matcher(next, now))
            {
               queue_.erase(queue_.begin() + i);
               return next;
            }
         }
      }
   END_LOCK_MUTEX
   return boost::shared_ptr<HttpConnection>();
}

void HttpConnectionQueue::convertConnections(
        const HttpConnectionConverter converter,
        const std::chrono::steady_clock::time_point now)
{
   LOCK_MUTEX(*pMutex_)
      {
         size_t sz = queue_.size();
         boost::shared_ptr<HttpConnection> next;
         for (size_t i = 0; i < sz; i++)
         {
            next = queue_[i];
            boost::shared_ptr<HttpConnection> convertedConn = converter(next, now);
            if (convertedConn)
            {
               queue_[i] = convertedConn;
            }
         }
      }
   END_LOCK_MUTEX
}

} // namespace session
} // namespace rstudio
